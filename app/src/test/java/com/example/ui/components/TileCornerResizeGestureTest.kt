package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.data.model.BatterySaverMode
import com.example.data.model.LauncherSettings
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Gesture entry/exit regression: four separate corners, no legacy edge target. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class TileCornerResizeGestureTest {
    @get:Rule val compose = createComposeRule()
    private val tile = TileEntity(id = "gesture", title = "Tile", size = TileSize.WIDE)
    private val settings = LauncherSettings(
        batterySaverMode = BatterySaverMode.ALWAYS_ON,
        turnstileAnimationEnabled = false
    )

    @Test fun fourCornersDispatchDistinctIdentityAndOneEndEach() {
        val starts = mutableListOf<ResizeCorner>()
        var ends = 0
        var cancels = 0
        var moves = 0
        var unpins = 0
        compose.setContent {
            TileView(
                tile = tile, settings = settings, telemetry = SystemTelemetry(),
                isTileSelected = true, isHomeEditMode = true,
                onClick = {}, onLongClick = {}, onResize = {}, onUnpin = { unpins++ },
                onResizeDragStart = { starts += it },
                onResizeDrag = { _, _ -> moves++ },
                onResizeDragEnd = { ends++ },
                onResizeDragCancel = { cancels++ },
                modifier = Modifier.size(220.dp).testTag("gesture_tile")
            )
        }
        for (corner in ResizeCorner.entries) {
            compose.onNodeWithTag("tile_resize_corner_${corner.name.lowercase()}_gesture")
                .performTouchInput {
                    val dx = if (corner == ResizeCorner.TOP_START ||
                        corner == ResizeCorner.BOTTOM_START) -40f else 40f
                    val dy = if (corner == ResizeCorner.TOP_START ||
                        corner == ResizeCorner.TOP_END) -40f else 40f
                    down(center)
                    moveBy(Offset(dx, dy))
                    up()
                }
        }
        compose.runOnIdle {
            assertEquals(ResizeCorner.entries.toList(), starts)
            assertEquals(4, ends)
            assertEquals(0, cancels)
            assertEquals(0, unpins)
            assertTrue(moves >= 4)
        }
    }

    @Test fun smallTileCornerControlsDoNotOverlapDestructiveUnpinTarget() {
        compose.setContent {
            TileView(
                tile = tile.copy(size = TileSize.SMALL),
                settings = settings, telemetry = SystemTelemetry(),
                isTileSelected = true, isHomeEditMode = true,
                onClick = {}, onLongClick = {}, onResize = {}, onUnpin = {},
                modifier = Modifier.size(96.dp)
            )
        }
        compose.onNodeWithTag("tile_remove_gesture").assertDoesNotExist()
        for (corner in ResizeCorner.entries) {
            compose.onNodeWithTag("tile_resize_corner_" + corner.name.lowercase() + "_gesture").assertExists()
        }
    }

    @Test fun narrowSixColumnTileDoesNotOverlapCornerPointerTargets() {
        // A 1x1 tile around 52dp is common with six Home columns on a narrow device.
        compose.setContent {
            TileView(
                tile = tile.copy(size = TileSize.SMALL),
                settings = settings, telemetry = SystemTelemetry(),
                isTileSelected = true, isHomeEditMode = true,
                onClick = {}, onLongClick = {}, onResize = {}, onUnpin = {},
                modifier = Modifier.size(52.dp)
            )
        }
        fun bounds(corner: ResizeCorner) =
            compose.onNodeWithTag("tile_resize_corner_" + corner.name.lowercase() + "_gesture")
                .fetchSemanticsNode().boundsInRoot
        val topStart = bounds(ResizeCorner.TOP_START)
        val topEnd = bounds(ResizeCorner.TOP_END)
        val bottomStart = bounds(ResizeCorner.BOTTOM_START)
        val bottomEnd = bounds(ResizeCorner.BOTTOM_END)
        assertTrue("Top handles overlap horizontally", topStart.right <= topEnd.left)
        assertTrue("Bottom handles overlap horizontally", bottomStart.right <= bottomEnd.left)
        assertTrue("Left handles overlap vertically", topStart.bottom <= bottomStart.top)
        assertTrue("Right handles overlap vertically", topEnd.bottom <= bottomEnd.top)
    }

    @Test fun fixedWidgetDoesNotShowInertCornerHandles() {
        compose.setContent {
            TileView(
                tile = tile.copy(
                    tileType = com.example.data.model.TileType.WIDGET,
                    widgetResizeMode = 0
                ),
                settings = settings, telemetry = SystemTelemetry(),
                isTileSelected = true, isHomeEditMode = true,
                onClick = {}, onLongClick = {}, onResize = {}, onUnpin = {},
                modifier = Modifier.size(220.dp)
            )
        }
        for (corner in ResizeCorner.entries) {
            compose.onNodeWithTag("tile_resize_corner_" + corner.name.lowercase() + "_gesture")
                .assertDoesNotExist()
        }
    }

    @Test fun cancelledDragDoesNotEmitEndOrCommitSignal() {
        var ends = 0
        var cancels = 0
        compose.setContent {
            TileView(
                tile = tile, settings = settings, telemetry = SystemTelemetry(),
                isTileSelected = true, isHomeEditMode = true,
                onClick = {}, onLongClick = {}, onResize = {}, onUnpin = {},
                onResizeDragStart = {},
                onResizeDrag = { _, _ -> },
                onResizeDragEnd = { ends++ },
                onResizeDragCancel = { cancels++ },
                modifier = Modifier.size(220.dp)
            )
        }
        compose.onNodeWithTag("tile_resize_corner_bottom_end_gesture")
            .performTouchInput {
                down(center)
                moveBy(Offset(40f, 40f))
                cancel()
            }
        compose.runOnIdle {
            assertEquals(0, ends)
            assertEquals(1, cancels)
        }
    }
}
