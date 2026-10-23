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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
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
    onResize: () -> Unit,
    onIncreaseSize: () -> Unit = onResize,
    onDecreaseSize: () -> Unit = onResize,
    onResizeDragStart: () -> Unit = {},
    onResizeDrag: (Float, Float) -> Unit = { _, _ -> },
    onResizeDragEnd: () -> Unit = {},
    onResizeDragCancel: () -> Unit = {},
    onUnpin: () -> Unit,
    onNudgeLeft: () -> Unit = {},
    onNudgeRight: () -> Unit = {},
    onNudgeUp: () -> Unit = {},
    onNudgeDown: () -> Unit = {},
    onMoveToPage: (() -> Unit)? = null,
    onCustomizeTile: (() -> Unit)? = null,
    onFinishSingleEdit: (() -> Unit)? = null,
    installedApps: List<AppItem> = emptyList(),
    onLaunchSmartFolderItem: (SmartFolderItem) -> Unit = {},
    onOpenSmartFolder: () -> Unit = onClick,
    onRequestBindWidgetPermission: (TileEntity) -> Unit = {},
    onReplaceMissingWidget: (TileEntity) -> Unit = { onCustomizeTile?.invoke() },
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isPerTileClear = remember(tile.colorOverrideHex) {
        tile.colorOverrideHex.equals("TRANSPARENT", ignoreCase = true)
    }
    val accent = remember(
        tile.colorOverrideHex,
        settings.accentColorHex,
        settings.autoWallpaperAccentEnabled,
        settings.wallpaperGradientIndex,
        settings.dynamicAccentMode,
        settings.dynamicSwatchIndex
    ) {
        val themeAccent = LumiaPalette.resolveEffectiveAccent(settings)
        val effectiveColorHex = if (isPerTileClear || tile.colorOverrideHex.isNullOrBlank()) {
            themeAccent.hex
        } else {
            tile.colorOverrideHex
        }
        LumiaPalette.getAccent(effectiveColorHex)
    }
    val cornerShape = remember(settings.tileCornerRadiusDp) {
        RoundedCornerShape(settings.tileCornerRadiusDp.dp)
    }

    val effectiveOpacity = when {
        isPerTileClear -> 0f
        settings.wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR -> 0f
        settings.wp10TileStyle == Wp10TileStyle.CLASSIC_METRO -> 1f
        else -> settings.tileOpacity.coerceIn(0f, 1f)
    }

    val isHighlyTransparent = effectiveOpacity < 0.35f ||
        settings.wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR ||
        settings.wp10TileStyle == Wp10TileStyle.FLUENT_ACRYLIC

    val tileBackgroundBrush: Brush = remember(
        isPerTileClear,
        settings.wp10TileStyle,
        effectiveOpacity,
        accent.color
    ) {
        when {
            isPerTileClear || settings.wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR -> {
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
                onClick = {
                    if (isTileSelected) {
                        onCustomizeTile?.invoke()
                    } else {
                        onClick()
                    }
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
                    // Top-left: 1-cell free grid nudge controls (or Customize on SMALL tile)
                    if (tile.size == TileSize.SMALL) {
                        if (onCustomizeTile != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(3.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF881798))
                                    .clickable { onCustomizeTile() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Đổi màu ô Tile",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            MiniTileBadgeButton(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Qua trái 1 ô",
                                bgColor = Color.Black.copy(alpha = 0.75f),
                                onClick = onNudgeLeft
                            )
                            MiniTileBadgeButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Lên trên 1 ô",
                                bgColor = Color.Black.copy(alpha = 0.75f),
                                onClick = onNudgeUp
                            )
                            MiniTileBadgeButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Xuống dưới 1 ô",
                                bgColor = Color.Black.copy(alpha = 0.75f),
                                onClick = onNudgeDown
                            )
                            MiniTileBadgeButton(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Qua phải 1 ô",
                                bgColor = Color.Black.copy(alpha = 0.75f),
                                onClick = onNudgeRight
                            )
                        }
                    }

                    // Top-right: Unpin button (Windows 10 Mobile / One UI corner badge)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(if (tile.size == TileSize.SMALL) 22.dp else 26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE81123))
                            .clickable { onUnpin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Bỏ ghim",
                            tint = Color.White,
                            modifier = Modifier.size(if (tile.size == TileSize.SMALL) 13.dp else 15.dp)
                        )
                    }

                    // Bottom-left: Customize Tile Color/Transparency + Move to another Home Page
                    if (tile.size != TileSize.SMALL) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                        ) {
                            if (onCustomizeTile != null) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF881798))
                                        .clickable { onCustomizeTile() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Đổi màu & độ trong suốt ô Tile",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (onMoveToPage != null) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF107C41))
                                        .clickable { onMoveToPage() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                        contentDescription = "Chuyển sang trang Home khác",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Border/Edge Drag Handles (One UI style resize handles along borders)
                    val handlePillLength by animateDpAsState(
                        targetValue = when {
                            isResizingActive -> if (tile.size == TileSize.SMALL) 22.dp else 38.dp
                            else -> if (tile.size == TileSize.SMALL) 16.dp else 28.dp
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "resizeHandlePillLength"
                    )

                    val handlePillThickness by animateDpAsState(
                        targetValue = if (isResizingActive) 7.dp else 5.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "resizeHandlePillThickness"
                    )

                    // 1. Right Border Drag Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(18.dp)
                            .fillMaxHeight(0.55f)
                            .pointerInput(tile.id, tile.size) {
                                detectDragGestures(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onResizeDragStart()
                                    },
                                    onDragEnd = { onResizeDragEnd() },
                                    onDragCancel = { onResizeDragCancel() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onResizeDrag(dragAmount.x, 0f)
                                    }
                                )
                            }
                            .testTag("tile_resize_edge_right_${tile.id}"),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 1.dp)
                                .width(handlePillThickness)
                                .height(handlePillLength)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFF0078D7), RoundedCornerShape(3.dp))
                        )
                    }

                    // 2. Bottom Border Drag Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.5f)
                            .height(18.dp)
                            .pointerInput(tile.id, tile.size) {
                                detectDragGestures(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onResizeDragStart()
                                    },
                                    onDragEnd = { onResizeDragEnd() },
                                    onDragCancel = { onResizeDragCancel() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onResizeDrag(0f, dragAmount.y)
                                    }
                                )
                            }
                            .testTag("tile_resize_edge_bottom_${tile.id}"),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 1.dp)
                                .width(handlePillLength)
                                .height(handlePillThickness)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFF0078D7), RoundedCornerShape(3.dp))
                        )
                    }

                    // 3. Left Border Drag Handle (drag left to expand, drag right to shrink)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(16.dp)
                            .fillMaxHeight(0.5f)
                            .pointerInput(tile.id, tile.size) {
                                detectDragGestures(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onResizeDragStart()
                                    },
                                    onDragEnd = { onResizeDragEnd() },
                                    onDragCancel = { onResizeDragCancel() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onResizeDrag(-dragAmount.x, 0f)
                                    }
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 1.dp)
                                .width(handlePillThickness)
                                .height(handlePillLength)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.85f))
                                .border(0.8.dp, Color(0xFF0078D7), RoundedCornerShape(2.dp))
                        )
                    }

                    // Bottom-right: Combined `-` / `+` Resize Stepper Buttons + Corner Border Drag Handle
                    val allowedSizes = remember(tile.tileType, tile.widgetProvider, tile.widgetMinSpanX, tile.widgetMinSpanY, tile.widgetResizeMode) {
                        tile.allowedSizesForTile()
                    }
                    val currentAllowedIdx = allowedSizes.indexOf(tile.size)
                    val canShrink = if (currentAllowedIdx != -1) currentAllowedIdx > 0 else tile.size != TileSize.SMALL
                    val canGrow = if (currentAllowedIdx != -1) currentAllowedIdx < allowedSizes.lastIndex else tile.size != TileSize.LARGE
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0078D7))
                            .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                            .pointerInput(tile.id, tile.size) {
                                detectDragGestures(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onResizeDragStart()
                                    },
                                    onDragEnd = { onResizeDragEnd() },
                                    onDragCancel = { onResizeDragCancel() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onResizeDrag(dragAmount.x, dragAmount.y)
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (tile.size == TileSize.SMALL) 20.dp else 25.dp)
                                .clickable(enabled = canShrink) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDecreaseSize()
                                }
                                .testTag("tile_resize_minus_${tile.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Thu nhỏ kích thước ô Tile (-)",
                                tint = if (canShrink) Color.White else Color.White.copy(alpha = 0.38f),
                                modifier = Modifier.size(if (tile.size == TileSize.SMALL) 12.dp else 15.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(if (tile.size == TileSize.SMALL) 12.dp else 15.dp)
                                .background(Color.White.copy(alpha = 0.35f))
                        )

                        Box(
                            modifier = Modifier
                                .size(if (tile.size == TileSize.SMALL) 20.dp else 25.dp)
                                .clickable(enabled = canGrow) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onIncreaseSize()
                                }
                                .testTag("tile_resize_plus_${tile.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Phóng to kích thước ô Tile (+)",
                                tint = if (canGrow) Color.White else Color.White.copy(alpha = 0.38f),
                                modifier = Modifier.size(if (tile.size == TileSize.SMALL) 12.dp else 15.dp)
                            )
                        }
                    }
                }

                if (tile.size != TileSize.SMALL) {
                    Text(
                        text = when (tile.size) {
                            TileSize.SMALL -> "1x1"
                            TileSize.MEDIUM -> "2x2 • Bấm +/- hoặc kéo viền"
                            TileSize.WIDE -> "4x2 • Bấm +/- hoặc kéo viền"
                            TileSize.LARGE -> "4x4 • Bấm +/- hoặc kéo viền"
                        },
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

@Composable
private fun MiniTileBadgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(23.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}
