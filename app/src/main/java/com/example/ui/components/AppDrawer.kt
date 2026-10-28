package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
    REMOVE_FROM_FOLDER,
    DELETE_FOLDER
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val accent = LumiaPalette.resolveEffectiveAccent(settings)

    var isAndroidSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }
    var activeFolderDialog by remember { mutableStateOf<AppFolder?>(null) }
    var showJumpList by remember { mutableStateOf(false) }

    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Bounds tracking for Grid View drag-and-drop grouping & reordering (plain maps so scrolling never triggers snapshot state writes)
    var rootBoxPosition by remember { mutableStateOf(Offset.Zero) }
    val gridItemBounds = remember(selectedFolderId, settings.allAppsViewMode) { mutableMapOf<String, Rect>() }
    val folderTabBounds = remember(selectedFolderId, settings.appDrawerFolders) { mutableMapOf<String, Rect>() }

    var draggingEntry by remember { mutableStateOf<GridDrawerEntry?>(null) }
    var dragFingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var dragTotalDistancePx by remember { mutableStateOf(0f) }
    var hoveredTargetKey by remember { mutableStateOf<String?>(null) }
    var hoveredFolderId by remember { mutableStateOf<String?>(null) }
    var activeDropIntent by remember { mutableStateOf(GridDropIntent.NONE) }

    LaunchedEffect(selectedFolderId, settings.appDrawerFolders) {
        if (selectedFolderId != null && settings.appDrawerFolders.none { it.id == selectedFolderId }) {
            selectedFolderId = null
        }
    }

    BackHandler(enabled = isAndroidSearchMode || draggingEntry != null || activeFolderDialog != null) {
        when {
            draggingEntry != null -> {
                draggingEntry = null
                hoveredTargetKey = null
                hoveredFolderId = null
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

    fun launchFinder(query: String) {
        if (!launchSamsungFinder(context, query)) {
            isAndroidSearchMode = true
            selectedFolderId = null
            Toast.makeText(context, "Không mở được Finder; tìm ứng dụng tại đây", Toast.LENGTH_SHORT).show()
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

    // Manual folders are the only grouping surface. The master list always includes
    // every installed app, including apps that also appear in a folder.
    val processedApps = remember(
        categorizedApps,
        searchQuery,
        selectedFolderId,
        settings.appDrawerFolders,
        settings.allAppsSortOrder
    ) {
        val folderPackages = settings.appDrawerFolders
            .firstOrNull { it.id == selectedFolderId }?.appPackages?.toSet()
        val filtered = categorizedApps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true) ||
                app.category.label.contains(searchQuery, ignoreCase = true)

            matchesQuery && (folderPackages == null || app.packageName in folderPackages)
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
        selectedFolderId,
        searchQuery,
        isAndroidSearchMode
    ) {
        if (selectedFolderId != null) {
            val byPackage = processedApps.associateBy { it.packageName }
            settings.appDrawerFolders.firstOrNull { it.id == selectedFolderId }
                ?.appPackages.orEmpty().mapNotNull { byPackage[it] }
                .map { GridDrawerEntry.AppEntry(it) }
        } else if (isAndroidSearchMode && searchQuery.isNotBlank()) {
            // During active text search, show matching apps directly for fast launching
            processedApps.map { GridDrawerEntry.AppEntry(it) }
        } else {
            val appsByPkg = categorizedApps.associateBy { it.packageName }
            val visibleFolders = settings.appDrawerFolders.mapNotNull { folder ->
                val memberApps = folder.appPackages.mapNotNull { appsByPkg[it] }
                if (memberApps.isNotEmpty()) {
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

    fun createManualFolder(app: AppItem) {
        val folder = AppFolder(
            id = "folder_${System.currentTimeMillis()}",
            title = "Thư mục mới",
            appPackages = listOf(app.packageName),
            category = AppCategory.ALL,
            orderIndex = settings.appDrawerFolders.size
        )
        val remaining = settings.appDrawerFolders.mapNotNull { old ->
            val packages = old.appPackages.filterNot { it == app.packageName }
            if (packages.isEmpty()) null else old.copy(appPackages = packages)
        }
        onUpdateSettings(settings.copy(
            appDrawerFolders = remaining + folder,
            appDrawerCustomOrder = settings.appDrawerCustomOrder + "folder:${folder.id}"
        ))
        activeFolderDialog = folder
    }

    fun addAppToExistingFolder(sourceApp: AppItem, targetFolder: AppFolder) {
        val updatedFolders = settings.appDrawerFolders.mapNotNull { f ->
            when {
                f.id == targetFolder.id -> f.copy(appPackages = (f.appPackages + sourceApp.packageName).distinct())
                sourceApp.packageName in f.appPackages -> {
                    val rem = f.appPackages.filterNot { it == sourceApp.packageName }
                    if (rem.isEmpty()) null else f.copy(appPackages = rem)
                }
                else -> f
            }
        }
        onUpdateSettings(settings.copy(appDrawerFolders = updatedFolders))
    }

    fun removeAppFromFolder(app: AppItem) {
        val updated = settings.appDrawerFolders.mapNotNull { folder ->
            val remaining = folder.appPackages.filterNot { it == app.packageName }
            if (remaining.isEmpty()) null else folder.copy(appPackages = remaining)
        }
        onUpdateSettings(settings.copy(appDrawerFolders = updated))
    }

    fun reorderGridEntries(fromKey: String, toKey: String) {
        if (fromKey == toKey) return
        if (selectedFolderId != null) {
            val fromPkg = fromKey.removePrefix("app:")
            val toPkg = toKey.removePrefix("app:")
            val updated = settings.appDrawerFolders.map { folder ->
                if (folder.id != selectedFolderId) folder else {
                    val packages = folder.appPackages.toMutableList()
                    val fromIndex = packages.indexOf(fromPkg)
                    val toIndex = packages.indexOf(toPkg)
                    if (fromIndex >= 0 && toIndex >= 0) {
                        packages.add(toIndex, packages.removeAt(fromIndex))
                    }
                    folder.copy(appPackages = packages)
                }
            }
            onUpdateSettings(settings.copy(appDrawerFolders = updated))
            return
        }
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
        if (dragged is GridDrawerEntry.AppEntry || dragged is GridDrawerEntry.FolderEntry) {
            val folderTarget = folderTabBounds.entries.firstOrNull { (id, rect) ->
                when (dragged) {
                    is GridDrawerEntry.AppEntry -> id != "__delete__" &&
                        (id != "__remove__" || selectedFolderId != null) && rect.contains(fingerRootPos)
                    is GridDrawerEntry.FolderEntry -> id == "__delete__" && rect.contains(fingerRootPos)
                }
            }?.key
            if (folderTarget != null) {
                hoveredFolderId = folderTarget
                hoveredTargetKey = null
                activeDropIntent = when (folderTarget) {
                    "__remove__" -> GridDropIntent.REMOVE_FROM_FOLDER
                    "__delete__" -> GridDropIntent.DELETE_FOLDER
                    else -> GridDropIntent.GROUP_INTO_FOLDER
                }
                return
            }
        }
        hoveredFolderId = null
        val hitEntry = gridItemBounds.entries.firstOrNull { (key, rect) ->
            key != dragged.key && rect.contains(fingerRootPos)
        }
        hoveredTargetKey = hitEntry?.key
        activeDropIntent = when {
            hitEntry == null -> GridDropIntent.NONE
            dragged is GridDrawerEntry.AppEntry && hitEntry.key.startsWith("folder:") -> GridDropIntent.GROUP_INTO_FOLDER
            else -> GridDropIntent.REORDER_SLOT
        }
    }

    fun commitGridDrop(dragged: GridDrawerEntry) {
        val app = (dragged as? GridDrawerEntry.AppEntry)?.app
        when (activeDropIntent) {
            GridDropIntent.GROUP_INTO_FOLDER -> if (app != null) {
                val folderId = hoveredFolderId ?: hoveredTargetKey?.removePrefix("folder:")
                if (folderId == "__new__") createManualFolder(app)
                else settings.appDrawerFolders.firstOrNull { it.id == folderId }?.let {
                    addAppToExistingFolder(app, it)
                }
            }
            GridDropIntent.REMOVE_FROM_FOLDER -> if (app != null && selectedFolderId != null) {
                removeAppFromFolder(app)
            }
            GridDropIntent.DELETE_FOLDER -> if (dragged is GridDrawerEntry.FolderEntry) {
                onUpdateSettings(settings.copy(appDrawerFolders = settings.appDrawerFolders.filterNot {
                    it.id == dragged.folder.id
                }))
            }
            GridDropIntent.REORDER_SLOT -> hoveredTargetKey?.let { target ->
                reorderGridEntries(dragged.key, target)
            }
            GridDropIntent.NONE -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // White Metro list text must remain legible over bright system wallpapers.
            // Apply a page-only scrim without changing Home wallpaper.
            .background(Color.Black.copy(alpha = 0.40f))
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
                .statusBarsPadding()
                .navigationBarsPadding()
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
                    onSubmitLocalSearch = { keyboardController?.hide() }
                )
            } else {
                CleanAppDrawerHeaderBar(
                    folders = settings.appDrawerFolders,
                    selectedFolderId = selectedFolderId,
                    viewMode = settings.allAppsViewMode,
                    accentColor = accent.color,
                    hoveredFolderId = hoveredFolderId,
                    showRemoveTarget = draggingEntry is GridDrawerEntry.AppEntry && selectedFolderId != null,
                    showDeleteTarget = draggingEntry is GridDrawerEntry.FolderEntry,
                    showAzButton = settings.allAppsViewMode == AllAppsViewMode.METRO_LIST,
                    onSelectFolder = { id ->
                        if (id == "__new__") {
                            val folder = AppFolder("folder_${System.currentTimeMillis()}",
                                "Thư mục mới", emptyList(), AppCategory.ALL,
                                settings.appDrawerFolders.size)
                            onUpdateSettings(settings.copy(appDrawerFolders = settings.appDrawerFolders + folder))
                            activeFolderDialog = folder
                        } else selectedFolderId = id
                    },
                    onFolderTabPositioned = { id, rect ->
                        folderTabBounds[id] = rect
                    },
                    onOpenAzJump = { showJumpList = true },
                    onToggleViewMode = {
                        val nextMode = if (settings.allAppsViewMode == AllAppsViewMode.METRO_LIST) {
                            AllAppsViewMode.ONE_UI_GRID
                        } else {
                            AllAppsViewMode.METRO_LIST
                        }
                        onUpdateSettings(settings.copy(allAppsViewMode = nextMode))
                    },
                    onOpenSearchMode = { launchFinder("") }
                )
            }

            // Live Drag-and-Drop Action Banner when dragging an app in Grid View
            AnimatedVisibility(visible = draggingEntry != null) {
                val hintText = when (activeDropIntent) {
                    GridDropIntent.GROUP_INTO_FOLDER -> {
                        val title = settings.appDrawerFolders.firstOrNull {
                            it.id == hoveredFolderId || "folder:${it.id}" == hoveredTargetKey
                        }?.title
                        if (hoveredFolderId == "__new__") "Thả để tạo thư mục"
                        else "Thả vào ${title ?: "thư mục"}"
                    }
                    GridDropIntent.REORDER_SLOT -> "Thả để đổi vị trí"
                    GridDropIntent.REMOVE_FROM_FOLDER -> "Thả để bỏ khỏi thư mục"
                    GridDropIntent.DELETE_FOLDER -> "Thả để xóa thư mục"
                    GridDropIntent.NONE -> "Thả vào thư mục hoặc vị trí mới"
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

            Spacer(modifier = Modifier.height(2.dp))

            // Main Content: Listing (METRO_LIST) or Grid (ONE_UI_GRID) — direct rendering without heavy crossfade cloning
            val activeCat = AppCategory.ALL
            val viewMode = settings.allAppsViewMode
            val inSearch = isAndroidSearchMode
            Box(modifier = Modifier.fillMaxSize()) {
                if (processedApps.isEmpty() && gridEntries.isEmpty()) {
                    EmptyCategoryPlaceholder(
                        category = activeCat,
                        folderTitle = settings.appDrawerFolders.firstOrNull { it.id == selectedFolderId }?.title,
                        searchQuery = searchQuery,
                        isAndroidSearchMode = inSearch,
                        accentColor = accent.color,
                        onResetFilter = {
                            if (searchQuery.isNotBlank()) {
                                searchQuery = ""
                            } else {
                                selectedFolderId = null
                            }
                        }
                    )
                } else if (viewMode == AllAppsViewMode.METRO_LIST) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
                                Text(
                                    text = currentGroupChar.toString(),
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = if (index == 0) 4.dp else 14.dp,
                                            bottom = 6.dp,
                                            start = 10.dp
                                        )
                                        .clickable { showJumpList = true }
                                        .testTag("letter_header_$currentGroupChar")
                                )
                            }

                            val entry = GridDrawerEntry.AppEntry(app)
                            Box(modifier = Modifier.fillMaxWidth()
                                .onGloballyPositioned { gridItemBounds[entry.key] = it.boundsInRoot() }
                                .pointerInput(entry.key, selectedFolderId, settings.appDrawerFolders) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { start ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingEntry = entry
                                            dragTotalDistancePx = 0f
                                            val bounds = gridItemBounds[entry.key]
                                            dragFingerRootPos = if (bounds == null) rootBoxPosition + start
                                                else Offset(bounds.left + start.x, bounds.top + start.y)
                                            hoveredTargetKey = null
                                            hoveredFolderId = null
                                            activeDropIntent = GridDropIntent.NONE
                                        },
                                        onDrag = { change, distance ->
                                            change.consume()
                                            dragFingerRootPos += distance
                                            dragTotalDistancePx += hypot(distance.x, distance.y)
                                            updateDragHoverTarget(dragFingerRootPos, entry)
                                        },
                                        onDragEnd = {
                                            if (with(density) { dragTotalDistancePx.toDp() } < 14.dp) {
                                                selectedAppForMenu = app
                                            } else commitGridDrop(entry)
                                            draggingEntry = null
                                            hoveredTargetKey = null
                                            hoveredFolderId = null
                                            activeDropIntent = GridDropIntent.NONE
                                        },
                                        onDragCancel = {
                                            draggingEntry = null
                                            hoveredTargetKey = null
                                            hoveredFolderId = null
                                            activeDropIntent = GridDropIntent.NONE
                                        }
                                    )
                                }) {
                                AppItemRow(app, accent.color, settings.tileCornerRadiusDp,
                                    settings.useRealAppIcons,
                                    onClick = { onLaunchApp(app.packageName) })
                            }
                        }

                        // Empty space at bottom of list — long-press enters launcher Edit/Settings Mode
                        item(key = "list_empty_space_footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
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
                        contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("app_drawer_grid")
                    ) {
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
                                    .pointerInput(entry.key, gridEntries.size, selectedFolderId,
                                        settings.appDrawerFolders) {
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
                                                hoveredFolderId = null
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
                                                if (movedDistanceDp < 14.dp) {
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
                                                hoveredFolderId = null
                                                activeDropIntent = GridDropIntent.NONE
                                            },
                                            onDragCancel = {
                                                draggingEntry = null
                                                hoveredTargetKey = null
                                                hoveredFolderId = null
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

        // One compact action sheet for both list and grid. Grouping happens by dragging
        // into an existing folder; app removal here always uses Android's own UI.
        selectedAppForMenu?.let { app ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val defaultHomeId = homePages.firstOrNull { it.id == settings.defaultHomePageId }?.id
                ?: homePages.firstOrNull()?.id ?: "page_home_1"
            ModalBottomSheet(
                onDismissRequest = { selectedAppForMenu = null },
                sheetState = sheetState,
                containerColor = Color(0xFF1A1B20),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(app.label, color = Color.White, fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    MenuActionRow(Icons.Default.PushPin, "Ghim vào Trang chủ", accent.color) {
                        onPinAppToPage(app, defaultHomeId)
                        selectedAppForMenu = null
                    }
                    MenuActionRow(Icons.Default.Info, "Thông tin ứng dụng", Color.White) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {
                            Toast.makeText(context, "Không mở được thông tin ứng dụng", Toast.LENGTH_SHORT).show()
                        }
                        selectedAppForMenu = null
                    }
                    if (!app.isSystemApp) {
                        MenuActionRow(Icons.Default.Delete, "Gỡ cài đặt", Color(0xFFE81123)) {
                            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {
                                Toast.makeText(context, "Không mở được trình gỡ cài đặt", Toast.LENGTH_SHORT).show()
                            }
                            selectedAppForMenu = null
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Metro app list controls: A-Z, layout toggle and Search on the first row.
 * Optional category filter chips sit BELOW. Keep their measured bounds for grid drag grouping.
 */
@Composable
private fun CleanAppDrawerHeaderBar(
    folders: List<AppFolder>,
    selectedFolderId: String?,
    viewMode: AllAppsViewMode,
    accentColor: Color,
    hoveredFolderId: String?,
    showRemoveTarget: Boolean,
    showDeleteTarget: Boolean,
    showAzButton: Boolean,
    onSelectFolder: (String?) -> Unit,
    onFolderTabPositioned: (String, Rect) -> Unit,
    onOpenAzJump: () -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenSearchMode: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().testTag("app_drawer_header")) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Ứng dụng", color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (showAzButton) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                    .clickable { onOpenAzJump() }.testTag("app_drawer_az_button"),
                    contentAlignment = Alignment.Center) {
                    Text("A–Z", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                .clickable { onToggleViewMode() }.testTag("toggle_all_apps_view_mode_button"),
                contentAlignment = Alignment.Center) {
                Icon(if (viewMode == AllAppsViewMode.METRO_LIST) Icons.Default.GridView
                    else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = if (viewMode == AllAppsViewMode.METRO_LIST)
                        "Chuyển sang dạng lưới" else "Chuyển sang danh sách",
                    tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                .clickable { onOpenSearchMode() }.testTag("app_search_button"),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Search, contentDescription = "Mở Finder", tint = Color.White,
                    modifier = Modifier.size(22.dp))
            }
        }

        // These are user-created folders, never automatic category assignments.
        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            .testTag("app_drawer_folder_bar"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(end = 6.dp)) {
            item(key = "all") {
                FolderFilterChip("Tất cả", selectedFolderId == null, false, accentColor,
                    onClick = { onSelectFolder(null) })
            }
            if (showRemoveTarget) {
                item(key = "remove_from_folder") {
                    FolderFilterChip("Bỏ khỏi thư mục", false, hoveredFolderId == "__remove__",
                        Color(0xFFE81123), onClick = {},
                        onPositioned = { onFolderTabPositioned("__remove__", it) })
                }
            }
            if (showDeleteTarget) {
                item(key = "delete_folder") {
                    FolderFilterChip("Xóa thư mục", false, hoveredFolderId == "__delete__",
                        Color(0xFFE81123), onClick = {},
                        onPositioned = { onFolderTabPositioned("__delete__", it) })
                }
            }
            item(key = "new_folder") {
                FolderFilterChip("+ Thư mục", false, hoveredFolderId == "__new__", accentColor,
                    onClick = { onSelectFolder("__new__") },
                    onPositioned = { onFolderTabPositioned("__new__", it) })
            }
            items(folders, key = { it.id }) { folder ->
                FolderFilterChip(folder.title.ifBlank { "Thư mục" },
                    selectedFolderId == folder.id, hoveredFolderId == folder.id, accentColor,
                    onClick = { onSelectFolder(folder.id) },
                    onPositioned = { onFolderTabPositioned(folder.id, it) })
            }
        }
    }
}

@Composable
private fun FolderFilterChip(
    label: String,
    selected: Boolean,
    hovered: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onPositioned: (Rect) -> Unit = {}
) {
    val active = selected || hovered
    Box(modifier = Modifier
        .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(if (active) accentColor.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.30f))
        .border(if (active) 1.dp else 0.dp, if (active) accentColor else Color.Transparent,
            RoundedCornerShape(24.dp))
        .clickable { onClick() }
        .padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1)
    }
}

/**
 * Local app search used when Finder has no public launch entry on this device.
 */
@Composable
private fun AndroidDefaultSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    accentColor: Color,
    focusRequester: FocusRequester,
    onCloseSearch: () -> Unit,
    onSubmitLocalSearch: () -> Unit
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
                    text = "Tìm ứng dụng",
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onSubmitLocalSearch() }
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

    }
}

@Composable
private fun EmptyCategoryPlaceholder(
    category: AppCategory,
    folderTitle: String?,
    searchQuery: String,
    isAndroidSearchMode: Boolean,
    accentColor: Color,
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
                    if (folderTitle != null) "Thư mục \"$folderTitle\" chưa có ứng dụng"
                    else "Chưa có ứng dụng"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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
    onClick: () -> Unit
) {
    val iconBitmap = if (useRealAppIcons) rememberAppIcon(app.packageName) else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
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
