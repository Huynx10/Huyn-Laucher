package com.example.ui.components

import kotlin.math.hypot

internal fun drawerHomeEdgeDirection(
    active: Boolean,
    hasDragIntent: Boolean,
    fingerDown: Boolean,
    locked: Boolean,
    fingerFractionX: Float
): Int = when {
    !active || !hasDragIntent || !fingerDown || locked -> 0
    fingerFractionX <= 0.10f -> -1
    fingerFractionX >= 0.90f -> 1
    else -> 0
}

/** Event gate for a drag whose Lazy source can disappear before physical UP. */
internal class DrawerAppDragGate {
    private var activeKey: String? = null
    private var consumedKey: String? = null
    private var startX = 0f
    private var startY = 0f
    private var hasIntent = false

    /** Only a new physical DOWN can unlock a canceled or completed pointer session. */
    fun onPhysicalDown() {
        activeKey = null
        consumedKey = null
        hasIntent = false
    }

    /** Reject late Lazy starts and competing items during the same physical gesture. */
    fun begin(key: String, rootX: Float, rootY: Float): Boolean {
        if (activeKey != null || consumedKey != null) return false
        activeKey = key
        startX = rootX
        startY = rootY
        hasIntent = false
        return true
    }

    fun recordMove(rootX: Float, rootY: Float, thresholdPx: Float): Boolean {
        if (activeKey == null) return false
        if (!hasIntent && thresholdPx > 0f &&
            hypot(rootX - startX, rootY - startY) >= thresholdPx
        ) hasIntent = true
        return hasIntent
    }

    fun cancel() {
        activeKey?.let { consumedKey = it }
        activeKey = null
    }

    fun releaseInDrawer() {
        activeKey = null // Let the source finish its existing menu/group/reorder flow.
    }

    /** Returns false for duplicate or late UP after cancellation. */
    fun releaseOnHome(): Boolean {
        val key = activeKey ?: return false
        activeKey = null
        if (!hasIntent) return false
        consumedKey = key
        return true
    }

    fun suppressSourceEnd(key: String, fingerStillDown: Boolean): Boolean =
        consumedKey == key || (activeKey == key && fingerStillDown)
}
