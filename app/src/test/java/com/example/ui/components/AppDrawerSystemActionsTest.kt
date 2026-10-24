package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppDrawerSystemActionsTest {
    @Test
    fun systemAppsOnlyExposeNativeAppInfo() {
        assertEquals(
            listOf(AppDrawerSystemAction.APP_INFO),
            appDrawerSystemActions(isSystemApp = true)
        )
    }

    @Test
    fun UserAppsExposeInfoAndSystemUninstallInOrder() {
        assertEquals(
            listOf(AppDrawerSystemAction.APP_INFO, AppDrawerSystemAction.UNINSTALL),
            appDrawerSystemActions(isSystemApp = false)
        )
    }

    @Test
    fun longPressMenuDoesNotDuplicateTapToLaunch() {
        assertEquals(2, AppDrawerSystemAction.entries.size)
        assertFalse(AppDrawerSystemAction.entries.any { it.name.contains("LAUNCH") || it.name.contains("OPEN") })
    }
}
