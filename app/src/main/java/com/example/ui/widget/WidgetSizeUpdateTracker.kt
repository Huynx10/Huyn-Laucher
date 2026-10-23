package com.example.ui.widget

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Remembers only widget sizes successfully published to AppWidgetManager.
 * A failed binder call must not suppress the next update attempt.
 */
internal class WidgetSizeUpdateTracker {
    private val publishedSizes = ConcurrentHashMap<Int, Pair<Int, Int>>()

    fun needsUpdate(appWidgetId: Int, widthDp: Int, heightDp: Int): Boolean {
        if (appWidgetId <= 0) return false
        val previous = publishedSizes[appWidgetId] ?: return true
        return abs(previous.first - widthDp) >= 4 || abs(previous.second - heightDp) >= 4
    }

    fun markUpdated(appWidgetId: Int, widthDp: Int, heightDp: Int) {
        if (appWidgetId > 0) publishedSizes[appWidgetId] = widthDp to heightDp
    }

    fun forget(appWidgetId: Int) {
        publishedSizes.remove(appWidgetId)
    }
}
