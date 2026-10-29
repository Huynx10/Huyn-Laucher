package com.example

import com.example.data.model.LauncherPage
import com.example.data.model.PageType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeEdgeDwellTest {
    private val pages = listOf(
        LauncherPage("all", "Apps", PageType.ALL_APPS),
        LauncherPage("home1", "Home 1"),
        LauncherPage("home2", "Home 2"),
        LauncherPage("home3", "Home 3"),
        LauncherPage("all_end", "Apps 2", PageType.ALL_APPS)
    )

    @Test fun hoverRequiresFullDwellAndThenMovesToAdjacentHomePage() = runTest {
        var destination = -1
        var held = true
        val job = launch {
            assertTrue(awaitHomeEdgeDwell(pages, 1, 1, { held }, { destination = it }))
        }
        runCurrent()
        advanceTimeBy(549)
        runCurrent()
        assertEquals(-1, destination)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, destination)
        job.join()
    }

    @Test fun liftOrBackBeforeTimeoutPreventsPageSwitch() = runTest {
        var destination = -1
        var held = true
        val job = launch {
            assertFalse(awaitHomeEdgeDwell(pages, 2, -1, { held }, { destination = it }))
        }
        runCurrent()
        advanceTimeBy(300)
        held = false
        advanceTimeBy(250)
        runCurrent()
        assertEquals(-1, destination)
        job.join()
    }

    @Test fun neverNavigatesIntoAllAppsOrOutsidePager() = runTest {
        var calls = 0
        assertFalse(awaitHomeEdgeDwell(pages, 1, -1, { true }, { calls++ }))
        assertFalse(awaitHomeEdgeDwell(pages, 3, 1, { true }, { calls++ }))
        assertFalse(awaitHomeEdgeDwell(pages, 1, 0, { true }, { calls++ }))
        assertFalse(awaitHomeEdgeDwell(pages, 1, 3, { true }, { calls++ }))
        assertEquals(0, calls)
    }
}
