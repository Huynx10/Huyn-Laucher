package com.example.ui.components

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.ComponentName

// Finder is an OEM app, so only public exported activities may be opened.
// Its package is a capability probe, not an assumed One UI API contract.
internal const val SAMSUNG_FINDER_PACKAGE = "com.samsung.android.app.galaxyfinder"
internal const val ONE_UI_HOME_PACKAGE = "com.sec.android.app.launcher"

internal fun finderSearchIntent(query: String): Intent =
    Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH).apply {
        setPackage(SAMSUNG_FINDER_PACKAGE)
        if (query.isNotBlank()) putExtra(SearchManager.QUERY, query)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

internal fun oneUiHomeSearchIntent(query: String): Intent = Intent(Intent.ACTION_SEARCH).apply {
    component = ComponentName(ONE_UI_HOME_PACKAGE, "$ONE_UI_HOME_PACKAGE.search.SearchActivity")
    if (query.isNotBlank()) putExtra(SearchManager.QUERY, query)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** Try Finder's public global-search handler, then its public front-door activity. */
internal fun launchSamsungFinder(context: Context, query: String = ""): Boolean {
    // Finder is integrated into One UI Home on some devices. Probe only an exported
    // search entry; this OEM component is optional and must never be a dependency.
    val oneUiSearch = oneUiHomeSearchIntent(query)
    try {
        val info = context.packageManager.resolveActivity(oneUiSearch, 0)?.activityInfo
        if (info?.exported == true && info.packageName == ONE_UI_HOME_PACKAGE) {
            context.startActivity(oneUiSearch)
            return true
        }
    } catch (_: ActivityNotFoundException) {
        // Continue to the standalone Finder probe.
    } catch (_: SecurityException) {
        // OEM entry is not available to this launcher.
    }
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
