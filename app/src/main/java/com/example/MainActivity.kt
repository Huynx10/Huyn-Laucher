package com.example

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.content.Context
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.view.WindowManager
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.data.model.TileEntity
import com.example.data.model.WidgetProviderItem
import com.example.data.model.WallpaperSource
import com.example.data.model.Wp10TileStyle
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.components.AppDrawer
import com.example.ui.components.GlanceScreenOverlay
import com.example.ui.components.LumiaStartGrid
import com.example.ui.components.SettingsSheet
import com.example.ui.components.CleanSettingsScreen
import com.example.ui.components.SmartFolderExpandedOverlay
import com.example.ui.components.resolveLinkedAppForTile
import com.example.ui.theme.LumiaPalette
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CloudAuthState
import com.example.ui.viewmodel.LauncherViewModel
import com.example.ui.widget.LumiaWidgetHostManager
import com.example.ui.widget.DefaultWidgetMatcher
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels { LauncherViewModel.Factory }

    @Suppress("DEPRECATION")
    fun openWidgetSettings(tile: TileEntity) {
        val manager = LumiaWidgetHostManager.getInstance(this)
        val result = manager.startWidgetReconfiguration(tile) { boundId ->
            manager.appWidgetHost.startAppWidgetConfigureActivityForResult(
                this, boundId, 0, 8201, null
            )
        }
        // A canceled provider screen has no pin/rebind callback: retain widget ID and layout.
        val message = when (result) {
            com.example.ui.widget.WidgetReconfigureResult.ACTIVITY_MISSING ->
                "Ứng dụng không mở được cài đặt widget."
            com.example.ui.widget.WidgetReconfigureResult.ACCESS_DENIED ->
                "Ứng dụng không cho mở cài đặt widget."
            com.example.ui.widget.WidgetReconfigureResult.FAILED ->
                "Không mở được cài đặt widget."
            com.example.ui.widget.WidgetReconfigureResult.STARTED,
            com.example.ui.widget.WidgetReconfigureResult.UNSUPPORTED -> null
        }
        message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show Samsung/Android's actual Home wallpaper (including live wallpapers) behind
        // a transparent Compose surface. Do not read or copy protected wallpaper pixels.
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = settings.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (settings.wallpaperSource == WallpaperSource.SYSTEM) Color.Transparent else if (settings.isDarkTheme) Color.Black else Color(0xFFF2F2F2)
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
        viewModel.refreshApps(fromResume = true)
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
    val isSavingSettings by viewModel.isSettingsBusy.collectAsStateWithLifecycle()
    var advancedSettingsOpen by rememberSaveable { mutableStateOf(false) }
    val firstRunPrefs = remember(context) {
        context.getSharedPreferences("lumia_first_run", Context.MODE_PRIVATE)
    }
    var showFirstRunGuide by rememberSaveable {
        mutableStateOf(!firstRunPrefs.getBoolean("guided_v1", false))
    }

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
    val widgetSetupPrefs = remember(context) {
        context.getSharedPreferences("lumia_default_widget_setup", Context.MODE_PRIVATE)
    }
    var showDefaultWidgetOffer by rememberSaveable { mutableStateOf(false) }
    var defaultWidgetQueue by remember { mutableStateOf<List<Pair<String, WidgetProviderItem>>>(emptyList()) }

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
                defaultWidgetQueue = emptyList()
            }
        }
    }

    fun launchConfigureOrFinalize(req: PendingWidgetBindRequest) {
        val providerInfo = widgetHostManager.findProviderInfo(req.widgetItem.providerComponent)
        val configureComponent = providerInfo?.configure ?: req.widgetItem.configureComponent.takeIf { it.isNotBlank() }?.let {
            ComponentName.unflattenFromString(it)
        }
        // Android 12+ providers may explicitly allow using their default configuration.
        // Configuration is skipped ONLY when optional AND reconfigurable flags are set.
        val optionalConfiguration = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            providerInfo != null &&
            (providerInfo.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0 &&
            (providerInfo.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE) != 0
        if (configureComponent != null && !optionalConfiguration) {
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
                Toast.makeText(context, "Đã hủy liên kết widget.", Toast.LENGTH_SHORT).show()
                defaultWidgetQueue = emptyList()
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

    // Offer one initial setup for factory tiles only. Never replace user-linked widgets
    // or modify an existing layout until the user explicitly agrees.
    val suggestedDefaultWidgets = remember(tiles, installedWidgets) {
        DefaultWidgetMatcher.suggestions(tiles, installedWidgets)
    }
    LaunchedEffect(suggestedDefaultWidgets, isSettingsOpen, isEditMode, showFirstRunGuide) {
        if (!showFirstRunGuide && !widgetSetupPrefs.getBoolean("offered_v1", false) &&
            suggestedDefaultWidgets.isNotEmpty() && !isSettingsOpen && !isEditMode
        ) {
            showDefaultWidgetOffer = true
        }
    }
    LaunchedEffect(defaultWidgetQueue, pendingWidgetRequest) {
        if (pendingWidgetRequest == null && defaultWidgetQueue.isNotEmpty()) {
            val (tileId, provider) = defaultWidgetQueue.first()
            defaultWidgetQueue = defaultWidgetQueue.drop(1)
            // Confirm the tile still exists and has not already been linked.
            val tile = tiles.firstOrNull { it.id == tileId && !it.hasBoundOrLinkedWidget() }
            if (tile != null) {
                startBindAndPinWidgetFlow(provider, tile.pageId, linkToTileId = tile.id)
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

    // Keep drag ownership above pager pages so page changes cannot reset the drag session.
    var draggingHomeTileId by remember { mutableStateOf<String?>(null) }
    var draggingHomeTileSourcePageId by remember { mutableStateOf<String?>(null) }
    var dragFingerX by remember { mutableFloatStateOf(0.5f) }
    var dragFingerY by remember { mutableFloatStateOf(0.5f) }
    val dragEdgeDirection = when {
        draggingHomeTileId == null || settings.lockHomeLayout -> 0
        dragFingerX <= 0.10f -> -1
        dragFingerX >= 0.90f -> 1
        else -> 0
    }

    // Hover deliberately at the edge to visit the adjacent HOME page without lifting.
    // Only active while dragging: no polling or background work when idle.
    LaunchedEffect(draggingHomeTileId, dragEdgeDirection, pagerState.currentPage) {
        val activeTileId = draggingHomeTileId
        val direction = dragEdgeDirection
        if (activeTileId != null && direction != 0) {
            delay(550)
            val next = pagerState.currentPage + direction
            if (draggingHomeTileId == activeTileId &&
                dragEdgeDirection == direction &&
                pages.getOrNull(next)?.pageType == PageType.HOME
            ) {
                pagerState.scrollToPage(next)
            }
        }
    }

    // Back press handling: exit Action Center -> exit Smart Folder -> exit Glance -> deselect single tile -> exit Home Edit Mode -> return to Default Home Page
    BackHandler(
        enabled = draggingHomeTileId != null || activeSmartFolderRequest != null || isGlanceScreenOpen || selectedTileId != null || isEditMode || pagerState.currentPage != defaultPageIndex
    ) {
        when {
            draggingHomeTileId != null -> {
                val sourceIndex = pages.indexOfFirst { it.id == draggingHomeTileSourcePageId }
                draggingHomeTileId = null
                draggingHomeTileSourcePageId = null
                if (sourceIndex >= 0) coroutineScope.launch { pagerState.scrollToPage(sourceIndex) }
            }
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
            dampingRatio = 1f,
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
    val safePagerHorizontalPadding = nonNegativeLayoutDp(pagerHorizontalPadding)
    val safePagerTopPadding = nonNegativeLayoutDp(pagerTopPadding)
    val safePagerBottomPadding = nonNegativeLayoutDp(pagerBottomPadding)
    val safePagerCornerRadius = nonNegativeLayoutDp(pagerCornerRadius)
    val safeFloatingIndicatorBottomPadding = nonNegativeLayoutDp(floatingIndicatorBottomPadding)

    // State for direct drag-and-drop page reordering on zoomed-out page card
    var draggingPageIndex by remember { mutableIntStateOf(-1) }
    var pageDragOffsetX by remember { mutableFloatStateOf(0f) }
    val pageSwapThresholdPx = with(uiDensity) { 96.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (settings.wallpaperSource == WallpaperSource.LUMIA) Modifier.background(wallpaperBrush)
                else Modifier.background(Color.Transparent)
            )
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
        if (settings.wallpaperSource == WallpaperSource.CUSTOM && customWallpaperBitmap != null) {
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
            // Keep the original pointer-input node composed while a drag crosses pages.
            // Extra pages are composed only during the gesture, never in idle/background.
            beyondViewportPageCount = if (draggingHomeTileId != null) (pages.size - 1).coerceAtLeast(0)
                else if (isBatterySaverActive) 0 else 1,
            userScrollEnabled = draggingHomeTileId == null,
            key = { idx -> pages.getOrNull(idx)?.id ?: "page_$idx" },
            modifier = Modifier
                .fillMaxSize()
                .testTag("launcher_horizontal_pager")
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
            val isPageDefaultHome = page.pageType == PageType.HOME && page.id == settings.defaultHomePageId
            val isThisPageDragging = isEditMode && draggingPageIndex == pageIndex

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (page.pageType == PageType.HOME) {
                            Modifier.statusBarsPadding().navigationBarsPadding()
                        } else Modifier
                    )
                    .padding(
                        start = safePagerHorizontalPadding,
                        end = safePagerHorizontalPadding,
                        top = safePagerTopPadding,
                        bottom = safePagerBottomPadding
                    )
                    .zIndex(if (isThisPageDragging) 10f else 1f)
                    .then(
                        if (isThisPageDragging) {
                            Modifier
                                .offset { IntOffset(pageDragOffsetX.roundToInt(), 0) }
                                .shadow(24.dp, RoundedCornerShape(safePagerCornerRadius))
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
                    .clip(RoundedCornerShape(safePagerCornerRadius))
                    .then(
                        if (isEditMode) {
                            Modifier
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(
                                    width = if (isPageDefaultHome) 2.dp else 1.2.dp,
                                    color = if (isPageDefaultHome) accent.color.copy(alpha = 0.9f)
                                    else Color.White.copy(alpha = 0.38f),
                                    shape = RoundedCornerShape(safePagerCornerRadius)
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
                                    viewModel.optimizeMemory()
                                    Toast.makeText(
                                        context,
                                        "Đã xóa bộ nhớ đệm của launcher",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                tile.tileType == TileType.BATTERY -> {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (_: Exception) {
                                        viewModel.openSettings()
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
                                    Toast.makeText(context, "Không thể mở mục này", Toast.LENGTH_SHORT).show()
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
                        onSetTileSpans = { id, spanX, spanY -> viewModel.setTileSpans(id, spanX, spanY) },
                        onGroupTilesManually = { sourceId, targetId ->
                            viewModel.groupHomeTilesManually(sourceId, targetId)
                        },
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
                        onCrossPageDragStart = { tileId, fractionX, fractionY ->
                            if (!settings.lockHomeLayout && homePages.size > 1) {
                                draggingHomeTileId = tileId
                                draggingHomeTileSourcePageId = page.id
                                dragFingerX = fractionX
                                dragFingerY = fractionY
                            }
                        },
                        onCrossPageDragMove = { tileId, fractionX, fractionY ->
                            if (draggingHomeTileId == tileId) {
                                dragFingerX = fractionX
                                dragFingerY = fractionY
                            }
                        },
                        onCrossPageDragEnd = { tileId, gridX, gridY ->
                            val targetPage = pages.getOrNull(pagerState.currentPage)
                            val crossed = draggingHomeTileId == tileId &&
                                draggingHomeTileSourcePageId == page.id &&
                                targetPage?.pageType == PageType.HOME &&
                                targetPage.id != page.id
                            draggingHomeTileId = null
                            draggingHomeTileSourcePageId = null
                            if (crossed && targetPage != null) {
                                viewModel.moveTileToPageAtGridPosition(
                                    tileId, targetPage.id, gridX, gridY
                                ) { moved ->
                                    if (!moved) {
                                        Toast.makeText(context, "Vị trí này không trống. Ô Tile chưa được di chuyển.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                viewModel.clearSelectedTile()
                            }
                            crossed
                        },
                        onCrossPageDragCancel = { tileId ->
                            if (draggingHomeTileId == tileId) {
                                val sourceIndex = pages.indexOfFirst { it.id == draggingHomeTileSourcePageId }
                                draggingHomeTileId = null
                                draggingHomeTileSourcePageId = null
                                if (sourceIndex >= 0 && sourceIndex != pagerState.currentPage) {
                                    coroutineScope.launch { pagerState.scrollToPage(sourceIndex) }
                                }
                            }
                        },
                        onOpenWidgetSettings = { tile ->
                            (context as? MainActivity)?.openWidgetSettings(tile)
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
                        onOpenSmartFolder = { folderTile, pivotX, pivotY ->
                            viewModel.openSmartFolder(folderTile.id, pivotX, pivotY)
                        },
                        onLaunchSmartFolderItem = { item ->
                            when {
                                item.packageName.isNotEmpty() -> {
                                    val launched = viewModel.launchApp(item.packageName)
                                }
                                item.tileType == TileType.PERFORMANCE -> {
                                    viewModel.optimizeMemory()
                                    Toast.makeText(
                                        context,
                                        "Đã xóa bộ nhớ đệm của launcher",
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
                                    Toast.makeText(context, "Không thể mở mục này", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
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
                        onMutateSettings = { transform -> viewModel.mutateSettings(transform) },
                        onEnterEditMode = { viewModel.enterEditMode() }
                    )
                }
            }
        }

        // Lightweight preview when the original Tile's page is offscreen.
        // Never re-host an Android widget: only show its label during drag.
        if (draggingHomeTileId != null &&
            currentPage?.pageType == PageType.HOME &&
            currentPage.id != draggingHomeTileSourcePageId
        ) {
            val draggedTile = tiles.firstOrNull { it.id == draggingHomeTileId }
            val screen = LocalConfiguration.current
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (screen.screenWidthDp * dragFingerX).dp - 36.dp,
                        y = (screen.screenHeightDp * dragFingerY).dp - 36.dp
                    )
                    .size(72.dp)
                    .zIndex(40f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.color.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = draggedTile?.title?.take(12) ?: "Tile",
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
            Text(
                text = "Thả để chuyển ô sang ${currentPage.title}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .zIndex(41f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
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
        if (currentPage?.pageType == PageType.HOME || isEditMode) OneUiPageDotsIndicator(
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
                .padding(bottom = safeFloatingIndicatorBottomPadding)
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
                    viewModel.cycleTileAppearance(acrylicOpacity = 0.45f, tintedOpacity = 0.35f)
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

        if (showDefaultWidgetOffer && suggestedDefaultWidgets.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {
                    widgetSetupPrefs.edit().putBoolean("offered_v1", true).apply()
                    showDefaultWidgetOffer = false
                },
                title = { Text("Kết nối widget có sẵn") },
                text = {
                    Text("Lumia tìm thấy ${suggestedDefaultWidgets.size} widget phù hợp cho các ô mặc định. Kết nối để hiển thị dữ liệu từ ứng dụng trên máy. Android có thể yêu cầu cấp quyền.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        widgetSetupPrefs.edit().putBoolean("offered_v1", true).apply()
                        showDefaultWidgetOffer = false
                        defaultWidgetQueue = suggestedDefaultWidgets.map { it.first.id to it.second }
                    }) { Text("Kết nối") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        widgetSetupPrefs.edit().putBoolean("offered_v1", true).apply()
                        showDefaultWidgetOffer = false
                    }) { Text("Để sau") }
                }
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

        if (showFirstRunGuide && pages.isNotEmpty() && tiles.isNotEmpty() &&
            !isSettingsOpen && !isEditMode) {
            AlertDialog(
                onDismissRequest = {
                    firstRunPrefs.edit().putBoolean("guided_v1", true).apply()
                    showFirstRunGuide = false
                },
                title = { Text("Bắt đầu với Lumia") },
                text = {
                    Text(
                        "Đặt Lumia làm màn hình chính nếu muốn sử dụng hằng ngày.\n\n" +
                        "Widget lấy dữ liệu từ ứng dụng đã cài. Android có thể yêu cầu xác nhận hoặc chọn nội dung khi kết nối.\n\n" +
                        "Lumia dùng hình nền hiện tại. Chỉ cần chọn ảnh nếu muốn thay bằng ảnh riêng."
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        firstRunPrefs.edit().putBoolean("guided_v1", true).apply()
                        showFirstRunGuide = false
                    }) { Text("Tiếp tục") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        firstRunPrefs.edit().putBoolean("guided_v1", true).apply()
                        showFirstRunGuide = false
                    }) { Text("Để sau") }
                }
            )
        }

        // Main settings: clean One UI-style list. Rare controls remain under Advanced.
        if (isSettingsOpen) {
            if (!advancedSettingsOpen) {
                CleanSettingsScreen(
                    settings = settings,
                    cloudAuthState = cloudAuthState,
                    cloudBackups = cloudBackups,
                    isCloudSyncing = isCloudSyncing,
                    isSavingSettings = isSavingSettings,
                    onSaveSettings = { viewModel.updateSettings(it) },
                    onUpdateTileAppearance = { style, opacity -> viewModel.updateTileAppearance(style, opacity) },
                    onPickCustomWallpaper = { uri -> viewModel.setCustomWallpaper(uri) },
                    onExportBackup = { viewModel.exportBackup() },
                    onRestoreBackup = { viewModel.restoreBackup(it) },
                    onResetDefaults = { viewModel.resetDefaultsAwait() },
                    onSignInWithGoogle = {
                        coroutineScope.launch {
                            GoogleSignInHelper.signInInteractive(
                                activityContext = context,
                                onIdTokenReceived = { token -> viewModel.signInWithGoogleIdToken(token) },
                                onError = { error -> viewModel.onGoogleSignInError(error) }
                            )
                        }
                    },
                    onSignOutCloud = { viewModel.signOutFromCloud(context) },
                    onSyncToCloudNow = { viewModel.syncToCloudNow(it) },
                    onRestoreCloudBackup = { backup ->
                        viewModel.restoreFromCloudBackup(backup) { ok ->
                            if (ok) viewModel.closeSettings()
                        }
                    },
                    onOpenAdvanced = { advancedSettingsOpen = true },
                    onDismiss = { viewModel.closeSettings() }
                )
            } else SettingsSheet(
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
                onSaveSettings = { baseline, edited -> viewModel.updateAdvancedSettings(baseline, edited) },
                onUpdateTileAppearance = { style, opacity -> viewModel.updateTileAppearance(style, opacity) },
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
                onOptimizeMemory = { viewModel.optimizeMemory() },
                onRefreshTelemetry = { viewModel.refreshTelemetry() },
                onDismiss = { advancedSettingsOpen = false }
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
