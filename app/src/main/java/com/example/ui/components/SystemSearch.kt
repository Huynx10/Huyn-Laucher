package com.example.ui.components

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

// Finder is optional OEM software. Its package is a capability probe, not a One UI API contract.
internal const val SAMSUNG_FINDER_PACKAGE = "com.samsung.android.app.galaxyfinder"

internal fun finderSearchIntent(query: String): Intent =
    Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH).apply {
        setPackage(SAMSUNG_FINDER_PACKAGE)
        if (query.isNotBlank()) putExtra(SearchManager.QUERY, query)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

/** Only launch an activity that Finder actually exports for the requested public intent. */
private fun startExportedFinderActivity(context: Context, intent: Intent): Boolean {
    val activity = try {
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
    } catch (_: SecurityException) {
        null
    }
    if (activity?.exported != true || activity.packageName != SAMSUNG_FINDER_PACKAGE) return false

    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/** Try Finder's public global-search handler, then its front door for an empty query. */
internal fun launchSamsungFinder(context: Context, query: String = ""): Boolean {
    if (startExportedFinderActivity(context, finderSearchIntent(query))) return true

    // A launcher entry has no search-query contract. Preserve typed queries for local search.
    if (query.isNotBlank()) return false
    val launchIntent = try {
        context.packageManager.getLaunchIntentForPackage(SAMSUNG_FINDER_PACKAGE)
    } catch (_: SecurityException) {
        null
    } ?: return false
    return startExportedFinderActivity(context, launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
