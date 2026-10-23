package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.*
import com.example.data.repository.LauncherRepository
import com.example.data.repository.validateCloudBackupJson
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DataSafetyRegressionTest {
    private lateinit var db: LumiaDatabase
    private lateinit var repo: LauncherRepository
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java).allowMainThreadQueries().build()
        repo = LauncherRepository(context, db.tileDao(), db)
    }
    @After fun close() = db.close()

    @Test fun malformedRestoreDoesNotWritePagesSettingsOrTiles() = runBlocking {
        repo.ensureDefaultTiles()
        val before = db.tileDao().getAllTiles()
        val pages = repo.loadPages()
        val settings = repo.loadSettings()
        val backup = JSONObject(repo.exportBackupJson())
        backup.getJSONArray("pages").getJSONObject(0).put("title", "Changed")
        backup.getJSONObject("settings").put("quickNotes", "Changed")
        backup.getJSONArray("tiles").getJSONObject(0).put("tileType", "INVALID")
        assertFalse(repo.restoreFromJson(backup.toString()))
        assertEquals(before, db.tileDao().getAllTiles())
        assertEquals(pages, repo.loadPages())
        assertEquals(settings, repo.loadSettings())
    }

    @Test fun emptyRestoreStaysEmptyAfterInitialization() = runBlocking {
        repo.ensureDefaultTiles()
        val backup = JSONObject(repo.exportBackupJson()).put("tiles", JSONArray())
        assertTrue(repo.restoreFromJson(backup.toString()))
        repo.ensureDefaultTiles()
        assertTrue(db.tileDao().getAllTiles().isEmpty())
    }

    @Test fun lastTileRemovalDoesNotReseed() = runBlocking {
        repo.ensureDefaultTiles()
        db.tileDao().getAllTiles().forEach { repo.unpinTile(it.id) }
        repo.ensureDefaultTiles()
        assertTrue(db.tileDao().getAllTiles().isEmpty())
    }

    @Test fun freePositionDoesNotWrapToOccupiedOriginAfterRow80() {
        val occupied = (0..90).map { TileEntity("$it", "$it", size = TileSize.WIDE, gridX = 0, gridY = it * 2) }
        assertEquals(0 to 182, repo.findFirstFreeGridPosition(occupied, 4, 2))
    }

    @Test fun gridShrinkKeepsTilesInBoundsAndNonOverlapping() = runBlocking {
        val settings = repo.loadSettings().copy(showMoreTiles = true)
        repo.saveSettings(settings)
        db.tileDao().insertAll(listOf(
            TileEntity("a", "a", gridX = 4, gridY = 0),
            TileEntity("b", "b", gridX = 0, gridY = 0)))
        repo.saveSettingsWithReflow(settings.copy(showMoreTiles = false))
        val tiles = db.tileDao().getAllTiles()
        assertEquals(2, tiles.size)
        assertTrue(tiles.all { it.gridX in 0..2 })
        assertTrue(tiles[0].gridX != tiles[1].gridX || tiles[0].gridY != tiles[1].gridY)
    }

    @Test fun widgetRestoreNeverTrustsSerializedHostIds() = runBlocking {
        repo.ensureDefaultTiles()
        val backup = JSONObject(repo.exportBackupJson())
        backup.getJSONArray("tiles").getJSONObject(0)
            .put("tileType", "WIDGET").put("appWidgetId", 12345).put("widgetProvider", "example/.Provider")
        assertTrue(repo.restoreFromJson(backup.toString()))
        val tile = db.tileDao().getAllTiles().first { it.tileType == TileType.WIDGET }
        assertEquals(-1, tile.appWidgetId)
        assertEquals("example/.Provider", tile.widgetProvider)
    }
    @Test fun cloudBackupPayloadRejectsOversizedJsonInsteadOfSilentlyTruncating() {
        val small = """{"app":"LumiaLauncher","tiles":[]}"""
        assertEquals(small, validateCloudBackupJson(small))
        val tooLarge = JSONObject().put("tiles", "x".repeat(200_001)).toString()
        try {
            validateCloudBackupJson(tooLarge)
            fail("Oversized backup must not be accepted")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.contains("200 KB") == true)
        }
        try {
            validateCloudBackupJson("""{"app":""")
            fail("Malformed JSON must not be accepted")
        } catch (expected: org.json.JSONException) {
            // Reject before uploading.
        }
    }

    @Test fun manualLayoutPreservesGapsWhenAutoArrangeIsOff() = runBlocking {
        val before = listOf(
            TileEntity("a", "A", gridX = 0, gridY = 8),
            TileEntity("b", "B", gridX = 2, gridY = 12)
        )
        db.tileDao().insertAll(before)
        repo.saveSettings(repo.loadSettings().copy(autoArrangeTiles = false))
        assertEquals(before.toSet(), db.tileDao().getAllTiles().toSet())
    }

    @Test fun autoArrangeCompactsTilesWithoutLosingWidgetBindings() = runBlocking {
        db.tileDao().insertAll(listOf(
            TileEntity("a", "A", gridX = 0, gridY = 8, size = TileSize.MEDIUM),
            TileEntity("widget", "Widget", gridX = 2, gridY = 12, size = TileSize.MEDIUM,
                tileType = TileType.WIDGET, appWidgetId = 102, widgetProvider = "example/.Widget")
        ))
        repo.saveSettingsWithReflow(repo.loadSettings().copy(autoArrangeTiles = true))
        val after = db.tileDao().getAllTiles().associateBy { it.id }
        assertEquals(0, after.getValue("a").gridY)
        assertEquals(0, after.getValue("widget").gridY)
        assertEquals(2, after.getValue("widget").gridX)
        assertEquals(102, after.getValue("widget").appWidgetId)
    }

    @Test fun wallpaperAndArrangeOptionsSurviveBackupRestore() = runBlocking {
        repo.ensureDefaultTiles()
        repo.saveSettings(repo.loadSettings().copy(
            wallpaperSource = WallpaperSource.LUMIA,
            autoArrangeTiles = true
        ))
        val backup = repo.exportBackupJson()
        repo.saveSettings(repo.loadSettings().copy(
            wallpaperSource = WallpaperSource.SYSTEM,
            autoArrangeTiles = false
        ))
        assertTrue(repo.restoreFromJson(backup))
        assertEquals(WallpaperSource.LUMIA, repo.loadSettings().wallpaperSource)
        assertTrue(repo.loadSettings().autoArrangeTiles)
    }

    @Test fun existingHomeLayoutIsNotModifiedByLegacySmartFolderSeeding() = runBlocking {
        val original = TileEntity("user_tile", "My tile", packageName = "example.app",
            gridX = 2, gridY = 8, pageId = "page_home_1")
        db.tileDao().insertAll(listOf(original))

        repo.ensureDefaultTiles()
        assertEquals(listOf(original), db.tileDao().getAllTiles())
        // Repeated launcher startup must remain idempotent.
        repo.ensureDefaultTiles()
        assertEquals(listOf(original), db.tileDao().getAllTiles())
    }

    @Test fun deliberatelyEmptyInitializedHomeStaysEmptyAfterLegacyUpgrade() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("home_initialized", true).commit()

        repo.ensureDefaultTiles()
        assertTrue(db.tileDao().getAllTiles().isEmpty())
    }

    @Test fun version2MigrationPreservesTileAndAddsUnboundWidgetFields() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_data_safety.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE tiles (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, packageName TEXT NOT NULL, tileType TEXT NOT NULL, size TEXT NOT NULL, orderIndex INTEGER NOT NULL, pageId TEXT NOT NULL, gridX INTEGER NOT NULL, gridY INTEGER NOT NULL, colorOverrideHex TEXT, customIconName TEXT NOT NULL, notificationCount INTEGER NOT NULL, isLiveEnabled INTEGER NOT NULL)")
            old.execSQL("INSERT INTO tiles VALUES ('saved', 'Saved title', 'example.app', 'APP', 'MEDIUM', 0, 'page_home_1', 2, 6, NULL, '', 0, 1)")
            old.version = 2
        }
        val migrated = Room.databaseBuilder(context, LumiaDatabase::class.java, name)
            .addMigrations(LumiaDatabase.MIGRATION_2_4).allowMainThreadQueries().build()
        try {
            val tile = migrated.tileDao().getAllTiles().single()
            assertEquals("Saved title", tile.title)
            assertEquals(2, tile.gridX)
            assertEquals(6, tile.gridY)
            assertEquals(-1, tile.appWidgetId)
            assertEquals("", tile.widgetProvider)
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

}
