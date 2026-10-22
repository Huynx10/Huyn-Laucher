package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.PageType
import com.example.data.model.TileType
import com.example.data.repository.LauncherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    assertTrue(tiles.any { it.tileType == TileType.PHOTOS && it.pageId == "page_home_2" })

    val pages = repo.loadPages()
    assertEquals(3, pages.size)
    assertEquals(PageType.HOME, pages[0].pageType)
    assertEquals(PageType.ALL_APPS, pages[2].pageType)

    // Move a tile from page_home_1 to page_home_2
    repo.moveTileToPage("tile_weather", "page_home_2")
    val updatedWeather = db.tileDao().getAllTiles().find { it.id == "tile_weather" }
    assertEquals("page_home_2", updatedWeather?.pageId)

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
}
