package com.example.ui.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SizeF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.data.model.TileSize
import com.example.data.model.WidgetProviderItem
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

sealed interface WidgetHostState {
    data class Ready(
        val appWidgetId: Int,
        val providerInfo: AppWidgetProviderInfo
    ) : WidgetHostState

    data class NeedsRebind(
        val providerInfo: AppWidgetProviderInfo,
        val providerItem: WidgetProviderItem,
        val reason: String
    ) : WidgetHostState

    data class ProviderMissing(
        val packageName: String,
        val providerComponent: String
    ) : WidgetHostState
}

/**
 * Custom [AppWidgetHostView] tailored for Lumia Start Screen Tiles:
 * - Preserves native internal widget interactions, layout, and scrollable lists (ListView, ScrollView, StackView).
 * - Disambiguates vertical/horizontal gestures inside scrollable widgets from the Start Screen's verticalScroll & HorizontalPager.
 * - Detects long-press to trigger Lumia Single-Tile Edit Mode without breaking normal widget taps.
 * - Gracefully recovers from RemoteViews inflation errors without crashing the launcher.
 */
class LumiaAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPressTile: (() -> Unit)? = null
    var isEditOverlayActive: Boolean = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var downX = 0f
    private var downY = 0f
    private var longPressTriggered = false
    private var gestureHasScrollableChild = false

    private val longPressRunnable = Runnable {
        if (!isEditOverlayActive) {
            longPressTriggered = true
            parent?.requestDisallowInterceptTouchEvent(false)
            onLongPressTile?.invoke()
        }
    }

    init {
        clipChildren = true
        clipToPadding = true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isEditOverlayActive) {
            // In single-tile or global edit mode, let the parent Tile container handle drag/resize/tap
            return true
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                gestureHasScrollableChild = hasScrollableChild(this)
                longPressTriggered = false
                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.postDelayed(longPressRunnable, longPressTimeout)

                if (gestureHasScrollableChild) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - downX)
                val dy = abs(ev.y - downY)
                if (dx > touchSlop || dy > touchSlop) {
                    mainHandler.removeCallbacks(longPressRunnable)
                    if (gestureHasScrollableChild && dy >= dx) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    } else if (dx > dy * 1.3f) {
                        // Horizontal swipe across pages: allow HorizontalPager to intercept
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                gestureHasScrollableChild = false
                parent?.requestDisallowInterceptTouchEvent(false)
                if (longPressTriggered) {
                    longPressTriggered = false
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            mainHandler.removeCallbacks(longPressRunnable)
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        mainHandler.removeCallbacks(longPressRunnable)
        gestureHasScrollableChild = false
        super.onDetachedFromWindow()
    }

    override fun getErrorView(): View {
        return FrameLayout(context).apply {
            val tv = TextView(context).apply {
                text = "Không thể hiển thị nội dung Widget"
                textSize = 11f
                setTextColor(0xCCFFFFFF.toInt())
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
            }
            addView(
                tv,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            )
        }
    }

    private fun hasScrollableChild(group: ViewGroup): Boolean {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i) ?: continue
            if (child is AbsListView ||
                child is ScrollView ||
                child.canScrollVertically(1) ||
                child.canScrollVertically(-1)
            ) {
                return true
            }
            if (child is ViewGroup && hasScrollableChild(child)) {
                return true
            }
        }
        return false
    }
}

class LumiaAppWidgetHost(
    context: Context,
    hostId: Int
) : AppWidgetHost(context.applicationContext, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return LumiaAppWidgetHostView(context)
    }
}

/**
 * Singleton manager coordinating `AppWidgetHost`, `AppWidgetManager`, widget discovery,
 * lifecycle (`startListening` / `stopListening`), size reporting (`updateAppWidgetOptions`),
 * and safe recovery / re-linking after backup restore or app uninstallation.
 */
class LumiaWidgetHostManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val appWidgetHost: LumiaAppWidgetHost = LumiaAppWidgetHost(appContext, HOST_ID)

    @Volatile
    private var isListening: Boolean = false

    private val lastReportedSizes = ConcurrentHashMap<Int, Pair<Int, Int>>()

    @Synchronized
    fun startListening() {
        if (isListening) return
        try {
            appWidgetHost.startListening()
            isListening = true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to start AppWidgetHost listening: ${e.message}")
        }
    }

    @Synchronized
    fun stopListening() {
        if (!isListening) return
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to stop AppWidgetHost listening: ${e.message}")
        } finally {
            isListening = false
        }
    }

    fun allocateWidgetId(): Int {
        return try {
            appWidgetHost.allocateAppWidgetId()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to allocate appWidgetId", e)
            -1
        }
    }

    fun releaseWidgetId(appWidgetId: Int) {
        if (appWidgetId <= 0) return
        lastReportedSizes.remove(appWidgetId)
        try {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete appWidgetId=$appWidgetId: ${e.message}")
        }
    }

    /**
     * Cleans up any orphan widget IDs held by the host that are no longer referenced by any saved TileEntity.
     */
    fun pruneOrphanWidgetIds(activeTileWidgetIds: Set<Int>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hostedIds = appWidgetHost.appWidgetIds ?: return
                for (id in hostedIds) {
                    if (id > 0 && id !in activeTileWidgetIds) {
                        releaseWidgetId(id)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error pruning orphan widget IDs: ${e.message}")
            }
        }
    }

    /**
     * Discovers all installed Android AppWidget providers on the device and maps them to [WidgetProviderItem].
     */
    fun discoverInstalledWidgets(): List<WidgetProviderItem> {
        val pm = appContext.packageManager
        val providers = try {
            appWidgetManager.installedProviders ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Error querying installed widget providers: ${e.message}")
            emptyList()
        }

        return providers.mapNotNull { info ->
            try {
                mapProviderInfoToItem(info, pm)
            } catch (e: Exception) {
                null
            }
        }.sortedWith(compareBy({ it.appName.lowercase() }, { it.label.lowercase() }))
    }

    fun getWidgetsForPackage(packageName: String): List<WidgetProviderItem> {
        if (packageName.isBlank()) return emptyList()
        return discoverInstalledWidgets().filter {
            it.packageName.equals(packageName, ignoreCase = true)
        }
    }

    fun findProviderInfo(providerComponent: String): AppWidgetProviderInfo? {
        if (providerComponent.isBlank()) return null
        val component = ComponentName.unflattenFromString(providerComponent) ?: return null
        return try {
            appWidgetManager.installedProviders?.firstOrNull { it.provider == component }
        } catch (_: Exception) {
            null
        }
    }

    fun findProviderItem(providerComponent: String): WidgetProviderItem? {
        val info = findProviderInfo(providerComponent) ?: return null
        return try {
            mapProviderInfoToItem(info, appContext.packageManager)
        } catch (_: Exception) {
            null
        }
    }

    fun inspectWidgetState(appWidgetId: Int, providerComponent: String): WidgetHostState {
        val normalizedProvider = providerComponent.trim()
        if (appWidgetId > 0) {
            try {
                val boundInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (boundInfo != null) {
                    val boundComponent = boundInfo.provider.flattenToString()
                    if (normalizedProvider.isBlank() || boundComponent.equals(normalizedProvider, ignoreCase = true)) {
                        return WidgetHostState.Ready(appWidgetId, boundInfo)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error inspecting appWidgetId=$appWidgetId: ${e.message}")
            }
        }

        // Widget ID is invalid (-1 after backup restore, or cleared by OS). Check if provider still exists on device.
        val installedInfo = findProviderInfo(normalizedProvider)
        if (installedInfo != null) {
            val item = mapProviderInfoToItem(installedInfo, appContext.packageManager)
            return WidgetHostState.NeedsRebind(
                providerInfo = installedInfo,
                providerItem = item,
                reason = if (appWidgetId <= 0) {
                    "Cần cấp quyền liên kết lại Widget sau khi khôi phục"
                } else {
                    "Phiên Widget đã thay đổi • Chạm để kết nối lại"
                }
            )
        }

        val pkg = normalizedProvider.substringBefore("/").ifBlank { "unknown" }
        return WidgetHostState.ProviderMissing(
            packageName = pkg,
            providerComponent = normalizedProvider
        )
    }

    /**
     * Attempts to bind [appWidgetId] to [providerComponent] silently using [AppWidgetManager.bindAppWidgetIdIfAllowed].
     * Returns true if bound immediately without requiring the system permission dialog.
     */
    fun bindWidgetIfAllowed(
        appWidgetId: Int,
        providerComponent: String,
        initialSize: TileSize = TileSize.MEDIUM
    ): Boolean {
        if (appWidgetId <= 0 || providerComponent.isBlank()) return false
        val component = ComponentName.unflattenFromString(providerComponent) ?: return false
        return try {
            val options = createSizeOptionsBundle(initialSize)
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, component, options)
        } catch (e: Exception) {
            Log.w(TAG, "bindAppWidgetIdIfAllowed failed for $providerComponent: ${e.message}")
            false
        }
    }

    /**
     * Creates the official Android system `ACTION_APPWIDGET_BIND` intent when the user needs to approve widget binding.
     */
    fun createBindPermissionIntent(
        appWidgetId: Int,
        providerComponent: String,
        initialSize: TileSize = TileSize.MEDIUM
    ): Intent? {
        val component = ComponentName.unflattenFromString(providerComponent) ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, component)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, createSizeOptionsBundle(initialSize))
        }
    }

    /**
     * Creates a configuration intent if the widget declares a configuration Activity and doesn't require host-only start.
     */
    fun getConfigureComponent(appWidgetId: Int, providerComponent: String): ComponentName? {
        val info = (if (appWidgetId > 0) appWidgetManager.getAppWidgetInfo(appWidgetId) else null)
            ?: findProviderInfo(providerComponent)
        return info?.configure
    }

    /**
     * Creates or updates the [LumiaAppWidgetHostView] for a bound widget and reports its real dp dimensions
     * only when the size changes, avoiding unnecessary RemoteViews re-inflations.
     */
    fun createHostView(
        activityOrAppContext: Context,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo,
        widthDp: Int,
        heightDp: Int
    ): LumiaAppWidgetHostView {
        val view = appWidgetHost.createView(
            activityOrAppContext,
            appWidgetId,
            providerInfo
        ) as? LumiaAppWidgetHostView ?: LumiaAppWidgetHostView(activityOrAppContext).apply {
            setAppWidget(appWidgetId, providerInfo)
        }
        view.setPadding(0, 0, 0, 0)
        updateWidgetSizeOptionsIfChanged(appWidgetId, widthDp, heightDp)
        return view
    }

    fun updateWidgetSizeOptionsIfChanged(
        appWidgetId: Int,
        widthDp: Int,
        heightDp: Int
    ) {
        if (appWidgetId <= 0) return
        val clampedW = widthDp.coerceIn(56, 800)
        val clampedH = heightDp.coerceIn(56, 800)
        val prev = lastReportedSizes[appWidgetId]
        if (prev != null && abs(prev.first - clampedW) < 4 && abs(prev.second - clampedH) < 4) {
            return
        }
        lastReportedSizes[appWidgetId] = clampedW to clampedH
        try {
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, clampedW)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, clampedH)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, clampedW)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, clampedH)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    putParcelableArrayList(
                        AppWidgetManager.OPTION_APPWIDGET_SIZES,
                        arrayListOf(SizeF(clampedW.toFloat(), clampedH.toFloat()))
                    )
                }
            }
            appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to updateAppWidgetOptions for id=$appWidgetId: ${e.message}")
        }
    }

    private fun createSizeOptionsBundle(size: TileSize): Bundle {
        val (wDp, hDp) = when (size) {
            TileSize.SMALL -> 80 to 80
            TileSize.MEDIUM -> 170 to 170
            TileSize.WIDE -> 348 to 170
            TileSize.LARGE -> 348 to 348
        }
        return Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, wDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, hDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, wDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, hDp)
        }
    }

    fun mapProviderInfoToItem(
        info: AppWidgetProviderInfo,
        pm: PackageManager
    ): WidgetProviderItem {
        val provider = info.provider
        val pkg = provider.packageName
        val label = info.loadLabel(pm)?.trim()?.takeIf { it.isNotEmpty() }
            ?: provider.shortClassName.substringAfterLast('.')
        val appName = try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast('.')
        }
        val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                info.loadDescription(appContext)?.toString() ?: ""
            } catch (_: Exception) {
                ""
            }
        } else ""

        val density = appContext.resources.displayMetrics.density.coerceAtLeast(1f)
        val minWidthDp = (info.minWidth / density).toInt().coerceAtLeast(48)
        val minHeightDp = (info.minHeight / density).toInt().coerceAtLeast(48)
        val minResizeWidthDp = (info.minResizeWidth / density).toInt().coerceAtLeast(48)
        val minResizeHeightDp = (info.minResizeHeight / density).toInt().coerceAtLeast(48)

        val rawCellW = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
            info.targetCellWidth
        } else {
            ((minWidthDp + 30) / 70).coerceAtLeast(1)
        }
        val rawCellH = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
            info.targetCellHeight
        } else {
            ((minHeightDp + 30) / 70).coerceAtLeast(1)
        }

        val targetCellW = rawCellW.coerceIn(1, 4)
        val targetCellH = rawCellH.coerceIn(1, 4)

        val recommendedSize = when {
            targetCellW <= 1 && targetCellH <= 1 -> TileSize.SMALL
            targetCellW >= 3 && targetCellH >= 3 -> TileSize.LARGE
            targetCellW >= 3 -> TileSize.WIDE
            else -> TileSize.MEDIUM
        }

        val minSpanX = ((minResizeWidthDp + 30) / 70).coerceIn(1, targetCellW)
        val minSpanY = ((minResizeHeightDp + 30) / 70).coerceIn(1, targetCellH)
        val resizeMode = info.resizeMode

        val allowedSizes = computeAllowedTileSizes(
            minSpanX = minSpanX,
            minSpanY = minSpanY,
            recommendedSize = recommendedSize,
            resizeMode = resizeMode
        )

        return WidgetProviderItem(
            providerComponent = provider.flattenToString(),
            packageName = pkg,
            className = provider.className,
            label = label,
            appName = appName,
            description = desc,
            minWidthDp = minWidthDp,
            minHeightDp = minHeightDp,
            minResizeWidthDp = minResizeWidthDp,
            minResizeHeightDp = minResizeHeightDp,
            targetCellWidth = targetCellW,
            targetCellHeight = targetCellH,
            resizeMode = resizeMode,
            recommendedTileSize = recommendedSize,
            allowedTileSizes = allowedSizes,
            hasConfigurationActivity = info.configure != null,
            configureComponent = info.configure?.flattenToString() ?: "",
            updatePeriodMillis = info.updatePeriodMillis
        )
    }

    companion object {
        const val HOST_ID = 1024
        private const val TAG = "LumiaWidgetHostManager"

        @Volatile
        private var INSTANCE: LumiaWidgetHostManager? = null

        fun getInstance(context: Context): LumiaWidgetHostManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LumiaWidgetHostManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        fun computeAllowedTileSizes(
            minSpanX: Int,
            minSpanY: Int,
            recommendedSize: TileSize,
            resizeMode: Int
        ): List<TileSize> {
            val canResizeH = (resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0
            val canResizeV = (resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0

            if (!canResizeH && !canResizeV) {
                return listOf(recommendedSize)
            }

            val list = TileSize.entries.filter { candidate ->
                val okX = if (canResizeH) candidate.spanX >= minSpanX else candidate.spanX == recommendedSize.spanX
                val okY = if (canResizeV) candidate.spanY >= minSpanY else candidate.spanY == recommendedSize.spanY
                okX && okY
            }
            return if (list.isNotEmpty()) {
                (list + recommendedSize).distinct().sortedBy { it.ordinal }
            } else {
                listOf(recommendedSize, TileSize.MEDIUM, TileSize.WIDE).distinct()
            }
        }
    }
}
