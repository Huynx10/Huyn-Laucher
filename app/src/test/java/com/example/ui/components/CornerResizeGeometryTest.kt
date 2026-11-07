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

    @Test fun bottomResizeClampsAtFiniteRowBoundary() {
        assertEquals(GridResizeRect(2, 3, 2, 5),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_END, 0, 100, columns = 6, rows = 8))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRectangleExtendingPastFiniteRowBoundary() {
        resolveCornerResize(initial, ResizeCorner.TOP_START, 0, 0, columns = 6, rows = 5)
    }

    @Test fun zeroDeltaPreservesRectangleForEveryCorner() {
        ResizeCorner.values().forEach { corner ->
            assertEquals(initial,
                resolveCornerResize(initial, corner, 0, 0, columns = 6, rows = 8))
        }
    }

    @Test fun topEndClampsToTopBoundaryWithoutMovingBottomLeftAnchor() {
        assertEquals(GridResizeRect(2, 0, 4, 6),
            resolveCornerResize(initial, ResizeCorner.TOP_END,
                deltaColumns = Int.MAX_VALUE, deltaRows = Int.MIN_VALUE,
                columns = 6, rows = 8))
    }

    @Test fun bottomStartClampsAtBottomBoundaryWithoutMovingTopRightAnchor() {
        assertEquals(GridResizeRect(0, 3, 4, 5),
            resolveCornerResize(initial, ResizeCorner.BOTTOM_START,
                deltaColumns = Int.MIN_VALUE, deltaRows = Int.MAX_VALUE,
                columns = 6, rows = 8))
    }

    @Test fun bottomCornersRespectPersistedMaximumHeightOnTallPages() {
        listOf(ResizeCorner.BOTTOM_START, ResizeCorner.BOTTOM_END).forEach { corner ->
            val resized = resolveCornerResize(initial, corner, 0, 100,
                columns = 6, rows = 16, maxHeight = 8)
            assertEquals(GridResizeRect(2, 3, 2, 8), resized)
        }
    }

    @Test fun topCornersRespectPersistedMaximumHeightAndKeepBottomAnchor() {
        listOf(ResizeCorner.TOP_START, ResizeCorner.TOP_END).forEach { corner ->
            val resized = resolveCornerResize(
                GridResizeRect(2, 8, 2, 3), corner, 0, -100,
                columns = 6, rows = 16, maxHeight = 8)
            assertEquals(GridResizeRect(2, 3, 2, 8), resized)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInitialHeightBeyondPersistedMaximum() {
        resolveCornerResize(GridResizeRect(0, 0, 2, 9),
            ResizeCorner.BOTTOM_END, 0, 0,
            columns = 6, rows = 16, maxHeight = 8)
    }

    @Test fun finiteShortPageClampsMaximumHeightToAvailableRows() {
        val rect = GridResizeRect(1, 0, 2, 2)
        assertEquals(GridResizeRect(1, 0, 2, 5),
            resolveCornerResize(rect, ResizeCorner.BOTTOM_END,
                deltaColumns = 0, deltaRows = 100, columns = 4, rows = 5))
    }

    @Test fun shortPageTopCornerPreservesBottomAnchor() {
        val rect = GridResizeRect(1, 2, 2, 3)
        assertEquals(GridResizeRect(1, 0, 2, 5),
            resolveCornerResize(rect, ResizeCorner.TOP_START,
                deltaColumns = 0, deltaRows = -100, columns = 4, rows = 5))
    }

    @Test fun placementRejectsOverlapButAllowsEdgeTouchAndOtherPages() {
        val candidate = GridResizeRect(1, 1, 2, 2)
        assertEquals(false, isCornerResizePlacementClear(candidate,
            listOf(GridResizeRect(2, 2, 2, 2))))
        assertEquals(true, isCornerResizePlacementClear(candidate,
            listOf(GridResizeRect(3, 1, 2, 2), GridResizeRect(0, 3, 2, 2))))
    }

    @Test fun placementRejectsInvalidOrOverflowingRects() {
        assertEquals(false, isCornerResizePlacementClear(
            GridResizeRect(-1, 0, 2, 2), emptyList()))
        assertEquals(false, isCornerResizePlacementClear(
            GridResizeRect(0, 0, 0, 2), emptyList()))
        assertEquals(false, isCornerResizePlacementClear(
            GridResizeRect(Int.MAX_VALUE, 0, 2, 2), emptyList()))
    }

    @Test fun providerMaximumWidthClampsRightCorners() {
        val rect = GridResizeRect(1, 1, 2, 2)
        listOf(ResizeCorner.TOP_END, ResizeCorner.BOTTOM_END).forEach { corner ->
            assertEquals(GridResizeRect(1, 1, 3, 2),
                resolveCornerResize(rect, corner, 100, 0, columns = 8, rows = 8, maxWidth = 3))
        }
    }

    @Test fun providerMaximumWidthClampsLeftCornersWithoutMovingRightAnchor() {
        val rect = GridResizeRect(4, 1, 2, 2)
        listOf(ResizeCorner.TOP_START, ResizeCorner.BOTTOM_START).forEach { corner ->
            assertEquals(GridResizeRect(3, 1, 3, 2),
                resolveCornerResize(rect, corner, -100, 0, columns = 8, rows = 8, maxWidth = 3))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInitialWidthBeyondProviderMaximum() {
        resolveCornerResize(GridResizeRect(0, 0, 4, 2),
            ResizeCorner.BOTTOM_END, 0, 0, columns = 8, rows = 8, maxWidth = 3)
    }

    @Test fun providerMinimumClampsTopStartWhileKeepingOppositeCornerFixed() {
        val rect = GridResizeRect(x = 2, y = 3, width = 3, height = 4)
        assertEquals(
            GridResizeRect(3, 5, 2, 2),
            resolveCornerResize(rect, ResizeCorner.TOP_START,
                deltaColumns = Int.MAX_VALUE, deltaRows = Int.MAX_VALUE,
                columns = 8, rows = 12, minWidth = 2, minHeight = 2)
        )
    }

    @Test fun providerFixedDimensionsRejectExpansionForAllCorners() {
        val rect = GridResizeRect(x = 2, y = 3, width = 2, height = 3)
        ResizeCorner.values().forEach { corner ->
            assertEquals(
                rect,
                resolveCornerResize(rect, corner,
                    deltaColumns = -100, deltaRows = 100,
                    columns = 8, rows = 12,
                    minWidth = 2, minHeight = 3,
                    maxWidth = 2, maxHeight = 3)
            )
        }
    }

}
