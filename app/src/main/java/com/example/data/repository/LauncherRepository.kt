package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.LumiaDatabase
import kotlinx.coroutines.CancellationException
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import com.example.data.db.TileDao
import com.example.data.model.AllAppsSortOrder
import com.example.data.model.AllAppsViewMode
import com.example.data.model.AppCategory
import com.example.data.model.AppFolder
import com.example.data.model.AppItem
import com.example.data.model.BatterySaverMode
import com.example.data.model.DynamicAccentMode
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.PageTransitionEffect
import com.example.data.model.PageType
import com.example.data.model.SmartFolderConfig
import com.example.data.model.SmartFolderItem
import com.example.data.model.SmartFolderLayoutMode
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import com.example.data.model.WidgetProviderItem
import com.example.data.model.Wp10TileStyle
import com.example.data.model.WallpaperSource
import com.example.ui.components.AppIconMemoryCache
import com.example.ui.theme.DynamicAccentEngine
import com.example.ui.widget.LumiaWidgetHostManager
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
    private val tileDao: TileDao,
    private val database: LumiaDatabase = LumiaDatabase.getDatabase(context)
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)

    fun discoverInstalledWidgets(): List<WidgetProviderItem> =
        LumiaWidgetHostManager.getInstance(context).discoverInstalledWidgets()

    val tilesFlow: Flow<List<TileEntity>> = tileDao.getAllTilesFlow()

    suspend fun ensureDefaultTiles() = withContext(Dispatchers.IO) {
        prefs.getString("pending_restore_json", null)?.let { pending ->
            check(restoreFromJson(pending)) { "Pending restore could not be completed" }
        }
        if (tileDao.getCount() == 0 && !prefs.getBoolean("home_initialized", false)) {
            val defaults = createDefaultTiles()
            tileDao.insertAll(defaults)
            prefs.edit().putBoolean("smart_folder_default_seeded_v1", true).apply()
        } else {
            val allTiles = tileDao.getAllTiles().toMutableList()
            if (!prefs.getBoolean("smart_folder_default_seeded_v1", false)) {
                prefs.edit().putBoolean("smart_folder_default_seeded_v1", true).apply()
                if (allTiles.none { it.tileType == TileType.SMART_FOLDER }) {
                    val totalCols = if (loadSettings().showMoreTiles) 6 else 4
                    val home1Tiles = allTiles.filter { it.pageId == "page_home_1" }
                    val (gx, gy) = findFirstFreeGridPosition(
                        existingPageTiles = home1Tiles,
                        spanX = TileSize.MEDIUM.effectiveSpanX(totalCols),
                        spanY = TileSize.MEDIUM.effectiveSpanY(),
                        totalCols = totalCols
                    )
                    val seedConfig = SmartFolderConfig(
                        category = AppCategory.TOOLS,
                        autoSyncCategory = true,
                        layoutMode = SmartFolderLayoutMode.HYPEROS_PEEK,
                        items = SmartFolderConfig.defaultSeedItemsForCategory(AppCategory.TOOLS)
                    )
                    val smartTile = TileEntity(
                        id = "tile_smart_folder_tools",
                        title = "Công cụ & Tiện ích",
                        packageName = seedConfig.toJsonString(),
                        tileType = TileType.SMART_FOLDER,
                        size = TileSize.MEDIUM,
                        orderIndex = home1Tiles.size,
                        pageId = "page_home_1",
                        gridX = gx,
                        gridY = gy,
                        customIconName = AppCategory.TOOLS.name.lowercase(),
                        isLiveEnabled = true
                    )
                    tileDao.insertOrUpdate(smartTile)
                    allTiles.add(smartTile)
                }
            }
            val needsCoordInit = allTiles.any { it.gridX < 0 || it.gridY < 0 }
            if (needsCoordInit) {
                val totalCols = if (loadSettings().showMoreTiles) 6 else 4
                val updated = mutableListOf<TileEntity>()
                allTiles.groupBy { it.pageId }.forEach { (_, pageTiles) ->
                    val placed = mutableListOf<TileEntity>()
                    pageTiles.sortedBy { it.orderIndex }.forEach { tile ->
                        if (tile.gridX >= 0 && tile.gridY >= 0) {
                            placed.add(tile)
                            updated.add(tile)
                        } else {
                            val (gx, gy) = findFirstFreeGridPosition(
                                existingPageTiles = placed,
                                spanX = tile.size.effectiveSpanX(totalCols),
                                spanY = tile.size.effectiveSpanY(),
                                totalCols = totalCols
                            )
                            val withCoords = tile.copy(gridX = gx, gridY = gy)
                            placed.add(withCoords)
                            updated.add(withCoords)
                        }
                    }
                }
                if (updated.isNotEmpty()) {
                    tileDao.insertAll(updated)
                }
            }
        }
        prefs.edit().putBoolean("home_initialized", true).commit()
    }

    fun findFirstFreeGridPosition(
        existingPageTiles: List<TileEntity>,
        spanX: Int,
        spanY: Int,
        totalCols: Int = 4,
        startY: Int = 0
    ): Pair<Int, Int> {
        val effSpanX = spanX.coerceIn(1, totalCols)
        val effSpanY = spanY.coerceAtLeast(1)
        require(totalCols in 1..12)
        val finalRow = maxOf(startY.coerceAtLeast(0), existingPageTiles.maxOfOrNull {
            it.gridY.coerceAtLeast(0) + it.size.effectiveSpanY()
        } ?: 0)
        for (y in startY.coerceAtLeast(0)..finalRow) {
            for (x in 0..(totalCols - effSpanX)) {
                val overlaps = existingPageTiles.any { other ->
                    val ox = other.gridX.coerceAtLeast(0)
                    val oy = other.gridY.coerceAtLeast(0)
                    val oSpanX = other.size.effectiveSpanX(totalCols)
                    val oSpanY = other.size.effectiveSpanY()
                    rectsOverlap(x, y, effSpanX, effSpanY, ox, oy, oSpanX, oSpanY)
                }
                if (!overlaps) {
                    return x to y
                }
            }
        }
        error("No free grid position")
    }

    private fun rectsOverlap(
        x1: Int, y1: Int, w1: Int, h1: Int,
        x2: Int, y2: Int, w2: Int, h2: Int
    ): Boolean {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2
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
                gridX = 0,
                gridY = 0,
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
                gridX = 0,
                gridY = 2,
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
                gridX = 2,
                gridY = 2,
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
                gridX = 0,
                gridY = 4,
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
                gridX = 0,
                gridY = 6,
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
                gridX = 2,
                gridY = 6,
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
                gridX = 0,
                gridY = 8,
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
                gridX = 2,
                gridY = 8,
                customIconName = "settings"
            ),
            TileEntity(
                id = "tile_smart_folder_tools",
                title = "Công cụ & Tiện ích",
                packageName = SmartFolderConfig(
                    category = AppCategory.TOOLS,
                    autoSyncCategory = true,
                    layoutMode = SmartFolderLayoutMode.HYPEROS_PEEK,
                    items = SmartFolderConfig.defaultSeedItemsForCategory(AppCategory.TOOLS)
                ).toJsonString(),
                tileType = TileType.SMART_FOLDER,
                size = TileSize.MEDIUM,
                orderIndex = 8,
                pageId = "page_home_1",
                gridX = 0,
                gridY = 10,
                customIconName = AppCategory.TOOLS.name.lowercase(),
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_smart_folder_comm",
                title = "Liên lạc nhanh",
                packageName = SmartFolderConfig(
                    category = AppCategory.COMMUNICATION,
                    autoSyncCategory = true,
                    layoutMode = SmartFolderLayoutMode.HYPEROS_PEEK,
                    items = SmartFolderConfig.defaultSeedItemsForCategory(AppCategory.COMMUNICATION)
                ).toJsonString(),
                tileType = TileType.SMART_FOLDER,
                size = TileSize.MEDIUM,
                orderIndex = 9,
                pageId = "page_home_1",
                gridX = 2,
                gridY = 10,
                customIconName = AppCategory.COMMUNICATION.name.lowercase(),
                isLiveEnabled = true
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
                gridX = 0,
                gridY = 0,
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
                gridX = 0,
                gridY = 4,
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_performance",
                title = "Hiệu năng máy",
                tileType = TileType.PERFORMANCE,
                size = TileSize.MEDIUM,
                orderIndex = 2,
                pageId = "page_home_2",
                gridX = 2,
                gridY = 4,
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_notes",
                title = "Ghi chú nhanh",
                tileType = TileType.NOTES,
                size = TileSize.WIDE,
                orderIndex = 3,
                pageId = "page_home_2",
                gridX = 0,
                gridY = 6,
                isLiveEnabled = true
            ),
            TileEntity(
                id = "tile_news_quote",
                title = "Cảm hứng mỗi ngày",
                tileType = TileType.NEWS_QUOTE,
                size = TileSize.WIDE,
                orderIndex = 4,
                pageId = "page_home_2",
                gridX = 0,
                gridY = 8,
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
        val allowed = target.allowedSizesForTile()
        val currentIdx = allowed.indexOf(target.size)
        val nextSize = if (currentIdx >= 0 && allowed.size > 1) {
            allowed[(currentIdx + 1) % allowed.size]
        } else {
            target.size.next()
        }
        setTileSizeInternal(tiles, target, nextSize)
    }

    suspend fun stepTileSize(tileId: String, increase: Boolean) = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val target = tiles.find { it.id == tileId } ?: return@withContext
        val allowed = target.allowedSizesForTile()
        val nextSize = if (target.hasBoundOrLinkedWidget() && allowed.isNotEmpty()) {
            val idx = allowed.indexOf(target.size).coerceAtLeast(0)
            if (increase) {
                allowed[(idx + 1).coerceAtMost(allowed.lastIndex)]
            } else {
                allowed[(idx - 1).coerceAtLeast(0)]
            }
        } else {
            if (increase) target.size.stepUp() else target.size.stepDown()
        }
        if (nextSize != target.size) {
            setTileSizeInternal(tiles, target, nextSize)
        }
    }

    suspend fun setTileSize(tileId: String, newSize: TileSize) = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val target = tiles.find { it.id == tileId } ?: return@withContext
        val effectiveSize = if (target.hasBoundOrLinkedWidget()) {
            val allowed = target.allowedSizesForTile()
            if (newSize in allowed) newSize
            else allowed.minByOrNull { kotlin.math.abs(it.ordinal - newSize.ordinal) } ?: newSize
        } else {
            newSize
        }
        if (effectiveSize != target.size) {
            setTileSizeInternal(tiles, target, effectiveSize)
        }
    }

    private suspend fun setTileSizeInternal(
        tiles: List<TileEntity>,
        target: TileEntity,
        newSize: TileSize
    ) {
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        val newSpanX = newSize.effectiveSpanX(totalCols)
        val clampedX = target.gridX.coerceAtLeast(0).coerceAtMost((totalCols - newSpanX).coerceAtLeast(0))
        val clampedY = target.gridY.coerceAtLeast(0)
        val resizedTarget = target.copy(size = newSize, gridX = clampedX, gridY = clampedY)

        // Resolve direct overlaps on the same page without auto-packing empty spaces
        val pageOthers = tiles.filter { it.pageId == target.pageId && it.id != target.id }
        val resolvedPageTiles = resolveCollisionsOnly(resizedTarget, pageOthers, totalCols)
        tileDao.insertAll(resolvedPageTiles)
        autoArrangeIfEnabled(target.pageId)
    }

    suspend fun moveTileToGridPosition(
        tileId: String,
        targetGridX: Int,
        targetGridY: Int,
        totalCols: Int = 4
    ) = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val target = tiles.find { it.id == tileId } ?: return@withContext
        val spanX = target.size.effectiveSpanX(totalCols)
        val spanY = target.size.effectiveSpanY()
        val clampedX = targetGridX.coerceIn(0, (totalCols - spanX).coerceAtLeast(0))
        val clampedY = targetGridY.coerceIn(0, 60)

        val movedTarget = target.copy(gridX = clampedX, gridY = clampedY)
        val pageOthers = tiles.filter { it.pageId == target.pageId && it.id != target.id }

        // Check if movedTarget overlaps with exactly 1 tile -> if so and the old spot is free, swap them; else shift only colliding tiles down
        val colliding = pageOthers.filter { other ->
            rectsOverlap(
                clampedX, clampedY, spanX, spanY,
                other.gridX.coerceAtLeast(0), other.gridY.coerceAtLeast(0),
                other.size.effectiveSpanX(totalCols), other.size.effectiveSpanY()
            )
        }

        if (colliding.size == 1 && target.gridX >= 0 && target.gridY >= 0) {
            val single = colliding.first()
            val singleSpanX = single.size.effectiveSpanX(totalCols)
            val singleSpanY = single.size.effectiveSpanY()
            val swapX = target.gridX.coerceIn(0, (totalCols - singleSpanX).coerceAtLeast(0))
            val swapY = target.gridY.coerceAtLeast(0)
            val rest = pageOthers.filter { it.id != single.id }
            val canSwapCleanly = !rectsOverlap(clampedX, clampedY, spanX, spanY, swapX, swapY, singleSpanX, singleSpanY) &&
                rest.none { r ->
                    rectsOverlap(
                        swapX, swapY, singleSpanX, singleSpanY,
                        r.gridX.coerceAtLeast(0), r.gridY.coerceAtLeast(0),
                        r.size.effectiveSpanX(totalCols), r.size.effectiveSpanY()
                    )
                }
            if (canSwapCleanly) {
                tileDao.insertAll(listOf(movedTarget, single.copy(gridX = swapX, gridY = swapY)))
                autoArrangeIfEnabled(target.pageId)
                return@withContext
            }
        }

        val resolved = resolveCollisionsOnly(movedTarget, pageOthers, totalCols)
        tileDao.insertAll(resolved)
        autoArrangeIfEnabled(target.pageId)
    }

    private fun resolveCollisionsOnly(
        primaryTile: TileEntity,
        otherPageTiles: List<TileEntity>,
        totalCols: Int
    ): List<TileEntity> {
        val placed = mutableListOf(primaryTile)
        // Keep non-colliding tiles in their exact (gridX, gridY) first so they never move
        val nonColliding = mutableListOf<TileEntity>()
        val colliding = mutableListOf<TileEntity>()

        val px = primaryTile.gridX.coerceAtLeast(0)
        val py = primaryTile.gridY.coerceAtLeast(0)
        val pw = primaryTile.size.effectiveSpanX(totalCols)
        val ph = primaryTile.size.effectiveSpanY()

        for (other in otherPageTiles) {
            val ox = other.gridX.coerceAtLeast(0)
            val oy = other.gridY.coerceAtLeast(0)
            val ow = other.size.effectiveSpanX(totalCols)
            val oh = other.size.effectiveSpanY()
            if (rectsOverlap(px, py, pw, ph, ox, oy, ow, oh)) {
                colliding.add(other)
            } else {
                nonColliding.add(other)
            }
        }

        placed.addAll(nonColliding)

        // Only relocate tiles that directly collided with primaryTile, placing them at the nearest free spot at or below their current Y
        for (colTile in colliding.sortedWith(compareBy({ it.gridY }, { it.gridX }))) {
            val cw = colTile.size.effectiveSpanX(totalCols)
            val ch = colTile.size.effectiveSpanY()
            val (newX, newY) = findFirstFreeGridPosition(
                existingPageTiles = placed,
                spanX = cw,
                spanY = ch,
                totalCols = totalCols,
                startY = colTile.gridY.coerceAtLeast(0)
            )
            placed.add(colTile.copy(gridX = newX, gridY = newY))
        }

        return placed
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

    /**
     * Compact existing tiles only when requested. The default remains free placement.
     * No widget IDs are reallocated; only Room tile coordinates are updated.
     */
    suspend fun arrangeTilesOnPage(pageId: String) = withContext(Dispatchers.IO) {
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        database.withTransaction {
            val tiles = tileDao.getAllTiles().filter { it.pageId == pageId }
                .sortedWith(compareBy<TileEntity>({ it.gridY.coerceAtLeast(0) }, { it.gridX.coerceAtLeast(0) }, { it.orderIndex }))
            val placed = mutableListOf<TileEntity>()
            tiles.forEachIndexed { index, tile ->
                val (x, y) = findFirstFreeGridPosition(
                    existingPageTiles = placed,
                    spanX = tile.size.effectiveSpanX(totalCols),
                    spanY = tile.size.effectiveSpanY(),
                    totalCols = totalCols
                )
                placed.add(tile.copy(gridX = x, gridY = y, orderIndex = index))
            }
            if (placed.isNotEmpty()) tileDao.insertAll(placed)
        }
    }

    private suspend fun autoArrangeIfEnabled(pageId: String) {
        if (loadSettings().autoArrangeTiles) arrangeTilesOnPage(pageId)
    }

    suspend fun unpinTile(tileId: String) = withContext(Dispatchers.IO) {
        val target = tileDao.getAllTiles().find { it.id == tileId }
        if (target != null && target.appWidgetId > 0) {
            LumiaWidgetHostManager.getInstance(context).releaseWidgetId(target.appWidgetId)
        }
        tileDao.deleteById(tileId)
        if (target != null) autoArrangeIfEnabled(target.pageId)
    }

    suspend fun pinWidgetTile(
        provider: WidgetProviderItem,
        appWidgetId: Int,
        pageId: String = "page_home_1",
        preferredSize: TileSize? = null
    ): TileEntity = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val pageTiles = allTiles.filter { it.pageId == pageId }
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        val minimumSize = TileSize.fromTargetSpans(provider.targetCellWidth, provider.targetCellHeight)
        val resolvedSize = preferredSize?.takeIf { it.spanX >= minimumSize.spanX && it.spanY >= minimumSize.spanY } ?: minimumSize
        val (gx, gy) = findFirstFreeGridPosition(
            existingPageTiles = pageTiles,
            spanX = resolvedSize.effectiveSpanX(totalCols),
            spanY = resolvedSize.effectiveSpanY(),
            totalCols = totalCols
        )
        val minSpanX = ((provider.minWidthDp + 69) / 70).coerceIn(1, provider.targetCellWidth)
        val minSpanY = ((provider.minHeightDp + 69) / 70).coerceIn(1, provider.targetCellHeight)
        val widgetTile = TileEntity(
            id = "widget_${provider.packageName}_${System.currentTimeMillis()}",
            title = provider.label.ifBlank { provider.appName },
            packageName = provider.packageName,
            tileType = TileType.WIDGET,
            size = resolvedSize,
            orderIndex = pageTiles.size,
            pageId = pageId,
            gridX = gx,
            gridY = gy,
            isLiveEnabled = true,
            appWidgetId = appWidgetId,
            widgetProvider = provider.providerComponent,
            widgetMinSpanX = minSpanX,
            widgetMinSpanY = minSpanY,
            widgetResizeMode = provider.resizeMode
        )
        tileDao.insertOrUpdate(widgetTile)
        autoArrangeIfEnabled(pageId)
        widgetTile
    }

    suspend fun linkWidgetToExistingTile(
        tileId: String,
        provider: WidgetProviderItem,
        appWidgetId: Int
    ): TileEntity? = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val target = allTiles.find { it.id == tileId } ?: return@withContext null
        if (target.appWidgetId > 0 && target.appWidgetId != appWidgetId) {
            LumiaWidgetHostManager.getInstance(context).releaseWidgetId(target.appWidgetId)
        }
        val minSpanX = ((provider.minWidthDp + 69) / 70).coerceIn(1, provider.targetCellWidth)
        val minSpanY = ((provider.minHeightDp + 69) / 70).coerceIn(1, provider.targetCellHeight)
        val newSize = if (target.size in provider.allowedTileSizes) {
            target.size
        } else {
            provider.recommendedTileSize
        }
        val updated = target.copy(
            tileType = TileType.WIDGET,
            packageName = provider.packageName.ifBlank { target.packageName },
            title = target.title.ifBlank { provider.label },
            size = newSize,
            appWidgetId = appWidgetId,
            widgetProvider = provider.providerComponent,
            widgetMinSpanX = minSpanX,
            widgetMinSpanY = minSpanY,
            widgetResizeMode = provider.resizeMode,
            isLiveEnabled = true
        )
        if (newSize != target.size) {
            setTileSizeInternal(allTiles, updated, newSize)
        } else {
            tileDao.insertOrUpdate(updated)
        }
        updated
    }

    suspend fun unlinkWidgetFromTile(tileId: String) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val target = allTiles.find { it.id == tileId } ?: return@withContext
        if (target.appWidgetId > 0) {
            LumiaWidgetHostManager.getInstance(context).releaseWidgetId(target.appWidgetId)
        }
        tileDao.update(
            target.copy(
                tileType = TileType.APP,
                appWidgetId = -1,
                widgetProvider = "",
                widgetMinSpanX = 1,
                widgetMinSpanY = 1,
                widgetResizeMode = 3
            )
        )
    }

    suspend fun moveTileInPage(tileId: String, pageId: String, moveUp: Boolean) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val target = allTiles.find { it.id == tileId && it.pageId == pageId } ?: return@withContext
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        val newY = if (moveUp) (target.gridY - 1).coerceAtLeast(0) else (target.gridY + 1).coerceAtMost(60)
        moveTileToGridPosition(tileId, target.gridX.coerceAtLeast(0), newY, totalCols)
    }

    suspend fun nudgeTileByDelta(tileId: String, deltaX: Int, deltaY: Int, totalCols: Int = 4) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val target = allTiles.find { it.id == tileId } ?: return@withContext
        val spanX = target.size.effectiveSpanX(totalCols)
        val newX = (target.gridX.coerceAtLeast(0) + deltaX).coerceIn(0, (totalCols - spanX).coerceAtLeast(0))
        val newY = (target.gridY.coerceAtLeast(0) + deltaY).coerceIn(0, 60)
        moveTileToGridPosition(tileId, newX, newY, totalCols)
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
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        val targetPageTiles = allTiles.filter { it.pageId == targetPageId }
        val (freeX, freeY) = findFirstFreeGridPosition(
            existingPageTiles = targetPageTiles,
            spanX = target.size.effectiveSpanX(totalCols),
            spanY = target.size.effectiveSpanY(),
            totalCols = totalCols
        )
        tileDao.update(
            target.copy(
                pageId = targetPageId,
                orderIndex = targetPageTiles.size,
                gridX = freeX,
                gridY = freeY
            )
        )
    }

    suspend fun reassignTilesFromDeletedPage(deletedPageId: String, fallbackPageId: String) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        val placedOnFallback = allTiles.filter { it.pageId == fallbackPageId }.toMutableList()
        val affected = allTiles.filter { it.pageId == deletedPageId }.mapIndexed { idx, tile ->
            val (gx, gy) = findFirstFreeGridPosition(
                existingPageTiles = placedOnFallback,
                spanX = tile.size.effectiveSpanX(totalCols),
                spanY = tile.size.effectiveSpanY(),
                totalCols = totalCols
            )
            val moved = tile.copy(
                pageId = fallbackPageId,
                orderIndex = placedOnFallback.size + idx,
                gridX = gx,
                gridY = gy
            )
            placedOnFallback.add(moved)
            moved
        }
        if (affected.isNotEmpty()) {
            tileDao.insertAll(affected)
        }
    }

    suspend fun pinApp(app: AppItem, pageId: String = "page_home_1") = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val pageTiles = allTiles.filter { it.pageId == pageId }
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        val (gx, gy) = findFirstFreeGridPosition(
            existingPageTiles = pageTiles,
            spanX = TileSize.MEDIUM.effectiveSpanX(totalCols),
            spanY = TileSize.MEDIUM.effectiveSpanY(),
            totalCols = totalCols
        )
        val tile = TileEntity(
            id = "app_${app.packageName}_${System.currentTimeMillis()}",
            title = app.label,
            packageName = app.packageName,
            tileType = TileType.APP,
            size = TileSize.MEDIUM,
            orderIndex = pageTiles.size,
            pageId = pageId,
            gridX = gx,
            gridY = gy,
            notificationCount = 0
        )
        tileDao.insertOrUpdate(tile)
        autoArrangeIfEnabled(pageId)
    }

    suspend fun pinTile(tile: TileEntity) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val pageTiles = allTiles.filter { it.pageId == tile.pageId }
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        val (gx, gy) = if (tile.gridX >= 0 && tile.gridY >= 0) {
            tile.gridX to tile.gridY
        } else {
            findFirstFreeGridPosition(
                existingPageTiles = pageTiles,
                spanX = tile.size.effectiveSpanX(totalCols),
                spanY = tile.size.effectiveSpanY(),
                totalCols = totalCols
            )
        }
        tileDao.insertOrUpdate(tile.copy(orderIndex = pageTiles.size, gridX = gx, gridY = gy))
        autoArrangeIfEnabled(tile.pageId)
    }

    suspend fun resetToDefaultTiles() = withContext(Dispatchers.IO) {
        val oldIds = tileDao.getAllTiles().map { it.appWidgetId }.filter { it > 0 }
        database.withTransaction {
            tileDao.clearAll()
            tileDao.insertAll(createDefaultTiles())
        }
        savePages(getDefaultPages())
        prefs.edit().putBoolean("smart_folder_default_seeded_v1", true).putBoolean("home_initialized", true).commit()
        oldIds.forEach { id -> runCatching { LumiaWidgetHostManager.getInstance(context).releaseWidgetId(id) } }
    }

    suspend fun createSmartFolderByCategory(
        category: AppCategory,
        pageId: String = "page_home_1",
        size: TileSize = TileSize.MEDIUM,
        absorbMatchingPageTiles: Boolean = false
    ): TileEntity = withContext(Dispatchers.IO) {
        database.withTransaction {
        val allTiles = tileDao.getAllTiles()
        val customCategories = loadCustomAppCategories()
        val pageTiles = allTiles.filter { it.pageId == pageId }.sortedBy { it.orderIndex }
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4

        val matchingTiles = if (absorbMatchingPageTiles) {
            pageTiles.filter {
                it.tileType != TileType.SMART_FOLDER &&
                    (category == AppCategory.ALL || SmartFolderConfig.classifyTileCategory(it, customCategories) == category)
            }
        } else {
            emptyList()
        }

        val absorbedItems = matchingTiles.map { t ->
            SmartFolderItem(
                id = "item_${t.id}",
                title = t.title,
                packageName = t.packageName,
                tileType = t.tileType,
                customIconName = t.customIconName,
                category = SmartFolderConfig.classifyTileCategory(t, customCategories)
            )
        }

        val mergedItems = (absorbedItems + SmartFolderConfig.defaultSeedItemsForCategory(category))
            .distinctBy { if (it.packageName.isNotBlank()) "pkg:${it.packageName}" else "title:${it.title.lowercase()}" }

        val remainingPageTiles = if (matchingTiles.isNotEmpty()) {
            val absorbedIds = matchingTiles.map { it.id }.toSet()
            absorbedIds.forEach { tileDao.deleteById(it) }
            pageTiles.filter { it.id !in absorbedIds }
        } else {
            pageTiles
        }

        val anchorTile = matchingTiles.firstOrNull()
        val (gx, gy) = if (anchorTile != null && anchorTile.gridX >= 0 && anchorTile.gridY >= 0) {
            val clampedX = anchorTile.gridX.coerceIn(0, (totalCols - size.effectiveSpanX(totalCols)).coerceAtLeast(0))
            val overlaps = remainingPageTiles.any { other ->
                rectsOverlap(
                    clampedX, anchorTile.gridY, size.effectiveSpanX(totalCols), size.effectiveSpanY(),
                    other.gridX.coerceAtLeast(0), other.gridY.coerceAtLeast(0),
                    other.size.effectiveSpanX(totalCols), other.size.effectiveSpanY()
                )
            }
            if (!overlaps) clampedX to anchorTile.gridY
            else findFirstFreeGridPosition(remainingPageTiles, size.effectiveSpanX(totalCols), size.effectiveSpanY(), totalCols)
        } else {
            findFirstFreeGridPosition(remainingPageTiles, size.effectiveSpanX(totalCols), size.effectiveSpanY(), totalCols)
        }

        val config = SmartFolderConfig(
            category = category,
            autoSyncCategory = true,
            layoutMode = SmartFolderLayoutMode.HYPEROS_PEEK,
            items = mergedItems
        )

        val folderTitle = if (category == AppCategory.ALL) "Thư mục Thông minh" else category.label
        val newFolderTile = TileEntity(
            id = "smart_folder_${category.name.lowercase()}_${System.currentTimeMillis()}",
            title = folderTitle,
            packageName = config.toJsonString(),
            tileType = TileType.SMART_FOLDER,
            size = size,
            orderIndex = remainingPageTiles.size,
            pageId = pageId,
            gridX = gx,
            gridY = gy,
            colorOverrideHex = if (category != AppCategory.ALL) category.badgeColorHex else null,
            customIconName = category.name.lowercase(),
            isLiveEnabled = true
        )
        tileDao.insertOrUpdate(newFolderTile)
        newFolderTile
        }
    }

    suspend fun groupPageTilesByCategoryIntoSmartFolders(pageId: String): Int = withContext(Dispatchers.IO) {
        database.withTransaction {
        val allTiles = tileDao.getAllTiles()
        val customCategories = loadCustomAppCategories()
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4
        val pageTiles = allTiles.filter { it.pageId == pageId }.sortedWith(compareBy({ it.gridY.coerceAtLeast(0) }, { it.gridX.coerceAtLeast(0) }, { it.orderIndex }))

        val looseTiles = pageTiles.filter { it.tileType != TileType.SMART_FOLDER }
        if (looseTiles.isEmpty()) return@withTransaction 0

        val existingFoldersByCat = pageTiles
            .filter { it.tileType == TileType.SMART_FOLDER }
            .associateBy { SmartFolderConfig.fromTile(it).category }
            .toMutableMap()

        val groupedByCat = looseTiles.groupBy { SmartFolderConfig.classifyTileCategory(it, customCategories) }

        // Delete loose tiles that are being grouped
        looseTiles.forEach { tileDao.deleteById(it.id) }

        val placedTiles = mutableListOf<TileEntity>()
        // Keep existing folders that didn't get new items first
        existingFoldersByCat.values.forEach { placedTiles.add(it) }

        var foldersCreatedOrUpdated = 0
        for ((cat, catTiles) in groupedByCat) {
            val newItems = catTiles.map { t ->
                SmartFolderItem(
                    id = "item_${t.id}",
                    title = t.title,
                    packageName = t.packageName,
                    tileType = t.tileType,
                    customIconName = t.customIconName,
                    category = cat
                )
            }
            val existingFolder = existingFoldersByCat[cat]
            if (existingFolder != null) {
                val oldConfig = SmartFolderConfig.fromTile(existingFolder)
                val combined = (oldConfig.items + newItems + SmartFolderConfig.defaultSeedItemsForCategory(cat))
                    .distinctBy { if (it.packageName.isNotBlank()) "pkg:${it.packageName}" else "title:${it.title.lowercase()}" }
                val updatedConfig = oldConfig.copy(items = combined)
                val updatedFolder = existingFolder.copy(packageName = updatedConfig.toJsonString())
                placedTiles.removeAll { it.id == existingFolder.id }
                placedTiles.add(updatedFolder)
                tileDao.insertOrUpdate(updatedFolder)
                foldersCreatedOrUpdated++
            } else {
                val folderSize = if (catTiles.size >= 4) TileSize.WIDE else TileSize.MEDIUM
                val (gx, gy) = findFirstFreeGridPosition(
                    existingPageTiles = placedTiles,
                    spanX = folderSize.effectiveSpanX(totalCols),
                    spanY = folderSize.effectiveSpanY(),
                    totalCols = totalCols
                )
                val combined = (newItems + SmartFolderConfig.defaultSeedItemsForCategory(cat))
                    .distinctBy { if (it.packageName.isNotBlank()) "pkg:${it.packageName}" else "title:${it.title.lowercase()}" }
                val config = SmartFolderConfig(
                    category = cat,
                    autoSyncCategory = true,
                    layoutMode = SmartFolderLayoutMode.HYPEROS_PEEK,
                    items = combined
                )
                val folderTile = TileEntity(
                    id = "smart_folder_${cat.name.lowercase()}_${System.currentTimeMillis()}_$foldersCreatedOrUpdated",
                    title = cat.label,
                    packageName = config.toJsonString(),
                    tileType = TileType.SMART_FOLDER,
                    size = folderSize,
                    orderIndex = placedTiles.size,
                    pageId = pageId,
                    gridX = gx,
                    gridY = gy,
                    colorOverrideHex = cat.badgeColorHex,
                    customIconName = cat.name.lowercase(),
                    isLiveEnabled = true
                )
                placedTiles.add(folderTile)
                tileDao.insertOrUpdate(folderTile)
                foldersCreatedOrUpdated++
            }
        }
        foldersCreatedOrUpdated
        }
    }

    suspend fun convertTileIntoSmartFolder(
        tileId: String,
        absorbSameCategoryOnPage: Boolean = true
    ): TileEntity? = withContext(Dispatchers.IO) {
        database.withTransaction {
        val allTiles = tileDao.getAllTiles()
        val target = allTiles.find { it.id == tileId } ?: return@withTransaction null
        if (target.tileType == TileType.SMART_FOLDER) return@withTransaction target

        val customCategories = loadCustomAppCategories()
        val cat = SmartFolderConfig.classifyTileCategory(target, customCategories)
        val pageTiles = allTiles.filter { it.pageId == target.pageId }
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4

        val tilesToAbsorb = if (absorbSameCategoryOnPage) {
            pageTiles.filter {
                it.id == target.id || (it.tileType != TileType.SMART_FOLDER && SmartFolderConfig.classifyTileCategory(it, customCategories) == cat)
            }
        } else {
            listOf(target)
        }

        val absorbedIds = tilesToAbsorb.map { it.id }.toSet()
        absorbedIds.forEach { tileDao.deleteById(it) }

        val remainingPageTiles = pageTiles.filter { it.id !in absorbedIds }
        val folderSize = if (target.size == TileSize.SMALL) TileSize.MEDIUM else target.size
        val spanX = folderSize.effectiveSpanX(totalCols)
        val spanY = folderSize.effectiveSpanY()
        val preferredX = target.gridX.coerceIn(0, (totalCols - spanX).coerceAtLeast(0))
        val preferredY = target.gridY.coerceAtLeast(0)

        val overlaps = remainingPageTiles.any { other ->
            rectsOverlap(
                preferredX, preferredY, spanX, spanY,
                other.gridX.coerceAtLeast(0), other.gridY.coerceAtLeast(0),
                other.size.effectiveSpanX(totalCols), other.size.effectiveSpanY()
            )
        }
        val (gx, gy) = if (!overlaps) {
            preferredX to preferredY
        } else {
            findFirstFreeGridPosition(remainingPageTiles, spanX, spanY, totalCols)
        }

        val items = (tilesToAbsorb.map { t ->
            SmartFolderItem(
                id = "item_${t.id}",
                title = t.title,
                packageName = t.packageName,
                tileType = t.tileType,
                customIconName = t.customIconName,
                category = cat
            )
        } + SmartFolderConfig.defaultSeedItemsForCategory(cat))
            .distinctBy { if (it.packageName.isNotBlank()) "pkg:${it.packageName}" else "title:${it.title.lowercase()}" }

        val config = SmartFolderConfig(
            category = cat,
            autoSyncCategory = true,
            layoutMode = SmartFolderLayoutMode.HYPEROS_PEEK,
            items = items
        )

        val folderTile = TileEntity(
            id = "smart_folder_${cat.name.lowercase()}_${System.currentTimeMillis()}",
            title = cat.label,
            packageName = config.toJsonString(),
            tileType = TileType.SMART_FOLDER,
            size = folderSize,
            orderIndex = target.orderIndex,
            pageId = target.pageId,
            gridX = gx,
            gridY = gy,
            colorOverrideHex = target.colorOverrideHex ?: cat.badgeColorHex,
            customIconName = cat.name.lowercase(),
            isLiveEnabled = true
        )
        tileDao.insertOrUpdate(folderTile)
        folderTile
        }
    }

    suspend fun updateSmartFolderConfig(
        tileId: String,
        newTitle: String,
        newConfig: SmartFolderConfig
    ) = withContext(Dispatchers.IO) {
        val allTiles = tileDao.getAllTiles()
        val target = allTiles.find { it.id == tileId } ?: return@withContext
        val updated = target.copy(
            title = newTitle.ifBlank { target.title },
            packageName = newConfig.toJsonString(),
            customIconName = newConfig.category.name.lowercase()
        )
        tileDao.update(updated)
    }

    suspend fun ungroupSmartFolderToTiles(folderTileId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
        val allTiles = tileDao.getAllTiles()
        val folderTile = allTiles.find { it.id == folderTileId && it.tileType == TileType.SMART_FOLDER } ?: return@withTransaction
        val config = SmartFolderConfig.fromTile(folderTile)
        val totalCols = if (loadSettings().showMoreTiles) 6 else 4

        tileDao.deleteById(folderTileId)
        val placed = allTiles.filter { it.pageId == folderTile.pageId && it.id != folderTileId }.toMutableList()

        config.items.take(6).forEachIndexed { index, item ->
            val (gx, gy) = findFirstFreeGridPosition(
                existingPageTiles = placed,
                spanX = TileSize.MEDIUM.effectiveSpanX(totalCols),
                spanY = TileSize.MEDIUM.effectiveSpanY(),
                totalCols = totalCols,
                startY = folderTile.gridY.coerceAtLeast(0)
            )
            val newTile = TileEntity(
                id = "unpacked_${item.id}_${System.currentTimeMillis()}_$index",
                title = item.title,
                packageName = item.packageName,
                tileType = if (item.tileType == TileType.SMART_FOLDER) TileType.APP else item.tileType,
                size = TileSize.MEDIUM,
                orderIndex = placed.size + index,
                pageId = folderTile.pageId,
                gridX = gx,
                gridY = gy,
                customIconName = item.customIconName,
                isLiveEnabled = true
            )
            placed.add(newTile)
            tileDao.insertOrUpdate(newTile)
        }
        }
    }

    private data class CachedAppMeta(
        val label: String,
        val sortKey: String,
        val isSystemApp: Boolean,
        val autoCategory: AppCategory
    )

    private val resolvedAppMetaCache = java.util.concurrent.ConcurrentHashMap<String, CachedAppMeta>()

    suspend fun getInstalledApps(pinnedPackageNames: Set<String>): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val customCategories = loadCustomAppCategories()
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = pm.queryIntentActivities(intent, 0)
        val seenPackages = HashSet<String>(activities.size)
        val list = ArrayList<Pair<String, AppItem>>(activities.size)
        val locale = Locale.getDefault()

        for (resolveInfo in activities) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName || !seenPackages.add(pkg)) continue

            val meta = resolvedAppMetaCache[pkg] ?: run {
                val label = resolveInfo.loadLabel(pm)?.toString() ?: pkg
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val isSystem = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val androidCat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
                    appInfo.category
                } else {
                    -1
                }
                val autoCat = AppCategory.classifyApp(
                    packageName = pkg,
                    label = label,
                    androidCategory = androidCat,
                    isSystemApp = isSystem
                )
                CachedAppMeta(
                    label = label,
                    sortKey = label.lowercase(locale),
                    isSystemApp = isSystem,
                    autoCategory = autoCat
                ).also { resolvedAppMetaCache[pkg] = it }
            }

            val overrideCatName = customCategories[pkg]
            val finalCat = if (!overrideCatName.isNullOrBlank()) {
                try {
                    AppCategory.valueOf(overrideCatName)
                } catch (_: Exception) {
                    meta.autoCategory
                }
            } else {
                meta.autoCategory
            }
            list.add(
                meta.sortKey to AppItem(
                    label = meta.label,
                    packageName = pkg,
                    isSystemApp = meta.isSystemApp,
                    isPinned = pinnedPackageNames.contains(pkg),
                    category = finalCat,
                    isCustomCategory = !overrideCatName.isNullOrBlank()
                )
            )
        }
        list.sortBy { it.first }
        list.map { it.second }
    }

    private fun loadCustomAppCategories(): Map<String, String> {
        val raw = prefs.getString("custom_app_categories_json", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k, "")
                if (k.isNotBlank() && v.isNotBlank()) {
                    map[k] = v
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeCustomAppCategories(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) {
                obj.put(k, v)
            }
        }
        return obj.toString()
    }

    private fun loadAppDrawerFolders(): List<AppFolder> {
        val raw = prefs.getString("app_drawer_folders_json", null) ?: return emptyList()
        return parseAppDrawerFoldersJson(raw)
    }

    private fun parseAppDrawerFoldersJson(raw: String): List<AppFolder> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AppFolder>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pkgsArr = obj.optJSONArray("appPackages") ?: JSONArray()
                val pkgs = mutableListOf<String>()
                for (j in 0 until pkgsArr.length()) {
                    val pkg = pkgsArr.optString(j, "")
                    if (pkg.isNotBlank()) pkgs.add(pkg)
                }
                if (pkgs.isNotEmpty()) {
                    list.add(
                        AppFolder(
                            id = obj.optString("id", "folder_$i"),
                            title = obj.optString("title", "Thư mục"),
                            appPackages = pkgs.distinct(),
                            category = try {
                                AppCategory.valueOf(obj.optString("category", AppCategory.ALL.name))
                            } catch (_: Exception) {
                                AppCategory.ALL
                            },
                            orderIndex = obj.optInt("orderIndex", i)
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeAppDrawerFolders(folders: List<AppFolder>): String {
        val arr = JSONArray()
        folders.forEachIndexed { idx, folder ->
            val pkgs = folder.appPackages.filter { it.isNotBlank() }.distinct()
            if (pkgs.isNotEmpty()) {
                val obj = JSONObject().apply {
                    put("id", folder.id)
                    put("title", folder.title)
                    put("category", folder.category.name)
                    put("orderIndex", idx)
                    val pkgArr = JSONArray()
                    pkgs.forEach { pkgArr.put(it) }
                    put("appPackages", pkgArr)
                }
                arr.put(obj)
            }
        }
        return arr.toString()
    }

    private fun loadAppDrawerCustomOrder(): List<String> {
        val raw = prefs.getString("app_drawer_custom_order_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i, "")
                if (item.isNotBlank()) list.add(item)
            }
            list.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeAppDrawerCustomOrder(order: List<String>): String {
        val arr = JSONArray()
        order.filter { it.isNotBlank() }.distinct().forEach { arr.put(it) }
        return arr.toString()
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

    @Volatile
    private var lastOptimizationTimeMs: Long = 0L

    @Volatile
    private var lastStoppedAppsCount: Int = 0

    @Volatile
    private var lastFreedRamMb: Long = 0L

    suspend fun saveCustomWallpaperFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val rawBytes = inputStream.use { it.readBytes() }
            if (rawBytes.isEmpty()) return@withContext null

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
            var sampleSize = 1
            val maxDim = 1920
            while (boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return@withContext null

            // Clean old wallpaper files
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("custom_wallpaper_")) {
                    file.delete()
                }
            }

            val outFile = File(context.filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearCustomWallpaper() = withContext(Dispatchers.IO) {
        try {
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("custom_wallpaper_")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
        }
    }

    fun optimizeMemory(): SystemTelemetry {
        AppIconMemoryCache.clearCache()
        resolvedAppMetaCache.clear()
        lastOptimizationTimeMs = SystemClock.elapsedRealtime()
        // Android owns other apps' processes; clearing our cache is not proof of RAM reclaimed.
        lastStoppedAppsCount = 0
        lastFreedRamMb = 0L
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
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode == true

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val availMb = memInfo.availMem / (1024 * 1024)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val ramUsagePct = if (totalMb > 0) (((totalMb - availMb).toFloat() / totalMb.toFloat()) * 100).toInt() else 45

        val runningProcessesCount = am?.runningAppProcesses
            ?.count { it.pkgList?.contains(context.packageName) != true }
            ?: 0

        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGb = (stat.availableBlocksLong * stat.blockSizeLong).toFloat() / (1024f * 1024f * 1024f)
        val totalGb = (stat.blockCountLong * stat.blockSizeLong).toFloat() / (1024f * 1024f * 1024f)

        val uptimeHours = SystemClock.elapsedRealtime() / (1000 * 60 * 60)

        return SystemTelemetry(
            batteryPct = batteryPct,
            isCharging = isCharging,
            isSystemPowerSaveMode = isPowerSave,
            batteryTempC = batteryTempC,
            availableRamMb = availMb,
            totalRamMb = totalMb,
            ramUsagePct = ramUsagePct,
            backgroundAppsCount = runningProcessesCount,
            lastStoppedAppsCount = lastStoppedAppsCount,
            lastFreedRamMb = lastFreedRamMb,
            internalFreeGb = String.format(Locale.US, "%.1f", freeGb).toFloat(),
            internalTotalGb = String.format(Locale.US, "%.1f", totalGb).toFloat(),
            uptimeHours = uptimeHours,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Lumia 925 • Minimalist Edition" },
            isDefaultLauncher = isDefaultLauncher(),
            currentDefaultLauncherName = getCurrentDefaultLauncherLabel()
        )
    }

    fun loadSettings(): LauncherSettings {
        val viewModeStr = prefs.getString("all_apps_view_mode", AllAppsViewMode.METRO_LIST.name) ?: AllAppsViewMode.METRO_LIST.name
        val sortOrderStr = prefs.getString("all_apps_sort_order", AllAppsSortOrder.NAME_ASC.name) ?: AllAppsSortOrder.NAME_ASC.name
        val transitionStr = prefs.getString("page_transition_effect", PageTransitionEffect.TURNSTILE_3D.name) ?: PageTransitionEffect.TURNSTILE_3D.name
        val wp10StyleStr = prefs.getString("wp10_tile_style", Wp10TileStyle.TINTED_TRANSLUCENT.name) ?: Wp10TileStyle.TINTED_TRANSLUCENT.name
        val dynamicModeStr = prefs.getString("dynamic_accent_mode", DynamicAccentMode.WP10_VIBRANT.name) ?: DynamicAccentMode.WP10_VIBRANT.name
        val batterySaverModeStr = prefs.getString("battery_saver_mode", BatterySaverMode.AUTO.name) ?: BatterySaverMode.AUTO.name

        val autoWallpaperAccent = prefs.getBoolean("auto_wallpaper_accent_enabled", true)
        val dynamicMode = try { DynamicAccentMode.valueOf(dynamicModeStr) } catch (_: Exception) { DynamicAccentMode.WP10_VIBRANT }
        val swatchIndex = prefs.getInt("dynamic_swatch_index", 0).coerceIn(0, 3)
        val wallpaperIdx = prefs.getInt("wallpaper_gradient_index", 0)
        val savedHex = prefs.getString("accent_color", null)
        val effectiveHex = if (autoWallpaperAccent && (savedHex == null || !prefs.contains("accent_color_manual_override"))) {
            DynamicAccentEngine.resolveAutoAccent(wallpaperIdx, dynamicMode, swatchIndex).hex
        } else {
            savedHex ?: "#0078D7"
        }

        return LauncherSettings(
            accentColorHex = effectiveHex,
            autoWallpaperAccentEnabled = autoWallpaperAccent,
            dynamicAccentMode = dynamicMode,
            dynamicSwatchIndex = swatchIndex,
            isDarkTheme = prefs.getBoolean("is_dark_theme", true),
            wp10TileStyle = try { Wp10TileStyle.valueOf(wp10StyleStr) } catch (_: Exception) { Wp10TileStyle.TINTED_TRANSLUCENT },
            tileOpacity = prefs.getFloat("tile_opacity", 0.45f).coerceIn(0f, 1f),
            wp10RevealBorder = prefs.getBoolean("wp10_reveal_border", true),
            showMoreTiles = prefs.getBoolean("show_more_tiles", false),
            tileCornerRadiusDp = prefs.getInt("tile_corner_radius", 2),
            tileSpacingDp = prefs.getInt("tile_spacing_dp", 6),
            useRealAppIcons = prefs.getBoolean("use_real_app_icons", true),
            miuiGlassBorder = prefs.getBoolean("miui_glass_border", true),
            pageTransitionEffect = try { PageTransitionEffect.valueOf(transitionStr) } catch (_: Exception) { PageTransitionEffect.TURNSTILE_3D },
            doubleTapToGlance = prefs.getBoolean("double_tap_to_glance", true),
            lockHomeLayout = prefs.getBoolean("lock_home_layout", false),
            autoArrangeTiles = prefs.getBoolean("auto_arrange_tiles", false),
            fullWallpaperEnabled = prefs.getBoolean("full_wallpaper_enabled", true),
            wallpaperSource = runCatching { WallpaperSource.valueOf(prefs.getString("wallpaper_source", "") ?: "") }
                .getOrElse { if (!prefs.getString("custom_wallpaper_uri", "").isNullOrBlank()) WallpaperSource.CUSTOM else if (prefs.contains("wallpaper_gradient_index")) WallpaperSource.LUMIA else WallpaperSource.SYSTEM },
            wallpaperGradientIndex = wallpaperIdx,
            customWallpaperUri = prefs.getString("custom_wallpaper_uri", "") ?: "",
            wallpaperDimAlpha = prefs.getFloat("wallpaper_dim_alpha", 0.18f).coerceIn(0f, 0.75f),
            turnstileAnimationEnabled = prefs.getBoolean("turnstile_animation", true),
            glanceScreenEnabled = prefs.getBoolean("glance_screen_enabled", true),
            batterySaverMode = try { BatterySaverMode.valueOf(batterySaverModeStr) } catch (_: Exception) { BatterySaverMode.AUTO },
            batterySaverThresholdPct = prefs.getInt("battery_saver_threshold_pct", 25).coerceIn(10, 50),
            autoOptimizeBackgroundApps = prefs.getBoolean("auto_optimize_background_apps", true),
            quickNotes = prefs.getString("quick_notes", "Chào mừng trở lại Windows 10 Mobile! Ô Tile hỗ trợ tuỳ chỉnh trong suốt xuyên thấu từ 0% đến 100%.") ?: "",
            cloudSyncState = prefs.getString("cloud_sync_state", "Đã đồng bộ cục bộ") ?: "Đã đồng bộ cục bộ",
            defaultHomePageId = prefs.getString("default_home_page_id", "page_home_1") ?: "page_home_1",
            showPageHeaderTitle = prefs.getBoolean("show_page_header_title", false),
            allAppsViewMode = try { AllAppsViewMode.valueOf(viewModeStr) } catch (_: Exception) { AllAppsViewMode.METRO_LIST },
            allAppsGridColumns = prefs.getInt("all_apps_grid_columns", 4),
            allAppsSortOrder = try { AllAppsSortOrder.valueOf(sortOrderStr) } catch (_: Exception) { AllAppsSortOrder.NAME_ASC },
            allAppsShowSearchBar = prefs.getBoolean("all_apps_show_search_bar", true),
            allAppsShowLetterHeaders = prefs.getBoolean("all_apps_show_letter_headers", true),
            allAppsCategorizedEnabled = prefs.getBoolean("all_apps_categorized_enabled", true),
            allAppsGroupByCategory = prefs.getBoolean("all_apps_group_by_category", false),
            customAppCategories = loadCustomAppCategories(),
            appDrawerFolders = loadAppDrawerFolders(),
            appDrawerCustomOrder = loadAppDrawerCustomOrder()
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        val resolvedHex = if (settings.autoWallpaperAccentEnabled) {
            DynamicAccentEngine.resolveAutoAccent(
                wallpaperIndex = settings.wallpaperGradientIndex,
                mode = settings.dynamicAccentMode,
                swatchIndex = settings.dynamicSwatchIndex
            ).hex
        } else {
            settings.accentColorHex
        }

        val editor = prefs.edit()
            .putString("accent_color", resolvedHex)
            .putBoolean("auto_wallpaper_accent_enabled", settings.autoWallpaperAccentEnabled)
            .putString("dynamic_accent_mode", settings.dynamicAccentMode.name)
            .putInt("dynamic_swatch_index", settings.dynamicSwatchIndex.coerceIn(0, 3))
            .putBoolean("is_dark_theme", settings.isDarkTheme)
            .putString("wp10_tile_style", settings.wp10TileStyle.name)
            .putFloat("tile_opacity", settings.tileOpacity.coerceIn(0f, 1f))
            .putBoolean("wp10_reveal_border", settings.wp10RevealBorder)
            .putBoolean("show_more_tiles", settings.showMoreTiles)
            .putInt("tile_corner_radius", settings.tileCornerRadiusDp)
            .putInt("tile_spacing_dp", settings.tileSpacingDp)
            .putBoolean("use_real_app_icons", settings.useRealAppIcons)
            .putBoolean("miui_glass_border", settings.miuiGlassBorder)
            .putString("page_transition_effect", settings.pageTransitionEffect.name)
            .putBoolean("double_tap_to_glance", settings.doubleTapToGlance)
            .putBoolean("lock_home_layout", settings.lockHomeLayout)
            .putBoolean("auto_arrange_tiles", settings.autoArrangeTiles)
            .putBoolean("full_wallpaper_enabled", settings.fullWallpaperEnabled)
            .putString("wallpaper_source", settings.wallpaperSource.name)
            .putInt("wallpaper_gradient_index", settings.wallpaperGradientIndex)
            .putString("custom_wallpaper_uri", settings.customWallpaperUri)
            .putFloat("wallpaper_dim_alpha", settings.wallpaperDimAlpha.coerceIn(0f, 0.75f))
            .putBoolean("turnstile_animation", settings.turnstileAnimationEnabled)
            .putBoolean("glance_screen_enabled", settings.glanceScreenEnabled)
            .putString("battery_saver_mode", settings.batterySaverMode.name)
            .putInt("battery_saver_threshold_pct", settings.batterySaverThresholdPct.coerceIn(10, 50))
            .putBoolean("auto_optimize_background_apps", settings.autoOptimizeBackgroundApps)
            .putString("quick_notes", settings.quickNotes)
            .putString("cloud_sync_state", settings.cloudSyncState)
            .putString("default_home_page_id", settings.defaultHomePageId)
            .putBoolean("show_page_header_title", settings.showPageHeaderTitle)
            .putString("all_apps_view_mode", settings.allAppsViewMode.name)
            .putInt("all_apps_grid_columns", settings.allAppsGridColumns)
            .putString("all_apps_sort_order", settings.allAppsSortOrder.name)
            .putBoolean("all_apps_show_search_bar", settings.allAppsShowSearchBar)
            .putBoolean("all_apps_show_letter_headers", settings.allAppsShowLetterHeaders)
            .putBoolean("all_apps_categorized_enabled", settings.allAppsCategorizedEnabled)
            .putBoolean("all_apps_group_by_category", settings.allAppsGroupByCategory)
            .putString("custom_app_categories_json", serializeCustomAppCategories(settings.customAppCategories))
            .putString("app_drawer_folders_json", serializeAppDrawerFolders(settings.appDrawerFolders))
            .putString("app_drawer_custom_order_json", serializeAppDrawerCustomOrder(settings.appDrawerCustomOrder))

        if (!settings.autoWallpaperAccentEnabled) {
            editor.putBoolean("accent_color_manual_override", true)
        } else {
            editor.remove("accent_color_manual_override")
        }
        editor.apply()
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val settings = loadSettings()
        val pages = loadPages()
        val root = JSONObject()
        root.put("version", 6)
        root.put("app", "LumiaLauncher")
        root.put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        val settingsObj = JSONObject().apply {
            put("accentColorHex", settings.accentColorHex)
            put("autoWallpaperAccentEnabled", settings.autoWallpaperAccentEnabled)
            put("dynamicAccentMode", settings.dynamicAccentMode.name)
            put("dynamicSwatchIndex", settings.dynamicSwatchIndex)
            put("isDarkTheme", settings.isDarkTheme)
            put("wp10TileStyle", settings.wp10TileStyle.name)
            put("tileOpacity", settings.tileOpacity.toDouble())
            put("wp10RevealBorder", settings.wp10RevealBorder)
            put("showMoreTiles", settings.showMoreTiles)
            put("tileCornerRadiusDp", settings.tileCornerRadiusDp)
            put("tileSpacingDp", settings.tileSpacingDp)
            put("useRealAppIcons", settings.useRealAppIcons)
            put("miuiGlassBorder", settings.miuiGlassBorder)
            put("pageTransitionEffect", settings.pageTransitionEffect.name)
            put("doubleTapToGlance", settings.doubleTapToGlance)
            put("lockHomeLayout", settings.lockHomeLayout)
            put("autoArrangeTiles", settings.autoArrangeTiles)
            put("fullWallpaperEnabled", settings.fullWallpaperEnabled)
            put("turnstileAnimationEnabled", settings.turnstileAnimationEnabled)
            put("glanceScreenEnabled", settings.glanceScreenEnabled)
            put("batterySaverMode", settings.batterySaverMode.name)
            put("batterySaverThresholdPct", settings.batterySaverThresholdPct)
            put("wallpaperSource", settings.wallpaperSource.name)
            put("wallpaperGradientIndex", settings.wallpaperGradientIndex)
            put("wallpaperDimAlpha", settings.wallpaperDimAlpha.toDouble())
            put("autoOptimizeBackgroundApps", settings.autoOptimizeBackgroundApps)
            put("quickNotes", settings.quickNotes)
            put("defaultHomePageId", settings.defaultHomePageId)
            put("showPageHeaderTitle", settings.showPageHeaderTitle)
            put("allAppsViewMode", settings.allAppsViewMode.name)
            put("allAppsGridColumns", settings.allAppsGridColumns)
            put("allAppsSortOrder", settings.allAppsSortOrder.name)
            put("allAppsShowSearchBar", settings.allAppsShowSearchBar)
            put("allAppsShowLetterHeaders", settings.allAppsShowLetterHeaders)
            put("allAppsCategorizedEnabled", settings.allAppsCategorizedEnabled)
            put("allAppsGroupByCategory", settings.allAppsGroupByCategory)
            put("customAppCategories", JSONObject(serializeCustomAppCategories(settings.customAppCategories)))
            put("appDrawerFolders", JSONArray(serializeAppDrawerFolders(settings.appDrawerFolders)))
            put("appDrawerCustomOrder", JSONArray(serializeAppDrawerCustomOrder(settings.appDrawerCustomOrder)))
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
                put("gridX", tile.gridX)
                put("gridY", tile.gridY)
                put("colorOverrideHex", tile.colorOverrideHex ?: "")
                put("customIconName", tile.customIconName)
                put("notificationCount", tile.notificationCount)
                put("isLiveEnabled", tile.isLiveEnabled)
                put("appWidgetId", tile.appWidgetId)
                put("widgetProvider", tile.widgetProvider)
                put("widgetMinSpanX", tile.widgetMinSpanX)
                put("widgetMinSpanY", tile.widgetMinSpanY)
                put("widgetResizeMode", tile.widgetResizeMode)
            }
            tilesArr.put(obj)
        }
        root.put("tiles", tilesArr)
        root.toString(2)
    }

    suspend fun restoreFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            require(jsonString.toByteArray(Charsets.UTF_8).size <= 1_048_576)
            val root = JSONObject(jsonString)
            require(root.getInt("version") in 1..6)
            require(root.getString("app") == "LumiaLauncher")
            require(root.has("tiles") && root.has("pages") && root.has("settings"))
            var restoredPages = loadPages()
            var restoredSettings = loadSettings()
            if (root.has("pages")) {
                val pagesArr = root.getJSONArray("pages")
                val pageList = mutableListOf<LauncherPage>()
                for (i in 0 until pagesArr.length()) {
                    val obj = pagesArr.getJSONObject(i)
                    pageList.add(
                        LauncherPage(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            pageType = PageType.valueOf(obj.getString("pageType")),
                            orderIndex = obj.optInt("orderIndex", i)
                        )
                    )
                }
                require(pageList.size in 2..100)
                require(pageList.all { it.id.isNotBlank() } && pageList.map { it.id }.distinct().size == pageList.size)
                require(pageList.any { it.pageType == PageType.HOME } && pageList.count { it.pageType == PageType.ALL_APPS } == 1)
                restoredPages = pageList
            }

            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                val cur = loadSettings()
                val newSettings = cur.copy(
                    accentColorHex = s.optString("accentColorHex", cur.accentColorHex),
                    autoWallpaperAccentEnabled = s.optBoolean("autoWallpaperAccentEnabled", cur.autoWallpaperAccentEnabled),
                    dynamicAccentMode = try {
                        DynamicAccentMode.valueOf(s.optString("dynamicAccentMode", cur.dynamicAccentMode.name))
                    } catch (_: Exception) {
                        cur.dynamicAccentMode
                    },
                    dynamicSwatchIndex = s.optInt("dynamicSwatchIndex", cur.dynamicSwatchIndex).coerceIn(0, 3),
                    isDarkTheme = s.optBoolean("isDarkTheme", cur.isDarkTheme),
                    wp10TileStyle = try { Wp10TileStyle.valueOf(s.optString("wp10TileStyle", cur.wp10TileStyle.name)) } catch (_: Exception) { cur.wp10TileStyle },
                    tileOpacity = s.optDouble("tileOpacity", cur.tileOpacity.toDouble()).toFloat().coerceIn(0f, 1f),
                    wp10RevealBorder = s.optBoolean("wp10RevealBorder", cur.wp10RevealBorder),
                    showMoreTiles = s.optBoolean("showMoreTiles", cur.showMoreTiles),
                    tileCornerRadiusDp = s.optInt("tileCornerRadiusDp", cur.tileCornerRadiusDp),
                    tileSpacingDp = s.optInt("tileSpacingDp", cur.tileSpacingDp),
                    useRealAppIcons = s.optBoolean("useRealAppIcons", cur.useRealAppIcons),
                    miuiGlassBorder = s.optBoolean("miuiGlassBorder", cur.miuiGlassBorder),
                    pageTransitionEffect = try { PageTransitionEffect.valueOf(s.optString("pageTransitionEffect", cur.pageTransitionEffect.name)) } catch (_: Exception) { cur.pageTransitionEffect },
                    doubleTapToGlance = s.optBoolean("doubleTapToGlance", cur.doubleTapToGlance),
                    lockHomeLayout = s.optBoolean("lockHomeLayout", s.optBoolean("lock_home_layout", cur.lockHomeLayout)),
                    autoArrangeTiles = s.optBoolean("autoArrangeTiles", cur.autoArrangeTiles),
                    fullWallpaperEnabled = s.optBoolean("fullWallpaperEnabled", cur.fullWallpaperEnabled),
                    turnstileAnimationEnabled = s.optBoolean("turnstileAnimationEnabled", cur.turnstileAnimationEnabled),
                    glanceScreenEnabled = s.optBoolean("glanceScreenEnabled", cur.glanceScreenEnabled),
                    batterySaverMode = BatterySaverMode.valueOf(s.optString("batterySaverMode", cur.batterySaverMode.name)),
                    batterySaverThresholdPct = s.optInt("batterySaverThresholdPct", cur.batterySaverThresholdPct).coerceIn(10, 50),
                    wallpaperSource = runCatching { WallpaperSource.valueOf(s.optString("wallpaperSource", cur.wallpaperSource.name)) }.getOrDefault(cur.wallpaperSource),
                    wallpaperGradientIndex = s.optInt("wallpaperGradientIndex", cur.wallpaperGradientIndex),
                    wallpaperDimAlpha = s.optDouble("wallpaperDimAlpha", cur.wallpaperDimAlpha.toDouble()).toFloat().coerceIn(0f, 0.75f),
                    autoOptimizeBackgroundApps = s.optBoolean("autoOptimizeBackgroundApps", cur.autoOptimizeBackgroundApps),
                    quickNotes = s.optString("quickNotes", cur.quickNotes),
                    defaultHomePageId = s.optString("defaultHomePageId", cur.defaultHomePageId),
                    showPageHeaderTitle = s.optBoolean("showPageHeaderTitle", cur.showPageHeaderTitle),
                    allAppsViewMode = try { AllAppsViewMode.valueOf(s.optString("allAppsViewMode", cur.allAppsViewMode.name)) } catch (_: Exception) { cur.allAppsViewMode },
                    allAppsGridColumns = s.optInt("allAppsGridColumns", cur.allAppsGridColumns),
                    allAppsSortOrder = try { AllAppsSortOrder.valueOf(s.optString("allAppsSortOrder", cur.allAppsSortOrder.name)) } catch (_: Exception) { cur.allAppsSortOrder },
                    allAppsShowSearchBar = s.optBoolean("allAppsShowSearchBar", cur.allAppsShowSearchBar),
                    allAppsShowLetterHeaders = s.optBoolean("allAppsShowLetterHeaders", cur.allAppsShowLetterHeaders),
                    allAppsCategorizedEnabled = s.optBoolean("allAppsCategorizedEnabled", cur.allAppsCategorizedEnabled),
                    allAppsGroupByCategory = s.optBoolean("allAppsGroupByCategory", cur.allAppsGroupByCategory),
                    customAppCategories = s.optJSONObject("customAppCategories")?.let { obj ->
                        val map = mutableMapOf<String, String>()
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val v = obj.optString(k, "")
                            if (k.isNotBlank() && v.isNotBlank()) map[k] = v
                        }
                        map
                    } ?: cur.customAppCategories,
                    appDrawerFolders = s.optJSONArray("appDrawerFolders")?.let { arr ->
                        parseAppDrawerFoldersJson(arr.toString())
                    } ?: cur.appDrawerFolders,
                    appDrawerCustomOrder = s.optJSONArray("appDrawerCustomOrder")?.let { arr ->
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            val item = arr.optString(i, "")
                            if (item.isNotBlank()) list.add(item)
                        }
                        list.distinct()
                    } ?: cur.appDrawerCustomOrder,
                    cloudSyncState = "Đã khôi phục từ tệp sao lưu (" + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) + ")"
                )
                restoredSettings = newSettings
            }

            if (root.has("tiles")) {
                val hostManager = LumiaWidgetHostManager.getInstance(context)
                val tilesArr = root.getJSONArray("tiles")
                val list = mutableListOf<TileEntity>()
                for (i in 0 until tilesArr.length()) {
                    val obj = tilesArr.getJSONObject(i)
                    val parsedType = TileType.valueOf(obj.getString("tileType"))
                    val parsedSize = TileSize.valueOf(obj.getString("size"))
                    val parsedProvider = obj.optString("widgetProvider", "")
                    val isWidget = parsedType == TileType.WIDGET || parsedProvider.isNotBlank()

                    // Widget IDs are device-local capabilities. Restore an explicit rebind placeholder.
                    val restoredWidgetId = -1

                    list.add(
                        TileEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            packageName = obj.optString("packageName", ""),
                            tileType = parsedType,
                            size = parsedSize,
                            orderIndex = obj.optInt("orderIndex", i),
                            pageId = obj.optString("pageId", "page_home_1"),
                            gridX = obj.optInt("gridX", -1),
                            gridY = obj.optInt("gridY", -1),
                            colorOverrideHex = obj.optString("colorOverrideHex").takeIf { it.isNotEmpty() },
                            customIconName = obj.optString("customIconName", ""),
                            notificationCount = obj.optInt("notificationCount", 0),
                            isLiveEnabled = obj.optBoolean("isLiveEnabled", true),
                            appWidgetId = restoredWidgetId,
                            widgetProvider = parsedProvider,
                            widgetMinSpanX = obj.optInt("widgetMinSpanX", 1).coerceIn(1, 4),
                            widgetMinSpanY = obj.optInt("widgetMinSpanY", 1).coerceIn(1, 4),
                            widgetResizeMode = obj.optInt("widgetResizeMode", 3)
                        )
                    )
                }
                require(list.size <= 5000)
                require(list.all { it.id.isNotBlank() } && list.map { it.id }.distinct().size == list.size)
                val homes = restoredPages.filter { it.pageType == PageType.HOME }.map { it.id }.toSet()
                require(restoredSettings.defaultHomePageId in homes)
                require(list.all { it.pageId in homes && it.gridX in -1..12 && it.gridY in -1..100000 })
                require(restoredSettings.accentColorHex.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")))
                require(restoredSettings.tileSpacingDp in 0..32 && restoredSettings.tileCornerRadiusDp in 0..64)
                require(restoredSettings.allAppsGridColumns in 2..8)
                val repaired = reflowTiles(list, if (restoredSettings.showMoreTiles) 6 else 4)
                val oldWidgetIds = tileDao.getAllTiles().map { it.appWidgetId }.filter { it > 0 }.distinct()
                // Journal bridges Room and preferences. Startup replays it if the process dies mid-commit.
                check(prefs.edit().putString("pending_restore_json", jsonString).commit())
                database.withTransaction {
                    tileDao.clearAll()
                    tileDao.insertAll(repaired)
                }
                savePages(restoredPages)
                saveSettings(restoredSettings)
                check(prefs.edit().putBoolean("home_initialized", true)
                    .putBoolean("smart_folder_default_seeded_v1", true).remove("pending_restore_json").commit())
                oldWidgetIds.forEach { id -> runCatching { hostManager.releaseWidgetId(id) } }
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    internal fun reflowTiles(tiles: List<TileEntity>, columns: Int): List<TileEntity> =
        tiles.groupBy { it.pageId }.flatMap { (_, page) ->
            val placed = mutableListOf<TileEntity>()
            page.sortedBy { it.orderIndex }.forEach { tile ->
                val w = tile.size.effectiveSpanX(columns)
                val h = tile.size.effectiveSpanY()
                val valid = tile.gridX >= 0 && tile.gridY >= 0 && tile.gridX + w <= columns &&
                    placed.none { rectsOverlap(tile.gridX, tile.gridY, w, h, it.gridX, it.gridY,
                        it.size.effectiveSpanX(columns), it.size.effectiveSpanY()) }
                val (x, y) = if (valid) tile.gridX to tile.gridY else findFirstFreeGridPosition(placed, w, h, columns)
                placed.add(tile.copy(gridX = x, gridY = y))
            }
            placed
        }

    suspend fun saveSettingsWithReflow(settings: LauncherSettings) = withContext(Dispatchers.IO) {
        if (settings.showMoreTiles != loadSettings().showMoreTiles) {
            database.withTransaction {
                tileDao.insertAll(reflowTiles(tileDao.getAllTiles(), if (settings.showMoreTiles) 6 else 4))
            }
        }
        val shouldCompact = settings.autoArrangeTiles && !loadSettings().autoArrangeTiles
        saveSettings(settings)
        if (shouldCompact) {
            tileDao.getAllTiles().map { it.pageId }.distinct().forEach { arrangeTilesOnPage(it) }
        }
    }
}
