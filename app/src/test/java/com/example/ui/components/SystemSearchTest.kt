package com.example.ui.components

import android.app.SearchManager
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemSearchTest {
    @Test
    fun oneUiHomeSearchUsesOnlyTheOptionalSamsungEntry() {
        val intent = oneUiHomeSearchIntent("")
        assertEquals(Intent.ACTION_SEARCH, intent.action)
        assertEquals(ONE_UI_HOME_PACKAGE, intent.component?.packageName)
        assertEquals("com.sec.android.app.launcher.search.SearchActivity", intent.component?.className)
        assertFalse(intent.hasExtra(SearchManager.QUERY))
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
}
