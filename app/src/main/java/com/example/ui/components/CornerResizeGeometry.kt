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
    minHeight: Int = 1,
    rows: Int = Int.MAX_VALUE
): GridResizeRect {
    require(columns > 0 && rows > 0 && minWidth in 1..columns && minHeight in 1..rows)
    require(rect.x >= 0 && rect.y >= 0 && rect.width in 1..columns && rect.height > 0)
    require(rect.x.toLong() + rect.width <= columns.toLong())
    require(rect.y.toLong() + rect.height <= rows.toLong())
    require(rect.width >= minWidth && rect.height >= minHeight)
    // A top/left drag must be able to preserve the opposite (right/bottom) anchor.
    require(rect.x.toLong() + rect.width >= minWidth && rect.y.toLong() + rect.height >= minHeight)
    val left = rect.x
    val right = rect.x + rect.width
    val top = rect.y
    val bottom = rect.y.toLong() + rect.height
    val movingLeft = corner == ResizeCorner.TOP_START || corner == ResizeCorner.BOTTOM_START
    val movingTop = corner == ResizeCorner.TOP_START || corner == ResizeCorner.TOP_END
    val newLeft = if (movingLeft) (left.toLong() + deltaColumns).coerceIn(0L, (right - minWidth).toLong()).toInt() else left
    val newRight = if (movingLeft) right else (right.toLong() + deltaColumns).coerceIn((left + minWidth).toLong(), columns.toLong()).toInt()
    val newTop = if (movingTop) (top.toLong() + deltaRows).coerceIn(0L, bottom - minHeight).toInt() else top
    val newBottom = if (movingTop) bottom else (bottom + deltaRows).coerceIn(top.toLong() + minHeight, rows.toLong())
    require(newBottom - newTop <= Int.MAX_VALUE)
    return GridResizeRect(newLeft, newTop, newRight - newLeft, (newBottom - newTop).toInt())
}
