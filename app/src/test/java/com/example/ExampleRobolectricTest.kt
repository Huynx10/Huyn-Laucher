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
  }
}
