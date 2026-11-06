package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * F3 contract tests: opposite corner stays fixed, including top/left origin changes.
 * These intentionally precede the production resolver.
 */
class CornerResizeGeometryTest {
    private val initial = GridResizeRect(x = 2, y = 3, width = 2, height = 3)

    @Test fun topStartKeepsBottomRightFixed() {
        assertEquals(GridResizeRect(1, 2, 3, 4),
            resolveCornerResize(initial, ResizeCorner.TOP_START, deltaColumns = -1, deltaRows = -1, columns = 6))
    }

    @Test fun topEndKeepsBottomLeftFixed() {
        assertEquals(GridResizeRect(2, 2, 3, 4),
            resolveCornerResize(initial, ResizeCorner.TOP_END, deltaColumns = 1, deltaRows = -1, columns = 6))
    }

    @Test fun bottomStartKeepsTopRightFixed() {
        assertEquals(GridResizeRect(1, 3, 3, 4),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_START, deltaColumns = -1, deltaRows = 1, columns = 6))
    }

    @Test fun bottomEndKeepsTopLeftFixed() {
        assertEquals(GridResizeRect(2, 3, 3, 4),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_END, deltaColumns = 1, deltaRows = 1, columns = 6))
    }

    @Test fun topLeftCannotMoveBeyondGridAndMinimumSpan() {
        assertEquals(GridResizeRect(0, 0, 4, 6),
            resolveCornerResize(initial, ResizeCorner.TOP_START, deltaColumns = -10, deltaRows = -10, columns = 6))
        assertEquals(GridResizeRect(3, 5, 1, 1),
            resolveCornerResize(initial, ResizeCorner.TOP_START, deltaColumns = 10, deltaRows = 10, columns = 6))
    }

    @Test fun rightEdgeCannotExceedColumnCount() {
        assertEquals(GridResizeRect(2, 3, 4, 3),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_END, deltaColumns = 100, deltaRows = 0, columns = 6))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsRectangleExtendingPastGrid() {
        resolveCornerResize(GridResizeRect(5, 1, 2, 2), ResizeCorner.TOP_START, -1, -1, columns = 6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeOrigin() {
        resolveCornerResize(GridResizeRect(-1, 1, 2, 2), ResizeCorner.BOTTOM_END, 1, 1, columns = 6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMinimumWidthGreaterThanExistingRightAnchor() {
        resolveCornerResize(GridResizeRect(0, 1, 1, 2), ResizeCorner.TOP_START, 0, 0, columns = 6, minWidth = 2)
    }
    @Test fun extremePositiveDeltaClampsWithoutIntegerOverflow() {
        assertEquals(GridResizeRect(3, 3, 1, 3),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_START,
                deltaColumns = Int.MAX_VALUE, deltaRows = 0, columns = 6))
        assertEquals(GridResizeRect(2, 3, 4, 3),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_END,
                deltaColumns = Int.MAX_VALUE, deltaRows = 0, columns = 6))
    }

    @Test fun extremeNegativeDeltaClampsWithoutIntegerOverflow() {
        assertEquals(GridResizeRect(0, 3, 4, 3),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_START,
                deltaColumns = Int.MIN_VALUE, deltaRows = 0, columns = 6))
        assertEquals(GridResizeRect(2, 3, 1, 3),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_END,
                deltaColumns = Int.MIN_VALUE, deltaRows = 0, columns = 6, minWidth = 1))
    }

}
