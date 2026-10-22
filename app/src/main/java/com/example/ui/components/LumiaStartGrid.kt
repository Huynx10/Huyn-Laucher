package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import com.example.ui.theme.LumiaPalette
import kotlin.math.roundToInt

private data class PositionedTile(
    val tile: TileEntity,
    val gridX: Int,
    val gridY: Int,
    val spanX: Int,
    val spanY: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiaStartGrid(
    currentPage: LauncherPage,
    allHomePages: List<LauncherPage>,
    tiles: List<TileEntity>,
    settings: LauncherSettings,
    telemetry: SystemTelemetry,
    isEditMode: Boolean,
    selectedTileId: String?,
    showAddTileSheet: Boolean,
    onDismissAddTileSheet: () -> Unit,
    onOpenAddTileSheet: () -> Unit,
    onTileClick: (TileEntity) -> Unit,
    onSelectTileForEdit: (String?) -> Unit,
    onResizeTile: (String) -> Unit,
    onStepTileSize: (String, Boolean) -> Unit = { id, _ -> onResizeTile(id) },
    onSetTileSize: (String, TileSize) -> Unit = { id, _ -> onResizeTile(id) },
    onUnpinTile: (String) -> Unit,
    onMoveTile: (String, Boolean) -> Unit,
    onMoveTileToGrid: (String, Int, Int, Int) -> Unit,
    onNudgeTile: (String, Int, Int, Int) -> Unit,
    onReorderTiles: (Int, Int) -> Unit,
    onMoveTileToPage: (String, String) -> Unit,
    onCustomizeTile: (String, String?, String, Boolean) -> Unit,
    onPinNewTile: (TileEntity) -> Unit,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit = {},
    onOpenGlance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent = LumiaPalette.resolveEffectiveAccent(settings)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var tileToMoveAcrossPages by remember { mutableStateOf<TileEntity?>(null) }
    var tileToCustomize by remember { mutableStateOf<TileEntity?>(null) }

    var draggingTileId by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragVelX by remember { mutableFloatStateOf(0f) }
    var dragVelY by remember { mutableFloatStateOf(0f) }

    var resizingTileId by remember { mutableStateOf<String?>(null) }
    var resizeOffsetX by remember { mutableFloatStateOf(0f) }
    var resizeOffsetY by remember { mutableFloatStateOf(0f) }

    val selectedTileOnThisPage = remember(tiles, selectedTileId) {
        tiles.find { it.id == selectedTileId }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isEditMode, selectedTileId, settings.doubleTapToGlance) {
                detectTapGestures(
                    onTap = {
                        when {
                            selectedTileId != null -> onSelectTileForEdit(null)
                            isEditMode -> onExitEditMode()
                        }
                    },
                    onDoubleTap = {
                        if (settings.doubleTapToGlance && !isEditMode && selectedTileId == null) {
                            onOpenGlance()
                        }
                    },
                    onLongPress = {
                        if (selectedTileId != null) {
                            // Đang sửa 1 tile mà bấm ra ngoài -> lưu & thoát chế độ sửa 1 tile
                            onSelectTileForEdit(null)
                        } else {
                            // Long-pressing empty area enters Home Screen Edit Mode (One UI style)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEnterEditMode()
                        }
                    }
                )
            }
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val totalCols = remember(screenWidth, settings.showMoreTiles) {
            when {
                screenWidth >= 840.dp -> 8
                screenWidth >= 600.dp -> 6
                settings.showMoreTiles -> 6
                else -> 4
            }
        }

        val horizontalPadding = 12.dp
        val topPadding = 10.dp
        val tileSpacing = settings.tileSpacingDp.coerceIn(2, 16).dp
        val availableContentWidth = (screenWidth - (horizontalPadding * 2)).coerceAtLeast(120.dp)
        val unitSize = (availableContentWidth - (tileSpacing * (totalCols - 1))) / totalCols
        val cellStepDp = unitSize + tileSpacing
        val cellStepPx = with(density) { cellStepDp.toPx() }.coerceAtLeast(1f)

        // Resolve deterministic 2D coordinates for each tile without auto-collapsing empty spaces
        val positionedTiles = remember(tiles, totalCols) {
            val placed = mutableListOf<PositionedTile>()
            tiles.forEach { tile ->
                val spanX = tile.size.effectiveSpanX(totalCols)
                val spanY = tile.size.effectiveSpanY()
                if (tile.gridX >= 0 && tile.gridY >= 0) {
                    val clampedX = tile.gridX.coerceIn(0, (totalCols - spanX).coerceAtLeast(0))
                    val clampedY = tile.gridY.coerceAtLeast(0)
                    placed.add(PositionedTile(tile, clampedX, clampedY, spanX, spanY))
                } else {
                    var foundX = 0
                    var foundY = 0
                    outer@ for (y in 0..80) {
                        for (x in 0..(totalCols - spanX)) {
                            val overlaps = placed.any { p ->
                                x < p.gridX + p.spanX && x + spanX > p.gridX &&
                                    y < p.gridY + p.spanY && y + spanY > p.gridY
                            }
                            if (!overlaps) {
                                foundX = x
                                foundY = y
                                break@outer
                            }
                        }
                    }
                    placed.add(PositionedTile(tile, foundX, foundY, spanX, spanY))
                }
            }
            placed
        }

        val minScreenRows = remember(screenHeight, cellStepDp) {
            ((screenHeight / cellStepDp).toInt() + 2).coerceAtLeast(12)
        }

        val maxTileBottomRow = remember(positionedTiles) {
            positionedTiles.maxOfOrNull { it.gridY + it.spanY } ?: 0
        }

        val canvasRows = remember(minScreenRows, maxTileBottomRow, isEditMode, selectedTileId) {
            val extraRows = if (isEditMode || selectedTileId != null) 6 else 3
            (maxTileBottomRow + extraRows).coerceAtLeast(minScreenRows)
        }

        val canvasHeight = topPadding + (cellStepDp * canvasRows) + if (isEditMode) 96.dp else 48.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Optional subtle page title only when page has tiles and user enabled it
            if (settings.showPageHeaderTitle && tiles.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(isEditMode, selectedTileId, settings.doubleTapToGlance) {
                            detectTapGestures(
                                onTap = {
                                    if (selectedTileId != null) onSelectTileForEdit(null)
                                    else if (isEditMode) onExitEditMode()
                                },
                                onDoubleTap = {
                                    if (settings.doubleTapToGlance && !isEditMode && selectedTileId == null) onOpenGlance()
                                },
                                onLongPress = {
                                    if (selectedTileId != null) onSelectTileForEdit(null)
                                    else {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onEnterEditMode()
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentPage.title,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // One UI Contextual Floating Action Pill when editing a SINGLE tile (does not cover the screen)
            AnimatedVisibility(
                visible = selectedTileOnThisPage != null && !isEditMode,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { -it }
                ) + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    targetOffsetY = { -it }
                ) + fadeOut()
            ) {
                selectedTileOnThisPage?.let { activeTile ->
                    SingleTileContextualBar(
                        tile = activeTile,
                        accentColor = accent.color,
                        canMovePage = allHomePages.size > 1,
                        isLocked = settings.lockHomeLayout,
                        onIncreaseSize = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStepTileSize(activeTile.id, true)
                        },
                        onDecreaseSize = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStepTileSize(activeTile.id, false)
                        },
                        onCustomize = { tileToCustomize = activeTile },
                        onMoveToPage = { tileToMoveAcrossPages = activeTile },
                        onUnpin = { onUnpinTile(activeTile.id) }
                    )
                }
            }

            if (tiles.isEmpty() && !isEditMode) {
                // Completely blank empty Home page in normal mode (100% pure wallpaper, no text/buttons)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("empty_home_page_${currentPage.id}")
                        .pointerInput(isEditMode, settings.doubleTapToGlance) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (settings.doubleTapToGlance && !isEditMode) onOpenGlance()
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onEnterEditMode()
                                }
                            )
                        }
                )
            } else {
                // 2D Free-Placement Grid Canvas (Cho phép sắp xếp tự do không auto sắp xếp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .testTag(
                            if (tiles.isEmpty()) "empty_home_page_${currentPage.id}"
                            else "tiles_grid_${currentPage.id}"
                        )
                        .pointerInput(isEditMode, selectedTileId, settings.doubleTapToGlance) {
                            detectTapGestures(
                                onTap = {
                                    when {
                                        selectedTileId != null -> onSelectTileForEdit(null)
                                        isEditMode -> onExitEditMode()
                                    }
                                },
                                onDoubleTap = {
                                    if (settings.doubleTapToGlance && !isEditMode && selectedTileId == null) {
                                        onOpenGlance()
                                    }
                                },
                                onLongPress = {
                                    if (selectedTileId != null) {
                                        // Đang chỉnh sửa 1 tile mà bấm ra ngoài -> lưu cấu hình & thoát sửa tile
                                        onSelectTileForEdit(null)
                                    } else {
                                        // Long-pressing empty area enters Home Screen Edit Mode
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onEnterEditMode()
                                    }
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(canvasHeight)
                    ) {
                        // Subtle 2D Grid Cell Guides shown when a single tile is being edited/dragged/resized or in Home Edit Mode
                        val showGridGuides = isEditMode || selectedTileOnThisPage != null || draggingTileId != null || resizingTileId != null
                        if (showGridGuides) {
                            for (row in 0 until canvasRows) {
                                for (col in 0 until totalCols) {
                                    val cellX = horizontalPadding + (cellStepDp * col)
                                    val cellY = topPadding + (cellStepDp * row)
                                    Box(
                                        modifier = Modifier
                                            .offset(x = cellX, y = cellY)
                                            .size(unitSize)
                                            .clip(RoundedCornerShape(settings.tileCornerRadiusDp.coerceAtLeast(2).dp))
                                            .border(
                                                width = 0.6.dp,
                                                color = Color.White.copy(alpha = if (selectedTileOnThisPage != null) 0.14f else 0.09f),
                                                shape = RoundedCornerShape(settings.tileCornerRadiusDp.coerceAtLeast(2).dp)
                                            )
                                    )
                                }
                            }
                        }

                        // Live One UI 9.0 / HyperOS Spring-Animated Drop Target Footprint Preview while dragging a tile
                        val activeDraggingPositioned = positionedTiles.find { it.tile.id == draggingTileId }
                        if (activeDraggingPositioned != null) {
                            val hoverGridX = (activeDraggingPositioned.gridX + (dragOffsetX / cellStepPx).roundToInt())
                                .coerceIn(0, (totalCols - activeDraggingPositioned.spanX).coerceAtLeast(0))
                            val hoverGridY = (activeDraggingPositioned.gridY + (dragOffsetY / cellStepPx).roundToInt())
                                .coerceIn(0, (canvasRows - activeDraggingPositioned.spanY).coerceAtLeast(0))

                            var lastHoverCellX by remember(activeDraggingPositioned.tile.id) { mutableIntStateOf(activeDraggingPositioned.gridX) }
                            var lastHoverCellY by remember(activeDraggingPositioned.tile.id) { mutableIntStateOf(activeDraggingPositioned.gridY) }
                            LaunchedEffect(hoverGridX, hoverGridY) {
                                if (hoverGridX != lastHoverCellX || hoverGridY != lastHoverCellY) {
                                    lastHoverCellX = hoverGridX
                                    lastHoverCellY = hoverGridY
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }

                            val targetDropX = horizontalPadding + (cellStepDp * hoverGridX)
                            val targetDropY = topPadding + (cellStepDp * hoverGridY)
                            val targetDropW = (unitSize * activeDraggingPositioned.spanX) + (tileSpacing * (activeDraggingPositioned.spanX - 1))
                            val targetDropH = (unitSize * activeDraggingPositioned.spanY) + (tileSpacing * (activeDraggingPositioned.spanY - 1))

                            val dropSpringSpec = spring<androidx.compose.ui.unit.Dp>(
                                dampingRatio = 0.72f,
                                stiffness = 440f
                            )
                            val animatedDropX by animateDpAsState(targetValue = targetDropX, animationSpec = dropSpringSpec, label = "springDropX")
                            val animatedDropY by animateDpAsState(targetValue = targetDropY, animationSpec = dropSpringSpec, label = "springDropY")
                            val animatedDropW by animateDpAsState(targetValue = targetDropW, animationSpec = dropSpringSpec, label = "springDropW")
                            val animatedDropH by animateDpAsState(targetValue = targetDropH, animationSpec = dropSpringSpec, label = "springDropH")

                            Box(
                                modifier = Modifier
                                    .offset(x = animatedDropX, y = animatedDropY)
                                    .width(animatedDropW)
                                    .height(animatedDropH)
                                    .clip(RoundedCornerShape(settings.tileCornerRadiusDp.coerceAtLeast(4).dp))
                                    .background(accent.color.copy(alpha = 0.22f))
                                    .border(
                                        width = 2.dp,
                                        color = Color.White.copy(alpha = 0.92f),
                                        shape = RoundedCornerShape(settings.tileCornerRadiusDp.coerceAtLeast(4).dp)
                                    )
                            )
                        }

                        // Live Border-Drag Resize Preview Frame with Spring Morphing & Haptic Step Ticks
                        val activeResizingPositioned = positionedTiles.find { it.tile.id == resizingTileId }
                        if (activeResizingPositioned != null) {
                            val stepThresholdPx = (cellStepPx * 0.6f).coerceAtLeast(1f)
                            val deltaCols = (resizeOffsetX / stepThresholdPx).roundToInt()
                            val deltaRows = (resizeOffsetY / stepThresholdPx).roundToInt()
                            val rawSpanX = (activeResizingPositioned.spanX + deltaCols).coerceIn(1, 4)
                            val rawSpanY = (activeResizingPositioned.spanY + deltaRows).coerceIn(1, 4)
                            val previewSize = TileSize.fromTargetSpans(rawSpanX, rawSpanY)
                            val previewSpanX = previewSize.effectiveSpanX(totalCols)
                            val previewSpanY = previewSize.effectiveSpanY()
                            val previewGridX = activeResizingPositioned.gridX
                                .coerceIn(0, (totalCols - previewSpanX).coerceAtLeast(0))
                            val previewGridY = activeResizingPositioned.gridY.coerceAtLeast(0)

                            var lastPreviewSize by remember(activeResizingPositioned.tile.id) {
                                mutableStateOf(activeResizingPositioned.tile.size)
                            }
                            LaunchedEffect(previewSize) {
                                if (previewSize != lastPreviewSize) {
                                    lastPreviewSize = previewSize
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }

                            val targetPreviewX = horizontalPadding + (cellStepDp * previewGridX)
                            val targetPreviewY = topPadding + (cellStepDp * previewGridY)
                            val targetPreviewW = (unitSize * previewSpanX) + (tileSpacing * (previewSpanX - 1))
                            val targetPreviewH = (unitSize * previewSpanY) + (tileSpacing * (previewSpanY - 1))

                            val resizeFrameSpring = spring<androidx.compose.ui.unit.Dp>(
                                dampingRatio = 0.68f,
                                stiffness = 420f
                            )
                            val animatedPreviewX by animateDpAsState(targetValue = targetPreviewX, animationSpec = resizeFrameSpring, label = "springPreviewX")
                            val animatedPreviewY by animateDpAsState(targetValue = targetPreviewY, animationSpec = resizeFrameSpring, label = "springPreviewY")
                            val animatedPreviewW by animateDpAsState(targetValue = targetPreviewW, animationSpec = resizeFrameSpring, label = "springPreviewW")
                            val animatedPreviewH by animateDpAsState(targetValue = targetPreviewH, animationSpec = resizeFrameSpring, label = "springPreviewH")

                            Box(
                                modifier = Modifier
                                    .offset(x = animatedPreviewX, y = animatedPreviewY)
                                    .width(animatedPreviewW)
                                    .height(animatedPreviewH)
                                    .zIndex(25f)
                                    .clip(RoundedCornerShape(settings.tileCornerRadiusDp.coerceAtLeast(4).dp))
                                    .background(Color(0xFF0078D7).copy(alpha = 0.25f))
                                    .border(
                                        width = 2.5.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(settings.tileCornerRadiusDp.coerceAtLeast(4).dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (previewSize) {
                                        TileSize.SMALL -> "1x1 (Nhỏ)"
                                        TileSize.MEDIUM -> "2x2 (Vừa)"
                                        TileSize.WIDE -> "4x2 (Rộng)"
                                        TileSize.LARGE -> "4x4 (Lớn)"
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFF0078D7), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Render each tile with keyed Spring-Based Physics (Windows 10 Mobile + One UI 9.0 + HyperOS)
                        positionedTiles.forEach { positioned ->
                            key(positioned.tile.id) {
                                val tile = positioned.tile
                                val isThisTileSelected = selectedTileId == tile.id
                                val isDragging = draggingTileId == tile.id
                                val isResizing = resizingTileId == tile.id

                                val baseTileX = horizontalPadding + (cellStepDp * positioned.gridX)
                                val baseTileY = topPadding + (cellStepDp * positioned.gridY)
                                val baseTileWidth = (unitSize * positioned.spanX) + (tileSpacing * (positioned.spanX - 1))
                                val baseTileHeight = (unitSize * positioned.spanY) + (tileSpacing * (positioned.spanY - 1))

                                // Elastic rubber-band stretching while user drags tile border handles
                                val elasticDeltaW = if (isResizing) {
                                    with(density) { (resizeOffsetX * 0.45f).toDp() }
                                } else 0.dp
                                val elasticDeltaH = if (isResizing) {
                                    with(density) { (resizeOffsetY * 0.45f).toDp() }
                                } else 0.dp

                                val maxGridWidth = (unitSize * totalCols) + (tileSpacing * (totalCols - 1))
                                val targetTileWidth = (baseTileWidth + elasticDeltaW)
                                    .coerceIn(unitSize * 0.72f, maxGridWidth)
                                val targetTileHeight = (baseTileHeight + elasticDeltaH)
                                    .coerceIn(unitSize * 0.72f, (unitSize * 4) + (tileSpacing * 3) + 24.dp)

                                // Spring physics for grid position changes (nudging, dropping, collision push)
                                val positionSpringSpec = spring<androidx.compose.ui.unit.Dp>(
                                    dampingRatio = 0.74f,
                                    stiffness = 380f
                                )
                                val animatedTileX by animateDpAsState(
                                    targetValue = baseTileX,
                                    animationSpec = positionSpringSpec,
                                    label = "tileSpringX_${tile.id}"
                                )
                                val animatedTileY by animateDpAsState(
                                    targetValue = baseTileY,
                                    animationSpec = positionSpringSpec,
                                    label = "tileSpringY_${tile.id}"
                                )

                                // Spring physics for tile resizing (+/- buttons and border drag release)
                                val sizeSpringSpec = if (isResizing) {
                                    spring<androidx.compose.ui.unit.Dp>(
                                        dampingRatio = 0.85f,
                                        stiffness = 850f
                                    )
                                } else {
                                    spring<androidx.compose.ui.unit.Dp>(
                                        dampingRatio = 0.66f,
                                        stiffness = 360f
                                    )
                                }
                                val animatedTileWidth by animateDpAsState(
                                    targetValue = targetTileWidth,
                                    animationSpec = sizeSpringSpec,
                                    label = "tileSpringW_${tile.id}"
                                )
                                val animatedTileHeight by animateDpAsState(
                                    targetValue = targetTileHeight,
                                    animationSpec = sizeSpringSpec,
                                    label = "tileSpringH_${tile.id}"
                                )

                                // Spring-based lift scale & 3D velocity-driven tilt while dragging (W10M / HyperOS inertia)
                                val dragLiftScale by animateFloatAsState(
                                    targetValue = if (isDragging) 1.065f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "tileDragLiftScale_${tile.id}"
                                )
                                val dragAlpha by animateFloatAsState(
                                    targetValue = if (isDragging) 0.92f else 1f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "tileDragAlpha_${tile.id}"
                                )
                                val dynamicTiltX by animateFloatAsState(
                                    targetValue = if (isDragging) (-dragVelY * 0.35f).coerceIn(-10f, 10f) else 0f,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                                    label = "tileDragTiltX_${tile.id}"
                                )
                                val dynamicTiltY by animateFloatAsState(
                                    targetValue = if (isDragging) (dragVelX * 0.35f).coerceIn(-10f, 10f) else 0f,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                                    label = "tileDragTiltY_${tile.id}"
                                )
                                val dynamicTiltZ by animateFloatAsState(
                                    targetValue = if (isDragging) (dragVelX * 0.16f).coerceIn(-4.5f, 4.5f) else 0f,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                                    label = "tileDragTiltZ_${tile.id}"
                                )

                                Box(
                                    modifier = Modifier
                                        .offset(x = animatedTileX, y = animatedTileY)
                                        .width(animatedTileWidth)
                                        .height(animatedTileHeight)
                                        .zIndex(
                                            when {
                                                isDragging -> 20f
                                                isResizing -> 18f
                                                isThisTileSelected -> 10f
                                                else -> 1f
                                            }
                                        )
                                        .then(
                                            if (isDragging) {
                                                Modifier
                                                    .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                                                    .shadow(22.dp, RoundedCornerShape(settings.tileCornerRadiusDp.dp))
                                            } else Modifier
                                        )
                                        .graphicsLayer {
                                            scaleX = dragLiftScale
                                            scaleY = dragLiftScale
                                            alpha = dragAlpha
                                            rotationX = dynamicTiltX
                                            rotationY = dynamicTiltY
                                            rotationZ = dynamicTiltZ
                                            cameraDistance = 16f * density.density
                                        }
                                        .then(
                                            if (!settings.lockHomeLayout) {
                                                if (isThisTileSelected) {
                                                    // Already selected single tile: allow immediate drag without waiting for another long-press
                                                    Modifier.pointerInput(tile.id, positioned.gridX, positioned.gridY, totalCols) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                draggingTileId = tile.id
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                            },
                                                            onDragEnd = {
                                                                val targetX = (positioned.gridX + (dragOffsetX / cellStepPx).roundToInt())
                                                                    .coerceIn(0, (totalCols - positioned.spanX).coerceAtLeast(0))
                                                                val targetY = (positioned.gridY + (dragOffsetY / cellStepPx).roundToInt())
                                                                    .coerceIn(0, 60)
                                                                draggingTileId = null
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                                onMoveTileToGrid(tile.id, targetX, targetY, totalCols)
                                                            },
                                                            onDragCancel = {
                                                                draggingTileId = null
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetX += dragAmount.x
                                                                dragOffsetY += dragAmount.y
                                                                dragVelX = (dragVelX * 0.55f) + (dragAmount.x * 0.45f)
                                                                dragVelY = (dragVelY * 0.55f) + (dragAmount.y * 0.45f)
                                                            }
                                                        )
                                                    }
                                                } else if (selectedTileId == null) {
                                                    // Long-press on a tile when NO tile is currently being edited: selects ONLY this 1 tile AND allows immediate free 2D drag
                                                    Modifier.pointerInput(tile.id, positioned.gridX, positioned.gridY, totalCols) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                onSelectTileForEdit(tile.id)
                                                                draggingTileId = tile.id
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                            },
                                                            onDragEnd = {
                                                                val deltaCellsX = (dragOffsetX / cellStepPx).roundToInt()
                                                                val deltaCellsY = (dragOffsetY / cellStepPx).roundToInt()
                                                                val targetX = (positioned.gridX + deltaCellsX)
                                                                    .coerceIn(0, (totalCols - positioned.spanX).coerceAtLeast(0))
                                                                val targetY = (positioned.gridY + deltaCellsY)
                                                                    .coerceIn(0, 60)
                                                                draggingTileId = null
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                                if (deltaCellsX != 0 || deltaCellsY != 0) {
                                                                    onMoveTileToGrid(tile.id, targetX, targetY, totalCols)
                                                                }
                                                            },
                                                            onDragCancel = {
                                                                draggingTileId = null
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetX += dragAmount.x
                                                                dragOffsetY += dragAmount.y
                                                                dragVelX = (dragVelX * 0.55f) + (dragAmount.x * 0.45f)
                                                                dragVelY = (dragVelY * 0.55f) + (dragAmount.y * 0.45f)
                                                            }
                                                        )
                                                    }
                                                } else Modifier
                                            } else Modifier
                                        )
                                ) {
                                    TileView(
                                        tile = tile,
                                        settings = settings,
                                        telemetry = telemetry,
                                        isTileSelected = isThisTileSelected,
                                        isHomeEditMode = isEditMode,
                                        isResizingActive = isResizing,
                                        onClick = {
                                            when {
                                                selectedTileId != null && !isThisTileSelected -> {
                                                    // Đang chỉnh sửa 1 tile -> bấm ra ngoài (kể cả bấm vào tile khác) chỉ lưu & thoát, KHÔNG chuyển sang sửa tile khác
                                                    onSelectTileForEdit(null)
                                                }
                                                isEditMode -> {
                                                    // In Home Edit Mode, tapping a tile selects that single tile
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onSelectTileForEdit(tile.id)
                                                }
                                                else -> {
                                                    onTileClick(tile)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (selectedTileId != null && !isThisTileSelected) {
                                                // Đang ở chế độ chỉnh sửa 1 tile -> bấm/nhấn giữ ra ngoài chỉ lưu cấu hình & thoát, không chuyển sang sửa tile khác
                                                onSelectTileForEdit(null)
                                            } else {
                                                // Long-pressing 1 tile edits ONLY that 1 tile!
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onSelectTileForEdit(tile.id)
                                            }
                                        },
                                        onResize = { onResizeTile(tile.id) },
                                        onIncreaseSize = { onStepTileSize(tile.id, true) },
                                        onDecreaseSize = { onStepTileSize(tile.id, false) },
                                        onResizeDragStart = {
                                            resizingTileId = tile.id
                                            resizeOffsetX = 0f
                                            resizeOffsetY = 0f
                                        },
                                        onResizeDrag = { dx, dy ->
                                            resizeOffsetX += dx
                                            resizeOffsetY += dy
                                        },
                                        onResizeDragEnd = {
                                            val stepThresholdPx = (cellStepPx * 0.6f).coerceAtLeast(1f)
                                            val deltaCols = (resizeOffsetX / stepThresholdPx).roundToInt()
                                            val deltaRows = (resizeOffsetY / stepThresholdPx).roundToInt()
                                            val rawSpanX = (positioned.spanX + deltaCols).coerceIn(1, 4)
                                            val rawSpanY = (positioned.spanY + deltaRows).coerceIn(1, 4)
                                            val newSize = TileSize.fromTargetSpans(rawSpanX, rawSpanY)
                                            resizingTileId = null
                                            resizeOffsetX = 0f
                                            resizeOffsetY = 0f
                                            if (newSize != tile.size) {
                                                onSetTileSize(tile.id, newSize)
                                            }
                                        },
                                        onResizeDragCancel = {
                                            resizingTileId = null
                                            resizeOffsetX = 0f
                                            resizeOffsetY = 0f
                                        },
                                        onUnpin = { onUnpinTile(tile.id) },
                                        onNudgeLeft = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onNudgeTile(tile.id, -1, 0, totalCols)
                                        },
                                        onNudgeRight = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onNudgeTile(tile.id, 1, 0, totalCols)
                                        },
                                        onNudgeUp = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onNudgeTile(tile.id, 0, -1, totalCols)
                                        },
                                        onNudgeDown = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onNudgeTile(tile.id, 0, 1, totalCols)
                                        },
                                        onMoveToPage = if (allHomePages.size > 1) {
                                            { tileToMoveAcrossPages = tile }
                                        } else null,
                                        onCustomizeTile = { tileToCustomize = tile },
                                        onFinishSingleEdit = { onSelectTileForEdit(null) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal Sheet: Customize Individual Tile (Color Override & Label)
        tileToCustomize?.let { tile ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var customLabel by remember(tile.id) { mutableStateOf(tile.title) }
            var selectedColorHex by remember(tile.id) { mutableStateOf(tile.colorOverrideHex) }

            ModalBottomSheet(
                onDismissRequest = { tileToCustomize = null },
                sheetState = sheetState,
                containerColor = Color(0xFF17171B),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TUỲ CHỈNH Ô TILE RIÊNG BIỆT",
                        color = accent.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { newText ->
                            customLabel = newText
                            onCustomizeTile(tile.id, selectedColorHex, newText, tile.isLiveEnabled)
                        },
                        label = { Text("Tên hiển thị trên ô Tile (Tự động lưu)", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accent.color,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Kiểu nền & Màu sắc riêng cho ô này (Chạm để áp dụng ngay):",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val isClearTileSelected = selectedColorHex.equals("TRANSPARENT", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isClearTileSelected) accent.color.copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.07f)
                            )
                            .border(
                                width = if (isClearTileSelected) 1.5.dp else 1.dp,
                                color = if (isClearTileSelected) accent.color else Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedColorHex = "TRANSPARENT"
                                onCustomizeTile(tile.id, "TRANSPARENT", customLabel, tile.isLiveEnabled)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trong suốt hoàn toàn (Windows 10 Mobile Clear Tile)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Làm ô này trong suốt xuyên thấu 100% hình nền + viền sáng Fluent",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
                        if (isClearTileSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = accent.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(106.dp)
                    ) {
                        items(LumiaPalette.Accents) { acc ->
                            val isSelected = selectedColorHex != null && acc.hex.equals(selectedColorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(acc.color)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedColorHex = acc.hex
                                        onCustomizeTile(tile.id, acc.hex, customLabel, tile.isLiveEnabled)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            selectedColorHex = null
                            onCustomizeTile(tile.id, null, customLabel, tile.isLiveEnabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dùng màu chủ đề mặc định", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Modal Sheet: Move Tile to another Home Page
        tileToMoveAcrossPages?.let { tile ->
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { tileToMoveAcrossPages = null },
                sheetState = sheetState,
                containerColor = Color(0xFF17171B),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "DI CHUYỂN Ô TILE SANG TRANG HOME KHÁC",
                        color = accent.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Chọn trang Home đích cho \"${tile.title}\":",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    allHomePages.forEach { targetPage ->
                        val isCurrent = targetPage.id == tile.pageId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) accent.color.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .clickable(enabled = !isCurrent) {
                                    onMoveTileToPage(tile.id, targetPage.id)
                                    tileToMoveAcrossPages = null
                                    onSelectTileForEdit(null)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null,
                                    tint = if (isCurrent) accent.color else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = targetPage.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (isCurrent) {
                                Text(
                                    text = "Trang hiện tại",
                                    color = accent.color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Modal Sheet: Add Live Tile to Current Home Page
        if (showAddTileSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = onDismissAddTileSheet,
                sheetState = sheetState,
                containerColor = Color(0xFF17171B),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "GHIM TIỆN ÍCH VÀO ${currentPage.title.uppercase()}",
                        color = accent.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Chọn tiện ích Live Tile để ghim vào vị trí trống trên trang",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val presets = listOf(
                        Triple("Đồng hồ & Lịch sống động", TileType.CLOCK_CALENDAR, TileSize.WIDE),
                        Triple("Thời tiết địa phương", TileType.WEATHER, TileSize.WIDE),
                        Triple("Mức Pin & Sức khỏe", TileType.BATTERY, TileSize.MEDIUM),
                        Triple("Hiệu năng RAM & Bộ nhớ", TileType.PERFORMANCE, TileSize.MEDIUM),
                        Triple("Album ảnh kỷ niệm Lumia", TileType.PHOTOS, TileSize.LARGE),
                        Triple("Ghi chú nhanh", TileType.NOTES, TileSize.WIDE),
                        Triple("Cảm hứng mỗi ngày", TileType.NEWS_QUOTE, TileSize.MEDIUM)
                    )

                    presets.forEach { (label, type, size) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val newTile = TileEntity(
                                        id = "tile_${type.name.lowercase()}_${System.currentTimeMillis()}",
                                        title = label,
                                        tileType = type,
                                        size = size,
                                        orderIndex = tiles.size,
                                        pageId = currentPage.id,
                                        gridX = -1,
                                        gridY = -1,
                                        isLiveEnabled = true
                                    )
                                    onPinNewTile(newTile)
                                    onDismissAddTileSheet()
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "Kích thước: ${when (size) { TileSize.SMALL -> "1x1"; TileSize.MEDIUM -> "2x2"; TileSize.WIDE -> "4x2"; TileSize.LARGE -> "4x4" }}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SingleTileContextualBar(
    tile: TileEntity,
    accentColor: Color,
    canMovePage: Boolean,
    isLocked: Boolean,
    onIncreaseSize: () -> Unit,
    onDecreaseSize: () -> Unit,
    onCustomize: () -> Unit,
    onMoveToPage: () -> Unit,
    onUnpin: () -> Unit
) {
    val canShrink = tile.size != TileSize.SMALL
    val canGrow = tile.size != TileSize.LARGE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C22).copy(alpha = 0.94f))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("single_tile_contextual_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tile.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isLocked) "Bố cục đang khóa" else "Bấm +/- hoặc kéo viền để đổi cỡ • Tự động lưu",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isLocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Stepper - / Size / + Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (canShrink) Color(0xFF0078D7) else Color.White.copy(alpha = 0.08f))
                            .clickable(enabled = canShrink) { onDecreaseSize() }
                            .testTag("contextual_bar_resize_minus"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Thu nhỏ (-)",
                            tint = if (canShrink) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = when (tile.size) {
                            TileSize.SMALL -> "1x1"
                            TileSize.MEDIUM -> "2x2"
                            TileSize.WIDE -> "4x2"
                            TileSize.LARGE -> "4x4"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (canGrow) Color(0xFF0078D7) else Color.White.copy(alpha = 0.08f))
                            .clickable(enabled = canGrow) { onIncreaseSize() }
                            .testTag("contextual_bar_resize_plus"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Phóng to (+)",
                            tint = if (canGrow) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                SingleTilePillAction(
                    icon = Icons.Default.Palette,
                    label = "Màu",
                    bgColor = Color.White.copy(alpha = 0.12f),
                    onClick = onCustomize
                )
                if (canMovePage) {
                    SingleTilePillAction(
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        label = "Trang",
                        bgColor = Color.White.copy(alpha = 0.12f),
                        onClick = onMoveToPage
                    )
                }
                SingleTilePillAction(
                    icon = Icons.Default.Close,
                    label = "Gỡ",
                    bgColor = Color(0xFFE81123).copy(alpha = 0.85f),
                    onClick = onUnpin
                )
            }
        }
    }
}

@Composable
private fun SingleTilePillAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
