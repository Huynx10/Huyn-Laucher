package com.example.ui.components

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.provider.Settings
import org.robolectric.Robolectric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class HomeChooserCompatibilityTest {
    @Test
    @Config(sdk = [24, 26, 28])
    fun androidSevenToNineOpensSystemHomeSettingsWithoutRequestingRole() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val roleRequests = mutableListOf<Intent>()

        launchHomeChooser(activity) { roleRequests.add(it) }

        val shadow = shadowOf(activity)
        val launched = shadow.nextStartedActivity
        assertNotNull("HOME settings should be opened on pre-Q Android", launched)
        assertEquals(Settings.ACTION_HOME_SETTINGS, launched.action)
        assertTrue("Pre-Q must never launch RoleManager requests", roleRequests.isEmpty())
        assertNull("Exactly one settings activity should be started", shadow.nextStartedActivity)
    }

    @Test
    @Config(sdk = [29, 34])
    @Suppress("DEPRECATION") // Robolectric's addAvailableRole is retained for test setup.
    fun androidTenPlusRequestsHomeRoleWithoutOpeningSettingsWhenAvailable() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val roleManager = requireNotNull(activity.getSystemService(RoleManager::class.java))
        shadowOf(roleManager).addAvailableRole(RoleManager.ROLE_HOME)
        assertTrue("HOME role must be available for this test", roleManager.isRoleAvailable(RoleManager.ROLE_HOME))
        assertFalse("Launcher must not already hold HOME role", roleManager.isRoleHeld(RoleManager.ROLE_HOME))

        val roleRequests = mutableListOf<Intent>()
        launchHomeChooser(activity) { roleRequests.add(it) }

        assertEquals("Exactly one HOME role request should be launched", 1, roleRequests.size)
        val expectedRequest = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        assertEquals("Must request the HOME role", expectedRequest.toUri(0), roleRequests.single().toUri(0))
        assertNull(
            "Available HOME role must not fall back to ACTION_HOME_SETTINGS",
            shadowOf(activity).nextStartedActivity
        )
    }

}
