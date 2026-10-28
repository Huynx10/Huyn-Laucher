package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MetroTileDragGestureTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun holdMoveReleaseTriggersOneDragWithoutAccidentalTileClick() {
        var taps = 0
        var starts = 0
        var ends = 0
        var moves = 0
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier.size(180.dp).testTag("tile").metroTilt(
                        enabled = false,
                        dragCallbacks = MetroTileDragCallbacks(
                            cancelToken = 0,
                            onStart = { starts++ },
                            onMove = { moves++ },
                            onEnd = { ends++ },
                            onCancel = {}
                        ),
                        onClick = { taps++ }
                    )
                )
            }
        }
        compose.onNodeWithTag("tile").performTouchInput {
            down(center)
            advanceEventTime(700)
            moveTo(center + Offset(50f, 0f))
            moveTo(center + Offset(74f, 0f))
            up()
        }
        compose.runOnIdle {
            assertEquals(1, starts)
            assertTrue(moves > 0)
            assertEquals(1, ends)
            assertEquals(0, taps)
        }
    }

    @Test
    fun shortTapDoesNotBeginDragOrInvokeLongPressHandler() {
        var taps = 0
        var starts = 0
        compose.setContent {
            Box(
                Modifier.size(180.dp).testTag("tile").metroTilt(
                    enabled = false,
                    dragCallbacks = MetroTileDragCallbacks(
                        cancelToken = 0,
                        onStart = { starts++ },
                        onMove = {},
                        onEnd = {},
                        onCancel = {}
                    ),
                    onClick = { taps++ }
                )
            )
        }
        compose.onNodeWithTag("tile").performTouchInput { down(center); up() }
        compose.runOnIdle {
            assertEquals(0, starts)
            assertEquals(1, taps)
        }
    }
}
