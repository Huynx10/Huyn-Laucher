package com.example.ui.components

import android.app.SearchManager
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemSearchTest {
    @Test
    fun noExportedFinderEntryFallsBackWithoutOpeningAnotherSearchProvider() {
        val context = RuntimeEnvironment.getApplication()

        assertFalse(launchSamsungFinder(context))
        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun exportedFinderGlobalSearchOpensExactlyOnceWithQuery() {
        val context = RuntimeEnvironment.getApplication()
        val intent = finderSearchIntent("Máy ảnh")
        shadowOf(context.packageManager).setResolveInfosForIntent(intent, listOf(finderResolveInfo(exported = true)))

        assertTrue(launchSamsungFinder(context, "Máy ảnh"))
        val started = shadowOf(context).nextStartedActivity
        assertEquals(SearchManager.INTENT_ACTION_GLOBAL_SEARCH, started.action)
        assertEquals(SAMSUNG_FINDER_PACKAGE, started.`package`)
        assertEquals("Máy ảnh", started.getStringExtra(SearchManager.QUERY))
        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun privateFinderSearchEntryIsNeverStarted() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setResolveInfosForIntent(
            finderSearchIntent("query"), listOf(finderResolveInfo(exported = false))
        )

        assertFalse(launchSamsungFinder(context, "query"))
        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun deniedStartReturnsToLocalSearchWithoutASecondLaunch() {
        val application = RuntimeEnvironment.getApplication()
        val intent = finderSearchIntent("query")
        shadowOf(application.packageManager).setResolveInfosForIntent(
            intent, listOf(finderResolveInfo(exported = true))
        )
        var starts = 0
        val deniedContext = object : ContextWrapper(application) {
            override fun startActivity(intent: Intent) {
                starts++
                throw SecurityException("Finder denied launch")
            }
        }

        assertFalse(launchSamsungFinder(deniedContext, "query"))
        assertEquals(1, starts)
    }

    @Test
    fun exportedLauncherEntryOpensOnlyWhenQueryIsEmpty() {
        val application = RuntimeEnvironment.getApplication()
        val component = ComponentName(SAMSUNG_FINDER_PACKAGE, "$SAMSUNG_FINDER_PACKAGE.FrontDoor")
        val packageManager = shadowOf(application.packageManager)
        packageManager.addPackage(SAMSUNG_FINDER_PACKAGE)
        packageManager.addActivityIfNotPresent(component).exported = true
        packageManager.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        })

        assertFalse(launchSamsungFinder(application, "typed query"))
        assertNull(shadowOf(application).nextStartedActivity)
        assertTrue(launchSamsungFinder(application))
        val started = shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_MAIN, started.action)
        assertEquals(component, started.component)
        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun finderSearchTargetsSamsungPackageInsteadOfWebProvider() {
        val intent = finderSearchIntent("Máy ảnh")

        assertEquals(SearchManager.INTENT_ACTION_GLOBAL_SEARCH, intent.action)
        assertEquals(SAMSUNG_FINDER_PACKAGE, intent.`package`)
        assertNull(intent.component)
        assertEquals("Máy ảnh", intent.getStringExtra(SearchManager.QUERY))
        assertFalse(intent.action == Intent.ACTION_WEB_SEARCH)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun emptySearchOpensProviderWithoutAQuery() {
        val intent = finderSearchIntent("")

        assertFalse(intent.hasExtra(SearchManager.QUERY))
    }

    private fun finderResolveInfo(exported: Boolean) =
        ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = SAMSUNG_FINDER_PACKAGE
                name = "$SAMSUNG_FINDER_PACKAGE.SearchActivity"
                this.exported = exported
            }
        }
}
