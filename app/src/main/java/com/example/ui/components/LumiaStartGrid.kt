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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.widget.WidgetPickerGrouping
import com.example.ui.widget.LumiaWidgetHostManager
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

private fun findManualFolderDropTarget(
    source: PositionedTile,
    candidates: List<PositionedTile>,
    offsetX: Float,
    offsetY: Float,
    cellStepPx: Float
): PositionedTile? {
    if (source.tile.tileType != TileType.APP || source.tile.hasBoundOrLinkedWidget()) return null
    val centerX = source.gridX + source.spanX / 2f + offsetX / cellStepPx
    val centerY = source.gridY + source.spanY / 2f + offsetY / cellStepPx
    return candidates.firstOrNull { target ->
        target.tile.id != source.tile.id && !target.tile.hasBoundOrLinkedWidget() &&
            (target.tile.tileType == TileType.APP || target.tile.tileType == TileType.SMART_FOLDER) &&
            centerX in (target.gridX + target.spanX * 0.25f)..(target.gridX + target.spanX * 0.75f) &&
            centerY in (target.gridY + target.spanY * 0.25f)..(target.gridY + target.spanY * 0.75f)
    }
}

/** Exact destination grid geometry in Compose root coordinates, independent of source scroll. */
@Immutable
data class HomePageDropGeometry(
    val pageId: String,
    val viewportRootX: Float,
    val viewportRootY: Float,
    val scrollYPx: Float,
    val horizontalPaddingPx: Float,
    val topPaddingPx: Float,
    val cellStepPx: Float,
    val columns: Int
) {
    fun resolveDrop(tileRootX: Float, tileRootY: Float, spanX: Int): Pair<Int, Int>? {
        if (!tileRootX.isFinite() || !tileRootY.isFinite() ||
            !viewportRootX.isFinite() || !viewportRootY.isFinite() ||
            !scrollYPx.isFinite() || !cellStepPx.isFinite() || cellStepPx <= 0f ||
            columns !in listOf(4, 6, 8) || spanX !in 1..columns
        ) return null
        val column = ((tileRootX - viewportRootX - horizontalPaddingPx) / cellStepPx).roundToInt()
        val row = ((tileRootY - viewportRootY + scrollYPx - topPaddingPx) / cellStepPx).roundToInt()
        return if (column >= 0 && column + spanX <= columns && row in 0..60) column to row else null
    }
}

private fun tileDisplayName(
    tile: TileEntity,
    installedApps: List<AppItem>,
    installedWidgets: List<WidgetProviderItem>
): String = when {
    tile.hasBoundOrLinkedWidget() -> installedWidgets
        .firstOrNull { it.providerComponent == tile.widgetProvider }?.label
        ?.takeIf { it.isNotBlank() } ?: tile.title
    tile.tileType == TileType.APP -> installedApps
        .firstOrNull { it.packageName == tile.packageName }?.label
        ?.takeIf { it.isNotBlank() } ?: tile.title
    else -> tile.title
}

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
    onSetTileSpans: (String, Int, Int) -> Unit = { _, _, _ -> },
    onGroupTilesManually: (String, String) -> Unit = { _, _ -> },
    onUnpinTile: (String) -> Unit,
    onMoveTile: (String, Boolean) -> Unit,
    onMoveTileToGrid: (String, Int, Int, Int) -> Unit,
    onNudgeTile: (String, Int, Int, Int) -> Unit,
    onReorderTiles: (Int, Int) -> Unit,
    onMoveTileToPage: (String, String) -> Unit,
    onCrossPageDragStart: (String, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onCrossPageDragMove: (String, Float, Float) -> Unit = { _, _, _ -> },
    onCrossPageDragEnd: (String) -> Boolean = { false },
    onCrossPageGridGeometry: (HomePageDropGeometry) -> Unit = {},
    onCrossPageDragCancel: (String) -> Unit = {},
    crossPageDragCancelToken: Int = 0,
    onOpenWidgetSettings: (TileEntity) -> Unit = {},
    onPinNewTile: (TileEntity) -> Unit,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit = {},
    onOpenGlance: () -> Unit = {},
    installedApps: List<AppItem> = emptyList(),
    installedWidgets: List<WidgetProviderItem> = emptyList(),
    onPickAndBindWidget: (WidgetProviderItem, String, String?) -> Unit = { _, _, _ -> },
    onRequestRebindWidget: (TileEntity) -> Unit = {},
    onOpenSmartFolder: (TileEntity, Float, Float) -> Unit = { _, _, _ -> },
    onLaunchSmartFolderItem: (SmartFolderItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent = LumiaPalette.resolveEffectiveAccent(settings)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val widgetHostManager = remember(context) { LumiaWidgetHostManager.getInstance(context) }

    var tileToMoveAcrossPages by remember { mutableStateOf<TileEntity?>(null) }

    var draggingTileId by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragVelX by remember { mutableFloatStateOf(0f) }
    var dragVelY by remember { mutableFloatStateOf(0f) }
    var dragStartFingerX by remember { mutableFloatStateOf(0f) }
    var dragStartFingerY by remember { mutableFloatStateOf(0f) }

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
        var gridViewportRoot by remember(currentPage.id) { mutableStateOf(Offset.Unspecified) }
        val latestGeometryCallback by rememberUpdatedState(onCrossPageGridGeometry)
        LaunchedEffect(currentPage.id, gridViewportRoot, scrollState.value, totalCols, cellStepPx) {
            if (gridViewportRoot != Offset.Unspecified) {
                latestGeometryCallback(
                    HomePageDropGeometry(
                        currentPage.id,
                        gridViewportRoot.x, gridViewportRoot.y, scrollState.value.toFloat(),
                        with(density) { horizontalPadding.toPx() },
                        with(density) { topPadding.toPx() }, cellStepPx, totalCols
                    )
                )
            }
        }

        fun reportCrossPageDrag(tileId: String) {
            // Frozen source-root finger origin survives pager composition/translation.
            onCrossPageDragMove(tileId, dragStartFingerX + dragOffsetX, dragStartFingerY + dragOffsetY)
        }

        // Resolve deterministic 2D coordinates for each tile without auto-collapsing empty spaces
        val positionedTiles = remember(tiles, totalCols) {
            val placed = mutableListOf<PositionedTile>()
            tiles.forEach { tile ->
                val spanX = tile.effectiveSpanX(totalCols)
                val spanY = tile.effectiveSpanY()
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
                visible = selectedTileOnThisPage?.hasBoundOrLinkedWidget() == true && !isEditMode,
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
                    // Capability may change after provider removal/rebind without a tile ID change.
                    val canConfigureWidget = remember(activeTile.id, activeTile.appWidgetId,
                        activeTile.widgetProvider) { widgetHostManager.canReconfigureTile(activeTile) }
                    if (canConfigureWidget && !settings.lockHomeLayout) {
                        SingleTileContextualBar(
                            tile = activeTile,
                            displayName = tileDisplayName(activeTile, installedApps, installedWidgets),
                            accentColor = accent.color,
                            onWidgetSettings = { onOpenWidgetSettings(activeTile) }
                        )
                    }
                }
            }

            if (tiles.isEmpty() && !isEditMode) {
                // Completely blank empty Home page in normal mode (100% pure wallpaper, no text/buttons)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("empty_home_page_${currentPage.id}")
                        .onGloballyPositioned { gridViewportRoot = it.positionInRoot() }
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
                        .onGloballyPositioned { gridViewportRoot = it.positionInRoot() }
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
                            val folderTarget = findManualFolderDropTarget(
                                activeDraggingPositioned, positionedTiles,
                                dragOffsetX, dragOffsetY, cellStepPx)
                            if (folderTarget != null) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = horizontalPadding + cellStepDp * folderTarget.gridX,
                                            y = topPadding + cellStepDp * folderTarget.gridY)
                                        .width(unitSize * folderTarget.spanX + tileSpacing * (folderTarget.spanX - 1))
                                        .height(unitSize * folderTarget.spanY + tileSpacing * (folderTarget.spanY - 1))
                                        .border(3.dp, accent.color, RoundedCornerShape(settings.tileCornerRadiusDp.dp))
                                        .testTag("home_folder_drop_target"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Thả vào thư mục", color = Color.White, fontSize = 11.sp,
                                        modifier = Modifier.background(Color.Black.copy(alpha = 0.75f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }

                        // Live Border-Drag Resize Preview Frame with Spring Morphing & Haptic Step Ticks
                        val activeResizingPositioned = positionedTiles.find { it.tile.id == resizingTileId }
                        if (activeResizingPositioned != null) {
                            val stepThresholdPx = (cellStepPx * 0.6f).coerceAtLeast(1f)
                            val previewSpans by remember(activeResizingPositioned, stepThresholdPx, totalCols) {
                                derivedStateOf {
                                    val deltaCols = (resizeOffsetX / stepThresholdPx).roundToInt()
                                    val deltaRows = (resizeOffsetY / stepThresholdPx).roundToInt()
                                    activeResizingPositioned.tile.constrainedResizeSpans(
                                        activeResizingPositioned.spanX + deltaCols,
                                        activeResizingPositioned.spanY + deltaRows,
                                        totalCols
                                    )
                                }
                            }
                            val previewSpanX = previewSpans.first
                            val previewSpanY = previewSpans.second
                            val previewGridX = activeResizingPositioned.gridX
                                .coerceIn(0, (totalCols - previewSpanX).coerceAtLeast(0))
                            val previewGridY = activeResizingPositioned.gridY.coerceAtLeast(0)

                            var lastPreviewSize by remember(activeResizingPositioned.tile.id) {
                                mutableStateOf(activeResizingPositioned.spanX to activeResizingPositioned.spanY)
                            }
                            LaunchedEffect(previewSpans) {
                                if (previewSpans != lastPreviewSize) {
                                    lastPreviewSize = previewSpans
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
                                    text = "${previewSpanX}×${previewSpanY}",
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
                                                    Modifier.pointerInput(tile.id, positioned.gridX, positioned.gridY, totalCols, crossPageDragCancelToken) {
                                                        detectDragGestures(
                                                            onDragStart = { offset ->
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                draggingTileId = tile.id
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                                dragStartFingerX = gridViewportRoot.x + with(density) { baseTileX.toPx() } + offset.x
                                                                dragStartFingerY = gridViewportRoot.y + with(density) { baseTileY.toPx() } + offset.y - scrollState.value
                                                                onCrossPageDragStart(
                                                                    tile.id, dragStartFingerX, dragStartFingerY, offset.x, offset.y
                                                                )
                                                            },
                                                            onDragEnd = {
                                                                val folderTarget = findManualFolderDropTarget(
                                                                    positioned, positionedTiles, dragOffsetX, dragOffsetY, cellStepPx)
                                                                val targetX = (positioned.gridX + (dragOffsetX / cellStepPx).roundToInt())
                                                                    .coerceIn(0, (totalCols - positioned.spanX).coerceAtLeast(0))
                                                                val targetY = (positioned.gridY + (dragOffsetY / cellStepPx).roundToInt())
                                                                    .coerceIn(0, 60)
                                                                draggingTileId = null
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                                if (!onCrossPageDragEnd(tile.id)) {
                                                                    if (folderTarget != null) onGroupTilesManually(tile.id, folderTarget.tile.id)
                                                                    else onMoveTileToGrid(tile.id, targetX, targetY, totalCols)
                                                                }
                                                            },
                                                            onDragCancel = {
                                                                onCrossPageDragCancel(tile.id)
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
                                                                reportCrossPageDrag(tile.id)
                                                            }
                                                        )
                                                    }
                                                } else if (selectedTileId == null) {
                                                    // Long-press on a tile when NO tile is currently being edited: selects ONLY this 1 tile AND allows immediate free 2D drag
                                                    Modifier.pointerInput(tile.id, positioned.gridX, positioned.gridY, totalCols, crossPageDragCancelToken) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = { offset ->
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                // Defer selection until finger-up: changing pointer modifiers here cancels the gesture.
                                                                draggingTileId = tile.id
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragVelX = 0f
                                                                dragVelY = 0f
                                                                dragStartFingerX = gridViewportRoot.x + with(density) { baseTileX.toPx() } + offset.x
                                                                dragStartFingerY = gridViewportRoot.y + with(density) { baseTileY.toPx() } + offset.y - scrollState.value
                                                                onCrossPageDragStart(
                                                                    tile.id, dragStartFingerX, dragStartFingerY, offset.x, offset.y
                                                                )
                                                            },
                                                            onDragEnd = {
                                                                val folderTarget = findManualFolderDropTarget(
                                                                    positioned, positionedTiles, dragOffsetX, dragOffsetY, cellStepPx)
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
                                                                val movedAcrossPage = onCrossPageDragEnd(tile.id)
                                                                if (!movedAcrossPage) {
                                                                    if (folderTarget != null) {
                                                                        onGroupTilesManually(tile.id, folderTarget.tile.id)
                                                                    } else if (deltaCellsX != 0 || deltaCellsY != 0) {
                                                                        onMoveTileToGrid(tile.id, targetX, targetY, totalCols)
                                                                    }
                                                                }
                                                                onSelectTileForEdit(if (movedAcrossPage) null else tile.id)
                                                            },
                                                            onDragCancel = {
                                                                onCrossPageDragCancel(tile.id)
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
                                                                reportCrossPageDrag(tile.id)
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
                                            val (newSpanX, newSpanY) = tile.constrainedResizeSpans(
                                                positioned.spanX + deltaCols,
                                                positioned.spanY + deltaRows,
                                                totalCols
                                            )
                                            resizingTileId = null
                                            resizeOffsetX = 0f
                                            resizeOffsetY = 0f
                                            if (newSpanX != positioned.spanX || newSpanY != positioned.spanY) {
                                                onSetTileSpans(tile.id, newSpanX, newSpanY)
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
                                        onFinishSingleEdit = { onSelectTileForEdit(null) },
                                        onRequestBindWidgetPermission = { targetTile ->
                                            onRequestRebindWidget(targetTile)
                                        },
                                        onReplaceMissingWidget = { targetTile ->
                                            onRequestRebindWidget(targetTile)
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
                        text = "Chọn trang cho \"${tileDisplayName(tile, installedApps, installedWidgets)}\":",
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
            val activePickerTab = 0
            var pickerSearchQuery by remember { mutableStateOf("") }
            var expandedWidgetApp by remember { mutableStateOf<String?>(null) }
            var selectedNewAppTileSize by remember { mutableStateOf(TileSize.MEDIUM) }

            val groupedWidgetApps = remember(installedWidgets, pickerSearchQuery) {
                WidgetPickerGrouping.group(installedWidgets, pickerSearchQuery)
            }
            val filteredAppsForTile = remember(installedApps, pickerSearchQuery) {
                val q = pickerSearchQuery.trim().lowercase(java.util.Locale.ROOT)
                installedApps.filter { q.isEmpty() || it.label.lowercase(java.util.Locale.ROOT).contains(q) ||
                    it.packageName.lowercase(java.util.Locale.ROOT).contains(q) }
            }
            val pickerMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
            ModalBottomSheet(
                onDismissRequest = onDismissAddTileSheet,
                sheetState = sheetState,
                containerColor = Color(0xFF17171B),
                contentColor = Color.White
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = pickerMaxHeight)
                        .testTag("add_tile_picker_list"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "picker_header", contentType = "header") {
                        Column {
                    Text("Thêm ô", color = Color.White, fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activePickerTab == 0) {
                    // Search bar for both Widgets and Apps
                    OutlinedTextField(
                        value = pickerSearchQuery,
                        onValueChange = { pickerSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "Tìm ứng dụng hoặc widget",
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

                    }
                        }
                    }
                    if (activePickerTab == 0) {
                        item(key = "widget_heading", contentType = "header") {
                            Column {
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
                            text = "Widget theo ứng dụng",
                            color = accent.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                            }
                        }
                        if (groupedWidgetApps.isEmpty()) {
                            item(key = "widget_empty", contentType = "empty") {
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
                                    "Chưa có widget từ ứng dụng đã cài."
                                } else {
                                    "Không tìm thấy widget phù hợp."
                                },
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp
                            )
                        }
                            }
                        } else {
                            groupedWidgetApps.forEach { (packageName, widgets) ->
                                val searching = pickerSearchQuery.isNotBlank()
                                val expanded = searching || expandedWidgetApp == packageName
                                item(key = "widget_app:$packageName", contentType = "widget_app") {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.07f))
                                        .semantics { stateDescription = if (expanded) "Đã mở" else "Đã thu gọn" }
                                        .clickable(enabled = !searching, role = Role.Button) {
                                            expandedWidgetApp = if (expandedWidgetApp == packageName) null else packageName
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                        .testTag("widget_app_group_$packageName"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val appIcon = rememberAppIcon(packageName)
                                    if (appIcon != null) {
                                        androidx.compose.foundation.Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(32.dp))
                                    } else {
                                        Icon(Icons.Default.Widgets, contentDescription = null, tint = accent.color)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(widgets.first().appName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${widgets.size} widget", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                    if (!searching) {
                                        Text(if (expanded) "Thu gọn" else "Xem", color = accent.color, fontSize = 12.sp)
                                    }
                                }
                                }
                                if (expanded) {
                                    items(widgets, key = { "widget:${it.providerComponent}" }, contentType = { "widget" }) { wItem ->
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
                                                text = "Tối thiểu ${wItem.targetCellWidth}×${wItem.targetCellHeight} • $resizeLabel",
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
                                            text = "Ghim",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                    }
                                }
                            }
                        }
                        item(key = "app_controls", contentType = "controls") {
                            Column {
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

                            }
                        }
                        items(filteredAppsForTile, key = { "app:${it.packageName}" }, contentType = { "app" }) { app ->
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
                    }
                    item(key = "bottom_space") { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SingleTileContextualBar(
    tile: TileEntity,
    displayName: String,
    accentColor: Color,
    onWidgetSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C22).copy(alpha = 0.94f))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("single_tile_contextual_bar"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Kéo viền để đổi cỡ",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                    SingleTilePillAction(
                        icon = Icons.Default.Settings,
                        label = "Cài đặt widget",
                        bgColor = Color.White.copy(alpha = 0.12f),
                        onClick = onWidgetSettings,
                        testTag = "contextual_bar_widget_settings"
                    )
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
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}


