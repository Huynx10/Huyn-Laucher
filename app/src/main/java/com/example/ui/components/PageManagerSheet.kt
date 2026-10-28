package com.example.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.PageType
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.Wp10TileStyle
import com.example.ui.theme.LumiaPalette
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerSheet(
    pages: List<LauncherPage>,
    tileCountsByPage: Map<String, Int>,
    settings: LauncherSettings,
    onAddHomePage: (String) -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onRenamePage: (String, String) -> Unit,
    onDeleteHomePage: (String) -> Unit,
    onSetDefaultHomePage: (String) -> Unit,
    onToggleShowHeaderTitle: (Boolean) -> Unit,
    onJumpToPage: (Int) -> Unit,
    onDismiss: () -> Unit,
    tiles: List<TileEntity> = emptyList(),
    currentPageIndex: Int = 0
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = LumiaPalette.getAccent(settings.accentColorHex)
    val wallpaperBrush = LumiaPalette.Wallpapers.getOrNull(settings.wallpaperGradientIndex)?.second
        ?: LumiaPalette.Wallpapers.first().second

    var newPageName by remember { mutableStateOf("") }
    var editingPageId by remember { mutableStateOf<String?>(null) }
    var editingPageTitle by remember { mutableStateOf("") }
    var showCustomNameInput by remember { mutableStateOf(false) }

    var draggingPageIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val cardStepPx = with(density) { 146.dp.toPx() }

    val homePagesCount = pages.count { it.pageType == PageType.HOME }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        contentColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LUMIA LAUNCHER",
                            color = accent.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Quản lý trang",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nhấn giữ để đổi vị trí trang. Chạm biểu tượng nhà để chọn trang chính.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Visual Mini-Screen Carousel Overview (Samsung One UI 9.0 & Xiaomi MIUI / HyperOS style)
            item {
                LazyRow(
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("page_preview_carousel")
                ) {
                    itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                        val isHome = page.pageType == PageType.HOME
                        val isDefaultHome = isHome && page.id == settings.defaultHomePageId
                        val isCurrentActive = index == currentPageIndex
                        val isDragging = draggingPageIndex == index
                        val pageTiles = remember(tiles, page.id) {
                            tiles.filter { it.pageId == page.id }.sortedBy { it.orderIndex }
                        }

                        Box(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = spring(stiffness = 480f)
                                )
                                .zIndex(if (isDragging) 12f else 1f)
                                .then(
                                    if (isDragging) {
                                        Modifier
                                            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                                            .shadow(18.dp, RoundedCornerShape(16.dp))
                                            .graphicsLayer {
                                                scaleX = 1.06f
                                                scaleY = 1.06f
                                            }
                                    } else Modifier
                                )
                                .pointerInput(index, pages.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingPageIndex = index
                                            dragOffsetX = 0f
                                        },
                                        onDragEnd = {
                                            draggingPageIndex = null
                                            dragOffsetX = 0f
                                        },
                                        onDragCancel = {
                                            draggingPageIndex = null
                                            dragOffsetX = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetX += dragAmount.x
                                            if (dragOffsetX > cardStepPx * 0.65f && index < pages.size - 1) {
                                                onMovePage(index, index + 1)
                                                draggingPageIndex = index + 1
                                                dragOffsetX = 0f
                                            } else if (dragOffsetX < -cardStepPx * 0.65f && index > 0) {
                                                onMovePage(index, index - 1)
                                                draggingPageIndex = index - 1
                                                dragOffsetX = 0f
                                            }
                                        }
                                    )
                                }
                        ) {
                            MiniScreenPreviewCard(
                                index = index,
                                totalPages = pages.size,
                                page = page,
                                pageTiles = pageTiles,
                                isDefaultHome = isDefaultHome,
                                isCurrentActive = isCurrentActive,
                                canDelete = isHome && homePagesCount > 1,
                                accentColor = accent.color,
                                wallpaperBrush = wallpaperBrush,
                                settings = settings,
                                onSelectPage = {
                                    onJumpToPage(index)
                                    onDismiss()
                                },
                                onSetDefault = {
                                    if (isHome) onSetDefaultHomePage(page.id)
                                },
                                onMoveLeft = {
                                    if (index > 0) onMovePage(index, index - 1)
                                },
                                onMoveRight = {
                                    if (index < pages.size - 1) onMovePage(index, index + 1)
                                },
                                onDelete = {
                                    if (isHome && homePagesCount > 1) onDeleteHomePage(page.id)
                                }
                            )
                        }
                    }

                    // One UI 9.0 / MIUI "+ Thêm trang trống" Mini-Screen Card at the end of carousel
                    item(key = "add_blank_home_page_card") {
                        Box(
                            modifier = Modifier
                                .width(136.dp)
                                .height(228.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(
                                    width = 1.5.dp,
                                    color = accent.color.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onAddHomePage("Trang Home ${homePagesCount + 1}")
                                }
                                .testTag("quick_add_blank_page_card"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accent.color.copy(alpha = 0.22f))
                                        .border(1.dp, accent.color, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Thêm trang trống",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "+ Trang trống",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Chạm 1 lần để thêm ngay kiểu One UI",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Sort Ergonomic Presets (One UI 9.0 / MIUI / Lumia)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "THỨ TỰ TRANG",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Preset 1: Standard Lumia / Windows Phone (Default Home at 0, All Apps at the end)
                        QuickSortPresetChip(
                            label = "Ứng dụng ở cuối",
                            accentColor = accent.color,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val allAppsIdx = pages.indexOfFirst { it.pageType == PageType.ALL_APPS }
                                if (allAppsIdx >= 0 && allAppsIdx != pages.size - 1) {
                                    onMovePage(allAppsIdx, pages.size - 1)
                                }
                            }
                        )

                        // Preset 2: Center Default Home (One UI 9.0 style when multiple pages exist)
                        QuickSortPresetChip(
                            label = "Trang chính ở giữa",
                            accentColor = accent.color,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val defIdx = pages.indexOfFirst { it.id == settings.defaultHomePageId }
                                val targetMid = (pages.size / 2).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                                if (defIdx >= 0 && defIdx != targetMid) {
                                    onMovePage(defIdx, targetMid)
                                }
                            }
                        )

                        // Preset 3: All Apps on far left (index 0)
                        QuickSortPresetChip(
                            label = "Ứng dụng ở đầu",
                            accentColor = accent.color,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val allAppsIdx = pages.indexOfFirst { it.pageType == PageType.ALL_APPS }
                                if (allAppsIdx > 0) {
                                    onMovePage(allAppsIdx, 0)
                                }
                            }
                        )
                    }
                }
            }

            // Fast Add Blank Page + Optional Custom Name Input
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onAddHomePage("Trang Home ${homePagesCount + 1}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.color),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_home_page_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thêm trang mới", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showCustomNameInput = !showCustomNameInput },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Đặt tên riêng", modifier = Modifier.size(16.dp))
                    }
                }

                if (showCustomNameInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPageName,
                            onValueChange = { newPageName = it },
                            placeholder = {
                                Text(
                                    text = "Tên trang",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accent.color,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_home_page_input")
                        )
                        Button(
                            onClick = {
                                val title = newPageName.trim().ifEmpty { "Trang Home ${homePagesCount + 1}" }
                                onAddHomePage(title)
                                newPageName = ""
                                showCustomNameInput = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                        ) {
                            Text("Tạo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Toggle whether to show subtle page title on Home pages (never shown on empty pages)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hiển thị tiêu đề tên trang trên Home",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Trang Home trống luôn tự động để trống hoàn toàn 100%",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.showPageHeaderTitle,
                        onCheckedChange = onToggleShowHeaderTitle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent.color
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            }

            // Detailed Page Order List with Left / Right / First / Last controls & Rename
            item {
                Text(
                    text = "THỨ TỰ TRANG",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            itemsIndexed(pages, key = { _, p -> "list_${p.id}" }) { index, page ->
                val isHome = page.pageType == PageType.HOME
                val isDefaultHome = isHome && page.id == settings.defaultHomePageId
                val tileCount = tileCountsByPage[page.id] ?: 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(
                            width = if (isDefaultHome) 1.5.dp else 1.dp,
                            color = if (isDefaultHome) accent.color else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home / All Apps badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDefaultHome) accent.color else Color.White.copy(alpha = 0.14f))
                                .clickable {
                                    onJumpToPage(index)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isHome) Icons.Default.Home else Icons.Default.Apps,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onJumpToPage(index)
                                    onDismiss()
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Trang ${index + 1}: ${page.title}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isDefaultHome) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Trang chủ",
                                        color = accent.color,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(accent.color.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = when {
                                    !isHome -> "Danh sách tất cả ứng dụng • Vuốt ngang để truy cập"
                                    tileCount == 0 -> "Trang Home trống hoàn toàn • Chạm để mở"
                                    else -> "$tileCount ô Live Tile • Chạm để mở"
                                },
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp
                            )
                        }

                        // Move to First (<<)
                        IconButton(
                            onClick = { if (index > 0) onMovePage(index, 0) },
                            enabled = index > 0,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FirstPage,
                                contentDescription = "Đưa lên trang đầu tiên",
                                tint = if (index > 0) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Move Left (<)
                        IconButton(
                            onClick = { if (index > 0) onMovePage(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Sang trái",
                                tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Move Right (>)
                        IconButton(
                            onClick = { if (index < pages.size - 1) onMovePage(index, index + 1) },
                            enabled = index < pages.size - 1,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Sang phải",
                                tint = if (index < pages.size - 1) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Move to Last (>>)
                        IconButton(
                            onClick = { if (index < pages.size - 1) onMovePage(index, pages.size - 1) },
                            enabled = index < pages.size - 1,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LastPage,
                                contentDescription = "Đưa xuống trang cuối",
                                tint = if (index < pages.size - 1) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Rename button
                        IconButton(
                            onClick = {
                                if (editingPageId == page.id) {
                                    editingPageId = null
                                } else {
                                    editingPageId = page.id
                                    editingPageTitle = page.title
                                }
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Đổi tên trang",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete button (only if Home page and > 1 Home page)
                        if (isHome && homePagesCount > 1) {
                            IconButton(
                                onClick = { onDeleteHomePage(page.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Xóa trang Home",
                                    tint = Color(0xFFE81123),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (editingPageId == page.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editingPageTitle,
                                onValueChange = { editingPageTitle = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (editingPageTitle.isNotBlank()) {
                                        onRenamePage(page.id, editingPageTitle.trim())
                                    }
                                    editingPageId = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent.color)
                            ) {
                                Text("Lưu")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MiniScreenPreviewCard(
    index: Int,
    totalPages: Int,
    page: LauncherPage,
    pageTiles: List<TileEntity>,
    isDefaultHome: Boolean,
    isCurrentActive: Boolean,
    canDelete: Boolean,
    accentColor: Color,
    wallpaperBrush: androidx.compose.ui.graphics.Brush,
    settings: LauncherSettings,
    onSelectPage: () -> Unit,
    onSetDefault: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit
) {
    val isHome = page.pageType == PageType.HOME

    Column(
        modifier = Modifier
            .width(136.dp)
            .height(228.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .border(
                width = when {
                    isDefaultHome -> 2.dp
                    isCurrentActive -> 1.5.dp
                    else -> 1.dp
                },
                color = when {
                    isDefaultHome -> accentColor
                    isCurrentActive -> Color.White.copy(alpha = 0.65f)
                    else -> Color.White.copy(alpha = 0.16f)
                },
                shape = RoundedCornerShape(16.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar (Samsung One UI 9.0 Default Home Crown/House toggle + Delete X)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDefaultHome) accentColor.copy(alpha = 0.28f)
                    else Color.Black.copy(alpha = 0.45f)
                )
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isHome) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSetDefault() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Đặt làm Trang chủ mặc định",
                        tint = if (isDefaultHome) accentColor else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isDefaultHome) "Trang chủ" else "Đặt chính",
                        color = if (isDefaultHome) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = if (isDefaultHome) FontWeight.Bold else FontWeight.Normal
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "App List",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (canDelete) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa trang",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        // Center Miniature Phone Screen Preview (tappable to jump to page)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(wallpaperBrush)
                .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .clickable { onSelectPage() }
                .padding(6.dp)
        ) {
            if (isHome) {
                if (pageTiles.isNotEmpty()) {
                    // Render miniature Live Tiles preview matching W10M transparency & colors
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val previewTiles = pageTiles.take(7)
                        var idx = 0
                        while (idx < previewTiles.size) {
                            val t = previewTiles[idx]
                            val isClear = settings.wp10TileStyle == Wp10TileStyle.CRYSTAL_CLEAR ||
                                settings.tileOpacity <= 0.03f
                            val baseColor = accentColor
                            val fillAlpha = when {
                                isClear -> 0.06f
                                settings.wp10TileStyle == Wp10TileStyle.FLUENT_ACRYLIC -> (settings.tileOpacity * 0.45f).coerceIn(0.12f, 0.65f)
                                else -> settings.tileOpacity.coerceIn(0.15f, 0.95f)
                            }

                            if (t.size == TileSize.WIDE || t.size == TileSize.LARGE) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (t.size == TileSize.LARGE) 34.dp else 20.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(baseColor.copy(alpha = fillAlpha))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                                )
                                idx += 1
                            } else {
                                // Pair two medium/small tiles side by side in the mini preview
                                val nextTile = previewTiles.getOrNull(idx + 1)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(baseColor.copy(alpha = fillAlpha))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                                    )
                                    if (nextTile != null && nextTile.size != TileSize.WIDE && nextTile.size != TileSize.LARGE) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(18.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(accentColor.copy(alpha = fillAlpha))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                                        )
                                        idx += 2
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                        idx += 1
                                    }
                                }
                            }
                        }
                    }
                }
                // Note: When pageTiles is empty, leave the mini-screen preview 100% blank wallpaper!
            } else {
                // Miniature All Apps list preview
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    repeat(5) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(accentColor.copy(alpha = 0.85f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }

        // Bottom Bar inside Mini-Screen Card: Page Title + Left (<) / Right (>) Swap Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${index + 1}. ${page.title}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (index > 0) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable(enabled = index > 0) { onMoveLeft() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Sang trái",
                        tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Kéo để đổi vị trí",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(13.dp)
                )

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (index < totalPages - 1) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable(enabled = index < totalPages - 1) { onMoveRight() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Sang phải",
                        tint = if (index < totalPages - 1) Color.White else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSortPresetChip(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
