package com.example.data.repository

import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.db.TileDao
import com.example.data.model.AllAppsSortOrder
import com.example.data.model.AllAppsViewMode
import com.example.data.model.AppItem
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.PageTransitionEffect
import com.example.data.model.PageType
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherRepository(
    private val context: Context,
    private val tileDao: TileDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)

    val tilesFlow: Flow<List<TileEntity>> = tileDao.getAllTilesFlow()

    suspend fun ensureDefaultTiles() = withContext(Dispatchers.IO) {
        if (tileDao.getCount() == 0) {
            val defaults = createDefaultTiles()
            tileDao.insertAll(defaults)
        }
    }

    fun getDefaultPages(): List<LauncherPage> {
        return listOf(
            LauncherPage(id = "page_home_1", title = "Trang chính", pageType = PageType.HOME, orderIndex = 0),
            LauncherPage(id = "page_home_2", title = "Tiện ích & Đời sống", pageType = PageType.HOME, orderIndex = 1),
            LauncherPage(id = "page_all_apps", title = "Tất cả ứng dụng", pageType = PageType.ALL_APPS, orderIndex = 2)
        )
    }

    fun loadPages(): List<LauncherPage> {
        val raw = prefs.getString("launcher_pages_json", null)
        if (raw.isNullOrBlank()) {
            val defaults = getDefaultPages()
            savePages(defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<LauncherPage>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    LauncherPage(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        pageType = try {
                            PageType.valueOf(obj.optString("pageType", "HOME"))
                        } catch (_: Exception) {
                            PageType.HOME
                        },
                        orderIndex = obj.optInt("orderIndex", i)
                    )
                )
            }
            if (list.none { it.pageType == PageType.HOME }) {
                list.add(0, LauncherPage("page_home_1", "Trang chính", PageType.HOME, 0))
            }
            if (list.none { it.pageType == PageType.ALL_APPS }) {
                list.add(LauncherPage("page_all_apps", "Tất cả ứng dụng", PageType.ALL_APPS, list.size))
            }
            list.sortedBy { it.orderIndex }.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
        } catch (_: Exception) {
            getDefaultPages()
        }
    }

    fun savePages(pages: List<LauncherPage>) {
        val arr = JSONArray()
        pages.mapIndexed { idx, page -> page.copy(orderIndex = idx) }.forEach { page ->
            val obj = JSONObject().apply {
                put("id", page.id)
                put("title", page.title)
                put("pageType", page.pageType.name)
                put("orderIndex", page.orderIndex)
            }
            arr.put(obj)
        }
        prefs.edit().putString("launcher_pages_json", arr.toString()).apply()
    }

    private fun createDefaultTiles(): List<TileEntity> {
        return listOf(
            // Page 1: Primary communication, clock, weather, camera, browser, store, settings
            TileEntity(
                id = "tile_clock_calendar",
                title = "Lịch & Giờ",
                tileType = TileType.CLOCK_CALENDAR,
                size = TileSize.WIDE,
                orderIndex = 0,
                pageId = "page_home_1",
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_phone",
                title = "Điện thoại",
                packageName = findPackageForCategory(Intent.CATEGORY_APP_CONTACTS, "com.google.android.dialer", "com.android.dialer"),
                tileType = TileType.APP,
                size = TileSize.MEDIUM,
                orderIndex = 1,
                pageId = "page_home_1",
                customIconName = "phone",
                notificationCount = 0
            ),
            TileEntity(
                id = "tile_messaging",
                title = "Tin nhắn",
                packageName = findPackageForCategory(Intent.CATEGORY_APP_MESSAGING, "com.google.android.apps.messaging", "com.android.mms"),
                tileType = TileType.APP,
                size = TileSize.MEDIUM,
                orderIndex = 2,
                pageId = "page_home_1",
                customIconName = "message",
                notificationCount = 0
            ),
            TileEntity(
                id = "tile_weather",
                title = "Thời tiết",
                tileType = TileType.WEATHER,
                size = TileSize.WIDE,
                orderIndex = 3,
                pageId = "page_home_1",
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_browser",
                title = "Trình duyệt",
                packageName = findPackageForCategory(Intent.CATEGORY_APP_BROWSER, "com.android.chrome", "com.google.android.browser"),
                tileType = TileType.APP,
                size = TileSize.MEDIUM,
                orderIndex = 4,
                pageId = "page_home_1",
                customIconName = "web"
            ),
            TileEntity(
                id = "tile_camera",
                title = "Máy ảnh",
                packageName = "SYSTEM_CAMERA",
                tileType = TileType.APP,
                size = TileSize.MEDIUM,
                orderIndex = 5,
                pageId = "page_home_1",
                customIconName = "camera"
            ),
            TileEntity(
                id = "tile_store",
                title = "Cửa hàng",
                packageName = "com.android.vending",
                tileType = TileType.APP,
                size = TileSize.MEDIUM,
                orderIndex = 6,
                pageId = "page_home_1",
                customIconName = "store"
            ),
            TileEntity(
                id = "tile_settings",
                title = "Cài đặt hệ thống",
                packageName = "SYSTEM_SETTINGS",
                tileType = TileType.APP,
                size = TileSize.MEDIUM,
                orderIndex = 7,
                pageId = "page_home_1",
                customIconName = "settings"
            ),

            // Page 2: Photos, Battery, Performance, Notes, Quotes
            TileEntity(
                id = "tile_photos",
                title = "Ảnh Lumia",
                packageName = findPackageForCategory(Intent.CATEGORY_APP_GALLERY, "com.google.android.apps.photos", "com.android.gallery3d"),
                tileType = TileType.PHOTOS,
                size = TileSize.LARGE,
                orderIndex = 0,
                pageId = "page_home_2",
                customIconName = "photo",
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_battery",
                title = "Pin & Nguồn",
                tileType = TileType.BATTERY,
                size = TileSize.MEDIUM,
                orderIndex = 1,
                pageId = "page_home_2",
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_performance",
                title = "Hiệu năng máy",
                tileType = TileType.PERFORMANCE,
                size = TileSize.MEDIUM,
                orderIndex = 2,
                pageId = "page_home_2",
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_notes",
                title = "Ghi chú nhanh",
                tileType = TileType.NOTES,
                size = TileSize.WIDE,
                orderIndex = 3,
                pageId = "page_home_2",
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_news_quote",
                title = "Cảm hứng mỗi ngày",
                tileType = TileType.NEWS_QUOTE,
                size = TileSize.WIDE,
                orderIndex = 4,
                pageId = "page_home_2",
                isLiveEnabled = true
            )
        )
    }

    private fun findPackageForCategory(category: String, vararg preferredPackages: String): String {
        val pm = context.packageManager
        for (pkg in preferredPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Exception) {}
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.firstOrNull()?.activityInfo?.packageName ?: ""
    }

    suspend fun resizeTile(tileId: String) = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val target = tiles.find { it.id == tileId } ?: return@withContext
        tileDao.update(target.copy(size = target.size.next()))
    }

    suspend fun updateTileCustomization(
        tileId: String,
        colorOverrideHex: String?,
        customTitle: String,
        isLiveEnabled: Boolean
    ) = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val target = tiles.find { it.id == tileId } ?: return@withContext
        tileDao.update(
            target.copy(
                colorOverrideHex = colorOverrideHex?.takeIf { it.isNotBlank() },
                title = customTitle.ifBlank { target.title },
                isLiveEnabled = isLiveEnabled
            )
        )
    }

    suspend fun unpinTile(tileId: String) = withContext(Dispatchers.IO) {
        tileDao.deleteById(tileId)
    }

    suspend fun moveTileInPage(tileId: String, pageId: String, moveUp: Boolean) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val pageTiles = allTiles.filter { it.pageId == pageId }.sortedBy { it.orderIndex }.toMutableList()
        val index = pageTiles.indexOfFirst { it.id == tileId }
        if (index == -1) return@withContext
        val newIndex = if (moveUp) index - 1 else index + 1
        if (newIndex in pageTiles.indices) {
            val item = pageTiles.removeAt(index)
            pageTiles.add(newIndex, item)
            val reindexed = pageTiles.mapIndexed { idx, tile -> tile.copy(orderIndex = idx) }
            tileDao.insertAll(reindexed)
        }
    }

    suspend fun reorderTilesInPage(pageId: String, fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val pageTiles = allTiles.filter { it.pageId == pageId }.sortedBy { it.orderIndex }.toMutableList()
        if (fromIndex in pageTiles.indices && toIndex in pageTiles.indices && fromIndex != toIndex) {
            val item = pageTiles.removeAt(fromIndex)
            pageTiles.add(toIndex, item)
            val reindexed = pageTiles.mapIndexed { idx, tile -> tile.copy(orderIndex = idx) }
            tileDao.insertAll(reindexed)
        }
    }

    suspend fun moveTileToPage(tileId: String, targetPageId: String) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val target = allTiles.find { it.id == tileId } ?: return@withContext
        if (target.pageId == targetPageId) return@withContext
        val targetPageCount = allTiles.count { it.pageId == targetPageId }
        tileDao.update(target.copy(pageId = targetPageId, orderIndex = targetPageCount))
    }

    suspend fun reassignTilesFromDeletedPage(deletedPageId: String, fallbackPageId: String) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val fallbackCount = allTiles.count { it.pageId == fallbackPageId }
        val affected = allTiles.filter { it.pageId == deletedPageId }
            .mapIndexed { idx, tile -> tile.copy(pageId = fallbackPageId, orderIndex = fallbackCount + idx) }
        if (affected.isNotEmpty()) {
            tileDao.insertAll(affected)
        }
    }

    suspend fun pinApp(app: AppItem, pageId: String = "page_home_1") = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val pageCount = allTiles.count { it.pageId == pageId }
        val tile = TileEntity(
            id = "app_${app.packageName}_${System.currentTimeMillis()}",
            title = app.label,
            packageName = app.packageName,
            tileType = TileType.APP,
            size = TileSize.MEDIUM,
            orderIndex = pageCount,
            pageId = pageId,
            notificationCount = 0
        )
        tileDao.insertOrUpdate(tile)
    }

    suspend fun pinTile(tile: TileEntity) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val pageCount = allTiles.count { it.pageId == tile.pageId }
        tileDao.insertOrUpdate(tile.copy(orderIndex = pageCount))
    }

    suspend fun resetToDefaultTiles() = withContext(Dispatchers.IO) {
        tileDao.clearAll()
        val defaultPages = getDefaultPages()
        savePages(defaultPages)
        tileDao.insertAll(createDefaultTiles())
    }

    suspend fun getInstalledApps(pinnedPackageNames: Set<String>): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<AppItem>()

        for (resolveInfo in activities) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue
            val label = resolveInfo.loadLabel(pm)?.toString() ?: pkg
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val isSystem = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            list.add(
                AppItem(
                    label = label,
                    packageName = pkg,
                    isSystemApp = isSystem,
                    isPinned = pinnedPackageNames.contains(pkg)
                )
            )
        }
        list.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun launchApp(packageName: String): Boolean {
        if (packageName.isEmpty()) return false
        return try {
            when (packageName) {
                "SYSTEM_SETTINGS", "com.android.settings" -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                "SYSTEM_CAMERA", "android.media.action.IMAGE_CAPTURE" -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                else -> {
                    val pm = context.packageManager
                    val intent = pm.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        true
                    } else {
                        false
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isDefaultLauncher(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return true
                }
            }
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val currentDefaultPkg = resolveInfo?.activityInfo?.packageName
            currentDefaultPkg == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    fun getCurrentDefaultLauncherLabel(): String {
        return try {
            if (isDefaultLauncher()) return "Lumia Launcher"
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val pkg = resolveInfo?.activityInfo?.packageName ?: return "Chưa chọn mặc định"
            if (pkg == "android" || pkg.contains("resolver", ignoreCase = true)) {
                "Chưa cố định (Hỏi mỗi lần)"
            } else {
                resolveInfo.loadLabel(context.packageManager)?.toString() ?: pkg
            }
        } catch (_: Exception) {
            "Hệ thống"
        }
    }

    fun optimizeMemory(): SystemTelemetry {
        System.gc()
        return getSystemTelemetry()
    }

    fun getSystemTelemetry(): SystemTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 85
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val batteryTempC = tempTenths / 10f

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val availMb = memInfo.availMem / (1024 * 1024)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val ramUsagePct = if (totalMb > 0) (((totalMb - availMb).toFloat() / totalMb.toFloat()) * 100).toInt() else 45

        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGb = (stat.availableBlocksLong * stat.blockSizeLong).toFloat() / (1024f * 1024f * 1024f)
        val totalGb = (stat.blockCountLong * stat.blockSizeLong).toFloat() / (1024f * 1024f * 1024f)

        val uptimeHours = SystemClock.elapsedRealtime() / (1000 * 60 * 60)

        return SystemTelemetry(
            batteryPct = batteryPct,
            isCharging = isCharging,
            batteryTempC = batteryTempC,
            availableRamMb = availMb,
            totalRamMb = totalMb,
            ramUsagePct = ramUsagePct,
            internalFreeGb = String.format(Locale.US, "%.1f", freeGb).toFloat(),
            internalTotalGb = String.format(Locale.US, "%.1f", totalGb).toFloat(),
            uptimeHours = uptimeHours,
            deviceModel = "Lumia 925 • One UI 9.0 • MIUI Bridge",
            isDefaultLauncher = isDefaultLauncher(),
            currentDefaultLauncherName = getCurrentDefaultLauncherLabel()
        )
    }

    fun loadSettings(): LauncherSettings {
        val viewModeStr = prefs.getString("all_apps_view_mode", AllAppsViewMode.METRO_LIST.name) ?: AllAppsViewMode.METRO_LIST.name
        val sortOrderStr = prefs.getString("all_apps_sort_order", AllAppsSortOrder.NAME_ASC.name) ?: AllAppsSortOrder.NAME_ASC.name
        val transitionStr = prefs.getString("page_transition_effect", PageTransitionEffect.TURNSTILE_3D.name) ?: PageTransitionEffect.TURNSTILE_3D.name

        return LauncherSettings(
            accentColorHex = prefs.getString("accent_color", "#0078D7") ?: "#0078D7",
            isDarkTheme = prefs.getBoolean("is_dark_theme", true),
            tileOpacity = prefs.getFloat("tile_opacity", 0.85f),
            showMoreTiles = prefs.getBoolean("show_more_tiles", false),
            tileCornerRadiusDp = prefs.getInt("tile_corner_radius", 4),
            tileSpacingDp = prefs.getInt("tile_spacing_dp", 8),
            useRealAppIcons = prefs.getBoolean("use_real_app_icons", true),
            miuiGlassBorder = prefs.getBoolean("miui_glass_border", true),
            pageTransitionEffect = try { PageTransitionEffect.valueOf(transitionStr) } catch (_: Exception) { PageTransitionEffect.TURNSTILE_3D },
            doubleTapToGlance = prefs.getBoolean("double_tap_to_glance", true),
            lockHomeLayout = prefs.getBoolean("lock_home_layout", false),
            fullWallpaperEnabled = prefs.getBoolean("full_wallpaper_enabled", true),
            wallpaperGradientIndex = prefs.getInt("wallpaper_gradient_index", 0),
            turnstileAnimationEnabled = prefs.getBoolean("turnstile_animation", true),
            glanceScreenEnabled = prefs.getBoolean("glance_screen_enabled", true),
            quickNotes = prefs.getString("quick_notes", "Chào mừng trở lại Lumia 925! Chạm đúp khoảng trống để mở Glance, nhấn giữ để tùy chỉnh.") ?: "",
            cloudSyncState = prefs.getString("cloud_sync_state", "Đã đồng bộ cục bộ") ?: "Đã đồng bộ cục bộ",
            defaultHomePageId = prefs.getString("default_home_page_id", "page_home_1") ?: "page_home_1",
            showPageHeaderTitle = prefs.getBoolean("show_page_header_title", false),
            allAppsViewMode = try { AllAppsViewMode.valueOf(viewModeStr) } catch (_: Exception) { AllAppsViewMode.METRO_LIST },
            allAppsGridColumns = prefs.getInt("all_apps_grid_columns", 4),
            allAppsSortOrder = try { AllAppsSortOrder.valueOf(sortOrderStr) } catch (_: Exception) { AllAppsSortOrder.NAME_ASC },
            allAppsShowSearchBar = prefs.getBoolean("all_apps_show_search_bar", true),
            allAppsShowLetterHeaders = prefs.getBoolean("all_apps_show_letter_headers", true)
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        prefs.edit()
            .putString("accent_color", settings.accentColorHex)
            .putBoolean("is_dark_theme", settings.isDarkTheme)
            .putFloat("tile_opacity", settings.tileOpacity)
            .putBoolean("show_more_tiles", settings.showMoreTiles)
            .putInt("tile_corner_radius", settings.tileCornerRadiusDp)
            .putInt("tile_spacing_dp", settings.tileSpacingDp)
            .putBoolean("use_real_app_icons", settings.useRealAppIcons)
            .putBoolean("miui_glass_border", settings.miuiGlassBorder)
            .putString("page_transition_effect", settings.pageTransitionEffect.name)
            .putBoolean("double_tap_to_glance", settings.doubleTapToGlance)
            .putBoolean("lock_home_layout", settings.lockHomeLayout)
            .putBoolean("full_wallpaper_enabled", settings.fullWallpaperEnabled)
            .putInt("wallpaper_gradient_index", settings.wallpaperGradientIndex)
            .putBoolean("turnstile_animation", settings.turnstileAnimationEnabled)
            .putBoolean("glance_screen_enabled", settings.glanceScreenEnabled)
            .putString("quick_notes", settings.quickNotes)
            .putString("cloud_sync_state", settings.cloudSyncState)
            .putString("default_home_page_id", settings.defaultHomePageId)
            .putBoolean("show_page_header_title", settings.showPageHeaderTitle)
            .putString("all_apps_view_mode", settings.allAppsViewMode.name)
            .putInt("all_apps_grid_columns", settings.allAppsGridColumns)
            .putString("all_apps_sort_order", settings.allAppsSortOrder.name)
            .putBoolean("all_apps_show_search_bar", settings.allAppsShowSearchBar)
            .putBoolean("all_apps_show_letter_headers", settings.allAppsShowLetterHeaders)
            .apply()
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val settings = loadSettings()
        val pages = loadPages()
        val root = JSONObject()
        root.put("version", 3)
        root.put("app", "LumiaLauncher")
        root.put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        val settingsObj = JSONObject().apply {
            put("accentColorHex", settings.accentColorHex)
            put("isDarkTheme", settings.isDarkTheme)
            put("tileOpacity", settings.tileOpacity.toDouble())
            put("showMoreTiles", settings.showMoreTiles)
            put("tileCornerRadiusDp", settings.tileCornerRadiusDp)
            put("tileSpacingDp", settings.tileSpacingDp)
            put("useRealAppIcons", settings.useRealAppIcons)
            put("miuiGlassBorder", settings.miuiGlassBorder)
            put("pageTransitionEffect", settings.pageTransitionEffect.name)
            put("doubleTapToGlance", settings.doubleTapToGlance)
            put("lockHomeLayout", settings.lockHomeLayout)
            put("wallpaperGradientIndex", settings.wallpaperGradientIndex)
            put("quickNotes", settings.quickNotes)
            put("defaultHomePageId", settings.defaultHomePageId)
            put("showPageHeaderTitle", settings.showPageHeaderTitle)
            put("allAppsViewMode", settings.allAppsViewMode.name)
            put("allAppsGridColumns", settings.allAppsGridColumns)
            put("allAppsSortOrder", settings.allAppsSortOrder.name)
            put("allAppsShowSearchBar", settings.allAppsShowSearchBar)
            put("allAppsShowLetterHeaders", settings.allAppsShowLetterHeaders)
        }
        root.put("settings", settingsObj)

        val pagesArr = JSONArray()
        pages.forEach { page ->
            val obj = JSONObject().apply {
                put("id", page.id)
                put("title", page.title)
                put("pageType", page.pageType.name)
                put("orderIndex", page.orderIndex)
            }
            pagesArr.put(obj)
        }
        root.put("pages", pagesArr)

        val tilesArr = JSONArray()
        tiles.forEach { tile ->
            val obj = JSONObject().apply {
                put("id", tile.id)
                put("title", tile.title)
                put("packageName", tile.packageName)
                put("tileType", tile.tileType.name)
                put("size", tile.size.name)
                put("orderIndex", tile.orderIndex)
                put("pageId", tile.pageId)
                put("colorOverrideHex", tile.colorOverrideHex ?: "")
                put("customIconName", tile.customIconName)
                put("notificationCount", tile.notificationCount)
                put("isLiveEnabled", tile.isLiveEnabled)
            }
            tilesArr.put(obj)
        }
        root.put("tiles", tilesArr)
        root.toString(2)
    }

    suspend fun restoreFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("pages")) {
                val pagesArr = root.getJSONArray("pages")
                val pageList = mutableListOf<LauncherPage>()
                for (i in 0 until pagesArr.length()) {
                    val obj = pagesArr.getJSONObject(i)
                    pageList.add(
                        LauncherPage(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            pageType = try { PageType.valueOf(obj.optString("pageType", "HOME")) } catch (_: Exception) { PageType.HOME },
                            orderIndex = obj.optInt("orderIndex", i)
                        )
                    )
                }
                if (pageList.isNotEmpty()) {
                    savePages(pageList)
                }
            }

            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                val cur = loadSettings()
                val newSettings = cur.copy(
                    accentColorHex = s.optString("accentColorHex", cur.accentColorHex),
                    isDarkTheme = s.optBoolean("isDarkTheme", cur.isDarkTheme),
                    tileOpacity = s.optDouble("tileOpacity", cur.tileOpacity.toDouble()).toFloat(),
                    showMoreTiles = s.optBoolean("showMoreTiles", cur.showMoreTiles),
                    tileCornerRadiusDp = s.optInt("tileCornerRadiusDp", cur.tileCornerRadiusDp),
                    tileSpacingDp = s.optInt("tileSpacingDp", cur.tileSpacingDp),
                    useRealAppIcons = s.optBoolean("useRealAppIcons", cur.useRealAppIcons),
                    miuiGlassBorder = s.optBoolean("miuiGlassBorder", cur.miuiGlassBorder),
                    pageTransitionEffect = try { PageTransitionEffect.valueOf(s.optString("pageTransitionEffect", cur.pageTransitionEffect.name)) } catch (_: Exception) { cur.pageTransitionEffect },
                    doubleTapToGlance = s.optBoolean("doubleTapToGlance", cur.doubleTapToGlance),
                    lockHomeLayout = s.optBoolean("lockHomeLayout", cur.lockHomeLayout),
                    wallpaperGradientIndex = s.optInt("wallpaperGradientIndex", cur.wallpaperGradientIndex),
                    quickNotes = s.optString("quickNotes", cur.quickNotes),
                    defaultHomePageId = s.optString("defaultHomePageId", cur.defaultHomePageId),
                    showPageHeaderTitle = s.optBoolean("showPageHeaderTitle", cur.showPageHeaderTitle),
                    allAppsViewMode = try { AllAppsViewMode.valueOf(s.optString("allAppsViewMode", cur.allAppsViewMode.name)) } catch (_: Exception) { cur.allAppsViewMode },
                    allAppsGridColumns = s.optInt("allAppsGridColumns", cur.allAppsGridColumns),
                    allAppsSortOrder = try { AllAppsSortOrder.valueOf(s.optString("allAppsSortOrder", cur.allAppsSortOrder.name)) } catch (_: Exception) { cur.allAppsSortOrder },
                    allAppsShowSearchBar = s.optBoolean("allAppsShowSearchBar", cur.allAppsShowSearchBar),
                    allAppsShowLetterHeaders = s.optBoolean("allAppsShowLetterHeaders", cur.allAppsShowLetterHeaders),
                    cloudSyncState = "Đã khôi phục từ tệp sao lưu (" + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) + ")"
                )
                saveSettings(newSettings)
            }

            if (root.has("tiles")) {
                val tilesArr = root.getJSONArray("tiles")
                val list = mutableListOf<TileEntity>()
                for (i in 0 until tilesArr.length()) {
                    val obj = tilesArr.getJSONObject(i)
                    list.add(
                        TileEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            packageName = obj.optString("packageName", ""),
                            tileType = try { TileType.valueOf(obj.getString("tileType")) } catch (_: Exception) { TileType.APP },
                            size = try { TileSize.valueOf(obj.getString("size")) } catch (_: Exception) { TileSize.MEDIUM },
                            orderIndex = obj.optInt("orderIndex", i),
                            pageId = obj.optString("pageId", "page_home_1"),
                            colorOverrideHex = obj.optString("colorOverrideHex").takeIf { it.isNotEmpty() },
                            customIconName = obj.optString("customIconName", ""),
                            notificationCount = obj.optInt("notificationCount", 0),
                            isLiveEnabled = obj.optBoolean("isLiveEnabled", true)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    tileDao.clearAll()
                    tileDao.insertAll(list)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
