package com.example.ui.components

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

// Finder is an OEM app, so only public exported activities may be opened.
// Its package is a capability probe, not an assumed One UI API contract.
internal const val SAMSUNG_FINDER_PACKAGE = "com.samsung.android.app.galaxyfinder"

internal fun finderSearchIntent(query: String): Intent =
    Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH).apply {
        setPackage(SAMSUNG_FINDER_PACKAGE)
        if (query.isNotBlank()) putExtra(SearchManager.QUERY, query)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

/** Try Finder's public global-search handler, then its public front-door activity. */
internal fun launchSamsungFinder(context: Context, query: String = ""): Boolean {
    try {
        context.startActivity(finderSearchIntent(query))
        return true
    } catch (_: ActivityNotFoundException) {
        // Finder may expose a launcher activity instead of GLOBAL_SEARCH.
    } catch (_: SecurityException) {
        // No access to an exported search activity; try its public launcher entry.
    }

    val launchIntent = context.packageManager.getLaunchIntentForPackage(SAMSUNG_FINDER_PACKAGE)
        ?: return false
    return try {
        context.startActivity(launchIntent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
