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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.AppCategory
import com.example.data.model.AppItem
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.SmartFolderItem
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import com.example.data.model.WidgetProviderItem
import com.example.ui.theme.LumiaPalette
import com.example.ui.widget.DefaultWidgetMatcher
import kotlin.math.roundToInt

private val DropFootprintSpringSpec = spring<Dp>(
    dampingRatio = 0.72f,
    stiffness = 440f
)

private val ResizeFrameSpringSpec = spring<Dp>(
    dampingRatio = 0.68f,
    stiffness = 420f
)

private val TilePositionSpringSpec = spring<Dp>(
    dampingRatio = 0.74f,
    stiffness = 380f
)

private val TileResizeActiveSpringSpec = spring<Dp>(
    dampingRatio = 0.85f,
    stiffness = 850f
)

private val TileResizeReleaseSpringSpec = spring<Dp>(
    dampingRatio = 0.66f,
    stiffness = 360f
)

@Immutable
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
    installedApps: List<AppItem> = emptyList(),
    installedWidgets: List<WidgetProviderItem> = emptyList(),
    onPickAndBindWidget: (WidgetProviderItem, String, String?) -> Unit = { _, _, _ -> },
    onRequestRebindWidget: (TileEntity) -> Unit = {},
    onUnlinkWidgetFromTile: (String) -> Unit = {},
    onOpenSmartFolder: (TileEntity, Float, Float) -> Unit = { _, _, _ -> },
    onLaunchSmartFolderItem: (SmartFolderItem) -> Unit = {},
    onCreateSmartFolderByCategory: (AppCategory, Boolean) -> Unit = { _, _ -> },
    onGroupPageTilesByCategory: () -> Unit = {},
    onConvertTileToSmartFolder: (String) -> Unit = {},
    onCycleSmartFolderMode: (String) -> Unit = {},
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
    val isBatterySaverActive = remember(
        settings.batterySaverMode,
        settings.batterySaverThresholdPct,
        telemetry.batteryPct,
        telemetry.isCharging,
        telemetry.isSystemPowerSaveMode
    ) {
        settings.isBatterySaverActive(telemetry)
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
                        onGroupToSmartFolder = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConvertTileToSmartFolder(activeTile.id)
                        },
                        onCycleSmartFolderMode = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCycleSmartFolderMode(activeTile.id)
                        },
                        onOpenSmartFolder = {
                            onOpenSmartFolder(activeTile, 0.5f, 0.5f)
                        },
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
                            val hoverGridCell by remember(activeDraggingPositioned, cellStepPx, totalCols, canvasRows) {
                                derivedStateOf {
                                    val gx = (activeDraggingPositioned.gridX + (dragOffsetX / cellStepPx).roundToInt())
                                        .coerceIn(0, (totalCols - activeDraggingPositioned.spanX).coerceAtLeast(0))
                                    val gy = (activeDraggingPositioned.gridY + (dragOffsetY / cellStepPx).roundToInt())
                                        .coerceIn(0, (canvasRows - activeDraggingPositioned.spanY).coerceAtLeast(0))
                                    gx to gy
                                }
                            }
                            val hoverGridX = hoverGridCell.first
                            val hoverGridY = hoverGridCell.second

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

                            val animatedDropX by animateDpAsState(targetValue = targetDropX, animationSpec = DropFootprintSpringSpec, label = "springDropX")
                            val animatedDropY by animateDpAsState(targetValue = targetDropY, animationSpec = DropFootprintSpringSpec, label = "springDropY")
                            val animatedDropW by animateDpAsState(targetValue = targetDropW, animationSpec = DropFootprintSpringSpec, label = "springDropW")
                            val animatedDropH by animateDpAsState(targetValue = targetDropH, animationSpec = DropFootprintSpringSpec, label = "springDropH")

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
                            val previewSize by remember(activeResizingPositioned, stepThresholdPx) {
                                derivedStateOf {
                                    val deltaCols = (resizeOffsetX / stepThresholdPx).roundToInt()
                                    val deltaRows = (resizeOffsetY / stepThresholdPx).roundToInt()
                                    val rawSpanX = (activeResizingPositioned.spanX + deltaCols).coerceIn(1, 4)
                                    val rawSpanY = (activeResizingPositioned.spanY + deltaRows).coerceIn(1, 4)
                                    val candidate = TileSize.fromTargetSpans(rawSpanX, rawSpanY)
                                    val allowed = activeResizingPositioned.tile.allowedSizesForTile()
                                    when {
                                        candidate in allowed -> candidate
                                        else -> allowed.minByOrNull {
                                            kotlin.math.abs(it.spanX - candidate.spanX) + kotlin.math.abs(it.spanY - candidate.spanY)
                                        } ?: activeResizingPositioned.tile.size
                                    }
                                }
                            }
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

                            val animatedPreviewX by animateDpAsState(targetValue = targetPreviewX, animationSpec = ResizeFrameSpringSpec, label = "springPreviewX")
                            val animatedPreviewY by animateDpAsState(targetValue = targetPreviewY, animationSpec = ResizeFrameSpringSpec, label = "springPreviewY")
                            val animatedPreviewW by animateDpAsState(targetValue = targetPreviewW, animationSpec = ResizeFrameSpringSpec, label = "springPreviewW")
                            val animatedPreviewH by animateDpAsState(targetValue = targetPreviewH, animationSpec = ResizeFrameSpringSpec, label = "springPreviewH")

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

                                val shouldAnimateTileBounds = !isBatterySaverActive &&
                                    (selectedTileId != null || isEditMode || draggingTileId != null || resizingTileId != null)

                                val animatedTileX = if (shouldAnimateTileBounds) {
                                    val x by animateDpAsState(
                                        targetValue = baseTileX,
                                        animationSpec = TilePositionSpringSpec,
                                        label = "tileSpringX_${tile.id}"
                                    )
                                    x
                                } else {
                                    baseTileX
                                }
                                val animatedTileY = if (shouldAnimateTileBounds) {
                                    val y by animateDpAsState(
                                        targetValue = baseTileY,
                                        animationSpec = TilePositionSpringSpec,
                                        label = "tileSpringY_${tile.id}"
                                    )
                                    y
                                } else {
                                    baseTileY
                                }

                                val sizeSpringSpec = if (isResizing) TileResizeActiveSpringSpec else TileResizeReleaseSpringSpec
                                val animatedTileWidth = if (shouldAnimateTileBounds) {
                                    val w by animateDpAsState(
                                        targetValue = targetTileWidth,
                                        animationSpec = sizeSpringSpec,
                                        label = "tileSpringW_${tile.id}"
                                    )
                                    w
                                } else {
                                    targetTileWidth
                                }
                                val animatedTileHeight = if (shouldAnimateTileBounds) {
                                    val h by animateDpAsState(
                                        targetValue = targetTileHeight,
                                        animationSpec = sizeSpringSpec,
                                        label = "tileSpringH_${tile.id}"
                                    )
                                    h
                                } else {
                                    targetTileHeight
                                }

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
                                                    .then(
                                                        if (isBatterySaverActive) Modifier
                                                        else Modifier.shadow(16.dp, RoundedCornerShape(settings.tileCornerRadiusDp.dp))
                                                    )
                                                    .graphicsLayer {
                                                        scaleX = 1.05f
                                                        scaleY = 1.05f
                                                        alpha = 0.92f
                                                        if (!isBatterySaverActive) {
                                                            rotationX = (-dragVelY * 0.35f).coerceIn(-10f, 10f)
                                                            rotationY = (dragVelX * 0.35f).coerceIn(-10f, 10f)
                                                            rotationZ = (dragVelX * 0.16f).coerceIn(-4.5f, 4.5f)
                                                            cameraDistance = 16f * this.density
                                                        }
                                                    }
                                            } else Modifier
                                        )
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
                                    val tileCenterFractionX = ((baseTileX + (baseTileWidth / 2f)) / screenWidth.coerceAtLeast(1.dp))
                                        .coerceIn(0.12f, 0.88f)
                                    val computeTileCenterFractionY: () -> Float = {
                                        val visibleY = (baseTileY + (baseTileHeight / 2f)) - with(density) { scrollState.value.toDp() }
                                        (visibleY / screenHeight.coerceAtLeast(1.dp)).coerceIn(0.12f, 0.88f)
                                    }

                                    TileView(
                                        tile = tile,
                                        settings = settings,
                                        telemetry = telemetry,
                                        isTileSelected = isThisTileSelected,
                                        isHomeEditMode = isEditMode,
                                        isResizingActive = isResizing,
                                        installedApps = installedApps,
                                        onLaunchSmartFolderItem = { folderItem ->
                                            if (selectedTileId != null) {
                                                onSelectTileForEdit(null)
                                            } else if (isEditMode) {
                                                onSelectTileForEdit(tile.id)
                                            } else {
                                                onLaunchSmartFolderItem(folderItem)
                                            }
                                        },
                                        onOpenSmartFolder = {
                                            when {
                                                selectedTileId != null && !isThisTileSelected -> onSelectTileForEdit(null)
                                                isEditMode -> onSelectTileForEdit(tile.id)
                                                else -> onOpenSmartFolder(tile, tileCenterFractionX, computeTileCenterFractionY())
                                            }
                                        },
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
                                                tile.tileType == TileType.SMART_FOLDER -> {
                                                    onOpenSmartFolder(tile, tileCenterFractionX, computeTileCenterFractionY())
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
                                            val candidate = TileSize.fromTargetSpans(rawSpanX, rawSpanY)
                                            val allowed = tile.allowedSizesForTile()
                                            val newSize = if (candidate in allowed) candidate else {
                                                allowed.minByOrNull {
                                                    kotlin.math.abs(it.spanX - candidate.spanX) + kotlin.math.abs(it.spanY - candidate.spanY)
                                                } ?: tile.size
                                            }
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
                                        onRequestBindWidgetPermission = { targetTile ->
                                            onRequestRebindWidget(targetTile)
                                        },
                                        onReplaceMissingWidget = { targetTile ->
                                            tileToCustomize = targetTile
                                        },
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
            var showAdvancedTileOptions by remember(tile.id) { mutableStateOf(false) }
            val recommendedWidget = remember(tile, installedWidgets) {
                DefaultWidgetMatcher.recommend(tile, installedWidgets)
            }

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
                        text = tile.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (recommendedWidget != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.color.copy(alpha = 0.18f))
                                .clickable {
                                    tileToCustomize = null
                                    onPickAndBindWidget(recommendedWidget, currentPage.id, tile.id)
                                }
                                .padding(horizontal = 16.dp, vertical = 15.dp)
                                .testTag("connect_recommended_widget"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Widgets, contentDescription = null, tint = accent.color)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Kết nối widget", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${recommendedWidget.appName} · ${recommendedWidget.label}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.Link, contentDescription = "Kết nối widget đề xuất", tint = Color.White)
                        }
                    } else if (tile.hasBoundOrLinkedWidget()) {
                        Text("Đã liên kết với widget trên điện thoại", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
                    } else if (tile.tileType == TileType.PERFORMANCE || tile.tileType == TileType.BATTERY) {
                        Text("Ô này dùng dữ liệu hệ thống. Không cần kết nối widget khác.",
                            color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
                    } else if (tile.id == "tile_clock_calendar") {
                        Text("Ô này đang dùng giờ từ thiết bị.", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { showAdvancedTileOptions = !showAdvancedTileOptions },
                        modifier = Modifier.fillMaxWidth().testTag("toggle_advanced_tile_options")
                    ) {
                        Text(if (showAdvancedTileOptions) "Thu gọn" else "Tùy chỉnh thêm")
                    }
                    if (showAdvancedTileOptions) {
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { newText ->
                            customLabel = newText
                            onCustomizeTile(tile.id, selectedColorHex, newText, tile.isLiveEnabled)
                        },
                        label = { Text("Tên ô", color = Color.White.copy(alpha = 0.7f)) },
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
                        text = "Màu ô",
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
                                text = "Trong suốt",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hiển thị hình nền qua ô",
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

                    // Link / Unlink Native Android AppWidget to this Start Screen Tile
                    if (tile.tileType != TileType.SMART_FOLDER) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = accent.color,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CHỌN WIDGET KHÁC",
                                color = accent.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        if (tile.hasBoundOrLinkedWidget()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE81123).copy(alpha = 0.18f))
                                    .border(1.dp, Color(0xFFE81123).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUnlinkWidgetFromTile(tile.id)
                                        tileToCustomize = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("unlink_widget_from_tile_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Gỡ widget",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Quay lại ô ứng dụng",
                                        color = Color.White.copy(alpha = 0.68f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = "Gỡ liên kết Widget",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        val prioritizedWidgets = remember(installedWidgets, tile.packageName) {
                            val sameApp = installedWidgets.filter { it.packageName == tile.packageName }
                            val others = installedWidgets.filter { it.packageName != tile.packageName }
                            (sameApp + others).take(8)
                        }

                        if (prioritizedWidgets.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                prioritizedWidgets.forEach { wItem ->
                                    val isSamePkg = wItem.packageName == tile.packageName && tile.packageName.isNotBlank()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSamePkg) accent.color.copy(alpha = 0.22f)
                                                else Color.White.copy(alpha = 0.06f)
                                            )
                                            .clickable {
                                                tileToCustomize = null
                                                onPickAndBindWidget(wItem, currentPage.id, tile.id)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("link_widget_option_${wItem.providerComponent}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${wItem.appName} • ${wItem.label}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Cỡ chuẩn: ${wItem.targetCellWidth}x${wItem.targetCellHeight} ô",
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 10.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = "Liên kết Widget",
                                            tint = accent.color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    } // Advanced: name, appearance and alternate widget providers
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

        // Modal Sheet: Unified Widget Picker, App Tile Picker & Smart Folders
        if (showAddTileSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var absorbExistingTiles by remember { mutableStateOf(false) }
            var activePickerTab by remember { mutableIntStateOf(0) } // 0 = Widgets & Apps, 1 = Smart Folders
            var pickerSearchQuery by remember { mutableStateOf("") }
            var selectedNewAppTileSize by remember { mutableStateOf(TileSize.MEDIUM) }

            ModalBottomSheet(
                onDismissRequest = onDismissAddTileSheet,
                sheetState = sheetState,
                containerColor = Color(0xFF17171B),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Header Tabs: 0 = Widget & App Tile, 1 = Smart Folders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf(
                            0 to "Widget & Ứng dụng",
                            1 to "Thư mục Thông minh"
                        )
                        tabs.forEach { (idx, title) ->
                            val selected = activePickerTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (selected) accent.color else Color.Transparent)
                                    .clickable { activePickerTab = idx }
                                    .padding(vertical = 8.dp)
                                    .testTag(if (idx == 0) "add_sheet_tab_widgets" else "add_sheet_tab_folders"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search bar for both Widgets and Apps
                    OutlinedTextField(
                        value = pickerSearchQuery,
                        onValueChange = { pickerSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "Tìm kiếm Widget Android hoặc Ứng dụng...",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Tìm kiếm",
                                tint = accent.color
                            )
                        },
                        trailingIcon = {
                            if (pickerSearchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Xóa tìm kiếm",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.clickable { pickerSearchQuery = "" }
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accent.color,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("widget_picker_search_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Section 1: Native Android App Widgets (P0 Core Priority)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = accent.color,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ANDROID APP WIDGETS TRÊN MÁY (${installedWidgets.size})",
                            color = accent.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = "Nhúng trực tiếp Widget gốc của ứng dụng vào Metro Tile — giữ nguyên tương tác & cập nhật chuẩn Android",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredWidgets = remember(installedWidgets, pickerSearchQuery) {
                        val q = pickerSearchQuery.trim().lowercase()
                        if (q.isEmpty()) installedWidgets
                        else installedWidgets.filter {
                            it.label.lowercase().contains(q) ||
                                it.appName.lowercase().contains(q) ||
                                it.packageName.lowercase().contains(q)
                        }
                    }

                    if (filteredWidgets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("widget_picker_empty_state")
                        ) {
                            Text(
                                text = if (pickerSearchQuery.isBlank()) {
                                    "Chưa phát hiện AppWidgetProvider nào trên thiết bị hoặc trình giả lập hiện tại. Bạn có thể ghim App Tile hoặc الترا System Tile bên dưới."
                                } else {
                                    "Không tìm thấy Widget nào khớp với \"$pickerSearchQuery\"."
                                },
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredWidgets.take(20).forEach { wItem ->
                                val recSize = wItem.recommendedTileSize
                                val resizeLabel = when {
                                    wItem.resizeMode == 3 -> "Co giãn 2 chiều"
                                    wItem.resizeMode == 1 -> "Co giãn ngang"
                                    wItem.resizeMode == 2 -> "Co giãn dọc"
                                    else -> "Cố định"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.07f))
                                        .border(1.dp, accent.color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            onDismissAddTileSheet()
                                            onPickAndBindWidget(wItem, currentPage.id, null)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("pin_widget_item_${wItem.providerComponent}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val iconBmp = rememberAppIcon(wItem.packageName)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(accent.color.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (iconBmp != null) {
                                                androidx.compose.foundation.Image(
                                                    bitmap = iconBmp,
                                                    contentDescription = wItem.appName,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Widgets,
                                                    contentDescription = null,
                                                    tint = accent.color,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = wItem.label,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${wItem.appName} • Tối thiểu ${wItem.targetCellWidth}x${wItem.targetCellHeight} (${recSize.spanX}x${recSize.spanY}) • $resizeLabel",
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (wItem.description.isNotBlank()) {
                                                Text(
                                                    text = wItem.description,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accent.color)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "+ Ghim",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section 2: Smart Folder Widget System (MIUI / HyperOS)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = accent.color,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "THƯ MỤC THÔNG MINH (MIUI / HYPEROS PEEK)",
                            color = accent.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = "Nhóm các Tile theo danh mục kèm góc xem trước Folder-Peek & hiệu ứng mở bung mượt mà",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // One-Tap Auto-Group Current Page Tiles by Category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.color.copy(alpha = 0.22f))
                            .border(1.dp, accent.color.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                            .clickable {
                                onGroupPageTilesByCategory()
                                onDismissAddTileSheet()
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                            .testTag("group_page_tiles_by_category_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(accent.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Tự động gom các Tile trên trang theo Danh mục",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Phân loại & gom gọn toàn bộ Tile rời trên ${currentPage.title} vào các Smart Folder",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle: Absorb matching page tiles when creating a category folder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable { absorbExistingTiles = !absorbExistingTiles }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("toggle_absorb_matching_tiles"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Thu gọn luôn các Tile cùng danh mục đang có trên trang",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (absorbExistingTiles) accent.color else Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (absorbExistingTiles) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Smart Folder Quick-Create Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppCategory.assignableCategories.forEach { cat ->
                            val catColor = parseHexColorSafe(cat.badgeColorHex, accent.color)
                            Column(
                                modifier = Modifier
                                    .width(108.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(catColor.copy(alpha = 0.18f))
                                    .border(1.dp, catColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onCreateSmartFolderByCategory(cat, absorbExistingTiles)
                                        onDismissAddTileSheet()
                                    }
                                    .padding(10.dp)
                                    .testTag("add_smart_folder_cat_${cat.name.lowercase()}"),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(catColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = resolveCategoryIcon(cat),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = "3+1 Peek",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = cat.shortLabel,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Ghim thư mục",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Section 3: App Tiles & Real OS Telemetry Tiles
                    Text(
                        text = "GHIM APP TILE TỪ ỨNG DỤNG ĐÃ CÀI VÀO ${currentPage.title.uppercase()}",
                        color = accent.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kích thước App Tile:",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                        TileSize.entries.forEach { sz ->
                            val isSel = selectedNewAppTileSize == sz
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) accent.color else Color.White.copy(alpha = 0.08f))
                                    .clickable { selectedNewAppTileSize = sz }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${sz.spanX}x${sz.spanY}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val systemTiles = listOf(
                        Triple("Tối ưu RAM & Bộ nhớ", TileType.PERFORMANCE, TileSize.WIDE),
                        Triple("Pin & Nhiệt độ Hệ thống", TileType.BATTERY, TileSize.MEDIUM)
                    )

                    systemTiles.forEach { (label, type, size) ->
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
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Telemetry phần cứng thực tế • ${if (size == TileSize.WIDE) "4x2" else "2x2"}",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    val filteredAppsForTile = remember(installedApps, pickerSearchQuery) {
                        val q = pickerSearchQuery.trim().lowercase()
                        if (q.isEmpty()) installedApps.take(16)
                        else installedApps.filter {
                            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                        }.take(20)
                    }

                    filteredAppsForTile.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val newTile = TileEntity(
                                        id = "tile_app_${app.packageName}_${System.currentTimeMillis()}",
                                        title = app.label,
                                        packageName = app.packageName,
                                        tileType = TileType.APP,
                                        size = selectedNewAppTileSize,
                                        orderIndex = tiles.size,
                                        pageId = currentPage.id,
                                        gridX = -1,
                                        gridY = -1,
                                        isLiveEnabled = true
                                    )
                                    onPinNewTile(newTile)
                                    onDismissAddTileSheet()
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = accent.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = app.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${app.category.label} • App Tile ${selectedNewAppTileSize.spanX}x${selectedNewAppTileSize.spanY}",
                                    color = Color.White.copy(alpha = 0.55f),
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
    onGroupToSmartFolder: () -> Unit = {},
    onCycleSmartFolderMode: () -> Unit = {},
    onOpenSmartFolder: () -> Unit = {},
    onUnpin: () -> Unit
) {
    val allowedSizes = remember(tile.tileType, tile.widgetProvider, tile.widgetMinSpanX, tile.widgetMinSpanY, tile.widgetResizeMode) {
        tile.allowedSizesForTile()
    }
    val currentAllowedIdx = allowedSizes.indexOf(tile.size)
    val canShrink = if (currentAllowedIdx != -1) currentAllowedIdx > 0 else tile.size != TileSize.SMALL
    val canGrow = if (currentAllowedIdx != -1) currentAllowedIdx < allowedSizes.lastIndex else tile.size != TileSize.LARGE
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
                if (tile.tileType == TileType.SMART_FOLDER) {
                    SingleTilePillAction(
                        icon = Icons.Default.GridView,
                        label = "Kiểu Peek",
                        bgColor = accentColor.copy(alpha = 0.35f),
                        onClick = onCycleSmartFolderMode,
                        testTag = "contextual_bar_smart_folder_mode"
                    )
                    SingleTilePillAction(
                        icon = Icons.Default.FolderSpecial,
                        label = "Mở",
                        bgColor = Color.White.copy(alpha = 0.14f),
                        onClick = onOpenSmartFolder,
                        testTag = "contextual_bar_open_smart_folder"
                    )
                } else if (!tile.hasBoundOrLinkedWidget()) {
                    SingleTilePillAction(
                        icon = Icons.Default.FolderSpecial,
                        label = "Gom nhóm",
                        bgColor = accentColor.copy(alpha = 0.32f),
                        onClick = onGroupToSmartFolder,
                        testTag = "contextual_bar_group_smart_folder"
                    )
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
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
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

