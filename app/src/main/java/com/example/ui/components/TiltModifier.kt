package com.example.ui.components

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

fun Modifier.metroTilt(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) {
        return@composed this.pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { onLongClick?.invoke() },
                onTap = { onClick() }
            )
        }
    }

    var isPressed by remember { mutableStateOf(false) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
        label = "scale"
    )
    val animatedTiltX by animateFloatAsState(
        targetValue = if (isPressed) tiltX else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = if (isPressed) tiltY else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
        label = "tiltY"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            rotationX = animatedTiltX
            rotationY = animatedTiltY
            cameraDistance = 16f * density
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val normX = ((offset.x - centerX) / (centerX.coerceAtLeast(1f))).coerceIn(-1f, 1f)
                    val normY = ((offset.y - centerY) / (centerY.coerceAtLeast(1f))).coerceIn(-1f, 1f)
                    tiltX = -normY * 12f
                    tiltY = normX * 12f
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
