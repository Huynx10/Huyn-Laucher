package com.example.ui.components

import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** F3: provider rules and collision validation before any Room write. */
class TileCornerResizePolicyTest {
    private val start = GridResizeRect(2, 3, 2, 3)
    private fun widget(mode: Int = 3, minX: Int = 1, minY: Int = 1) =
        TileEntity(id = "widget", title = "Widget", tileType = TileType.WIDGET,
            size = TileSize.MEDIUM, gridX = start.x, gridY = start.y,
            customSpanX = start.width, customSpanY = start.height,
            widgetResizeMode = mode, widgetMinSpanX = minX, widgetMinSpanY = minY)

    @Test fun allFourCornersKeepOppositeCornerFixed() {
        val expectations = mapOf(
            ResizeCorner.TOP_START to GridResizeRect(1, 2, 3, 4),
            ResizeCorner.TOP_END to GridResizeRect(2, 2, 3, 4),
            ResizeCorner.BOTTOM_START to GridResizeRect(1, 3, 3, 4),
            ResizeCorner.BOTTOM_END to GridResizeRect(2, 3, 3, 4)
        )
        expectations.forEach { (corner, expected) ->
            val dx = if (corner == ResizeCorner.TOP_START || corner == ResizeCorner.BOTTOM_START) -1 else 1
            val dy = if (corner == ResizeCorner.TOP_START || corner == ResizeCorner.TOP_END) -1 else 1
            assertEquals(expected,
                resolveTileCornerResize(widget(), start, corner, dx, dy, 6, emptyList()))
        }
    }

    @Test fun verticalOnlyWidgetKeepsHorizontalBounds() {
        assertEquals(GridResizeRect(2, 2, 2, 4),
            resolveTileCornerResize(widget(mode = 2), start,
                ResizeCorner.TOP_START, -1, -1, 6, emptyList()))
    }

    @Test fun horizontalOnlyWidgetKeepsVerticalBounds() {
        assertEquals(GridResizeRect(1, 3, 3, 3),
            resolveTileCornerResize(widget(mode = 1), start,
                ResizeCorner.TOP_START, -1, -1, 6, emptyList()))
    }

    @Test fun fixedWidgetRejectsAllCorners() {
        ResizeCorner.entries.forEach { corner ->
            assertNull(resolveTileCornerResize(widget(mode = 0), start,
                corner, -2, 2, 6, emptyList()))
        }
    }

    @Test fun providerMinimumClampsWithoutMovingOppositeCorner() {
        assertEquals(GridResizeRect(2, 4, 2, 2),
            resolveTileCornerResize(widget(minX = 2, minY = 2), start,
                ResizeCorner.TOP_START, 100, 100, 6, emptyList()))
    }

    @Test fun collisionRejectsCandidateWithoutDisplacingOtherTiles() {
        assertNull(resolveTileCornerResize(widget(), start,
            ResizeCorner.TOP_START, -1, -1, 6, listOf(GridResizeRect(1, 2, 1, 1))))
    }

    @Test fun adjacentPlacementIsAllowed() {
        assertEquals(GridResizeRect(1, 2, 3, 4),
            resolveTileCornerResize(widget(), start,
                ResizeCorner.TOP_START, -1, -1, 6, listOf(GridResizeRect(4, 2, 1, 1))))
    }

    @Test fun unchangedOrInvalidStartingPositionNeverProducesWrite() {
        assertNull(resolveTileCornerResize(widget(), start,
            ResizeCorner.BOTTOM_END, 0, 0, 6, emptyList()))
        assertNull(resolveTileCornerResize(widget(), GridResizeRect(-1, 3, 2, 3),
            ResizeCorner.TOP_START, -1, -1, 6, emptyList()))
    }

    @Test fun largeGestureClampsToGridAndTileMaximum() {
        assertEquals(GridResizeRect(2, 3, 4, 8),
            resolveTileCornerResize(widget(), start,
                ResizeCorner.BOTTOM_END, 100, 100, 6, emptyList()))
    }
}
