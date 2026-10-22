package com.example.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiaStartGrid(
    currentPage: LauncherPage,
    allHomePages: List<LauncherPage>,
    tiles: List<TileEntity>,
    settings: LauncherSettings,
    telemetry: SystemTelemetry,
    isEditMode: Boolean,
    showAddTileSheet: Boolean,
    onDismissAddTileSheet: () -> Unit,
    onOpenAddTileSheet: () -> Unit,
    onTileClick: (TileEntity) -> Unit,
    onTileLongClick: (TileEntity) -> Unit,
    onResizeTile: (String) -> Unit,
    onUnpinTile: (String) -> Unit,
    onMoveTile: (String, Boolean) -> Unit,
    onReorderTiles: (Int, Int) -> Unit,
    onMoveTileToPage: (String, String) -> Unit,
    onCustomizeTile: (String, String?, String, Boolean) -> Unit,
    onPinNewTile: (TileEntity) -> Unit,
    onEnterEditMode: () -> Unit,
    onOpenPageManager: () -> Unit,
    onOpenGlance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent = LumiaPalette.getAccent(settings.accentColorHex)
    val gridState = rememberLazyGridState()
    var tileToMoveAcrossPages by remember { mutableStateOf<TileEntity?>(null) }
    var tileToCustomize by remember { mutableStateOf<TileEntity?>(null) }

    var draggingTileIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isEditMode, settings.doubleTapToGlance) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapToGlance && !isEditMode) {
                            onOpenGlance()
                        }
                    },
                    onLongPress = {
                        onEnterEditMode()
                    }
                )
            }
    ) {
        val screenWidth = maxWidth

        val totalCols = remember(screenWidth, settings.showMoreTiles) {
            when {
                screenWidth >= 840.dp -> 8
                screenWidth >= 600.dp -> 6
                settings.showMoreTiles -> 6
                else -> 4
            }
        }

        val horizontalPadding = 12.dp
        val tileSpacing = settings.tileSpacingDp.coerceIn(2, 16).dp
        val availableContentWidth = screenWidth - (horizontalPadding * 2)
        val unitSize = (availableContentWidth - (tileSpacing * (totalCols - 1))) / totalCols

        Column(modifier = Modifier.fillMaxSize()) {
            // Optional subtle page title if user enabled it in Page Manager
            if (settings.showPageHeaderTitle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(isEditMode, settings.doubleTapToGlance) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (settings.doubleTapToGlance && !isEditMode) onOpenGlance()
                                },
                                onLongPress = { onEnterEditMode() }
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

            if (tiles.isEmpty()) {
                // Empty Home Page Placeholder - long press anywhere enters Edit/Settings Mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(isEditMode, settings.doubleTapToGlance) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (settings.doubleTapToGlance && !isEditMode) onOpenGlance()
                                },
                                onLongPress = { onEnterEditMode() }
                            )
                        }
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${currentPage.title} đang trống",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isEditMode)
                                "Chọn tác vụ trên thanh cài đặt bên dưới hoặc nhấn nút để ghim tiện ích Live Tile."
                            else
                                "Nhấn giữ màn hình để mở chế độ cài đặt & ghim tiện ích Live Tile.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                        if (isEditMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onOpenAddTileSheet,
                                    colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ghim tiện ích Live Tile")
                                }
                                Button(
                                    onClick = onOpenPageManager,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                                ) {
                                    Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Quản lý trang")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(totalCols),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 10.dp,
                        bottom = if (isEditMode) 115.dp else 44.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                    verticalArrangement = Arrangement.spacedBy(tileSpacing),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("tiles_grid_${currentPage.id}")
                ) {
                    itemsIndexed(
                        items = tiles,
                        key = { _, tile -> tile.id },
                        span = { _, tile ->
                            val span = when (tile.size) {
                                TileSize.SMALL -> 1
                                TileSize.MEDIUM -> 2
                                TileSize.WIDE -> if (totalCols >= 6) (if (totalCols >= 8) 4 else 6) else 4
                                TileSize.LARGE -> if (totalCols >= 6) (if (totalCols >= 8) 4 else 6) else 4
                            }
                            GridItemSpan(span.coerceAtMost(totalCols))
                        }
                    ) { index, tile ->
                        val tileHeight = when (tile.size) {
                            TileSize.SMALL -> unitSize
                            TileSize.MEDIUM -> unitSize * 2 + tileSpacing
                            TileSize.WIDE -> unitSize * 2 + tileSpacing
                            TileSize.LARGE -> (unitSize * 2 + tileSpacing) * 2 + tileSpacing
                        }

                        val isDragging = draggingTileIndex == index

                        Box(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = spring(stiffness = 500f)
                                )
                                .zIndex(if (isDragging) 10f else 1f)
                                .then(
                                    if (isDragging) {
                                        Modifier
                                            .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                                            .shadow(16.dp, RoundedCornerShape(settings.tileCornerRadiusDp.dp))
                                            .graphicsLayer {
                                                scaleX = 1.05f
                                                scaleY = 1.05f
                                                alpha = 0.92f
                                            }
                                    } else Modifier
                                )
                                .then(
                                    if (isEditMode && !settings.lockHomeLayout) {
                                        Modifier.pointerInput(index, tiles.size) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingTileIndex = index
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                },
                                                onDragEnd = {
                                                    draggingTileIndex = null
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingTileIndex = null
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetX += dragAmount.x
                                                    dragOffsetY += dragAmount.y

                                                    val thresholdY = tileHeight.toPx() * 0.75f
                                                    if (dragOffsetY > thresholdY && index < tiles.size - 1) {
                                                        onReorderTiles(index, index + 1)
                                                        draggingTileIndex = index + 1
                                                        dragOffsetY = 0f
                                                    } else if (dragOffsetY < -thresholdY && index > 0) {
                                                        onReorderTiles(index, index - 1)
                                                        draggingTileIndex = index - 1
                                                        dragOffsetY = 0f
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            TileView(
                                tile = tile,
                                settings = settings,
                                telemetry = telemetry,
                                isEditMode = isEditMode,
                                onClick = { onTileClick(tile) },
                                onLongClick = { onTileLongClick(tile) },
                                onResize = { onResizeTile(tile.id) },
                                onUnpin = { onUnpinTile(tile.id) },
                                onMovePrev = { onMoveTile(tile.id, true) },
                                onMoveNext = { onMoveTile(tile.id, false) },
                                onMoveToPage = if (allHomePages.size > 1) {
                                    { tileToMoveAcrossPages = tile }
                                } else null,
                                onCustomizeTile = { tileToCustomize = tile },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(tileHeight)
                            )
                        }
                    }

                    // Blank bottom area that responds to long-press to enter Edit Mode & double-tap for Glance
                    item(span = { GridItemSpan(totalCols) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .pointerInput(isEditMode, settings.doubleTapToGlance) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (settings.doubleTapToGlance && !isEditMode) {
                                                onOpenGlance()
                                            }
                                        },
                                        onLongPress = {
                                            onEnterEditMode()
                                        }
                                    )
                                }
                        )
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
                containerColor = Color(0xFF1A1A1A),
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
                        onValueChange = { customLabel = it },
                        label = { Text("Tên hiển thị trên ô Tile", color = Color.White.copy(alpha = 0.7f)) },
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
                        text = "Màu sắc riêng cho ô này (Lumia / One UI / MIUI):",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

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
                                    .clickable { selectedColorHex = acc.hex },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedColorHex = null
                                onCustomizeTile(tile.id, null, customLabel, tile.isLiveEnabled)
                                tileToCustomize = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dùng màu chủ đề", color = Color.White)
                        }
                        Button(
                            onClick = {
                                onCustomizeTile(tile.id, selectedColorHex, customLabel, tile.isLiveEnabled)
                                tileToCustomize = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                        ) {
                            Text("Lưu thay đổi")
                        }
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
                containerColor = Color(0xFF1E1E1E),
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) accent.color.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .clickable(enabled = !isCurrent) {
                                    onMoveTileToPage(tile.id, targetPage.id)
                                    tileToMoveAcrossPages = null
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
                containerColor = Color(0xFF1E1E1E),
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
                        text = "Chọn tiện ích Live Tile để ghim vào trang này",
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
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val newTile = TileEntity(
                                        id = "tile_${type.name.lowercase()}_${System.currentTimeMillis()}",
                                        title = label,
                                        tileType = type,
                                        size = size,
                                        orderIndex = tiles.size,
                                        pageId = currentPage.id,
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
                                    .clip(RoundedCornerShape(6.dp))
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
