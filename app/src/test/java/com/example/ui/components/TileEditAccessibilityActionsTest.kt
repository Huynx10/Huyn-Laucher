package com.example.ui.components

import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileEditAccessibilityActionsTest {
    private fun app(size: TileSize) = TileEntity(id = "tile", title = "Ứng dụng", size = size)

    @Test
    fun lockedHomeNeverOffersEditActions() {
        assertTrue(tileEditAccessibilityActions(app(TileSize.MEDIUM), layoutLocked = true).isEmpty())
    }

    @Test
    fun smallTileCanGrowButCannotShrink() {
        val actions = tileEditAccessibilityActions(app(TileSize.SMALL), layoutLocked = false)
        assertEquals(
            listOf(
                TileEditAccessibilityAction.MOVE_LEFT,
                TileEditAccessibilityAction.MOVE_RIGHT,
                TileEditAccessibilityAction.MOVE_UP,
                TileEditAccessibilityAction.MOVE_DOWN,
                TileEditAccessibilityAction.GROW
            ),
            actions
        )
    }

    @Test
    fun intermediateTileCanGrowAndShrink() {
        val actions = tileEditAccessibilityActions(app(TileSize.MEDIUM), layoutLocked = false)
        assertTrue(actions.contains(TileEditAccessibilityAction.GROW))
        assertTrue(actions.contains(TileEditAccessibilityAction.SHRINK))
    }

    @Test
    fun largestTileCannotGrow() {
        val actions = tileEditAccessibilityActions(app(TileSize.LARGE), layoutLocked = false)
        assertFalse(actions.contains(TileEditAccessibilityAction.GROW))
        assertTrue(actions.contains(TileEditAccessibilityAction.SHRINK))
    }

    @Test
    fun fixedNativeWidgetDoesNotExposeUnsupportedResizeActions() {
        val widget = TileEntity(
            id = "widget", title = "Đồng hồ", tileType = TileType.WIDGET,
            size = TileSize.MEDIUM, widgetResizeMode = 0,
            widgetMinSpanX = 2, widgetMinSpanY = 2
        )
        val actions = tileEditAccessibilityActions(widget, layoutLocked = false)
        assertFalse(actions.contains(TileEditAccessibilityAction.GROW))
        assertFalse(actions.contains(TileEditAccessibilityAction.SHRINK))
    }
}
