package com.example.ui.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LauncherBackgroundGuardTest {
    @Test fun failingScanRetainsLastKnownCatalogAndReportsError() = runBlocking {
        var apps = listOf("Existing app")
        var errorCount = 0
        val result = runLauncherBackgroundRead(
            onError = { error ->
                assertEquals("Package manager unavailable", error.message)
                errorCount++
            }
        ) { throw IllegalStateException("Package manager unavailable") }
        result?.let { apps = it }
        assertEquals(listOf("Existing app"), apps)
        assertEquals(1, errorCount)
    }

    @Test fun oneFailedStageDoesNotPreventFollowingStages() = runBlocking {
        val completed = mutableListOf<String>()
        val failures = mutableListOf<String>()
        runLauncherBackgroundRead(onError = { failures.add(it.message.orEmpty()) }) {
            completed.add("apps")
            throw IllegalArgumentException("apps failed")
        }
        val widgets = runLauncherBackgroundRead(onError = { failures.add(it.message.orEmpty()) }) {
            completed.add("widgets")
            listOf("Real widget")
        }
        val telemetry = runLauncherBackgroundRead(onError = { failures.add(it.message.orEmpty()) }) {
            completed.add("telemetry")
            42
        }
        assertEquals(listOf("apps", "widgets", "telemetry"), completed)
        assertEquals(listOf("apps failed"), failures)
        assertEquals(listOf("Real widget"), widgets)
        assertEquals(42, telemetry)
    }

    @Test fun cancellationIsNotSwallowedOrReportedAsFailure() = runBlocking {
        var reported = false
        try {
            runLauncherBackgroundRead(onError = { reported = true }) {
                throw CancellationException("superseded refresh")
            }
            fail("Cancellation must propagate")
        } catch (cancelled: CancellationException) {
            assertEquals("superseded refresh", cancelled.message)
        }
        assertTrue(!reported)
    }

    @Test fun successfulRefreshPublishesNewValueWithoutError() = runBlocking {
        var apps = listOf("Old app")
        val result = runLauncherBackgroundRead(
            onError = { fail("No error expected: ${it.message}") }
        ) { listOf("New app") }
        result?.let { apps = it }
        assertEquals(listOf("New app"), apps)
    }
}
