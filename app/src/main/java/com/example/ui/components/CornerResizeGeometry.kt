package com.example.ui.components

/** Pure grid geometry for F3; no Room, widget or gesture side effects. */
internal data class GridResizeRect(val x: Int, val y: Int, val width: Int, val height: Int)

internal enum class ResizeCorner { TOP_START, TOP_END, BOTTOM_START, BOTTOM_END }

/**
 * Resize by moving only the edges adjacent to [corner]. The opposite corner is anchored.
 * Provider minimum dimensions and collision checks are enforced by the caller before persistence.
 */
internal fun resolveCornerResize(
    rect: GridResizeRect,
    corner: ResizeCorner,
    deltaColumns: Int,
    deltaRows: Int,
    columns: Int,
    minWidth: Int = 1,
    minHeight: Int = 1
): GridResizeRect {
    require(columns > 0 && minWidth in 1..columns && minHeight > 0)
    require(rect.width in 1..columns && rect.height > 0)
    val left = rect.x
    val right = rect.x + rect.width
    val top = rect.y
    val bottom = rect.y + rect.height
    val movingLeft = corner == ResizeCorner.TOP_START || corner == ResizeCorner.BOTTOM_START
    val movingTop = corner == ResizeCorner.TOP_START || corner == ResizeCorner.TOP_END
    val newLeft = if (movingLeft) (left + deltaColumns).coerceIn(0, right - minWidth) else left
    val newRight = if (movingLeft) right else (right + deltaColumns).coerceIn(left + minWidth, columns)
    val newTop = if (movingTop) (top + deltaRows).coerceIn(0, bottom - minHeight) else top
    val newBottom = if (movingTop) bottom else (bottom + deltaRows).coerceAtLeast(top + minHeight)
    return GridResizeRect(newLeft, newTop, newRight - newLeft, newBottom - newTop)
}
