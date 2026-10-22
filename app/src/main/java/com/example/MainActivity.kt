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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.LauncherPage
import com.example.data.model.PageTransitionEffect
import com.example.data.model.PageType
import com.example.data.model.TileType
import com.example.data.model.Wp10TileStyle
import com.example.ui.components.AppDrawer
import com.example.ui.components.GlanceScreenOverlay
import com.example.ui.components.LumiaStartGrid
import com.example.ui.components.SettingsSheet
import com.example.ui.theme.LumiaPalette
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

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
    val uiDensity = LocalDensity.current
    val tiles by viewModel.tiles.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val selectedTileId by viewModel.selectedTileId.collectAsState()
    val isAddTileSheetOpen by viewModel.isAddTileSheetOpen.collectAsState()
    val isGlanceScreenOpen by viewModel.isGlanceScreenOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val accent = LumiaPalette.resolveEffectiveAccent(settings)
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

    // Back press handling: exit Glance -> deselect single tile -> exit Home Edit Mode -> return to Default Home Page
    BackHandler(
        enabled = isGlanceScreenOpen || selectedTileId != null || isEditMode || pagerState.currentPage != defaultPageIndex
    ) {
        when {
            isGlanceScreenOpen -> viewModel.closeGlanceScreen()
            selectedTileId != null -> viewModel.clearSelectedTile()
            isEditMode -> viewModel.closeEditMode()
            pagerState.currentPage != defaultPageIndex -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(defaultPageIndex)
                }
            }
        }
    }

    val currentPage = pages.getOrNull(pagerState.currentPage)
    val editModeZoomSpring = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.76f,
        stiffness = 360f
    )
    val pagerHorizontalPadding by animateDpAsState(
        targetValue = if (isEditMode) 28.dp else 0.dp,
        animationSpec = editModeZoomSpring,
        label = "oneUiZoomOutHorizontalPadding"
    )
    val pagerTopPadding by animateDpAsState(
        targetValue = if (isEditMode) 48.dp else 0.dp,
        animationSpec = editModeZoomSpring,
        label = "oneUiZoomOutTopPadding"
    )
    val pagerBottomPadding by animateDpAsState(
        targetValue = if (isEditMode) 104.dp else 0.dp,
        animationSpec = editModeZoomSpring,
        label = "oneUiZoomOutBottomPadding"
    )
    val pagerCornerRadius by animateDpAsState(
        targetValue = if (isEditMode) 24.dp else 0.dp,
        animationSpec = editModeZoomSpring,
        label = "oneUiZoomOutCornerRadius"
    )
    val floatingIndicatorBottomPadding by animateDpAsState(
        targetValue = if (isEditMode) 72.dp else 6.dp,
        animationSpec = editModeZoomSpring,
        label = "floatingIndicatorBottomPadding"
    )

    // State for direct drag-and-drop page reordering on zoomed-out page card
    var draggingPageIndex by remember { mutableIntStateOf(-1) }
    var pageDragOffsetX by remember { mutableFloatStateOf(0f) }
    val pageSwapThresholdPx = with(uiDensity) { 96.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wallpaperBrush)
            .pointerInput(isEditMode, selectedTileId) {
                detectTapGestures(
                    onTap = {
                        when {
                            selectedTileId != null -> viewModel.clearSelectedTile()
                            isEditMode -> viewModel.closeEditMode()
                        }
                    },
                    onLongPress = {
                        if (selectedTileId != null) {
                            viewModel.clearSelectedTile()
                        } else {
                            // Long-pressing empty screen area enters One UI Home Screen Edit Mode
                            viewModel.enterEditMode()
                        }
                    }
                )
            }
    ) {
        // 1. Full-Screen HorizontalPager (Takes 100% of screen area; page indicator floats on top)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("launcher_horizontal_pager")
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
            val isPageDefaultHome = page.pageType == PageType.HOME && page.id == settings.defaultHomePageId
            val isThisPageDragging = isEditMode && draggingPageIndex == pageIndex

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = pagerHorizontalPadding,
                        end = pagerHorizontalPadding,
                        top = pagerTopPadding,
                        bottom = pagerBottomPadding
                    )
                    .zIndex(if (isThisPageDragging) 10f else 1f)
                    .then(
                        if (isThisPageDragging) {
                            Modifier
                                .offset { IntOffset(pageDragOffsetX.roundToInt(), 0) }
                                .shadow(24.dp, RoundedCornerShape(pagerCornerRadius))
                        } else Modifier
                    )
                    .graphicsLayer {
                        val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                        when (settings.pageTransitionEffect) {
                            PageTransitionEffect.CLASSIC_SLIDE -> {
                                alpha = 1f - (absOffset * 0.12f)
                            }
                            PageTransitionEffect.TURNSTILE_3D -> {
                                cameraDistance = 16f * density
                                rotationY = -pageOffset * 28f
                                val scale = 1f - (absOffset * 0.08f)
                                scaleX = if (isThisPageDragging) 1.03f else scale
                                scaleY = if (isThisPageDragging) 1.03f else scale
                                alpha = 1f - (absOffset * 0.25f)
                            }
                            PageTransitionEffect.MIUI_CUBE -> {
                                cameraDistance = 18f * density
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = if (pageOffset < 0f) 0f else 1f,
                                    pivotFractionY = 0.5f
                                )
                                rotationY = pageOffset * 55f
                                alpha = 1f - (absOffset * 0.2f)
                            }
                            PageTransitionEffect.ONE_UI_DEPTH -> {
                                val scale = 1f - (absOffset * 0.16f)
                                scaleX = if (isThisPageDragging) 1.03f else scale
                                scaleY = if (isThisPageDragging) 1.03f else scale
                                alpha = 1f - (absOffset * 0.35f)
                            }
                        }
                    }
                    .clip(RoundedCornerShape(pagerCornerRadius))
                    .then(
                        if (isEditMode) {
                            Modifier
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(
                                    width = if (isPageDefaultHome) 2.dp else 1.2.dp,
                                    color = if (isPageDefaultHome) accent.color.copy(alpha = 0.9f)
                                    else Color.White.copy(alpha = 0.38f),
                                    shape = RoundedCornerShape(pagerCornerRadius)
                                )
                        } else Modifier
                    )
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
                        selectedTileId = selectedTileId,
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
                        onSelectTileForEdit = { tileId ->
                            // Long-pressing a tile ONLY edits that single tile!
                            viewModel.selectTileForEdit(tileId)
                        },
                        onResizeTile = { id -> viewModel.resizeTile(id) },
                        onStepTileSize = { id, increase -> viewModel.stepTileSize(id, increase) },
                        onSetTileSize = { id, newSize -> viewModel.setTileSize(id, newSize) },
                        onUnpinTile = { id -> viewModel.unpinTile(id) },
                        onMoveTile = { id, up -> viewModel.moveTileInPage(id, page.id, up) },
                        onMoveTileToGrid = { id, gx, gy, cols ->
                            viewModel.moveTileToGridPosition(id, gx, gy, cols)
                        },
                        onNudgeTile = { id, dx, dy, cols ->
                            viewModel.nudgeTileByDelta(id, dx, dy, cols)
                        },
                        onReorderTiles = { from, to -> viewModel.reorderTilesInPage(page.id, from, to) },
                        onMoveTileToPage = { tileId, targetPageId ->
                            viewModel.moveTileToPage(tileId, targetPageId)
                        },
                        onCustomizeTile = { tileId, colorHex, customTitle, isLive ->
                            viewModel.updateTileCustomization(tileId, colorHex, customTitle, isLive)
                        },
                        onPinNewTile = { tile -> viewModel.pinTile(tile) },
                        onEnterEditMode = { viewModel.enterEditMode() },
                        onExitEditMode = { viewModel.closeEditMode() },
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
                        onEnterEditMode = { viewModel.enterEditMode() }
                    )
                }
            }
        }

        // 2. Samsung One UI Home Edit Top Bar (Floating at TopCenter in Edit Mode: ONLY Home icon to set default + Drag handle to reorder page)
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            OneUiHomeEditTopBar(
                currentPage = currentPage,
                homePagesCount = homePages.size,
                isDefaultHome = currentPage?.id == settings.defaultHomePageId && currentPage.pageType == PageType.HOME,
                accentColor = accent.color,
                onSetDefaultHome = {
                    if (currentPage != null && currentPage.pageType == PageType.HOME) {
                        viewModel.setDefaultHomePage(currentPage.id)
                    }
                },
                onDeleteCurrentPage = {
                    if (currentPage != null && currentPage.pageType == PageType.HOME && homePages.size > 1) {
                        viewModel.deleteHomePage(currentPage.id)
                    }
                },
                onDragPageDelta = { deltaX ->
                    draggingPageIndex = pagerState.currentPage
                    pageDragOffsetX += deltaX
                    val currentIdx = pagerState.currentPage
                    if (pageDragOffsetX <= -pageSwapThresholdPx && currentIdx > 0) {
                        pageDragOffsetX = 0f
                        draggingPageIndex = currentIdx - 1
                        viewModel.movePage(currentIdx, currentIdx - 1)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(currentIdx - 1)
                        }
                    } else if (pageDragOffsetX >= pageSwapThresholdPx && currentIdx < pages.size - 1) {
                        pageDragOffsetX = 0f
                        draggingPageIndex = currentIdx + 1
                        viewModel.movePage(currentIdx, currentIdx + 1)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(currentIdx + 1)
                        }
                    }
                },
                onDragPageEnd = {
                    draggingPageIndex = -1
                    pageDragOffsetX = 0f
                }
            )
        }

        // 3. Compact Floating Page Indicator (Floats at BottomCenter without stealing display height; supports drag-and-drop page reordering in Edit Mode)
        OneUiPageDotsIndicator(
            pages = pages,
            currentPageIndex = pagerState.currentPage,
            defaultHomePageId = settings.defaultHomePageId,
            isEditMode = isEditMode,
            accentColor = accent.color,
            onSelectPage = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            onMovePage = { fromIndex, toIndex ->
                viewModel.movePage(fromIndex, toIndex)
                coroutineScope.launch {
                    pagerState.animateScrollToPage(toIndex)
                }
            },
            onAddBlankPage = {
                val newTitle = "Trang Home ${homePages.size + 1}"
                viewModel.addHomePage(newTitle)
            },
            onLongPress = { viewModel.enterEditMode() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = floatingIndicatorBottomPadding)
        )

        // 4. One UI Bottom Edit Bar (Floating at BottomCenter in Home Screen Edit Mode — 3 clean items: Trong suốt, Tiện ích, Cài đặt)
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            OneUiBottomEditBar(
                accentColor = accent.color,
                wp10TileStyle = settings.wp10TileStyle,
                onCycleTileTransparency = {
                    val styles = Wp10TileStyle.entries
                    val nextStyle = styles[(styles.indexOf(settings.wp10TileStyle) + 1) % styles.size]
                    val nextOpacity = when (nextStyle) {
                        Wp10TileStyle.CRYSTAL_CLEAR -> 0f
                        Wp10TileStyle.CLASSIC_METRO -> 1f
                        Wp10TileStyle.FLUENT_ACRYLIC -> 0.45f
                        Wp10TileStyle.TINTED_TRANSLUCENT -> 0.35f
                    }
                    viewModel.updateSettings(
                        settings.copy(
                            wp10TileStyle = nextStyle,
                            tileOpacity = nextOpacity
                        )
                    )
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
                onOpenSettings = { viewModel.openSettings() }
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

        // One UI Home Screen Settings Sheet ("Cài đặt Màn hình chờ") — auto-saves every change
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
private fun OneUiHomeEditTopBar(
    currentPage: LauncherPage?,
    homePagesCount: Int,
    isDefaultHome: Boolean,
    accentColor: Color,
    onSetDefaultHome: () -> Unit,
    onDeleteCurrentPage: () -> Unit,
    onDragPageDelta: (Float) -> Unit,
    onDragPageEnd: () -> Unit
) {
    val isHomePage = currentPage?.pageType == PageType.HOME

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 6.dp)
            .pointerInput(currentPage?.id) {
                detectHorizontalDragGestures(
                    onDragEnd = { onDragPageEnd() },
                    onDragCancel = { onDragPageEnd() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        onDragPageDelta(dragAmount)
                    }
                )
            }
    ) {
        // Center: Samsung One UI Default Home Icon ONLY (Selected = bright white/accent, Unselected = dimmed white)
        if (isHomePage) {
            val iconTint by animateColorAsState(
                targetValue = if (isDefaultHome) Color.White else Color.White.copy(alpha = 0.42f),
                label = "oneUiDefaultHomeIconTint"
            )
            val badgeBg by animateColorAsState(
                targetValue = if (isDefaultHome) accentColor else Color.Black.copy(alpha = 0.36f),
                label = "oneUiDefaultHomeBadgeBg"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(badgeBg)
                    .border(
                        width = if (isDefaultHome) 1.5.dp else 0.8.dp,
                        color = if (isDefaultHome) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
                    .clickable { onSetDefaultHome() }
                    .testTag("set_default_home_icon_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Đặt làm Trang chủ mặc định",
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.36f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Tất cả ứng dụng",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Right: Subtle delete page icon ONLY when there are multiple Home pages
        if (isHomePage && homePagesCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable { onDeleteCurrentPage() }
                    .testTag("delete_current_page_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa trang hiện tại",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun OneUiPageDotsIndicator(
    pages: List<LauncherPage>,
    currentPageIndex: Int,
    defaultHomePageId: String,
    isEditMode: Boolean,
    accentColor: Color,
    onSelectPage: (Int) -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onAddBlankPage: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val slotStepPx = with(density) { 24.dp.toPx() }.coerceAtLeast(1f)
    var draggingDotIndex by remember { mutableIntStateOf(-1) }
    var dotDragAccumX by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = if (isEditMode) 0.44f else 0.22f))
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            }
            .padding(
                horizontal = if (isEditMode) 10.dp else 7.dp,
                vertical = if (isEditMode) 5.dp else 3.dp
            )
            .testTag("floating_page_indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isEditMode) 7.dp else 5.dp)
    ) {
        pages.forEachIndexed { index, page ->
            val isSelected = index == currentPageIndex
            val isDefaultHome = page.pageType == PageType.HOME && page.id == defaultHomePageId
            val isBeingDragged = isEditMode && draggingDotIndex == index

            val dragModifier = if (isEditMode) {
                Modifier
                    .offset {
                        IntOffset(
                            if (isBeingDragged) dotDragAccumX.roundToInt() else 0,
                            0
                        )
                    }
                    .pointerInput(pages.size, index) {
                        detectDragGestures(
                            onDragStart = {
                                draggingDotIndex = index
                                dotDragAccumX = 0f
                            },
                            onDragEnd = {
                                draggingDotIndex = -1
                                dotDragAccumX = 0f
                            },
                            onDragCancel = {
                                draggingDotIndex = -1
                                dotDragAccumX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dotDragAccumX += dragAmount.x
                                val activeIdx = if (draggingDotIndex >= 0) draggingDotIndex else index
                                if (dotDragAccumX <= -slotStepPx && activeIdx > 0) {
                                    val target = activeIdx - 1
                                    dotDragAccumX = 0f
                                    draggingDotIndex = target
                                    onMovePage(activeIdx, target)
                                } else if (dotDragAccumX >= slotStepPx && activeIdx < pages.size - 1) {
                                    val target = activeIdx + 1
                                    dotDragAccumX = 0f
                                    draggingDotIndex = target
                                    onMovePage(activeIdx, target)
                                }
                            }
                        )
                    }
            } else Modifier

            when {
                page.pageType == PageType.ALL_APPS -> {
                    Box(
                        modifier = dragModifier
                            .size(if (isEditMode) 16.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.22f))
                            .clickable { onSelectPage(index) }
                            .testTag("page_indicator_all_apps"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = page.title,
                            tint = Color.White,
                            modifier = Modifier.size(if (isEditMode) 10.dp else 8.dp)
                        )
                    }
                }
                isDefaultHome -> {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = page.title,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = dragModifier
                            .size(if (isEditMode) 15.dp else 11.dp)
                            .clickable { onSelectPage(index) }
                            .testTag("page_indicator_$index")
                    )
                }
                else -> {
                    val dotWidth by animateDpAsState(
                        targetValue = when {
                            isEditMode && isSelected -> 14.dp
                            isEditMode -> 7.dp
                            isSelected -> 10.dp
                            else -> 5.dp
                        },
                        label = "oneUiDotWidth"
                    )
                    val dotHeight = if (isEditMode) 7.dp else 5.dp
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.38f),
                        label = "oneUiDotColor"
                    )
                    Box(
                        modifier = dragModifier
                            .height(dotHeight)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable { onSelectPage(index) }
                            .testTag("page_indicator_$index")
                    )
                }
            }
        }

        if (isEditMode) {
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable { onAddBlankPage() }
                    .testTag("add_blank_page_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm trang mới",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun OneUiBottomEditBar(
    accentColor: Color,
    wp10TileStyle: Wp10TileStyle,
    onCycleTileTransparency: () -> Unit,
    onAddTile: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag("edit_mode_settings_bar"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OneUiBottomActionItem(
            icon = Icons.Default.BlurOn,
            label = when (wp10TileStyle) {
                Wp10TileStyle.CRYSTAL_CLEAR -> "Trong suốt"
                Wp10TileStyle.FLUENT_ACRYLIC -> "Acrylic"
                Wp10TileStyle.TINTED_TRANSLUCENT -> "W10M Trong"
                Wp10TileStyle.CLASSIC_METRO -> "Metro Đặc"
            },
            accentHighlight = if (wp10TileStyle != Wp10TileStyle.CLASSIC_METRO) accentColor.copy(alpha = 0.5f) else null,
            onClick = onCycleTileTransparency,
            testTag = "cycle_tile_transparency_button"
        )

        OneUiBottomActionItem(
            icon = Icons.Default.Add,
            label = "Tiện ích",
            onClick = onAddTile,
            testTag = "add_tile_button"
        )

        OneUiBottomActionItem(
            icon = Icons.Default.Settings,
            label = "Cài đặt",
            onClick = onOpenSettings,
            testTag = "open_settings_button"
        )
    }
}

@Composable
private fun OneUiBottomActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    accentHighlight: Color? = null
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentHighlight ?: Color.Black.copy(alpha = 0.42f))
                .border(0.8.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(0f, 1f),
                    blurRadius = 3f
                )
            )
        )
    }
}
