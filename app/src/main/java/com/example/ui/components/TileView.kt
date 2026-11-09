package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppItem
import com.example.data.model.LauncherSettings
import com.example.data.model.SmartFolderItem
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import com.example.data.model.Wp10TileStyle
import com.example.ui.theme.LumiaPalette

private val AcrylicOverlayBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.12f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.10f)
    )
)

private val ClearTileGradientBrush = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.04f),
        Color.Black.copy(alpha = 0.14f)
    )
)

@Composable
fun TileView(
    tile: TileEntity,
    settings: LauncherSettings,
    telemetry: SystemTelemetry,
    isTileSelected: Boolean,
    isHomeEditMode: Boolean,
    isResizingActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    dragCallbacks: MetroTileDragCallbacks? = null,
    onResize: () -> Unit,
    onIncreaseSize: () -> Unit = onResize,
    onDecreaseSize: () -> Unit = onResize,
    onResizeDragStart: (ResizeCorner) -> Unit = { _ -> },
    onResizeDrag: (Float, Float) -> Unit = { _, _ -> },
    onResizeDragEnd: () -> Unit = {},
    onResizeDragCancel: () -> Unit = {},
    onUnpin: () -> Unit,
    onNudgeLeft: () -> Unit = {},
    onNudgeRight: () -> Unit = {},
    onNudgeUp: () -> Unit = {},
    onNudgeDown: () -> Unit = {},
    onFinishSingleEdit: (() -> Unit)? = null,
    installedApps: List<AppItem> = emptyList(),
    onLaunchSmartFolderItem: (SmartFolderItem) -> Unit = {},
    onOpenSmartFolder: () -> Unit = onClick,
    onRequestBindWidgetPermission: (TileEntity) -> Unit = {},
    onReplaceMissingWidget: (TileEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val accent = remember(
        settings.accentColorHex,
        settings.autoWallpaperAccentEnabled,
        settings.wallpaperGradientIndex,
        settings.dynamicAccentMode,
        settings.dynamicSwatchIndex
    ) {
        LumiaPalette.resolveEffectiveAccent(settings)
    }
    val cornerShape = remember(settings.tileCornerRadiusDp) {
        RoundedCornerShape(settings.tileCornerRadiusDp.dp)
    }

    val effectiveOpacity = when {
        settings.wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR -> 0f
        settings.wp10TileStyle == Wp10TileStyle.CLASSIC_METRO -> 1f
        else -> settings.tileOpacity.coerceIn(0f, 1f)
    }

    val isHighlyTransparent = effectiveOpacity < 0.35f ||
        settings.wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR ||
        settings.wp10TileStyle == Wp10TileStyle.FLUENT_ACRYLIC

    val tileBackgroundBrush: Brush = remember(
        settings.wp10TileStyle,
        effectiveOpacity,
        accent.color
    ) {
        when {
            settings.wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR -> {
                ClearTileGradientBrush
            }
            settings.wp10TileStyle == Wp10TileStyle.FLUENT_ACRYLIC -> {
                val tintAlpha = effectiveOpacity.coerceIn(0.08f, 0.72f)
                Brush.linearGradient(
                    colors = listOf(
                        accent.color.copy(alpha = tintAlpha),
                        Color(0xFF0A1424).copy(alpha = (tintAlpha * 0.65f).coerceIn(0.10f, 0.50f)),
                        accent.color.copy(alpha = (tintAlpha * 0.85f).coerceIn(0.06f, 0.65f))
                    )
                )
            }
            settings.wp10TileStyle == Wp10TileStyle.CLASSIC_METRO -> {
                Brush.verticalGradient(
                    listOf(accent.color, accent.color)
                )
            }
            else -> {
                if (effectiveOpacity <= 0.02f) {
                    ClearTileGradientBrush
                } else {
                    Brush.verticalGradient(
                        listOf(
                            accent.color.copy(alpha = effectiveOpacity),
                            accent.color.copy(alpha = (effectiveOpacity * 0.92f).coerceIn(0f, 1f))
                        )
                    )
                }
            }
        }
    }

    val isBatterySaverActive = remember(
        settings.batterySaverMode,
        settings.batterySaverThresholdPct,
        telemetry.batteryPct,
        telemetry.isCharging,
        telemetry.isSystemPowerSaveMode
    ) {
        settings.isBatterySaverActive(telemetry)
    }

    val shouldWiggle = isTileSelected && !settings.lockHomeLayout && !isResizingActive && !isBatterySaverActive
    val wiggleAngle = if (shouldWiggle) {
        val infiniteTransition = rememberInfiniteTransition(label = "singleTileWiggle")
        val animatedWiggle by infiniteTransition.animateFloat(
            initialValue = -0.75f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(210),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wiggleAngle"
        )
        animatedWiggle
    } else {
        0f
    }

    val targetSelectionScale = when {
        isResizingActive -> 1.01f
        isTileSelected && !settings.lockHomeLayout -> 1.03f
        isHomeEditMode -> 0.985f
        else -> 1f
    }

    val selectionScale = if (isBatterySaverActive || (targetSelectionScale == 1f && !isTileSelected && !isHomeEditMode && !isResizingActive)) {
        targetSelectionScale
    } else {
        val animatedScale by animateFloatAsState(
            targetValue = targetSelectionScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "tileSelectionSpringScale"
        )
        animatedScale
    }

    val revealBorderBrush = remember(isHighlyTransparent, accent.color) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (isHighlyTransparent) 0.36f else 0.26f),
                Color.White.copy(alpha = 0.08f),
                accent.color.copy(alpha = if (isHighlyTransparent) 0.42f else 0.18f)
            )
        )
    }

    Box(
        modifier = modifier
            .testTag("tile_${tile.id}")
            .graphicsLayer {
                scaleX = selectionScale
                scaleY = selectionScale
                rotationZ = wiggleAngle
            }
            .clip(cornerShape)
            .background(tileBackgroundBrush)
            .then(
                when {
                    isResizingActive -> Modifier.border(2.5.dp, Color(0xFF3AA0FF), cornerShape)
                    isTileSelected -> Modifier.border(2.dp, Color.White, cornerShape)
                    isHomeEditMode -> Modifier.border(0.9.dp, Color.White.copy(alpha = 0.32f), cornerShape)
                    settings.wp10RevealBorder && isHighlyTransparent ->
                        Modifier.border(0.9.dp, revealBorderBrush, cornerShape)
                    settings.miuiGlassBorder ->
                        Modifier.border(0.75.dp, revealBorderBrush, cornerShape)
                    else -> Modifier
                }
            )
            .metroTilt(
                enabled = settings.turnstileAnimationEnabled && !isBatterySaverActive && !isTileSelected && !isHomeEditMode,
                onLongClick = onLongClick,
                dragCallbacks = dragCallbacks,
                onClick = {
                    if (isTileSelected) onFinishSingleEdit?.invoke() else onClick()
                }
            )
    ) {
        if (settings.wp10TileStyle == Wp10TileStyle.FLUENT_ACRYLIC && !isTileSelected && !isBatterySaverActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AcrylicOverlayBrush)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                tile.tileType == TileType.SMART_FOLDER -> {
                    SmartFolderLiveTileContent(
                        tile = tile,
                        settings = settings,
                        installedApps = installedApps,
                        interactiveSubSlots = !isTileSelected && !isHomeEditMode && !isResizingActive,
                        isBatterySaverActive = isBatterySaverActive,
                        onLaunchItem = onLaunchSmartFolderItem,
                        onOpenFolder = onOpenSmartFolder
                    )
                }
                tile.hasBoundOrLinkedWidget() -> {
                    WidgetTileContent(
                        tile = tile,
                        accentColor = accent.color,
                        isTileSelected = isTileSelected,
                        isHomeEditMode = isHomeEditMode,
                        useRealAppIcons = settings.useRealAppIcons,
                        onLongClickTile = onLongClick,
                        onRequestRebindWidget = { targetTile, _ ->
                            onRequestBindWidgetPermission(targetTile)
                        },
                        onRemoveBrokenWidgetTile = { onUnpin() },
                        onOpenWidgetPickerForTile = { targetTile ->
                            onReplaceMissingWidget(targetTile)
                        }
                    )
                }
                else -> {
                    AppDrivenLiveTileContent(
                        tile = tile,
                        installedApps = installedApps,
                        telemetry = telemetry,
                        useRealAppIcons = settings.useRealAppIcons,
                        isHighlyTransparent = isHighlyTransparent
                    )
                }
            }

            if (tile.notificationCount > 0 && !isTileSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE81123)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${tile.notificationCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Single-Tile Edit Overlay: ONLY shown on the 1 tile that was long-pressed!
        if (isTileSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
                    .semantics {
                        customActions = tileEditAccessibilityActions(tile, settings.lockHomeLayout).map { action ->
                            CustomAccessibilityAction(action.label) {
                                when (action) {
                                    TileEditAccessibilityAction.MOVE_LEFT -> onNudgeLeft()
                                    TileEditAccessibilityAction.MOVE_RIGHT -> onNudgeRight()
                                    TileEditAccessibilityAction.MOVE_UP -> onNudgeUp()
                                    TileEditAccessibilityAction.MOVE_DOWN -> onNudgeDown()
                                    TileEditAccessibilityAction.SHRINK -> onDecreaseSize()
                                    TileEditAccessibilityAction.GROW -> onIncreaseSize()
                                }
                                true
                            }
                        }
                    }
            ) {
                if (settings.lockHomeLayout) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.78f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Đã khóa bố cục",
                            tint = Color(0xFFFFC700),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(48.dp)
                            .clickable { onUnpin() }
                            .testTag("tile_remove_${tile.id}"),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.82f))
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Bỏ ô khỏi trang",
                                tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    // Four independent corner gestures; never convert top/left drags into a
                    // bottom-right resize. Compact SMALL touch regions avoid overlap.
                    ResizeCorner.entries.forEach { corner ->
                        val alignment = when (corner) {
                            ResizeCorner.TOP_START -> Alignment.TopStart
                            ResizeCorner.TOP_END -> Alignment.TopEnd
                            ResizeCorner.BOTTOM_START -> Alignment.BottomStart
                            ResizeCorner.BOTTOM_END -> Alignment.BottomEnd
                        }
                        val label = when (corner) {
                            ResizeCorner.TOP_START -> "trên trái"
                            ResizeCorner.TOP_END -> "trên phải"
                            ResizeCorner.BOTTOM_START -> "dưới trái"
                            ResizeCorner.BOTTOM_END -> "dưới phải"
                        }
                        Box(
                            modifier = Modifier
                                .align(alignment)
                                .padding(2.dp)
                                .size(if (tile.size == TileSize.SMALL) 32.dp else 44.dp)
                                .semantics { contentDescription = "Kéo góc $label để đổi kích thước ô" }
                                .pointerInput(tile.id, tile.size, corner) {
                                    detectDragGestures(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onResizeDragStart(corner)
                                        },
                                        onDragEnd = { onResizeDragEnd() },
                                        onDragCancel = { onResizeDragCancel() },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            onResizeDrag(amount.x, amount.y)
                                        }
                                    )
                                }
                                .testTag("tile_resize_corner_${corner.name.lowercase()}_${tile.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(alpha = 0.78f))
                                    .border(1.5.dp, Color.White, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }

                if (tile.size != TileSize.SMALL) {
                    Text(
                        text = "${tile.customSpanX.takeIf { it > 0 } ?: tile.size.spanX}×${tile.effectiveSpanY()} • Kéo góc để đổi cỡ",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}


// TalkBack can move/resize a tile without persistent on-screen arrows or +/- buttons.
internal enum class TileEditAccessibilityAction(val label: String) {
    MOVE_LEFT("Di chuyển ô sang trái"),
    MOVE_RIGHT("Di chuyển ô sang phải"),
    MOVE_UP("Di chuyển ô lên trên"),
    MOVE_DOWN("Di chuyển ô xuống dưới"),
    SHRINK("Thu nhỏ ô"),
    GROW("Phóng to ô")
}

internal fun tileEditAccessibilityActions(
    tile: TileEntity,
    layoutLocked: Boolean
): List<TileEditAccessibilityAction> {
    if (layoutLocked) return emptyList()
    val result = mutableListOf(
        TileEditAccessibilityAction.MOVE_LEFT,
        TileEditAccessibilityAction.MOVE_RIGHT,
        TileEditAccessibilityAction.MOVE_UP,
        TileEditAccessibilityAction.MOVE_DOWN
    )
    val allowed = tile.allowedSizesForTile()
    val index = allowed.indexOf(tile.size)
    if (index > 0) result += TileEditAccessibilityAction.SHRINK
    if (index >= 0 && index < allowed.lastIndex) result += TileEditAccessibilityAction.GROW
    return result
}
