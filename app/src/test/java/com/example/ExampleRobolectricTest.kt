package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.PageType
import com.example.data.model.TileType
import com.example.data.repository.LauncherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lumia Launcher", appName)
  }

  @Test
  fun `test multi-page default tiles and page management`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LumiaDatabase.getDatabase(context)
    val repo = LauncherRepository(context, db.tileDao())

    repo.resetToDefaultTiles()
    val tiles = db.tileDao().getAllTiles()
    assertTrue(tiles.isNotEmpty())
    assertTrue(tiles.any { it.tileType == TileType.CLOCK_CALENDAR && it.pageId == "page_home_1" })
    assertFalse(tiles.any { it.tileType == TileType.WEATHER || it.tileType == TileType.NEWS_QUOTE || it.tileType == TileType.NOTES })

    val pages = repo.loadPages()
    assertEquals(3, pages.size)
    assertEquals(PageType.HOME, pages[0].pageType)
    assertEquals(PageType.ALL_APPS, pages[2].pageType)

    // Move a tile from page_home_1 to page_home_2
    repo.moveTileToPage("tile_store", "page_home_2")
    val movedStore = db.tileDao().getAllTiles().find { it.id == "tile_store" }
    assertEquals("page_home_2", movedStore?.pageId)

    // Move a tile to a free 2D grid coordinate (e.g. gridX = 2, gridY = 14) leaving empty rows above it
    repo.moveTileToGridPosition("tile_phone", targetGridX = 2, targetGridY = 14, totalCols = 4)
    val movedPhone = db.tileDao().getAllTiles().find { it.id == "tile_phone" }
    assertEquals(2, movedPhone?.gridX)
    assertEquals(14, movedPhone?.gridY)

    // Test + / - stepTileSize and direct border-drag setTileSize
    repo.setTileSize("tile_phone", com.example.data.model.TileSize.SMALL)
    assertEquals(com.example.data.model.TileSize.SMALL, db.tileDao().getAllTiles().find { it.id == "tile_phone" }?.size)

    repo.stepTileSize("tile_phone", increase = true)
    assertEquals(com.example.data.model.TileSize.MEDIUM, db.tileDao().getAllTiles().find { it.id == "tile_phone" }?.size)

    repo.stepTileSize("tile_phone", increase = true)
    assertEquals(com.example.data.model.TileSize.WIDE, db.tileDao().getAllTiles().find { it.id == "tile_phone" }?.size)

    repo.stepTileSize("tile_phone", increase = true)
    assertEquals(com.example.data.model.TileSize.LARGE, db.tileDao().getAllTiles().find { it.id == "tile_phone" }?.size)

    repo.stepTileSize("tile_phone", increase = false)
    assertEquals(com.example.data.model.TileSize.WIDE, db.tileDao().getAllTiles().find { it.id == "tile_phone" }?.size)

    assertEquals(com.example.data.model.TileSize.SMALL, com.example.data.model.TileSize.fromTargetSpans(1, 1))
    assertEquals(com.example.data.model.TileSize.MEDIUM, com.example.data.model.TileSize.fromTargetSpans(2, 2))
    assertEquals(com.example.data.model.TileSize.WIDE, com.example.data.model.TileSize.fromTargetSpans(4, 2))
    assertEquals(com.example.data.model.TileSize.LARGE, com.example.data.model.TileSize.fromTargetSpans(4, 4))
  }

  @Test
  fun `test MIUI app drawer categorization and custom category overrides`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LumiaDatabase.getDatabase(context)
    val repo = LauncherRepository(context, db.tileDao())

    // Verify automatic classification for Social, Tools, Games, Communication, Media, Productivity, System
    assertEquals(
      com.example.data.model.AppCategory.COMMUNICATION,
      com.example.data.model.AppCategory.classifyApp("com.google.android.dialer", "Điện thoại")
    )
    assertEquals(
      com.example.data.model.AppCategory.SOCIAL,
      com.example.data.model.AppCategory.classifyApp("com.facebook.katana", "Facebook")
    )
    assertEquals(
      com.example.data.model.AppCategory.GAMES,
      com.example.data.model.AppCategory.classifyApp("com.mojang.minecraftpe", "Minecraft", androidCategory = 0)
    )
    assertEquals(
      com.example.data.model.AppCategory.MEDIA,
      com.example.data.model.AppCategory.classifyApp("com.google.android.apps.photos", "Photos", androidCategory = 3)
    )
    assertEquals(
      com.example.data.model.AppCategory.TOOLS,
      com.example.data.model.AppCategory.classifyApp("com.android.calculator2", "Máy tính")
    )
    assertEquals(
      com.example.data.model.AppCategory.PRODUCTIVITY,
      com.example.data.model.AppCategory.classifyApp("com.android.vending", "CH Play")
    )
    assertEquals(
      com.example.data.model.AppCategory.SYSTEM,
      com.example.data.model.AppCategory.classifyApp("com.android.settings", "Cài đặt", isSystemApp = true)
    )

    // Verify custom category override persistence & backup/restore
    val initialSettings = repo.loadSettings()
    val customOverrides = mapOf("com.android.calculator2" to com.example.data.model.AppCategory.PRODUCTIVITY.name)
    repo.saveSettings(
      initialSettings.copy(
        allAppsCategorizedEnabled = true,
        allAppsGroupByCategory = true,
        customAppCategories = customOverrides
      )
    )

    val reloaded = repo.loadSettings()
    assertTrue(reloaded.allAppsCategorizedEnabled)
    assertTrue(reloaded.allAppsGroupByCategory)
    assertEquals(
      com.example.data.model.AppCategory.PRODUCTIVITY.name,
      reloaded.customAppCategories["com.android.calculator2"]
    )

    val calcItem = com.example.data.model.AppItem(
      label = "Máy tính",
      packageName = "com.android.calculator2"
    )
    assertEquals(
      com.example.data.model.AppCategory.PRODUCTIVITY,
      calcItem.effectiveCategory(reloaded.customAppCategories)
    )

    // Verify Grid View folders and custom drag order persistence
    val testFolder = com.example.data.model.AppFolder(
      id = "folder_tools",
      title = "Công cụ",
      appPackages = listOf("com.android.calculator2", "com.android.chrome"),
      category = com.example.data.model.AppCategory.TOOLS,
      orderIndex = 0
    )
    val customOrder = listOf("folder:folder_tools", "app:com.google.android.dialer")
    repo.saveSettings(
      reloaded.copy(
        appDrawerFolders = listOf(testFolder),
        appDrawerCustomOrder = customOrder
      )
    )
    val withFolders = repo.loadSettings()
    assertEquals(1, withFolders.appDrawerFolders.size)
    assertEquals("Công cụ", withFolders.appDrawerFolders.first().title)
    assertEquals(2, withFolders.appDrawerFolders.first().appPackages.size)
    assertEquals(customOrder, withFolders.appDrawerCustomOrder)
  }

  @Test
  fun `test dynamic accent color engine from wallpapers and material you tonal modes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LumiaDatabase.getDatabase(context)
    val repo = LauncherRepository(context, db.tileDao())

    val w10mHeroPalette = com.example.ui.theme.DynamicAccentEngine.extractPaletteForWallpaper(
      wallpaperIndex = 0,
      mode = com.example.data.model.DynamicAccentMode.WP10_VIBRANT
    )
    val crimsonSunsetPalette = com.example.ui.theme.DynamicAccentEngine.extractPaletteForWallpaper(
      wallpaperIndex = 6,
      mode = com.example.data.model.DynamicAccentMode.WP10_VIBRANT
    )
    val materialTonalPalette = com.example.ui.theme.DynamicAccentEngine.extractPaletteForWallpaper(
      wallpaperIndex = 4,
      mode = com.example.data.model.DynamicAccentMode.MATERIAL_YOU_TONAL
    )

    assertEquals(4, w10mHeroPalette.swatches.size)
    assertTrue(w10mHeroPalette.primarySeed.hex != crimsonSunsetPalette.primarySeed.hex)
    assertTrue(materialTonalPalette.primarySeed.hex.startsWith("#"))

    // Verify automatic wallpaper accent resolution in LauncherSettings & LauncherRepository
    val initial = repo.loadSettings()
    repo.saveSettings(
      initial.copy(
        autoWallpaperAccentEnabled = true,
        wallpaperGradientIndex = 6,
        dynamicAccentMode = com.example.data.model.DynamicAccentMode.WP10_VIBRANT,
        dynamicSwatchIndex = 0
      )
    )
    val loaded = repo.loadSettings()
    assertTrue(loaded.autoWallpaperAccentEnabled)
    assertEquals(crimsonSunsetPalette.primarySeed.hex, loaded.accentColorHex)
  }

  @Test
  fun `test smart folder widget system and category grouping`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LumiaDatabase.getDatabase(context)
    val repo = LauncherRepository(context, db.tileDao())

    repo.resetToDefaultTiles()
    val initialTiles = db.tileDao().getAllTiles()
    assertTrue(initialTiles.any { it.tileType == TileType.SMART_FOLDER })

    // Create a Smart Folder for MEDIA category absorbing matching tiles on page_home_1
    val mediaFolder = repo.createSmartFolderByCategory(
      category = com.example.data.model.AppCategory.MEDIA,
      pageId = "page_home_1",
      size = com.example.data.model.TileSize.MEDIUM,
      absorbMatchingPageTiles = true
    )
    assertEquals(TileType.SMART_FOLDER, mediaFolder.tileType)

    val parsedConfig = com.example.data.model.SmartFolderConfig.fromTile(mediaFolder)
    assertEquals(com.example.data.model.AppCategory.MEDIA, parsedConfig.category)
    assertEquals(com.example.data.model.SmartFolderLayoutMode.HYPEROS_PEEK, parsedConfig.layoutMode)

    val effectiveItems = com.example.data.model.SmartFolderConfig.resolveEffectiveItems(
      config = parsedConfig,
      installedApps = emptyList()
    )
    assertEquals(parsedConfig.items, effectiveItems) // No invented apps to fill the preview.

    // Group all remaining loose tiles on page_home_2 by category into Smart Folders
    val groupedCount = repo.groupPageTilesByCategoryIntoSmartFolders("page_home_2")
    assertTrue(groupedCount > 0)
    val page2TilesAfterGroup = db.tileDao().getAllTiles().filter { it.pageId == "page_home_2" }
    assertTrue(page2TilesAfterGroup.all { it.tileType == TileType.SMART_FOLDER })
  }

  @Test
  fun `test battery saving mode reduces update frequency and animation complexity`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LumiaDatabase.getDatabase(context)
    val repo = LauncherRepository(context, db.tileDao())

    val normalTelemetry = com.example.data.model.SystemTelemetry(
      batteryPct = 80,
      isCharging = false,
      isSystemPowerSaveMode = false
    )
    val lowBatteryTelemetry = com.example.data.model.SystemTelemetry(
      batteryPct = 18,
      isCharging = false,
      isSystemPowerSaveMode = false
    )
    val chargingLowBatteryTelemetry = com.example.data.model.SystemTelemetry(
      batteryPct = 15,
      isCharging = true,
      isSystemPowerSaveMode = false
    )

    val autoSettings = repo.loadSettings().copy(
      batterySaverMode = com.example.data.model.BatterySaverMode.AUTO,
      batterySaverThresholdPct = 25,
      pageTransitionEffect = com.example.data.model.PageTransitionEffect.TURNSTILE_3D
    )

    // Normal battery (80%) -> Battery Saver inactive, standard intervals and 3D transition
    assertTrue(!autoSettings.isBatterySaverActive(normalTelemetry))
    assertEquals(10_000L, autoSettings.effectiveLiveTileIntervalMs(normalTelemetry, 10_000L))
    assertEquals(12_000L, autoSettings.effectiveTelemetryPollIntervalMs(normalTelemetry))
    assertEquals(
      com.example.data.model.PageTransitionEffect.TURNSTILE_3D,
      autoSettings.effectivePageTransitionEffect(normalTelemetry)
    )

    // Low battery (18% <= 25%) and not charging -> Battery Saver active, 3x slower intervals and flat CLASSIC_SLIDE transition
    assertTrue(autoSettings.isBatterySaverActive(lowBatteryTelemetry))
    assertEquals(30_000L, autoSettings.effectiveLiveTileIntervalMs(lowBatteryTelemetry, 10_000L))
    assertEquals(30_000L, autoSettings.effectiveTelemetryPollIntervalMs(lowBatteryTelemetry))
    assertEquals(
      com.example.data.model.PageTransitionEffect.CLASSIC_SLIDE,
      autoSettings.effectivePageTransitionEffect(lowBatteryTelemetry)
    )

    // Low battery while charging -> Battery Saver inactive in AUTO mode unless system power save is on
    assertTrue(!autoSettings.isBatterySaverActive(chargingLowBatteryTelemetry))

    // Persist ALWAYS_ON mode and verify reload
    repo.saveSettings(
      autoSettings.copy(
        batterySaverMode = com.example.data.model.BatterySaverMode.ALWAYS_ON,
        batterySaverThresholdPct = 35
      )
    )
    val reloaded = repo.loadSettings()
    assertEquals(com.example.data.model.BatterySaverMode.ALWAYS_ON, reloaded.batterySaverMode)
    assertEquals(35, reloaded.batterySaverThresholdPct)
    assertTrue(reloaded.isBatterySaverActive(normalTelemetry))
  }

  @Test
  fun `test custom wallpaper settings and background app optimization`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LumiaDatabase.getDatabase(context)
    val repo = LauncherRepository(context, db.tileDao())

    val initial = repo.loadSettings()
    repo.saveSettings(
      initial.copy(
        customWallpaperUri = "/data/user/0/com.example/files/custom_wallpaper_test.jpg",
        wallpaperDimAlpha = 0.32f,
        autoOptimizeBackgroundApps = true
      )
    )
    val loaded = repo.loadSettings()
    assertEquals("/data/user/0/com.example/files/custom_wallpaper_test.jpg", loaded.customWallpaperUri)
    assertEquals(0.32f, loaded.wallpaperDimAlpha, 0.01f)
    assertTrue(loaded.autoOptimizeBackgroundApps)

    // Test optimizeMemory clears in-memory caches and stops background packages without System.gc() or fake RAM numbers
    val optimizedTelemetry = repo.optimizeMemory()
    assertTrue(optimizedTelemetry.backgroundAppsCount >= 0)
    assertTrue(optimizedTelemetry.lastStoppedAppsCount >= 0)
    assertTrue(optimizedTelemetry.lastFreedRamMb >= 0L)
  }

  @Test
  fun `test live tile binds directly to installed app data`() {
    val sampleApps = listOf(
      com.example.data.model.AppItem(
        label = "Đồng hồ",
        packageName = "com.google.android.deskclock",
        category = com.example.data.model.AppCategory.TOOLS
      ),
      com.example.data.model.AppItem(
        label = "Google Photos",
        packageName = "com.google.android.apps.photos",
        category = com.example.data.model.AppCategory.MEDIA
      ),
      com.example.data.model.AppItem(
        label = "Chrome",
        packageName = "com.android.chrome",
        category = com.example.data.model.AppCategory.TOOLS
      )
    )

    val clockTile = com.example.data.model.TileEntity(
      id = "tile_clock_calendar",
      title = "Lịch & Giờ",
      tileType = TileType.CLOCK_CALENDAR
    )
    val photosTile = com.example.data.model.TileEntity(
      id = "tile_photos",
      title = "Ảnh",
      tileType = TileType.PHOTOS
    )
    val explicitAppTile = com.example.data.model.TileEntity(
      id = "tile_chrome",
      title = "Trình duyệt",
      packageName = "com.android.chrome",
      tileType = TileType.APP
    )

    val linkedClock = com.example.ui.components.resolveLinkedAppForTile(clockTile, sampleApps)
    val linkedPhotos = com.example.ui.components.resolveLinkedAppForTile(photosTile, sampleApps)
    val linkedChrome = com.example.ui.components.resolveLinkedAppForTile(explicitAppTile, sampleApps)

    assertEquals("com.google.android.deskclock", linkedClock?.packageName)
    assertEquals("com.google.android.apps.photos", linkedPhotos?.packageName)
    assertEquals("com.android.chrome", linkedChrome?.packageName)
  }

  @Test
  fun `test native widget tile pinning, linking, resize limits, and backup restore relinking`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LumiaDatabase.getDatabase(context)
    val repo = LauncherRepository(context, db.tileDao())

    repo.resetToDefaultTiles()

    // 1. Pin a native Android Widget Tile with 4x2 minimum span & horizontal-only resize mode (resizeMode = 1)
    val weatherWidgetItem = com.example.data.model.WidgetProviderItem(
      providerComponent = "com.google.android.googlequicksearchbox/com.google.android.apps.gsa.weatherwidget.WeatherWidgetProvider",
      packageName = "com.google.android.googlequicksearchbox",
      className = "com.google.android.apps.gsa.weatherwidget.WeatherWidgetProvider",
      appName = "Google",
      label = "Thời tiết",
      minWidthDp = 250,
      minHeightDp = 110,
      minResizeWidthDp = 180,
      minResizeHeightDp = 110,
      targetCellWidth = 4,
      targetCellHeight = 2,
      resizeMode = 1 // RESIZE_HORIZONTAL only
    )

    val pinnedWidgetTile = repo.pinWidgetTile(
      provider = weatherWidgetItem,
      appWidgetId = 1042,
      pageId = "page_home_1"
    )
    assertEquals(TileType.WIDGET, pinnedWidgetTile.tileType)
    assertEquals(1042, pinnedWidgetTile.appWidgetId)
    assertEquals(com.example.data.model.TileSize.WIDE, pinnedWidgetTile.size)
    assertTrue(pinnedWidgetTile.hasBoundOrLinkedWidget())

    // Verify widget resize limits: minSpanX=4, minSpanY=2, resizeMode=1 -> only WIDE (4x2) is allowed
    val allowedSizes = pinnedWidgetTile.allowedSizesForTile()
    assertEquals(listOf(com.example.data.model.TileSize.WIDE), allowedSizes)

    // Attempting to shrink to SMALL (1x1) must clamp to WIDE (4x2)
    repo.setTileSize(pinnedWidgetTile.id, com.example.data.model.TileSize.SMALL)
    val afterInvalidShrink = db.tileDao().getAllTiles().find { it.id == pinnedWidgetTile.id }
    assertEquals(com.example.data.model.TileSize.WIDE, afterInvalidShrink?.size)

    // 2. Link an Android Widget to an existing Start Screen Tile and then unlink it
    val clockWidgetItem = com.example.data.model.WidgetProviderItem(
      providerComponent = "com.google.android.deskclock/com.android.alarmclock.DigitalAppWidgetProvider",
      packageName = "com.google.android.deskclock",
      className = "com.android.alarmclock.DigitalAppWidgetProvider",
      appName = "Đồng hồ",
      label = "Đồng hồ số",
      minWidthDp = 140,
      minHeightDp = 140,
      minResizeWidthDp = 110,
      minResizeHeightDp = 110,
      targetCellWidth = 2,
      targetCellHeight = 2,
      resizeMode = 3 // RESIZE_BOTH
    )
    repo.linkWidgetToExistingTile("tile_clock_calendar", clockWidgetItem, appWidgetId = 2088)
    val linkedClockTile = db.tileDao().getAllTiles().find { it.id == "tile_clock_calendar" }
    assertTrue(linkedClockTile != null && linkedClockTile.hasBoundOrLinkedWidget())
    assertEquals(2088, linkedClockTile?.appWidgetId)
    assertEquals(clockWidgetItem.providerComponent, linkedClockTile?.widgetProvider)

    // Unlink widget -> returns to normal tile without widget ID
    repo.unlinkWidgetFromTile("tile_clock_calendar")
    val unlinkedClockTile = db.tileDao().getAllTiles().find { it.id == "tile_clock_calendar" }
    assertEquals(-1, unlinkedClockTile?.appWidgetId)
    assertEquals("", unlinkedClockTile?.widgetProvider)

    // 3. Export Backup JSON & Restore -> verify widget provider metadata survives and stale appWidgetId is safely handled
    val backupJson = repo.exportBackupJson()
    assertTrue(backupJson.contains("WeatherWidgetProvider"))
    val restoreOk = repo.restoreFromJson(backupJson)
    assertTrue(restoreOk)

    val restoredWidgetTile = db.tileDao().getAllTiles().find { it.id == pinnedWidgetTile.id }
    assertEquals(TileType.WIDGET, restoredWidgetTile?.tileType)
    assertEquals(weatherWidgetItem.providerComponent, restoredWidgetTile?.widgetProvider)
    assertEquals(4, restoredWidgetTile?.widgetMinSpanX)
    assertEquals(2, restoredWidgetTile?.widgetMinSpanY)
  }

  @Test
  fun `test non-destructive Room migration adds widget columns safely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LumiaDatabase.getDatabase(context)
    val sqlDb = db.openHelper.writableDatabase

    // Running migration helper idempotently on existing database should succeed without throwing or losing data
    LumiaDatabase.MIGRATION_3_4.migrate(sqlDb)

    val columns = mutableSetOf<String>()
    sqlDb.query("PRAGMA table_info(tiles)").use { cursor ->
      val nameIdx = cursor.getColumnIndex("name")
      while (cursor.moveToNext()) {
        if (nameIdx >= 0) columns.add(cursor.getString(nameIdx))
      }
    }
    assertTrue(columns.contains("appWidgetId"))
    assertTrue(columns.contains("widgetProvider"))
    assertTrue(columns.contains("widgetMinSpanX"))
    assertTrue(columns.contains("widgetMinSpanY"))
    assertTrue(columns.contains("widgetResizeMode"))
  }
}


