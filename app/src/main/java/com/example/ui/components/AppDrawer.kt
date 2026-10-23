package com.example.ui.components

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.AllAppsSortOrder
import com.example.data.model.AllAppsViewMode
import com.example.data.model.AppCategory
import com.example.data.model.AppFolder
import com.example.data.model.AppItem
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.WidgetProviderItem
import com.example.ui.theme.LumiaPalette
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

private sealed class GridDrawerEntry {
    abstract val key: String

    data class FolderEntry(
        val folder: AppFolder,
        val apps: List<AppItem>
    ) : GridDrawerEntry() {
        override val key: String = "folder:${folder.id}"
    }

    data class AppEntry(
        val app: AppItem
    ) : GridDrawerEntry() {
        override val key: String = "app:${app.packageName}"
    }
}

private enum class GridDropIntent {
    NONE,
    GROUP_INTO_FOLDER,
    REORDER_SLOT,
    ASSIGN_CATEGORY
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppItem>,
    homePages: List<LauncherPage>,
    settings: LauncherSettings,
    isEditMode: Boolean = false,
    onLaunchApp: (String) -> Unit,
    onPinAppToPage: (AppItem, String) -> Unit,
    installedWidgets: List<WidgetProviderItem> = emptyList(),
    onPinWidgetToPage: (WidgetProviderItem, String) -> Unit = { _, _ -> },
    onUpdateSettings: (LauncherSettings) -> Unit,
    onEnterEditMode: () -> Unit = {},
    onOpenActionCenter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val accent = LumiaPalette.resolveEffectiveAccent(settings)

    var isAndroidSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }
    var activeFolderDialog by remember { mutableStateOf<AppFolder?>(null) }
    var showJumpList by remember { mutableStateOf(false) }

    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Bounds tracking for Grid View drag-and-drop grouping & reordering (plain maps so scrolling never triggers snapshot state writes)
    var rootBoxPosition by remember { mutableStateOf(Offset.Zero) }
    val gridItemBounds = remember { mutableMapOf<String, Rect>() }
    val categoryTabBounds = remember { mutableMapOf<AppCategory, Rect>() }

    var draggingEntry by remember { mutableStateOf<GridDrawerEntry?>(null) }
    var dragFingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var dragTotalDistancePx by remember { mutableStateOf(0f) }
    var hoveredTargetKey by remember { mutableStateOf<String?>(null) }
    var hoveredCategoryTab by remember { mutableStateOf<AppCategory?>(null) }
    var activeDropIntent by remember { mutableStateOf(GridDropIntent.NONE) }

    BackHandler(enabled = isAndroidSearchMode || draggingEntry != null || activeFolderDialog != null) {
        when {
            draggingEntry != null -> {
                draggingEntry = null
                hoveredTargetKey = null
                hoveredCategoryTab = null
                activeDropIntent = GridDropIntent.NONE
            }
            activeFolderDialog != null -> activeFolderDialog = null
            isAndroidSearchMode -> {
                isAndroidSearchMode = false
                searchQuery = ""
                keyboardController?.hide()
            }
        }
    }

    LaunchedEffect(isAndroidSearchMode) {
        if (isAndroidSearchMode) {
            try {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        }
    }

    fun launchAndroidDefaultSearch(query: String) {
        try {
            val intent = if (query.isNotBlank()) {
                Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent("android.search.action.GLOBAL_SEARCH").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    // Enrich apps with reactive effective category (accounting for customAppCategories overrides)
    val categorizedApps = remember(apps, settings.customAppCategories) {
        apps.map { app ->
            val effectiveCat = app.effectiveCategory(settings.customAppCategories)
            val isCustom = settings.customAppCategories.containsKey(app.packageName)
            if (effectiveCat != app.category || isCustom != app.isCustomCategory) {
                app.copy(category = effectiveCat, isCustomCategory = isCustom)
            } else {
                app
            }
        }
    }

    // Filter and sort apps according to active category & search query
    val processedApps = remember(
        categorizedApps,
        searchQuery,
        selectedCategory,
        settings.allAppsCategorizedEnabled,
        settings.allAppsSortOrder
    ) {
        val filtered = categorizedApps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true) ||
                app.category.label.contains(searchQuery, ignoreCase = true)

            val matchesCategory = if (settings.allAppsCategorizedEnabled && !isAndroidSearchMode) {
                selectedCategory == AppCategory.ALL || app.category == selectedCategory
            } else {
                true
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

    // Build unified Grid entries (Folders + ungrouped Apps, ordered by appDrawerCustomOrder when present)
    val gridEntries = remember(
        processedApps,
        categorizedApps,
        settings.appDrawerFolders,
        settings.appDrawerCustomOrder,
        selectedCategory,
        searchQuery,
        isAndroidSearchMode
    ) {
        if (isAndroidSearchMode && searchQuery.isNotBlank()) {
            // During active text search, show matching apps directly for fast launching
            processedApps.map { GridDrawerEntry.AppEntry(it) }
        } else {
            val appsByPkg = categorizedApps.associateBy { it.packageName }
            val visibleFolders = settings.appDrawerFolders.mapNotNull { folder ->
                val memberApps = folder.appPackages.mapNotNull { appsByPkg[it] }
                val matchesCat = selectedCategory == AppCategory.ALL ||
                    folder.category == selectedCategory ||
                    memberApps.any { it.category == selectedCategory }
                if (memberApps.isNotEmpty() && matchesCat) {
                    GridDrawerEntry.FolderEntry(folder = folder, apps = memberApps)
                } else {
                    null
                }
            }
            val groupedPkgs = visibleFolders.flatMap { entry -> entry.apps.map { it.packageName } }.toSet()
            val ungroupedApps = processedApps
                .filter { it.packageName !in groupedPkgs }
                .map { GridDrawerEntry.AppEntry(it) }

            val combined = visibleFolders + ungroupedApps
            if (settings.appDrawerCustomOrder.isEmpty()) {
                combined
            } else {
                val orderMap = settings.appDrawerCustomOrder.withIndex().associate { it.value to it.index }
                combined.sortedWith(
                    compareBy<GridDrawerEntry> { orderMap[it.key] ?: Int.MAX_VALUE }
                )
            }
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

    fun assignAppCategory(app: AppItem, newCategory: AppCategory) {
        val updatedMap = settings.customAppCategories.toMutableMap()
        updatedMap[app.packageName] = newCategory.name
        onUpdateSettings(settings.copy(customAppCategories = updatedMap))
    }

    fun groupTwoAppsIntoFolder(sourceApp: AppItem, targetApp: AppItem) {
        if (sourceApp.packageName == targetApp.packageName) return
        val currentFolders = settings.appDrawerFolders.toMutableList()
        // Remove both apps from any existing folders first
        val cleanedFolders = currentFolders.mapNotNull { f ->
            val remaining = f.appPackages.filter { it != sourceApp.packageName && it != targetApp.packageName }
            if (remaining.isEmpty()) null else f.copy(appPackages = remaining)
        }.toMutableList()

        val defaultFolderTitle = if (sourceApp.category == targetApp.category && targetApp.category != AppCategory.ALL) {
            targetApp.category.shortLabel
        } else {
            targetApp.category.shortLabel.ifBlank { "Thư mục mới" }
        }
        val newFolder = AppFolder(
            id = "folder_${System.currentTimeMillis()}",
            title = defaultFolderTitle,
            appPackages = listOf(targetApp.packageName, sourceApp.packageName).distinct(),
            category = targetApp.category,
            orderIndex = cleanedFolders.size
        )
        cleanedFolders.add(0, newFolder)

        // Place the new folder at the targetApp's position in custom order
        val currentOrder = gridEntries.map { it.key }.toMutableList()
        val targetIdx = currentOrder.indexOf("app:${targetApp.packageName}")
        if (targetIdx >= 0) {
            currentOrder[targetIdx] = "folder:${newFolder.id}"
            currentOrder.remove("app:${sourceApp.packageName}")
        } else {
            currentOrder.add(0, "folder:${newFolder.id}")
        }

        onUpdateSettings(
            settings.copy(
                appDrawerFolders = cleanedFolders,
                appDrawerCustomOrder = currentOrder.distinct()
            )
        )
    }

    fun addAppToExistingFolder(sourceApp: AppItem, targetFolder: AppFolder) {
        val updatedFolders = settings.appDrawerFolders.mapNotNull { f ->
            when {
                f.id == targetFolder.id -> {
                    f.copy(appPackages = (f.appPackages + sourceApp.packageName).distinct())
                }
                sourceApp.packageName in f.appPackages -> {
                    val rem = f.appPackages.filter { it != sourceApp.packageName }
                    if (rem.isEmpty()) null else f.copy(appPackages = rem)
                }
                else -> f
            }
        }
        val updatedOrder = settings.appDrawerCustomOrder.filter { it != "app:${sourceApp.packageName}" }
        onUpdateSettings(
            settings.copy(
                appDrawerFolders = updatedFolders,
                appDrawerCustomOrder = updatedOrder
            )
        )
    }

    fun reorderGridEntries(fromKey: String, toKey: String) {
        if (fromKey == toKey) return
        val currentKeys = gridEntries.map { it.key }.toMutableList()
        val fromIdx = currentKeys.indexOf(fromKey)
        val toIdx = currentKeys.indexOf(toKey)
        if (fromIdx != -1 && toIdx != -1) {
            val item = currentKeys.removeAt(fromIdx)
            currentKeys.add(toIdx, item)
            onUpdateSettings(settings.copy(appDrawerCustomOrder = currentKeys))
        }
    }

    fun updateDragHoverTarget(fingerRootPos: Offset, dragged: GridDrawerEntry) {
        // 1. Check if hovering over a Category Filter Pill
        if (dragged is GridDrawerEntry.AppEntry) {
            val hitCat = categoryTabBounds.entries.firstOrNull { (cat, rect) ->
                cat != AppCategory.ALL && rect.contains(fingerRootPos)
            }?.key
            if (hitCat != null) {
                hoveredCategoryTab = hitCat
                hoveredTargetKey = null
                activeDropIntent = GridDropIntent.ASSIGN_CATEGORY
                return
            }
        }
        hoveredCategoryTab = null

        // 2. Check if hovering over another Grid Cell
        val hitEntry = gridItemBounds.entries.firstOrNull { (key, rect) ->
            key != dragged.key && rect.contains(fingerRootPos)
        }
        if (hitEntry == null) {
            hoveredTargetKey = null
            activeDropIntent = GridDropIntent.NONE
            return
        }

        val targetKey = hitEntry.key
        val rect = hitEntry.value
        hoveredTargetKey = targetKey

        // Determine whether finger is in the inner 68% (Group into Folder) or outer border (Reorder slot)
        val center = rect.center
        val normDx = kotlin.math.abs(fingerRootPos.x - center.x) / (rect.width * 0.5f).coerceAtLeast(1f)
        val normDy = kotlin.math.abs(fingerRootPos.y - center.y) / (rect.height * 0.5f).coerceAtLeast(1f)
        val isInnerCenter = normDx < 0.68f && normDy < 0.68f

        activeDropIntent = if (dragged is GridDrawerEntry.AppEntry && isInnerCenter) {
            GridDropIntent.GROUP_INTO_FOLDER
        } else {
            GridDropIntent.REORDER_SLOT
        }
    }

    fun commitGridDrop(dragged: GridDrawerEntry) {
        val targetCat = hoveredCategoryTab
        val targetKey = hoveredTargetKey
        val intent = activeDropIntent

        if (intent == GridDropIntent.ASSIGN_CATEGORY && targetCat != null && dragged is GridDrawerEntry.AppEntry) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            assignAppCategory(dragged.app, targetCat)
        } else if (intent == GridDropIntent.GROUP_INTO_FOLDER && targetKey != null && dragged is GridDrawerEntry.AppEntry) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val targetEntry = gridEntries.find { it.key == targetKey }
            when (targetEntry) {
                is GridDrawerEntry.AppEntry -> {
                    groupTwoAppsIntoFolder(dragged.app, targetEntry.app)
                }
                is GridDrawerEntry.FolderEntry -> {
                    addAppToExistingFolder(dragged.app, targetEntry.folder)
                }
                else -> {}
            }
        } else if (intent == GridDropIntent.REORDER_SLOT && targetKey != null) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            reorderGridEntries(dragged.key, targetKey)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootBoxPosition = coords.positionInRoot()
            }
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onLongPress = {
                        // Long-pressing empty space enters Settings / Edit Mode
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnterEditMode()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Actions first, category filters second. Preserve native Metro list as the default.
            if (isAndroidSearchMode) {
                AndroidDefaultSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    accentColor = accent.color,
                    focusRequester = searchFocusRequester,
                    onCloseSearch = {
                        isAndroidSearchMode = false
                        searchQuery = ""
                        keyboardController?.hide()
                    },
                    onTriggerAndroidSystemSearch = {
                        launchAndroidDefaultSearch(searchQuery)
                    }
                )
            } else {
                CleanAppDrawerHeaderBar(
                    selectedCategory = selectedCategory,
                    categorizedEnabled = settings.allAppsCategorizedEnabled,
                    viewMode = settings.allAppsViewMode,
                    accentColor = accent.color,
                    hoveredCategoryTab = hoveredCategoryTab,
                    showAzButton = settings.allAppsViewMode == AllAppsViewMode.METRO_LIST,
                    onSelectCategory = { cat -> selectedCategory = cat },
                    onCategoryTabPositioned = { cat, rect ->
                        categoryTabBounds[cat] = rect
                    },
                    onOpenAzJump = { showJumpList = true },
                    onOpenActionCenter = onOpenActionCenter,
                    onToggleViewMode = {
                        val nextMode = if (settings.allAppsViewMode == AllAppsViewMode.METRO_LIST) {
                            AllAppsViewMode.ONE_UI_GRID
                        } else {
                            AllAppsViewMode.METRO_LIST
                        }
                        onUpdateSettings(settings.copy(allAppsViewMode = nextMode))
                    },
                    onOpenSearchMode = {
                        isAndroidSearchMode = true
                    }
                )
            }

            // Live Drag-and-Drop Action Banner when dragging an app in Grid View
            AnimatedVisibility(visible = draggingEntry != null) {
                val hintText = when (activeDropIntent) {
                    GridDropIntent.ASSIGN_CATEGORY -> {
                        "Thả để chuyển sang phân loại \"${hoveredCategoryTab?.label ?: ""}\""
                    }
                    GridDropIntent.GROUP_INTO_FOLDER -> {
                        val target = gridEntries.find { it.key == hoveredTargetKey }
                        when (target) {
                            is GridDrawerEntry.FolderEntry -> "Thả để gom vào nhóm \"${target.folder.title}\""
                            is GridDrawerEntry.AppEntry -> "Thả để tạo nhóm thư mục với \"${target.app.label}\""
                            else -> "Thả vào giữa ứng dụng khác để gom nhóm"
                        }
                    }
                    GridDropIntent.REORDER_SLOT -> "Thả để đổi vị trí sắp xếp trên lưới"
                    GridDropIntent.NONE -> "Kéo đè lên ứng dụng khác để gom nhóm • Kéo lên thanh phân loại để đổi mục"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.color.copy(alpha = 0.24f))
                        .border(1.dp, accent.color.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = hintText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Content: Listing (METRO_LIST) or Grid (ONE_UI_GRID) — direct rendering without heavy crossfade cloning
            val activeCat = selectedCategory
            val viewMode = settings.allAppsViewMode
            val inSearch = isAndroidSearchMode
            Box(modifier = Modifier.fillMaxSize()) {
                if (processedApps.isEmpty() && gridEntries.isEmpty()) {
                    EmptyCategoryPlaceholder(
                        category = activeCat,
                        searchQuery = searchQuery,
                        isAndroidSearchMode = inSearch,
                        accentColor = accent.color,
                        onSearchAndroidSystem = { launchAndroidDefaultSearch(searchQuery) },
                        onResetFilter = {
                            if (searchQuery.isNotBlank()) {
                                searchQuery = ""
                            } else {
                                selectedCategory = AppCategory.ALL
                            }
                        }
                    )
                } else if (viewMode == AllAppsViewMode.METRO_LIST) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (inSearch && searchQuery.isNotBlank()) {
                            item(key = "android_default_search_row") {
                                AndroidDefaultSearchActionCard(
                                    query = searchQuery,
                                    accentColor = accent.color,
                                    onClick = { launchAndroidDefaultSearch(searchQuery) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

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
                                Text(
                                    text = currentGroupChar.toString(),
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = if (index == 0) 6.dp else 22.dp,
                                            bottom = 6.dp,
                                            start = 10.dp
                                        )
                                        .clickable { showJumpList = true }
                                        .testTag("letter_header_$currentGroupChar")
                                )
                            }

                            AppItemRow(
                                app = app,
                                accentColor = accent.color,
                                cornerRadiusDp = settings.tileCornerRadiusDp,
                                useRealAppIcons = settings.useRealAppIcons,
                                onClick = { onLaunchApp(app.packageName) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedAppForMenu = app
                                }
                            )
                        }

                        // Empty space at bottom of list — long-press enters launcher Edit/Settings Mode
                        item(key = "list_empty_space_footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .pointerInput(isEditMode) {
                                        detectTapGestures(
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onEnterEditMode()
                                            }
                                        )
                                    }
                            )
                        }
                    }
                } else {
                    // Clean Grid View with Long-Press Drag & Drop Grouping + Empty Space Long-Press for Settings Mode
                    val cols = settings.allAppsGridColumns.coerceIn(3, 6)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 92.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("app_drawer_grid")
                    ) {
                        if (inSearch && searchQuery.isNotBlank()) {
                            item(
                                key = "grid_android_default_search_card",
                                span = { GridItemSpan(cols) }
                            ) {
                                AndroidDefaultSearchActionCard(
                                    query = searchQuery,
                                    accentColor = accent.color,
                                    onClick = { launchAndroidDefaultSearch(searchQuery) }
                                )
                            }
                        }

                        items(gridEntries, key = { it.key }) { entry ->
                            val isBeingDragged = draggingEntry?.key == entry.key
                            val isHoverTarget = hoveredTargetKey == entry.key
                            val isFolderMergeTarget = isHoverTarget && activeDropIntent == GridDropIntent.GROUP_INTO_FOLDER
                            val isReorderTarget = isHoverTarget && activeDropIntent == GridDropIntent.REORDER_SLOT

                            val targetScale = if (draggingEntry == null) {
                                1f
                            } else {
                                val animatedScale by animateFloatAsState(
                                    targetValue = when {
                                        isBeingDragged -> 0.9f
                                        isFolderMergeTarget -> 1.10f
                                        isReorderTarget -> 0.95f
                                        else -> 1f
                                    },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "gridEntryHoverScale"
                                )
                                animatedScale
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(targetScale)
                                    .onGloballyPositioned { coords ->
                                        gridItemBounds[entry.key] = coords.boundsInRoot()
                                    }
                                    .pointerInput(entry.key, gridEntries.size) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { startOffset ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val cellBounds = gridItemBounds[entry.key]
                                                draggingEntry = entry
                                                dragTotalDistancePx = 0f
                                                dragFingerRootPos = if (cellBounds != null) {
                                                    Offset(
                                                        cellBounds.left + startOffset.x,
                                                        cellBounds.top + startOffset.y
                                                    )
                                                } else {
                                                    rootBoxPosition + startOffset
                                                }
                                                hoveredTargetKey = null
                                                hoveredCategoryTab = null
                                                activeDropIntent = GridDropIntent.NONE
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragFingerRootPos += dragAmount
                                                dragTotalDistancePx += hypot(dragAmount.x, dragAmount.y)
                                                updateDragHoverTarget(dragFingerRootPos, entry)
                                            },
                                            onDragEnd = {
                                                val movedDistanceDp = with(density) { dragTotalDistancePx.toDp() }
                                                if (movedDistanceDp < 14.dp && activeDropIntent == GridDropIntent.NONE) {
                                                    // Short hold without dragging opens quick actions for the app or folder
                                                    when (entry) {
                                                        is GridDrawerEntry.AppEntry -> selectedAppForMenu = entry.app
                                                        is GridDrawerEntry.FolderEntry -> activeFolderDialog = entry.folder
                                                    }
                                                } else {
                                                    commitGridDrop(entry)
                                                }
                                                draggingEntry = null
                                                hoveredTargetKey = null
                                                hoveredCategoryTab = null
                                                activeDropIntent = GridDropIntent.NONE
                                            },
                                            onDragCancel = {
                                                draggingEntry = null
                                                hoveredTargetKey = null
                                                hoveredCategoryTab = null
                                                activeDropIntent = GridDropIntent.NONE
                                            }
                                        )
                                    }
                            ) {
                                when (entry) {
                                    is GridDrawerEntry.FolderEntry -> {
                                        AppFolderGridCell(
                                            folder = entry.folder,
                                            memberApps = entry.apps,
                                            accentColor = accent.color,
                                            cornerRadiusDp = settings.tileCornerRadiusDp,
                                            useRealAppIcons = settings.useRealAppIcons,
                                            isHighlightedForMerge = isFolderMergeTarget,
                                            onClick = { activeFolderDialog = entry.folder }
                                        )
                                    }
                                    is GridDrawerEntry.AppEntry -> {
                                        AppGridCell(
                                            app = entry.app,
                                            accentColor = accent.color,
                                            cornerRadiusDp = settings.tileCornerRadiusDp,
                                            useRealAppIcons = settings.useRealAppIcons,
                                            isBeingDragged = isBeingDragged,
                                            isHighlightedForMerge = isFolderMergeTarget,
                                            isHighlightedForReorder = isReorderTarget,
                                            onClick = { onLaunchApp(entry.app.packageName) }
                                        )
                                    }
                                }
                            }
                        }

                        // Empty space at bottom of grid — long-press enters launcher Edit/Settings Mode
                        item(
                            key = "grid_empty_space_footer",
                            span = { GridItemSpan(cols) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .pointerInput(isEditMode) {
                                        detectTapGestures(
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onEnterEditMode()
                                            }
                                        )
                                    }
                                    .testTag("app_drawer_empty_space")
                            )
                        }
                    }
                }
            }
        }

        // Floating Dragged App/Folder Ghost Preview during Grid Drag-and-Drop
        draggingEntry?.let { currentDragged ->
            val localOffset = dragFingerRootPos - rootBoxPosition
            val ghostSizePx = with(density) { 64.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (localOffset.x - ghostSizePx / 2f).roundToInt(),
                            (localOffset.y - ghostSizePx / 2f).roundToInt()
                        )
                    }
                    .zIndex(50f)
                    .scale(1.12f)
                    .shadow(18.dp, RoundedCornerShape(16.dp))
            ) {
                when (currentDragged) {
                    is GridDrawerEntry.AppEntry -> {
                        val iconBitmap = if (settings.useRealAppIcons) {
                            rememberAppIcon(currentDragged.app.packageName)
                        } else null
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.color)
                                .border(2.dp, Color.White, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = currentDragged.app.label,
                                    modifier = Modifier.size(38.dp)
                                )
                            } else {
                                Text(
                                    text = (currentDragged.app.label.firstOrNull() ?: '?').uppercaseChar().toString(),
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is GridDrawerEntry.FolderEntry -> {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E293B))
                                .border(2.dp, accent.color, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = currentDragged.folder.title,
                                tint = accent.color,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }

        // Jump List Modal (for A-Z in Listing mode)
        if (showJumpList) {
            JumpListModal(
                availableLetters = availableLetters,
                accentHex = accent.hex,
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

        // Folder Content Popup Sheet (Open folder, launch apps, rename folder, add/remove apps)
        activeFolderDialog?.let { currentFolder ->
            val freshFolder = settings.appDrawerFolders.find { it.id == currentFolder.id } ?: currentFolder
            val folderApps = freshFolder.appPackages.mapNotNull { pkg ->
                categorizedApps.find { it.packageName == pkg }
            }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var folderTitleText by remember(freshFolder.id) { mutableStateOf(freshFolder.title) }
            var showAddAppsList by remember(freshFolder.id) { mutableStateOf(false) }

            ModalBottomSheet(
                onDismissRequest = { activeFolderDialog = null },
                sheetState = sheetState,
                containerColor = Color(0xFF141821),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = folderTitleText,
                            onValueChange = { newTitle ->
                                folderTitleText = newTitle
                                val updated = settings.appDrawerFolders.map {
                                    if (it.id == freshFolder.id) it.copy(title = newTitle) else it
                                }
                                onUpdateSettings(settings.copy(appDrawerFolders = updated))
                            },
                            label = { Text("Tên nhóm thư mục", color = Color.White.copy(alpha = 0.65f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accent.color,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = {
                                // Dissolve folder (ungroup apps back to grid)
                                val updated = settings.appDrawerFolders.filter { it.id != freshFolder.id }
                                onUpdateSettings(settings.copy(appDrawerFolders = updated))
                                activeFolderDialog = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Bỏ gom nhóm",
                                tint = Color(0xFFFF6B6B)
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    ) {
                        items(folderApps, key = { it.packageName }) { memberApp ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AppGridCell(
                                    app = memberApp,
                                    accentColor = accent.color,
                                    cornerRadiusDp = settings.tileCornerRadiusDp,
                                    useRealAppIcons = settings.useRealAppIcons,
                                    onClick = {
                                        activeFolderDialog = null
                                        onLaunchApp(memberApp.packageName)
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.72f))
                                        .clickable {
                                            val remainingPkgs = freshFolder.appPackages.filter { it != memberApp.packageName }
                                            val updatedFolders = if (remainingPkgs.isEmpty()) {
                                                settings.appDrawerFolders.filter { it.id != freshFolder.id }
                                            } else {
                                                settings.appDrawerFolders.map {
                                                    if (it.id == freshFolder.id) it.copy(appPackages = remainingPkgs) else it
                                                }
                                            }
                                            onUpdateSettings(settings.copy(appDrawerFolders = updatedFolders))
                                            if (remainingPkgs.isEmpty()) {
                                                activeFolderDialog = null
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Bỏ khỏi nhóm",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        item(key = "add_more_to_folder_btn") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showAddAppsList = !showAddAppsList }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, accent.color.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Thêm ứng dụng",
                                        tint = accent.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Thêm app",
                                    color = accent.color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showAddAppsList) {
                        val candidateApps = categorizedApps.filter { it.packageName !in freshFolder.appPackages }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(candidateApps, key = { it.packageName }) { cand ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            addAppToExistingFolder(cand, freshFolder)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "+ ${cand.label}",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // App Context Action Sheet (Pin to Home, Group into Folder, Assign Category, App Info)
        selectedAppForMenu?.let { currentMenuApp ->
            val app = categorizedApps.find { it.packageName == currentMenuApp.packageName } ?: currentMenuApp
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { selectedAppForMenu = null },
                sheetState = sheetState,
                containerColor = Color(0xFF1A1B20),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = app.packageName,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        CategoryBadgePill(
                            category = app.category,
                            isCustom = app.isCustomCategory
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Group into Folder section (for Grid View)
                    Text(
                        text = "GOM NHÓM THƯ MỤC (GRID VIEW):",
                        color = accent.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item(key = "create_new_folder_for_app") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.color.copy(alpha = 0.22f))
                                    .border(1.dp, accent.color, RoundedCornerShape(14.dp))
                                    .clickable {
                                        val newFolder = AppFolder(
                                            id = "folder_${System.currentTimeMillis()}",
                                            title = app.category.shortLabel,
                                            appPackages = listOf(app.packageName),
                                            category = app.category,
                                            orderIndex = settings.appDrawerFolders.size
                                        )
                                        onUpdateSettings(
                                            settings.copy(
                                                allAppsViewMode = AllAppsViewMode.ONE_UI_GRID,
                                                appDrawerFolders = listOf(newFolder) + settings.appDrawerFolders
                                            )
                                        )
                                        selectedAppForMenu = null
                                        activeFolderDialog = newFolder
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("create_folder_from_app_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = null,
                                        tint = accent.color,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "+ Tạo nhóm thư mục mới",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        items(settings.appDrawerFolders, key = { it.id }) { folder ->
                            val alreadyIn = app.packageName in folder.appPackages
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (alreadyIn) accent.color.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable {
                                        addAppToExistingFolder(app, folder)
                                        selectedAppForMenu = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = accent.color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = folder.title,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Filter Assignment (Auto-Save on Tap)
                    Text(
                        text = "BỘ LỌC PHÂN LOẠI (CHẠM ĐỂ ĐỔI NHÓM):",
                        color = accent.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AppCategory.assignableCategories) { cat ->
                            val isCurrent = app.category == cat
                            val catColor = parseHexColor(cat.badgeColorHex, accent.color)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isCurrent) catColor.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.07f)
                                    )
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) catColor else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        assignAppCategory(app, cat)
                                        selectedAppForMenu = app.copy(category = cat, isCustomCategory = true)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("assign_category_${cat.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(cat),
                                        contentDescription = cat.label,
                                        tint = if (isCurrent) catColor else Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = cat.label,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    val appWidgets = remember(installedWidgets, app.packageName) {
                        installedWidgets.filter { it.packageName == app.packageName }
                    }
                    if (appWidgets.isNotEmpty()) {
                        val defaultHomeId = homePages.firstOrNull { it.id == settings.defaultHomePageId }?.id
                            ?: homePages.firstOrNull()?.id ?: "page_home_1"
                        appWidgets.take(3).forEach { wItem ->
                            MenuActionRow(
                                icon = Icons.Default.Widgets,
                                label = "Ghim Widget \"${wItem.label}\" (${wItem.targetCellWidth}x${wItem.targetCellHeight})",
                                tint = accent.color,
                                onClick = {
                                    onPinWidgetToPage(wItem, defaultHomeId)
                                    selectedAppForMenu = null
                                }
                            )
                        }
                    }

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
                        label = "Thông tin ứng dụng",
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

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

/**
 * Metro app list controls: Action Center, A-Z, layout toggle and Search on the FIRST row.
 * Optional category filter chips sit BELOW. Keep their measured bounds for grid drag grouping.
 */
@Composable
private fun CleanAppDrawerHeaderBar(
    selectedCategory: AppCategory,
    categorizedEnabled: Boolean,
    viewMode: AllAppsViewMode,
    accentColor: Color,
    hoveredCategoryTab: AppCategory?,
    showAzButton: Boolean,
    onSelectCategory: (AppCategory) -> Unit,
    onCategoryTabPositioned: (AppCategory, Rect) -> Unit,
    onOpenAzJump: () -> Unit,
    onOpenActionCenter: () -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenSearchMode: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().testTag("app_drawer_header")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenActionCenter() }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .testTag("app_drawer_action_center"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = accentColor, modifier = Modifier.size(19.dp))
                Text("Action Center", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showAzButton) {
                Box(
                    modifier = Modifier.size(44.dp)
                        .clip(CircleShape)
                        .clickable { onOpenAzJump() }
                        .testTag("app_drawer_az_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A-Z", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                modifier = Modifier.size(44.dp)
                    .clip(CircleShape)
                    .clickable { onToggleViewMode() }
                    .testTag("toggle_all_apps_view_mode_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (viewMode == AllAppsViewMode.METRO_LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = if (viewMode == AllAppsViewMode.METRO_LIST) "Chuyển sang dạng lưới" else "Chuyển sang danh sách",
                    tint = Color.White, modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier.size(44.dp)
                    .clip(CircleShape)
                    .clickable { onOpenSearchMode() }
                    .testTag("app_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = "Tìm ứng dụng", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        if (categorizedEnabled) {
            LazyRow(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 4.dp, bottom = 5.dp)
                    .testTag("app_drawer_category_bar"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 6.dp)
            ) {
                items(AppCategory.entries, key = { it.name }) { category ->
                    val selected = selectedCategory == category
                    val dragHovered = hoveredCategoryTab == category
                    val active = selected || dragHovered
                    val color = if (category == AppCategory.ALL) accentColor else parseHexColor(category.badgeColorHex, accentColor)
                    Row(
                        modifier = Modifier
                            .onGloballyPositioned { coords -> onCategoryTabPositioned(category, coords.boundsInRoot()) }
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (active) color.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.06f))
                            .clickable { onSelectCategory(category) }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("app_category_tab_${category.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = category.shortLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean Android Default Search Mode Bar with instant keyboard focus and native Android Search integration.
 */
@Composable
private fun AndroidDefaultSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    accentColor: Color,
    focusRequester: FocusRequester,
    onCloseSearch: () -> Unit,
    onTriggerAndroidSystemSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1E2430).copy(alpha = 0.94f))
            .border(1.2.dp, accentColor.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onCloseSearch,
            modifier = Modifier
                .size(34.dp)
                .testTag("close_android_search_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Đóng tìm kiếm",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Tìm kiếm ứng dụng & Android...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onTriggerAndroidSystemSearch() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("app_search_input")
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Xóa",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Launch Android System Default Search directly
        IconButton(
            onClick = onTriggerAndroidSystemSearch,
            modifier = Modifier
                .size(34.dp)
                .testTag("launch_android_system_search_button")
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Mở tìm kiếm mặc định của Android",
                tint = accentColor,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun AndroidDefaultSearchActionCard(
    query: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.18f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Tìm \"$query\" bằng Tìm kiếm mặc định Android",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun EmptyCategoryPlaceholder(
    category: AppCategory,
    searchQuery: String,
    isAndroidSearchMode: Boolean,
    accentColor: Color,
    onSearchAndroidSystem: () -> Unit,
    onResetFilter: () -> Unit
) {
    val catColor = parseHexColor(category.badgeColorHex, accentColor)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = if (isAndroidSearchMode) Icons.Default.Search else categoryIcon(category),
                contentDescription = null,
                tint = catColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "Không có ứng dụng khớp \"$searchQuery\""
                } else {
                    "Chưa có ứng dụng trong mục \"${category.label}\""
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor)
                        .clickable { onSearchAndroidSystem() }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "Tìm \"$searchQuery\" trên Android Search",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onResetFilter() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Xoá từ khoá" else "Xem Tất cả ứng dụng",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
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
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Real app icon only: no tinted square behind it.
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Text(
                    text = (app.label.firstOrNull() ?: '?').uppercaseChar().toString(),
                    color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = app.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Clean Grid Cell for a single App (no cluttered subtitle below app name, supports drag-to-group highlight).
 */
@Composable
private fun AppGridCell(
    app: AppItem,
    accentColor: Color,
    cornerRadiusDp: Int,
    useRealAppIcons: Boolean,
    isBeingDragged: Boolean = false,
    isHighlightedForMerge: Boolean = false,
    isHighlightedForReorder: Boolean = false,
    onClick: () -> Unit
) {
    val iconBitmap = if (useRealAppIcons) rememberAppIcon(app.packageName) else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                when {
                    isHighlightedForMerge -> Modifier
                        .background(accentColor.copy(alpha = 0.28f))
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    isHighlightedForReorder -> Modifier
                        .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
                    else -> Modifier
                }
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Keep drag highlight on the CELL only. Never frame individual app logos.
        Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            if (iconBitmap != null) {
                Image(bitmap = iconBitmap, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp))
            } else {
                Text(text = (app.label.firstOrNull() ?: '?').uppercaseChar().toString(),
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
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

/**
 * Clean 2x2 Mini-Icon Folder Grid Cell in Grid View (created when dragging one app onto another).
 */
@Composable
private fun AppFolderGridCell(
    folder: AppFolder,
    memberApps: List<AppItem>,
    accentColor: Color,
    cornerRadiusDp: Int,
    useRealAppIcons: Boolean,
    isHighlightedForMerge: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isHighlightedForMerge) {
                    Modifier
                        .background(accentColor.copy(alpha = 0.3f))
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("app_folder_${folder.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(cornerRadiusDp.coerceAtLeast(12).dp))
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(cornerRadiusDp.coerceAtLeast(12).dp))
                .padding(6.dp)
        ) {
            val previewApps = memberApps.take(4)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (rowIdx in 0..1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (colIdx in 0..1) {
                            val itemIdx = rowIdx * 2 + colIdx
                            val app = previewApps.getOrNull(itemIdx)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        if (app != null) accentColor.copy(alpha = 0.85f)
                                        else Color.White.copy(alpha = 0.06f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app != null) {
                                    val bmp = if (useRealAppIcons) rememberAppIcon(app.packageName) else null
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = app.label,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Text(
                                            text = (app.label.firstOrNull() ?: '?').uppercaseChar().toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = folder.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CategoryBadgePill(
    category: AppCategory,
    isCustom: Boolean
) {
    val badgeColor = parseHexColor(category.badgeColorHex, Color(0xFF0078D7))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(badgeColor.copy(alpha = 0.22f))
            .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isCustom) "${category.shortLabel} • Tuỳ chỉnh" else category.shortLabel,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun categoryIcon(category: AppCategory): ImageVector = when (category) {
    AppCategory.ALL -> Icons.Default.GridView
    AppCategory.COMMUNICATION -> Icons.Default.Phone
    AppCategory.SOCIAL -> Icons.Default.Forum
    AppCategory.GAMES -> Icons.Default.SportsEsports
    AppCategory.MEDIA -> Icons.Default.Movie
    AppCategory.TOOLS -> Icons.Default.Build
    AppCategory.PRODUCTIVITY -> Icons.Default.Work
    AppCategory.SYSTEM -> Icons.Default.Settings
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
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
