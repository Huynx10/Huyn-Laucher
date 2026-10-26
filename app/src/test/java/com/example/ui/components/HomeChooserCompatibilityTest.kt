package com.example.ui.components

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
        val application = ApplicationProvider.getApplicationContext<Application>()
        val roleRequests = mutableListOf<Intent>()

        launchHomeChooser(application) { roleRequests.add(it) }

        val shadow = shadowOf(application)
        val launched = shadow.nextStartedActivity
        assertNotNull("HOME settings should be opened on pre-Q Android", launched)
        assertEquals(Settings.ACTION_HOME_SETTINGS, launched.action)
        assertTrue("Pre-Q must never launch RoleManager requests", roleRequests.isEmpty())
        assertNull("Exactly one settings activity should be started", shadow.nextStartedActivity)
    }
}
