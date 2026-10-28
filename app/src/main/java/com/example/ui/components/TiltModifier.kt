package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

private val MetroPressSpring = spring<Float>(
    dampingRatio = 0.68f,
    stiffness = 720f
)
private val MetroReleaseSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * One owner for a Home Tile's tap / long-press / drag. Keeping this inside the
 * TileView input node avoids racing an ancestor drag detector against metroTilt's
 * long-press callback (which previously selected the Tile and replaced the node).
 */
class MetroTileDragCallbacks(
    val cancelToken: Int,
    val onStart: (Offset) -> Unit,
    val onMove: (Offset) -> Unit,
    val onEnd: () -> Unit,
    val onCancel: () -> Unit
)

/**
 * Observe in the Initial pass so an interactive RemoteViews child cannot starve
 * the Home long-press. Do not consume DOWN: short taps and scroll gestures still
 * reach native widgets. Once long-press wins, consume movements and UP.
 */
internal suspend fun PointerInputScope.detectMetroHomeTileDrag(
    latestDrag: () -> MetroTileDragCallbacks?,
    latestTap: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        var lastPosition = down.position
        val finalDown = awaitPointerEvent(PointerEventPass.Final)
        val childClaimedDown = finalDown.changes.firstOrNull { it.id == pointerId }?.isConsumed == true
        val slop = viewConfiguration.touchSlop

        val aborted = withTimeoutOrNull<Boolean>(viewConfiguration.longPressTimeoutMillis.toLong()) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == pointerId }
                    ?: return@withTimeoutOrNull true
                if (!change.pressed) {
                    val finalUp = awaitPointerEvent(PointerEventPass.Final)
                    val wasConsumed = finalUp.changes.firstOrNull { it.id == pointerId }?.isConsumed == true
                    if (!childClaimedDown && !wasConsumed &&
                        (change.position - down.position).getDistance() <= slop
                    ) latestTap()
                    return@withTimeoutOrNull true
                }
                if (event.changes.count { it.pressed } > 1 ||
                    (change.position - down.position).getDistance() > slop
                ) return@withTimeoutOrNull true
                lastPosition = change.position
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }
        if (aborted == true) return@awaitEachGesture
        val drag = latestDrag() ?: return@awaitEachGesture
        drag.onStart(lastPosition)
        var finished = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == pointerId }
                    ?: break
                if (event.changes.count { it.pressed } > 1) break
                if (!change.pressed) {
                    change.consume()
                    drag.onEnd()
                    finished = true
                    break
                }
                if (change.position != lastPosition) {
                    lastPosition = change.position
                    change.consume()
                    drag.onMove(lastPosition)
                }
            }
        } finally {
            if (!finished) drag.onCancel()
        }
    }
}

/**
 * Metro perspective and press feedback. For draggable Home Tiles use the
 * unified gesture owner; other surfaces keep their existing tap recognizer.
 */
fun Modifier.metroTilt(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    dragCallbacks: MetroTileDragCallbacks? = null,
    onClick: () -> Unit
): Modifier = composed {
    val currentDrag = rememberUpdatedState(dragCallbacks)
    val currentClick = rememberUpdatedState(onClick)
    val input = if (dragCallbacks != null) {
        Modifier.pointerInput(dragCallbacks.cancelToken) {
            detectMetroHomeTileDrag(
                latestDrag = { currentDrag.value },
                latestTap = { currentClick.value() }
            )
        }
    } else {
        Modifier.pointerInput(onLongClick, onClick) {
            detectTapGestures(
                onLongPress = onLongClick?.let { callback -> { callback() } },
                onTap = { onClick() }
            )
        }
    }
    if (!enabled) return@composed this.then(input)

    var isPressed by remember { mutableStateOf(false) }
    var targetTiltX by remember { mutableFloatStateOf(0f) }
    var targetTiltY by remember { mutableFloatStateOf(0f) }
    val pressProgressState = animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = if (isPressed) MetroPressSpring else MetroReleaseSpring,
        label = "metroSpringPressProgress"
    )

    this.graphicsLayer {
        val p = pressProgressState.value
        if (p != 0f) {
            val s = 1f - (0.05f * p)
            scaleX = s
            scaleY = s
            rotationX = targetTiltX * p
            rotationY = targetTiltY * p
            translationY = 3f * p
            cameraDistance = 16f * density
        }
    }.then(
        if (dragCallbacks != null) input
        else Modifier.pointerInput(onLongClick, onClick) {
            detectTapGestures(
                onPress = { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val normX = ((offset.x - centerX) / centerX.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                    val normY = ((offset.y - centerY) / centerY.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                    targetTiltX = -normY * 13f
                    targetTiltY = normX * 13f
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onLongPress = onLongClick?.let { callback ->
                    { isPressed = false; callback() }
                },
                onTap = { isPressed = false; onClick() }
            )
        }
    )
}
