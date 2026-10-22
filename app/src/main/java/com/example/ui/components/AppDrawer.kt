package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.AllAppsSortOrder
import com.example.data.model.AllAppsViewMode
import com.example.data.model.AppItem
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.ui.theme.LumiaPalette
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppItem>,
    homePages: List<LauncherPage>,
    settings: LauncherSettings,
    isEditMode: Boolean = false,
    onLaunchApp: (String) -> Unit,
    onPinAppToPage: (AppItem, String) -> Unit,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onOpenPageManager: () -> Unit,
    onEnterEditMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = LumiaPalette.getAccent(settings.accentColorHex)
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PINNED, USER, SYSTEM
    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }
    var showJumpList by remember { mutableStateOf(false) }
    var showCustomizeSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Filter and sort apps according to custom settings
    val processedApps = remember(apps, searchQuery, selectedFilter, settings.allAppsSortOrder) {
        val filtered = apps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedFilter) {
                "PINNED" -> app.isPinned
                "USER" -> !app.isSystemApp
                "SYSTEM" -> app.isSystemApp
                else -> true
            }
            matchesQuery && matchesCategory
        }

        when (settings.allAppsSortOrder) {
            AllAppsSortOrder.NAME_ASC -> filtered.sortedBy { it.label.lowercase(Locale.getDefault()) }
            AllAppsSortOrder.NAME_DESC -> filtered.sortedByDescending { it.label.lowercase(Locale.getDefault()) }
            AllAppsSortOrder.USER_FIRST -> filtered.sortedWith(
                compareBy<AppItem> { it.isSystemApp }.thenBy { it.label.lowercase(Locale.getDefault()) }
            )
        }
    }

    val availableLetters = remember(processedApps) {
        processedApps.map { app ->
            val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar in 'A'..'Z') firstChar else '#'
        }.toSet()
    }

    val letterIndexMap = remember(processedApps) {
        val map = mutableMapOf<Char, Int>()
        processedApps.forEachIndexed { index, app ->
            val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar in 'A'..'Z') firstChar else '#'
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        map
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onLongPress = {
                        if (!isEditMode) onEnterEditMode()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Optional Search Bar
            if (settings.allAppsShowSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Tìm kiếm trong ${apps.size} ứng dụng...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accent.color,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Filter Chips & Customization Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val filters = listOf(
                        "ALL" to "Tất cả (${apps.size})",
                        "PINNED" to "Đã ghim",
                        "USER" to "Cá nhân",
                        "SYSTEM" to "Hệ thống"
                    )
                    items(filters) { (key, label) ->
                        val active = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) accent.color else Color.White.copy(alpha = 0.09f))
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (settings.allAppsViewMode == AllAppsViewMode.METRO_LIST) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { showJumpList = true }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "A-Z",
                                color = accent.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isEditMode) {
                        // Quick toggle between List and Grid (shown inside Edit/Settings mode)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    val nextMode = if (settings.allAppsViewMode == AllAppsViewMode.METRO_LIST)
                                        AllAppsViewMode.ONE_UI_GRID else AllAppsViewMode.METRO_LIST
                                    onUpdateSettings(settings.copy(allAppsViewMode = nextMode))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.allAppsViewMode == AllAppsViewMode.METRO_LIST)
                                    Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Đổi kiểu xem",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Customize All Apps page button (shown inside Edit/Settings mode)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                                .clickable { showCustomizeSheet = true }
                                .testTag("customize_all_apps_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Tuỳ chỉnh trang ứng dụng",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content: Metro List or One UI Grid
            if (settings.allAppsViewMode == AllAppsViewMode.METRO_LIST) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(processedApps, key = { _, item -> item.packageName }) { index, app ->
                        val currentChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
                        val currentGroupChar = if (currentChar in 'A'..'Z') currentChar else '#'
                        val prevChar = if (index > 0) {
                            val p = processedApps[index - 1].label.firstOrNull()?.uppercaseChar() ?: '#'
                            if (p in 'A'..'Z') p else '#'
                        } else null

                        val showHeader = settings.allAppsShowLetterHeaders &&
                            settings.allAppsSortOrder != AllAppsSortOrder.USER_FIRST &&
                            prevChar != currentGroupChar

                        if (showHeader) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { showJumpList = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(settings.tileCornerRadiusDp.dp))
                                        .background(accent.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentGroupChar.toString(),
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        AppItemRow(
                            app = app,
                            accentColor = accent.color,
                            cornerRadiusDp = settings.tileCornerRadiusDp,
                            useRealAppIcons = settings.useRealAppIcons,
                            isEditMode = isEditMode,
                            onClick = { onLaunchApp(app.packageName) },
                            onLongClick = {
                                onEnterEditMode()
                                selectedAppForMenu = app
                            }
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .pointerInput(isEditMode) {
                                    detectTapGestures(
                                        onLongPress = {
                                            if (!isEditMode) onEnterEditMode()
                                        }
                                    )
                                }
                        )
                    }
                }
            } else {
                // One UI Grid Mode
                LazyVerticalGrid(
                    columns = GridCells.Fixed(settings.allAppsGridColumns.coerceIn(3, 6)),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 84.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(processedApps, key = { it.packageName }) { app ->
                        AppGridCell(
                            app = app,
                            accentColor = accent.color,
                            cornerRadiusDp = settings.tileCornerRadiusDp,
                            useRealAppIcons = settings.useRealAppIcons,
                            onClick = { onLaunchApp(app.packageName) },
                            onLongClick = {
                                onEnterEditMode()
                                selectedAppForMenu = app
                            }
                        )
                    }
                }
            }
        }

        // Jump List Modal
        if (showJumpList) {
            JumpListModal(
                availableLetters = availableLetters,
                accentHex = settings.accentColorHex,
                onLetterSelected = { letter ->
                    val targetIndex = letterIndexMap[letter]
                    if (targetIndex != null) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                },
                onDismiss = { showJumpList = false }
            )
        }

        // Customize All Apps Sheet
        if (showCustomizeSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showCustomizeSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF181818),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "TUỲ CHỈNH TRANG TẤT CẢ ỨNG DỤNG",
                        color = accent.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // View Mode Selection
                    Text(text = "Kiểu hiển thị", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AllAppsViewMode.entries.forEach { mode ->
                            val selected = settings.allAppsViewMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) accent.color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.07f))
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) accent.color else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onUpdateSettings(settings.copy(allAppsViewMode = mode)) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Grid column selector when in Grid Mode
                    if (settings.allAppsViewMode == AllAppsViewMode.ONE_UI_GRID) {
                        Text(text = "Số cột lưới ứng dụng", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(3, 4, 5).forEach { cols ->
                                val selected = settings.allAppsGridColumns == cols
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) accent.color else Color.White.copy(alpha = 0.08f))
                                        .clickable { onUpdateSettings(settings.copy(allAppsGridColumns = cols)) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$cols cột",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Sort Order
                    Text(text = "Thứ tự sắp xếp", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AllAppsSortOrder.entries.forEach { order ->
                            val selected = settings.allAppsSortOrder == order
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) accent.color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.07f))
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) accent.color else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onUpdateSettings(settings.copy(allAppsSortOrder = order)) }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = order.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Toggles: Search bar & Letter Headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Hiển thị thanh tìm kiếm", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = settings.allAppsShowSearchBar,
                            onCheckedChange = { onUpdateSettings(settings.copy(allAppsShowSearchBar = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent.color)
                        )
                    }

                    if (settings.allAppsViewMode == AllAppsViewMode.METRO_LIST) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Hiển thị chữ cái phân nhóm A-Z", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = settings.allAppsShowLetterHeaders,
                                onCheckedChange = { onUpdateSettings(settings.copy(allAppsShowLetterHeaders = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent.color)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Button to open Page Manager to reorder All Apps page position
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                showCustomizeSheet = false
                                onOpenPageManager()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = accent.color)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Đổi vị trí trang Tất cả ứng dụng & Trang Home",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Đặt trang ứng dụng ở đầu, giữa hoặc cuối màn hình",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Context Menu Sheet on Long-press (with multi-Home-page Pin selection!)
        selectedAppForMenu?.let { app ->
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { selectedAppForMenu = null },
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
                        text = app.label,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "GHIM VÀO TRANG HOME:",
                        color = accent.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    homePages.forEach { homePage ->
                        MenuActionRow(
                            icon = Icons.Default.PushPin,
                            label = "Ghim vào \"${homePage.title}\"",
                            tint = accent.color,
                            onClick = {
                                onPinAppToPage(app, homePage.id)
                                selectedAppForMenu = null
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(6.dp))

                    MenuActionRow(
                        icon = Icons.Default.PlayArrow,
                        label = "Mở ứng dụng",
                        tint = Color.White,
                        onClick = {
                            onLaunchApp(app.packageName)
                            selectedAppForMenu = null
                        }
                    )

                    MenuActionRow(
                        icon = Icons.Default.Info,
                        label = "Thông tin ứng dụng (Hệ thống mặc định)",
                        tint = Color.White,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            selectedAppForMenu = null
                        }
                    )

                    if (!app.isSystemApp) {
                        MenuActionRow(
                            icon = Icons.Default.Delete,
                            label = "Gỡ cài đặt",
                            tint = Color(0xFFE81123),
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                        data = Uri.fromParts("package", app.packageName, null)
                                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                                selectedAppForMenu = null
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItemRow(
    app: AppItem,
    accentColor: Color,
    cornerRadiusDp: Int,
    useRealAppIcons: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val iconBitmap = if (useRealAppIcons) rememberAppIcon(app.packageName) else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                .background(accentColor.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Text(
                    text = (app.label.firstOrNull() ?: '?').uppercaseChar().toString(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (app.isSystemApp) {
                Text(
                    text = "Hệ thống",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp
                )
            }
        }

        if (app.isPinned) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Đã ghim",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 10.sp
                )
            }
        }

        if (isEditMode) {
            IconButton(
                onClick = onLongClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Tùy chọn ứng dụng",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridCell(
    app: AppItem,
    accentColor: Color,
    cornerRadiusDp: Int,
    useRealAppIcons: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val iconBitmap = if (useRealAppIcons) rememberAppIcon(app.packageName) else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(cornerRadiusDp.coerceAtLeast(6).dp))
                .background(accentColor.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Text(
                    text = (app.label.firstOrNull() ?: '?').uppercaseChar().toString(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MenuActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
    }
}
