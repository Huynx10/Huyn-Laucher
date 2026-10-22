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
import com.example.data.model.AppCategory
import com.example.data.model.AppFolder
import com.example.data.model.AppItem
import com.example.data.model.DynamicAccentMode
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.PageTransitionEffect
import com.example.data.model.PageType
import com.example.data.model.SystemTelemetry
import com.example.data.model.TileEntity
import com.example.data.model.TileSize
import com.example.data.model.TileType
import com.example.data.model.Wp10TileStyle
import com.example.ui.theme.DynamicAccentEngine
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
        } else {
            val allTiles = tileDao.getAllTiles()
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
        for (y in startY.coerceAtLeast(0)..80) {
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
        return 0 to 0
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
        setTileSizeInternal(tiles, target, target.size.next())
    }

    suspend fun stepTileSize(tileId: String, increase: Boolean) = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val target = tiles.find { it.id == tileId } ?: return@withContext
        val nextSize = if (increase) target.size.stepUp() else target.size.stepDown()
        if (nextSize != target.size) {
            setTileSizeInternal(tiles, target, nextSize)
        }
    }

    suspend fun setTileSize(tileId: String, newSize: TileSize) = withContext(Dispatchers.IO) {
        val tiles = tileDao.getAllTiles()
        val target = tiles.find { it.id == tileId } ?: return@withContext
        if (newSize != target.size) {
            setTileSizeInternal(tiles, target, newSize)
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
                return@withContext
            }
        }

        val resolved = resolveCollisionsOnly(movedTarget, pageOthers, totalCols)
        tileDao.insertAll(resolved)
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

    suspend fun unpinTile(tileId: String) = withContext(Dispatchers.IO) {
        tileDao.deleteById(tileId)
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
    }

    suspend fun resetToDefaultTiles() = withContext(Dispatchers.IO) {
        tileDao.clearAll()
        val defaultPages = getDefaultPages()
        savePages(defaultPages)
        tileDao.insertAll(createDefaultTiles())
    }

    suspend fun getInstalledApps(pinnedPackageNames: Set<String>): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val customCategories = loadCustomAppCategories()
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
            val overrideCatName = customCategories[pkg]
            val finalCat = if (!overrideCatName.isNullOrBlank()) {
                try {
                    AppCategory.valueOf(overrideCatName)
                } catch (_: Exception) {
                    autoCat
                }
            } else {
                autoCat
            }
            list.add(
                AppItem(
                    label = label,
                    packageName = pkg,
                    isSystemApp = isSystem,
                    isPinned = pinnedPackageNames.contains(pkg),
                    category = finalCat,
                    isCustomCategory = !overrideCatName.isNullOrBlank()
                )
            )
        }
        list.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
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
        val wp10StyleStr = prefs.getString("wp10_tile_style", Wp10TileStyle.TINTED_TRANSLUCENT.name) ?: Wp10TileStyle.TINTED_TRANSLUCENT.name
        val dynamicModeStr = prefs.getString("dynamic_accent_mode", DynamicAccentMode.WP10_VIBRANT.name) ?: DynamicAccentMode.WP10_VIBRANT.name

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
            fullWallpaperEnabled = prefs.getBoolean("full_wallpaper_enabled", true),
            wallpaperGradientIndex = wallpaperIdx,
            turnstileAnimationEnabled = prefs.getBoolean("turnstile_animation", true),
            glanceScreenEnabled = prefs.getBoolean("glance_screen_enabled", true),
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
        root.put("version", 5)
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
            put("wallpaperGradientIndex", settings.wallpaperGradientIndex)
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
                            gridX = obj.optInt("gridX", -1),
                            gridY = obj.optInt("gridY", -1),
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
                    ensureDefaultTiles()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
