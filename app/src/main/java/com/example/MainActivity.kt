package com.example

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.example.data.model.WidgetProviderItem
import com.example.data.model.Wp10TileStyle
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.components.ActionCenterOverlay
import com.example.ui.components.AppDrawer
import com.example.ui.components.GlanceScreenOverlay
import com.example.ui.components.LumiaStartGrid
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SmartFolderExpandedOverlay
import com.example.ui.components.resolveLinkedAppForTile
import com.example.ui.theme.LumiaPalette
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CloudAuthState
import com.example.ui.viewmodel.LauncherViewModel
import com.example.ui.widget.LumiaWidgetHostManager
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels { LauncherViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
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

    override fun onStart() {
        super.onStart()
        LumiaWidgetHostManager.getInstance(this).startListening()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }

    override fun onStop() {
        super.onStop()
        LumiaWidgetHostManager.getInstance(this).stopListening()
    }
}

private data class PendingWidgetBindRequest(
    val widgetItem: WidgetProviderItem,
    val appWidgetId: Int,
    val pageId: String,
    val linkToTileId: String? = null
)

@Composable
fun LumiaLauncherApp(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val uiDensity = LocalDensity.current
    val widgetHostManager = remember(context) { LumiaWidgetHostManager.getInstance(context) }
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val installedWidgets by viewModel.installedWidgets.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val selectedTileId by viewModel.selectedTileId.collectAsStateWithLifecycle()
    val isAddTileSheetOpen by viewModel.isAddTileSheetOpen.collectAsStateWithLifecycle()
    val isGlanceScreenOpen by viewModel.isGlanceScreenOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val activeSmartFolderRequest by viewModel.activeSmartFolderRequest.collectAsStateWithLifecycle()
    val cloudAuthState by viewModel.cloudAuthState.collectAsStateWithLifecycle()
    val cloudBackups by viewModel.cloudBackups.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val cloudSyncMessage by viewModel.cloudSyncMessage.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val requestSaver = remember(widgetHostManager) {
        Saver<PendingWidgetBindRequest?, ArrayList<String>>(
            save = { req -> req?.let { arrayListOf(it.widgetItem.providerComponent, it.appWidgetId.toString(), it.pageId, it.linkToTileId.orEmpty()) } ?: arrayListOf() },
            restore = { saved ->
                if (saved.size == 4) widgetHostManager.findProviderItem(saved[0])?.let {
                    PendingWidgetBindRequest(it, saved[1].toInt(), saved[2], saved[3].ifBlank { null })
                } else null
            }
        )
    }
    var pendingWidgetRequest by rememberSaveable(stateSaver = requestSaver) { mutableStateOf<PendingWidgetBindRequest?>(null) }

    fun cancelWidgetRequest(req: PendingWidgetBindRequest, message: String) {
        pendingWidgetRequest = null
        widgetHostManager.releaseWidgetId(req.appWidgetId)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun finalizeWidgetPinOrLink(req: PendingWidgetBindRequest) {
        pendingWidgetRequest = null
        if (widgetHostManager.inspectWidgetState(req.appWidgetId, req.widgetItem.providerComponent) !is com.example.ui.widget.WidgetHostState.Ready) {
            cancelWidgetRequest(req, "Widget chưa được cấp quyền liên kết.")
            return
        }
        if (!req.linkToTileId.isNullOrBlank()) {
            viewModel.linkWidgetToExistingTile(
                tileId = req.linkToTileId,
                widgetItem = req.widgetItem,
                appWidgetId = req.appWidgetId
            )
            Toast.makeText(
                context,
                "Đã liên kết Widget \"${req.widgetItem.label}\" vào Tile!",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.pinWidgetTile(
                widgetItem = req.widgetItem,
                appWidgetId = req.appWidgetId,
                pageId = req.pageId
            )
            Toast.makeText(
                context,
                "Đã ghim Widget \"${req.widgetItem.label}\" ra màn hình chính!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val widgetConfigureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val req = pendingWidgetRequest
        pendingWidgetRequest = null
        if (req != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                finalizeWidgetPinOrLink(req)
            } else {
                widgetHostManager.releaseWidgetId(req.appWidgetId)
            }
        }
    }

    fun launchConfigureOrFinalize(req: PendingWidgetBindRequest) {
        val providerInfo = widgetHostManager.findProviderInfo(req.widgetItem.providerComponent)
        val configureComponent = providerInfo?.configure ?: req.widgetItem.configureComponent.takeIf { it.isNotBlank() }?.let {
            ComponentName.unflattenFromString(it)
        }
        if (configureComponent != null) {
            try {
                pendingWidgetRequest = req
                val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = configureComponent
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, req.appWidgetId)
                }
                widgetConfigureLauncher.launch(configIntent)
            } catch (_: Exception) {
                cancelWidgetRequest(req, "Không thể mở cấu hình Widget. Vui lòng thử lại.")
            }
        } else {
            pendingWidgetRequest = null
            finalizeWidgetPinOrLink(req)
        }
    }

    val widgetBindPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val req = pendingWidgetRequest
        if (req != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                launchConfigureOrFinalize(req)
            } else {
                pendingWidgetRequest = null
                widgetHostManager.releaseWidgetId(req.appWidgetId)
                Toast.makeText(context, "Đã hủy cấp quyền gắn Widget.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startBindAndPinWidgetFlow(
        widgetItem: WidgetProviderItem,
        pageId: String,
        linkToTileId: String? = null,
        existingWidgetId: Int = -1
    ) {
        if (pendingWidgetRequest != null) return
        // Allocate a fresh ID: cancel must never delete a widget already saved on Home.
        val allocatedId = widgetHostManager.allocateWidgetId()
        if (allocatedId <= 0) {
            Toast.makeText(context, "Không thể cấp phát ID cho Widget trên thiết bị này.", Toast.LENGTH_SHORT).show()
            return
        }
        val req = PendingWidgetBindRequest(
            widgetItem = widgetItem,
            appWidgetId = allocatedId,
            pageId = pageId,
            linkToTileId = linkToTileId
        )
        val boundSilently = widgetHostManager.bindWidgetIfAllowed(allocatedId, widgetItem.providerComponent)
        if (boundSilently) {
            launchConfigureOrFinalize(req)
        } else {
            val permIntent = widgetHostManager.createBindPermissionIntent(allocatedId, widgetItem.providerComponent)
            if (permIntent != null) {
                try {
                    pendingWidgetRequest = req
                    widgetBindPermissionLauncher.launch(permIntent)
                } catch (_: Exception) {
                    cancelWidgetRequest(req, "Không thể mở hộp thoại cấp quyền Widget.")
                }
            } else {
                cancelWidgetRequest(req, "Nhà cung cấp Widget không hợp lệ.")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (cloudAuthState is CloudAuthState.Unauthenticated) {
            GoogleSignInHelper.trySilentSignIn(
                activityContext = context,
                onIdTokenReceived = { idToken ->
                    viewModel.signInWithGoogleIdToken(idToken)
                }
            )
        }
    }

    LaunchedEffect(cloudSyncMessage) {
        val msg = cloudSyncMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearCloudSyncMessage()
        }
    }
    val accent = LumiaPalette.resolveEffectiveAccent(settings)
    val wallpaperBrush = LumiaPalette.Wallpapers.getOrNull(settings.wallpaperGradientIndex)?.second
        ?: LumiaPalette.Wallpapers.first().second
    val customWallpaperBitmap = remember(settings.customWallpaperUri) {
        val path = settings.customWallpaperUri
        if (path.isNotBlank()) {
            try {
                val file = java.io.File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

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

    var isActionCenterOpen by remember { mutableStateOf(false) }

    // Back press handling: exit Action Center -> exit Smart Folder -> exit Glance -> deselect single tile -> exit Home Edit Mode -> return to Default Home Page
    BackHandler(
        enabled = isActionCenterOpen || activeSmartFolderRequest != null || isGlanceScreenOpen || selectedTileId != null || isEditMode || pagerState.currentPage != defaultPageIndex
    ) {
        when {
            isActionCenterOpen -> isActionCenterOpen = false
            activeSmartFolderRequest != null -> viewModel.closeSmartFolder()
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
    val isBatterySaverActive = remember(
        settings.batterySaverMode,
        settings.batterySaverThresholdPct,
        telemetry.batteryPct,
        telemetry.isCharging,
        telemetry.isSystemPowerSaveMode
    ) {
        settings.isBatterySaverActive(telemetry)
    }
    val effectiveTransition = remember(settings.pageTransitionEffect, isBatterySaverActive) {
        settings.effectivePageTransitionEffect(telemetry)
    }
    val editModeZoomSpring = remember {
        androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = 0.76f,
            stiffness = 360f
        )
    }
    val pagerHorizontalPadding = if (isBatterySaverActive) {
        if (isEditMode) 28.dp else 0.dp
    } else {
        val v by animateDpAsState(
            targetValue = if (isEditMode) 28.dp else 0.dp,
            animationSpec = editModeZoomSpring,
            label = "oneUiZoomOutHorizontalPadding"
        )
        v
    }
    val pagerTopPadding = if (isBatterySaverActive) {
        if (isEditMode) 48.dp else 0.dp
    } else {
        val v by animateDpAsState(
            targetValue = if (isEditMode) 48.dp else 0.dp,
            animationSpec = editModeZoomSpring,
            label = "oneUiZoomOutTopPadding"
        )
        v
    }
    val pagerBottomPadding = if (isBatterySaverActive) {
        if (isEditMode) 104.dp else 0.dp
    } else {
        val v by animateDpAsState(
            targetValue = if (isEditMode) 104.dp else 0.dp,
            animationSpec = editModeZoomSpring,
            label = "oneUiZoomOutBottomPadding"
        )
        v
    }
    val pagerCornerRadius = if (isBatterySaverActive) {
        if (isEditMode) 24.dp else 0.dp
    } else {
        val v by animateDpAsState(
            targetValue = if (isEditMode) 24.dp else 0.dp,
            animationSpec = editModeZoomSpring,
            label = "oneUiZoomOutCornerRadius"
        )
        v
    }
    val floatingIndicatorBottomPadding = if (isBatterySaverActive) {
        if (isEditMode) 72.dp else 6.dp
    } else {
        val v by animateDpAsState(
            targetValue = if (isEditMode) 72.dp else 6.dp,
            animationSpec = editModeZoomSpring,
            label = "floatingIndicatorBottomPadding"
        )
        v
    }

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
        if (customWallpaperBitmap != null) {
            Image(
                bitmap = customWallpaperBitmap,
                contentDescription = "Hình nền tùy chỉnh",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (settings.wallpaperDimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.wallpaperDimAlpha.coerceIn(0f, 0.75f)))
            )
        }

        // 1. Full-Screen HorizontalPager (Blurs dynamically when Action Center Glassmorphism overlay is open)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = if (isBatterySaverActive) 0 else 1,
            key = { idx -> pages.getOrNull(idx)?.id ?: "page_$idx" },
            modifier = Modifier
                .fillMaxSize()
                .then(if (isActionCenterOpen) Modifier.blur(18.dp) else Modifier)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("launcher_horizontal_pager")
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
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
                        val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                        val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                        when (effectiveTransition) {
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
                            val linkedApp = resolveLinkedAppForTile(tile, apps)
                            when {
                                tile.packageName.isNotEmpty() && !tile.packageName.startsWith("{") -> {
                                    viewModel.launchApp(tile.packageName)
                                }
                                tile.tileType == TileType.PERFORMANCE -> {
                                    val fresh = viewModel.optimizeMemory()
                                    Toast.makeText(
                                        context,
                                        "Đã đóng ${fresh.lastStoppedAppsCount} app chạy ngầm • Giải phóng +${fresh.lastFreedRamMb} MB RAM (Trống: ${fresh.availableRamMb} MB)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                tile.tileType == TileType.BATTERY -> {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (_: Exception) {
                                        isActionCenterOpen = true
                                    }
                                }
                                linkedApp != null -> {
                                    viewModel.launchApp(linkedApp.packageName)
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
                                else -> {
                                    isActionCenterOpen = true
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
                        onOpenGlance = { viewModel.openGlanceScreen() },
                        installedApps = apps,
                        installedWidgets = installedWidgets,
                        onPickAndBindWidget = { widgetItem, targetPageId, linkTileId ->
                            startBindAndPinWidgetFlow(
                                widgetItem = widgetItem,
                                pageId = targetPageId,
                                linkToTileId = linkTileId
                            )
                        },
                        onRequestRebindWidget = { targetTile ->
                            val matchedWidget = installedWidgets.find { it.providerComponent == targetTile.widgetProvider }
                                ?: WidgetProviderItem(
                                    providerComponent = targetTile.widgetProvider,
                                    className = targetTile.widgetProvider.substringAfter("/", ""),
                                    packageName = targetTile.packageName.ifBlank {
                                        targetTile.widgetProvider.substringBefore("/")
                                    },
                                    appName = targetTile.title,
                                    label = targetTile.title,
                                    minWidthDp = targetTile.widgetMinSpanX * 70,
                                    minHeightDp = targetTile.widgetMinSpanY * 70,
                                    minResizeWidthDp = targetTile.widgetMinSpanX * 70,
                                    minResizeHeightDp = targetTile.widgetMinSpanY * 70,
                                    targetCellWidth = targetTile.widgetMinSpanX,
                                    targetCellHeight = targetTile.widgetMinSpanY,
                                    resizeMode = targetTile.widgetResizeMode
                                )
                            startBindAndPinWidgetFlow(
                                widgetItem = matchedWidget,
                                pageId = targetTile.pageId,
                                linkToTileId = targetTile.id,
                                existingWidgetId = targetTile.appWidgetId
                            )
                        },
                        onUnlinkWidgetFromTile = { tileId ->
                            viewModel.unlinkWidgetFromTile(tileId)
                            Toast.makeText(context, "Đã gỡ liên kết Widget khỏi ô Tile.", Toast.LENGTH_SHORT).show()
                        },
                        onOpenSmartFolder = { folderTile, pivotX, pivotY ->
                            viewModel.openSmartFolder(folderTile.id, pivotX, pivotY)
                        },
                        onLaunchSmartFolderItem = { item ->
                            when {
                                item.packageName.isNotEmpty() -> {
                                    val launched = viewModel.launchApp(item.packageName)
                                }
                                item.tileType == TileType.PERFORMANCE -> {
                                    val fresh = viewModel.optimizeMemory()
                                    Toast.makeText(
                                        context,
                                        "Đã tối ưu hệ thống! RAM trống: ${fresh.availableRamMb} MB",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                item.tileType == TileType.BATTERY -> {
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
                                item.tileType == TileType.CLOCK_CALENDAR -> {
                                    try {
                                        context.startActivity(
                                            Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (_: Exception) {
                                        viewModel.openGlanceScreen()
                                    }
                                }
                                item.tileType == TileType.NOTES -> {
                                    viewModel.openSettings()
                                }
                                else -> {
                                    Toast.makeText(context, "Đang mở: ${item.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCreateSmartFolderByCategory = { category, absorbExisting ->
                            viewModel.createSmartFolderByCategory(
                                category = category,
                                pageId = page.id,
                                absorbMatchingPageTiles = absorbExisting
                            )
                            Toast.makeText(
                                context,
                                "Đã thêm Thư mục Thông minh: ${category.label}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onGroupPageTilesByCategory = {
                            viewModel.groupPageTilesByCategory(page.id) { count ->
                                Toast.makeText(
                                    context,
                                    "Đã tự động gom $count Thư mục Thông minh theo danh mục!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onConvertTileToSmartFolder = { tileId ->
                            viewModel.convertTileIntoSmartFolder(tileId, absorbSameCategoryOnPage = true)
                            Toast.makeText(
                                context,
                                "Đã gom các Tile cùng danh mục vào Thư mục Thông minh!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onCycleSmartFolderMode = { tileId ->
                            viewModel.cycleSmartFolderLayoutMode(tileId)
                        }
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
                        installedWidgets = installedWidgets,
                        onPinWidgetToPage = { widgetItem, targetPageId ->
                            startBindAndPinWidgetFlow(
                                widgetItem = widgetItem,
                                pageId = targetPageId,
                                linkToTileId = null
                            )
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
                onGroupSmartFolders = {
                    val targetPageId = currentPage?.takeIf { it.pageType == PageType.HOME }?.id
                        ?: settings.defaultHomePageId
                    viewModel.groupPageTilesByCategory(targetPageId) { count ->
                        Toast.makeText(
                            context,
                            "Đã gom $count Thư mục Thông minh theo danh mục!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onOpenSettings = { viewModel.openSettings() }
            )
        }

        // MIUI / HyperOS Expanded Smart Folder Overlay with origin-aware spring opening animation
        activeSmartFolderRequest?.let { req ->
            val folderTile = tiles.find { it.id == req.tileId && it.tileType == TileType.SMART_FOLDER }
            if (folderTile != null) {
                SmartFolderExpandedOverlay(
                    folderTile = folderTile,
                    pivotFractionX = req.pivotFractionX,
                    pivotFractionY = req.pivotFractionY,
                    settings = settings,
                    installedApps = apps,
                    onLaunchItem = { item ->
                        when {
                            item.packageName.isNotEmpty() -> {
                                viewModel.launchApp(item.packageName)
                            }
                            item.tileType == TileType.PERFORMANCE -> {
                                val fresh = viewModel.optimizeMemory()
                                Toast.makeText(
                                    context,
                                    "Đã tối ưu hệ thống! RAM trống: ${fresh.availableRamMb} MB",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            item.tileType == TileType.BATTERY -> {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {}
                            }
                            item.tileType == TileType.CLOCK_CALENDAR -> {
                                viewModel.openGlanceScreen()
                            }
                            item.tileType == TileType.NOTES -> {
                                viewModel.openSettings()
                            }
                            else -> {
                                Toast.makeText(context, "Đang mở: ${item.title}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onUpdateFolder = { newTitle, newConfig ->
                        viewModel.updateSmartFolderConfig(folderTile.id, newTitle, newConfig)
                    },
                    onUngroupFolder = {
                        viewModel.ungroupSmartFolderToTiles(folderTile.id)
                    },
                    onDismiss = { viewModel.closeSmartFolder() }
                )
            }
        }

        // Minimalist Windows Action Center Pull Pill at Top-Right when on Home Screen
        AnimatedVisibility(
            visible = !isEditMode && !isSettingsOpen && !isActionCenterOpen && activeSmartFolderRequest == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 4.dp, end = 12.dp)
                .zIndex(15f)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .clickable { isActionCenterOpen = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("action_center_trigger_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BlurOn,
                    contentDescription = "Mở Action Center",
                    tint = accent.color,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Action Center • ${telemetry.batteryPct}%",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Windows 11 / W10M Fluent Acrylic Action Center Overlay
        if (isActionCenterOpen) {
            ActionCenterOverlay(
                settings = settings,
                telemetry = telemetry,
                customWallpaperBitmap = customWallpaperBitmap,
                onUpdateSettings = { viewModel.updateSettings(it) },
                onOptimizeMemory = { viewModel.optimizeMemory() },
                onOpenGlance = { viewModel.openGlanceScreen() },
                onOpenFullSettings = { viewModel.openSettings() },
                onDismiss = { isActionCenterOpen = false }
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
                customWallpaperBitmap = customWallpaperBitmap,
                cloudAuthState = cloudAuthState,
                cloudBackups = cloudBackups,
                isCloudSyncing = isCloudSyncing,
                onSignInWithGoogle = {
                    coroutineScope.launch {
                        GoogleSignInHelper.signInInteractive(
                            activityContext = context,
                            onIdTokenReceived = { idToken ->
                                viewModel.signInWithGoogleIdToken(idToken)
                            },
                            onError = { errMsg ->
                                viewModel.onGoogleSignInError(errMsg)
                            }
                        )
                    }
                },
                onSignOutCloud = { viewModel.signOutFromCloud(context) },
                onSyncToCloudNow = { title -> viewModel.syncToCloudNow(title) },
                onRestoreCloudBackup = { backup ->
                    viewModel.restoreFromCloudBackup(backup) { ok ->
                        if (ok) viewModel.closeSettings()
                    }
                },
                onDeleteCloudBackup = { backupId -> viewModel.deleteCloudBackup(backupId) },
                onSaveSettings = { viewModel.updateSettings(it) },
                onPickCustomWallpaper = { uri ->
                    viewModel.setCustomWallpaper(uri)
                    Toast.makeText(context, "Đã áp dụng hình nền mới!", Toast.LENGTH_SHORT).show()
                },
                onClearCustomWallpaper = {
                    viewModel.clearCustomWallpaper()
                    Toast.makeText(context, "Đã chuyển về hình nền Gradient", Toast.LENGTH_SHORT).show()
                },
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
    onGroupSmartFolders: () -> Unit = {},
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
            icon = Icons.Default.FolderSpecial,
            label = "Gom Nhóm",
            accentHighlight = accentColor.copy(alpha = 0.42f),
            onClick = onGroupSmartFolders,
            testTag = "group_smart_folders_bar_button"
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
