package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

private val MetroPressSpring = spring<Float>(
    dampingRatio = 0.68f,
    stiffness = 720f
)

private val MetroReleaseSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Windows 10 Mobile 3D Perspective Tilt + Xiaomi HyperOS / One UI 9.0 Spring Physics.
 * Applies responsive 3D plane depression on press and an under-damped spring rebound on release.
 * Reads animation state strictly inside graphicsLayer to avoid recomposition during touch feedback.
 */
fun Modifier.metroTilt(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) {
        return@composed this.pointerInput(onLongClick, onClick) {
            detectTapGestures(
                onLongPress = { onLongClick?.invoke() },
                onTap = { onClick() }
            )
        }
    }

    var isPressed by remember { mutableStateOf(false) }
    var targetTiltX by remember { mutableFloatStateOf(0f) }
    var targetTiltY by remember { mutableFloatStateOf(0f) }

    val pressProgressState = animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = if (isPressed) MetroPressSpring else MetroReleaseSpring,
        label = "metroSpringPressProgress"
    )

    this
        .graphicsLayer {
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
        }
        .pointerInput(onLongClick, onClick) {
            detectTapGestures(
                onPress = { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val normX = ((offset.x - centerX) / (centerX.coerceAtLeast(1f))).coerceIn(-1f, 1f)
                    val normY = ((offset.y - centerY) / (centerY.coerceAtLeast(1f))).coerceIn(-1f, 1f)
                    targetTiltX = -normY * 13f
                    targetTiltY = normX * 13f
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onLongPress = {
                    isPressed = false
                    onLongClick?.invoke()
                },
                onTap = {
                    isPressed = false
                    onClick()
                }
            )
        }
}
