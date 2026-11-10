package com.example.ui.components

import com.example.data.model.TileEntity

/** Produce one whole drag rectangle, or null for invalid, blocked or unchanged gestures.
 * No mutation occurs here; Room rechecks this proposal before any persistence.
 */
internal fun resolveTileCornerResize(
    tile: TileEntity,
    start: GridResizeRect,
    corner: ResizeCorner,
    deltaColumns: Int,
    deltaRows: Int,
    columns: Int,
    occupied: List<GridResizeRect>,
    rows: Int = 60
): GridResizeRect? {
    if (columns !in 1..8 || rows !in 1..60) return null
    val widget = tile.hasBoundOrLinkedWidget()
    val horizontal = !widget || (tile.widgetResizeMode and 1) != 0
    val vertical = !widget || (tile.widgetResizeMode and 2) != 0
    val minimumWidth = when {
        !horizontal -> start.width
        widget -> tile.widgetMinSpanX.coerceIn(1, columns)
        else -> 1
    }
    val minimumHeight = when {
        !vertical -> start.height
        widget -> tile.widgetMinSpanY.coerceIn(1, 8)
        else -> 1
    }
    val result = runCatching {
        resolveCornerResize(
            start, corner,
            if (horizontal) deltaColumns else 0,
            if (vertical) deltaRows else 0,
            columns = columns, rows = rows,
            minWidth = minimumWidth, minHeight = minimumHeight,
            maxWidth = if (horizontal) columns else start.width,
            maxHeight = if (vertical) 8 else start.height
        )
    }.getOrNull() ?: return null
    if (result == start) return null
    if (tile.constrainedResizeSpans(result.width, result.height, columns) !=
        (result.width to result.height)) return null
    if (!isCornerResizePlacementClear(result, occupied)) return null
    return result
}

/**
 * Ignore a late UP after Back, deselection, a different drag owner, or a lock.
 * Only the active selected tile may persist a completed corner gesture.
 */
internal fun isCornerResizeCommitAllowed(
    tileId: String,
    resizingTileId: String?,
    selectedTileId: String?,
    corner: ResizeCorner?,
    lockHomeLayout: Boolean
): Boolean =
    !lockHomeLayout &&
        resizingTileId == tileId &&
        selectedTileId == tileId &&
        corner != null
