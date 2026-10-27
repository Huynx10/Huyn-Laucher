package com.example.ui.components

import android.app.SearchManager
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemSearchTest {
    @Test
    fun externalSearchUsesGlobalSearchInsteadOfWebSearch() {
        val intent = systemSearchIntent("Máy ảnh")

        assertEquals(SearchManager.INTENT_ACTION_GLOBAL_SEARCH, intent.action)
        assertEquals("Máy ảnh", intent.getStringExtra(SearchManager.QUERY))
        assertFalse(intent.action == Intent.ACTION_WEB_SEARCH)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun emptySearchOpensProviderWithoutAQuery() {
        val intent = systemSearchIntent("")

        assertFalse(intent.hasExtra(SearchManager.QUERY))
    }
}
