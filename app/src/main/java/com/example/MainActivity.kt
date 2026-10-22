package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LauncherPage
import com.example.data.model.PageTransitionEffect
import com.example.data.model.PageType
import com.example.data.model.TileType
import com.example.ui.components.AppDrawer
import com.example.ui.components.GlanceScreenOverlay
import com.example.ui.components.LumiaStartGrid
import com.example.ui.components.PageManagerSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.theme.LumiaPalette
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()
            MyApplicationTheme(darkTheme = settings.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (settings.isDarkTheme) Color.Black else Color(0xFFF2F2F2)
                ) {
                    LumiaLauncherApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }
}

@Composable
fun LumiaLauncherApp(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val tiles by viewModel.tiles.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val isAddTileSheetOpen by viewModel.isAddTileSheetOpen.collectAsState()
    val isPageManagerOpen by viewModel.isPageManagerOpen.collectAsState()
    val isGlanceScreenOpen by viewModel.isGlanceScreenOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val accent = LumiaPalette.getAccent(settings.accentColorHex)
    val wallpaperBrush = LumiaPalette.Wallpapers.getOrNull(settings.wallpaperGradientIndex)?.second
        ?: LumiaPalette.Wallpapers.first().second

    val defaultPageIndex = remember(pages, settings.defaultHomePageId) {
        pages.indexOfFirst { it.id == settings.defaultHomePageId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = defaultPageIndex,
        pageCount = { pages.size.coerceAtLeast(1) }
    )

    val homePages = remember(pages) {
        pages.filter { it.pageType == PageType.HOME }
    }

    val tileCountsByPage = remember(tiles, pages) {
        pages.associate { page ->
            page.id to tiles.count { it.pageId == page.id }
        }
    }

    // Back press handling: exit Glance Screen -> exit Edit Mode -> return to Default Home Page
    BackHandler(
        enabled = isGlanceScreenOpen || isEditMode || pagerState.currentPage != defaultPageIndex
    ) {
        when {
            isGlanceScreenOpen -> viewModel.closeGlanceScreen()
            isEditMode -> viewModel.closeEditMode()
            pagerState.currentPage != defaultPageIndex -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(defaultPageIndex)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wallpaperBrush)
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onLongPress = {
                        viewModel.enterEditMode()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Full-screen HorizontalPager with 3D Page Transition Effects (Lumia / MIUI / One UI 9.0)
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("launcher_horizontal_pager")
            ) { pageIndex ->
                val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                            when (settings.pageTransitionEffect) {
                                PageTransitionEffect.CLASSIC_SLIDE -> {
                                    alpha = 1f - (absOffset * 0.12f)
                                }
                                PageTransitionEffect.TURNSTILE_3D -> {
                                    // Authentic Windows Phone Lumia 3D Turnstile page rotation
                                    cameraDistance = 16f * density
                                    rotationY = -pageOffset * 28f
                                    val scale = 1f - (absOffset * 0.08f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (absOffset * 0.25f)
                                }
                                PageTransitionEffect.MIUI_CUBE -> {
                                    // Xiaomi MIUI / HyperOS 3D Cube Transition
                                    cameraDistance = 18f * density
                                    transformOrigin = TransformOrigin(
                                        pivotFractionX = if (pageOffset < 0f) 0f else 1f,
                                        pivotFractionY = 0.5f
                                    )
                                    rotationY = pageOffset * 55f
                                    alpha = 1f - (absOffset * 0.2f)
                                }
                                PageTransitionEffect.ONE_UI_DEPTH -> {
                                    // Samsung One UI 9.0 Depth Stack Transition
                                    val scale = 1f - (absOffset * 0.16f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (absOffset * 0.35f)
                                }
                            }
                        }
                ) {
                    if (page.pageType == PageType.HOME) {
                        val pageTiles = remember(tiles, page.id) {
                            tiles.filter { it.pageId == page.id }.sortedBy { it.orderIndex }
                        }

                        LumiaStartGrid(
                            currentPage = page,
                            allHomePages = homePages,
                            tiles = pageTiles,
                            settings = settings,
                            telemetry = telemetry,
                            isEditMode = isEditMode,
                            showAddTileSheet = isAddTileSheetOpen && pagerState.currentPage == pageIndex,
                            onDismissAddTileSheet = { viewModel.closeAddTileSheet() },
                            onOpenAddTileSheet = { viewModel.openAddTileSheet() },
                            onTileClick = { tile ->
                                when {
                                    tile.packageName.isNotEmpty() -> {
                                        viewModel.launchApp(tile.packageName)
                                    }
                                    tile.tileType == TileType.PERFORMANCE -> {
                                        val fresh = viewModel.optimizeMemory()
                                        Toast.makeText(
                                            context,
                                            "Đã tối ưu hệ thống! RAM trống: ${fresh.availableRamMb} MB",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    tile.tileType == TileType.BATTERY -> {
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (_: Exception) {
                                            try {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    tile.tileType == TileType.CLOCK_CALENDAR -> {
                                        try {
                                            context.startActivity(
                                                Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (_: Exception) {
                                            viewModel.openGlanceScreen()
                                        }
                                    }
                                    tile.tileType == TileType.NOTES -> {
                                        viewModel.openSettings()
                                    }
                                }
                            },
                            onTileLongClick = { viewModel.enterEditMode() },
                            onResizeTile = { id -> viewModel.resizeTile(id) },
                            onUnpinTile = { id -> viewModel.unpinTile(id) },
                            onMoveTile = { id, up -> viewModel.moveTileInPage(id, page.id, up) },
                            onReorderTiles = { from, to -> viewModel.reorderTilesInPage(page.id, from, to) },
                            onMoveTileToPage = { tileId, targetPageId ->
                                viewModel.moveTileToPage(tileId, targetPageId)
                            },
                            onCustomizeTile = { tileId, colorHex, customTitle, isLive ->
                                viewModel.updateTileCustomization(tileId, colorHex, customTitle, isLive)
                            },
                            onPinNewTile = { tile -> viewModel.pinTile(tile) },
                            onEnterEditMode = { viewModel.enterEditMode() },
                            onOpenPageManager = { viewModel.openPageManager() },
                            onOpenGlance = { viewModel.openGlanceScreen() }
                        )
                    } else {
                        AppDrawer(
                            apps = apps,
                            homePages = homePages,
                            settings = settings,
                            isEditMode = isEditMode,
                            onLaunchApp = { pkg -> viewModel.launchApp(pkg) },
                            onPinAppToPage = { app, targetPageId ->
                                viewModel.pinAppToPage(app, targetPageId)
                            },
                            onUpdateSettings = { updated -> viewModel.updateSettings(updated) },
                            onOpenPageManager = { viewModel.openPageManager() },
                            onEnterEditMode = { viewModel.enterEditMode() }
                        )
                    }
                }
            }
        }

        // Normal Mode: Minimal Page Dots Indicator only (no settings bar clutter)
        AnimatedVisibility(
            visible = !isEditMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            MinimalPageDotsIndicator(
                pages = pages,
                currentPageIndex = pagerState.currentPage,
                accentColor = accent.color,
                onSelectPage = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                onLongPress = { viewModel.enterEditMode() }
            )
        }

        // Edit / Settings Mode: Full Settings & Launcher Control Bar (shown when long-pressing an item or screen)
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            EditModeSettingsBar(
                pages = pages,
                currentPageIndex = pagerState.currentPage,
                accentColor = accent.color,
                glanceEnabled = settings.glanceScreenEnabled,
                isLayoutLocked = settings.lockHomeLayout,
                onSelectPage = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                onAddTile = {
                    val current = pages.getOrNull(pagerState.currentPage)
                    if (current?.pageType != PageType.HOME) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(defaultPageIndex)
                            viewModel.openAddTileSheet()
                        }
                    } else {
                        viewModel.openAddTileSheet()
                    }
                },
                onOpenPageManager = { viewModel.openPageManager() },
                onToggleLockLayout = {
                    val nextLock = !settings.lockHomeLayout
                    viewModel.updateSettings(settings.copy(lockHomeLayout = nextLock))
                    Toast.makeText(
                        context,
                        if (nextLock) "Đã khóa bố cục Màn hình chính" else "Đã mở khóa bố cục Màn hình chính",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onOpenGlance = { viewModel.openGlanceScreen() },
                onOpenSettings = { viewModel.openSettings() },
                onExitEditMode = { viewModel.closeEditMode() }
            )
        }

        // Page Manager Sheet (Add Home pages, reorder Home & All Apps pages, rename/delete)
        if (isPageManagerOpen) {
            PageManagerSheet(
                pages = pages,
                tileCountsByPage = tileCountsByPage,
                settings = settings,
                onAddHomePage = { title -> viewModel.addHomePage(title) },
                onMovePage = { from, to -> viewModel.movePage(from, to) },
                onRenamePage = { pageId, newTitle -> viewModel.renamePage(pageId, newTitle) },
                onDeleteHomePage = { pageId -> viewModel.deleteHomePage(pageId) },
                onSetDefaultHomePage = { pageId -> viewModel.setDefaultHomePage(pageId) },
                onToggleShowHeaderTitle = { show ->
                    viewModel.updateSettings(settings.copy(showPageHeaderTitle = show))
                },
                onJumpToPage = { targetIndex ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))
                    }
                },
                onDismiss = { viewModel.closePageManager() }
            )
        }

        // Glance Screen Overlay
        if (isGlanceScreenOpen) {
            GlanceScreenOverlay(
                telemetry = telemetry,
                accentHex = settings.accentColorHex,
                onDismiss = { viewModel.closeGlanceScreen() }
            )
        }

        // Settings Sheet
        if (isSettingsOpen) {
            SettingsSheet(
                settings = settings,
                telemetry = telemetry,
                onSaveSettings = { viewModel.updateSettings(it) },
                onExportBackup = { viewModel.exportBackup() },
                onRestoreBackup = { viewModel.restoreBackup(it) },
                onResetDefaults = { viewModel.resetDefaults() },
                onOptimizeMemory = { viewModel.optimizeMemory() },
                onRefreshTelemetry = { viewModel.refreshTelemetry() },
                onDismiss = { viewModel.closeSettings() }
            )
        }
    }
}

@Composable
private fun MinimalPageDotsIndicator(
    pages: List<LauncherPage>,
    currentPageIndex: Int,
    accentColor: Color,
    onSelectPage: (Int) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.38f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        pages.forEachIndexed { index, page ->
            val isSelected = index == currentPageIndex
            if (page.pageType == PageType.HOME) {
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 16.dp else 6.dp,
                    label = "minimalDotWidth"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else Color.White.copy(alpha = 0.35f),
                    label = "minimalDotColor"
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(dotColor)
                        .clickable { onSelectPage(index) }
                        .testTag("page_indicator_$index")
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.16f))
                        .clickable { onSelectPage(index) }
                        .testTag("page_indicator_all_apps"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = page.title,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditModeSettingsBar(
    pages: List<LauncherPage>,
    currentPageIndex: Int,
    accentColor: Color,
    glanceEnabled: Boolean,
    isLayoutLocked: Boolean,
    onSelectPage: (Int) -> Unit,
    onAddTile: () -> Unit,
    onOpenPageManager: () -> Unit,
    onToggleLockLayout: () -> Unit,
    onOpenGlance: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitEditMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141414).copy(alpha = 0.94f))
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("edit_mode_settings_bar"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top row inside Settings Mode bar: Page switcher pills
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pages.forEachIndexed { index, page ->
                val isSelected = index == currentPageIndex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.1f))
                        .clickable { onSelectPage(index) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (page.pageType == PageType.ALL_APPS) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = page.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Bottom row: Settings & Customization Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsBarActionItem(
                icon = Icons.Default.Add,
                label = "Thêm Tile",
                onClick = onAddTile,
                testTag = "add_tile_button"
            )

            SettingsBarActionItem(
                icon = Icons.Default.Layers,
                label = "Trang",
                onClick = onOpenPageManager,
                testTag = "open_page_manager_button"
            )

            SettingsBarActionItem(
                icon = if (isLayoutLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                label = if (isLayoutLocked) "Đã khóa" else "Khóa",
                accentHighlight = if (isLayoutLocked) Color(0xFFF7630C) else null,
                onClick = onToggleLockLayout,
                testTag = "toggle_lock_layout_button"
            )

            if (glanceEnabled) {
                SettingsBarActionItem(
                    icon = Icons.Default.Nightlight,
                    label = "Glance",
                    onClick = onOpenGlance,
                    testTag = "open_glance_button"
                )
            }

            SettingsBarActionItem(
                icon = Icons.Default.Settings,
                label = "Cài đặt",
                onClick = onOpenSettings,
                testTag = "open_settings_button"
            )

            SettingsBarActionItem(
                icon = Icons.Default.Done,
                label = "Xong",
                accentHighlight = accentColor,
                onClick = onExitEditMode,
                testTag = "toggle_edit_mode_button"
            )
        }
    }
}

@Composable
private fun SettingsBarActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    accentHighlight: Color? = null
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accentHighlight ?: Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = if (accentHighlight != null) FontWeight.Bold else FontWeight.Medium
        )
    }
}
