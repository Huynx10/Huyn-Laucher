package com.example.ui.components

import android.app.SearchManager
import android.content.Intent

/** Opens the device's public global search provider without assuming Samsung Finder is exported. */
internal fun systemSearchIntent(query: String): Intent =
    Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH).apply {
        if (query.isNotBlank()) putExtra(SearchManager.QUERY, query)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
