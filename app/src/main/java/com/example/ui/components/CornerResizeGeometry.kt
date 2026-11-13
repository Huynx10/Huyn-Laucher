package com.example.ui.components

/** Pure grid geometry for F3; no Room, widget or gesture side effects. */
internal data class GridResizeRect(val x: Int, val y: Int, val width: Int, val height: Int)

enum class ResizeCorner { TOP_START, TOP_END, BOTTOM_START, BOTTOM_END }

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
    rows: Int = Int.MAX_VALUE,
    maxHeight: Int = 8,
    maxWidth: Int = columns
): GridResizeRect {
    require(columns > 0 && rows > 0 && minWidth in 1..columns && minHeight in 1..rows)
    require(maxHeight >= minHeight && maxWidth >= minWidth)
    val effectiveMaxWidth = minOf(maxWidth, columns)
    val effectiveMaxHeight = minOf(maxHeight, rows)
    require(rect.x >= 0 && rect.y >= 0 && rect.width in minWidth..effectiveMaxWidth && rect.height in minHeight..effectiveMaxHeight)
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
    val newLeft = if (movingLeft) (left.toLong() + deltaColumns).coerceIn(0L, (right - minWidth).toLong()).coerceAtLeast(right.toLong() - effectiveMaxWidth).toInt() else left
    val newRight = if (movingLeft) right else (right.toLong() + deltaColumns).coerceIn((left + minWidth).toLong(), minOf(columns.toLong(), left.toLong() + effectiveMaxWidth)).toInt()
    val newTop = if (movingTop) (top.toLong() + deltaRows).coerceIn(maxOf(0L, bottom - effectiveMaxHeight), bottom - minHeight).toInt() else top
    val newBottom = if (movingTop) bottom else (bottom + deltaRows).coerceIn(top.toLong() + minHeight, minOf(rows.toLong(), top.toLong() + effectiveMaxHeight))
    require(newBottom - newTop <= Int.MAX_VALUE)
    return GridResizeRect(newLeft, newTop, newRight - newLeft, (newBottom - newTop).toInt())
}

/**
 * Fail-closed collision predicate for a proposed resize on one Home page.
 * Callers must exclude the selected tile and tiles on other pages.
 * Long arithmetic prevents overflow when checking untrusted persisted coordinates.
 */
internal fun isCornerResizePlacementClear(
    candidate: GridResizeRect,
    otherPageRects: List<GridResizeRect>
): Boolean {
    fun valid(rect: GridResizeRect): Boolean =
        rect.x >= 0 && rect.y >= 0 && rect.width > 0 && rect.height > 0 &&
            rect.x.toLong() + rect.width <= Int.MAX_VALUE.toLong() &&
            rect.y.toLong() + rect.height <= Int.MAX_VALUE.toLong()
    if (!valid(candidate) || otherPageRects.any { !valid(it) }) return false
    val right = candidate.x.toLong() + candidate.width
    val bottom = candidate.y.toLong() + candidate.height
    return otherPageRects.none { other ->
        val otherRight = other.x.toLong() + other.width
        val otherBottom = other.y.toLong() + other.height
        candidate.x.toLong() < otherRight && right > other.x.toLong() &&
            candidate.y.toLong() < otherBottom && bottom > other.y.toLong()
    }
}
