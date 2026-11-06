package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.AppItem
import com.example.data.model.TileEntity
import com.example.data.model.TileType
import com.example.data.repository.LauncherRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DrawerHomePinTest {
    private lateinit var context: Context
    private lateinit var db: LumiaDatabase
    private lateinit var repo: LauncherRepository
    private val app = AppItem(
        label = "Editor", packageName = "com.acme.demo",
        launcherComponent = "com.acme.demo/.Editor", launcherUserSerial = 10L
    )

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java)
            .allowMainThreadQueries().build()
        // Resolver is injected only to exercise the transaction with a known valid target.
        repo = LauncherRepository(context, db.tileDao(), db,
            exactAppTargetAvailable = { it == app }, releaseWidgetBinding = {})
    }

    @After fun tearDown() = db.close()

    @Test fun exactCellPinPreservesWidgetAndIdentityAcrossRepositoryReopenAndBackup() = runBlocking {
        val widget = TileEntity(id = "widget", title = "Clock", tileType = TileType.WIDGET,
            pageId = "page_home_2", gridX = 0, gridY = 0,
            appWidgetId = 211, widgetProvider = "com.acme/.Clock")
        db.tileDao().insertOrUpdate(widget)
        repo.saveSettings(repo.loadSettings().copy(autoArrangeTiles = true))

        assertTrue(repo.pinAppAtGridPosition(app, "page_home_2", 2, 3, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", 2, 3, 4, false, 6))
        val reopened = LauncherRepository(context, db.tileDao(), db,
            exactAppTargetAvailable = { it == app }, releaseWidgetBinding = {})
        val rows = db.tileDao().getAllTiles()
        assertEquals(2, rows.size)
        assertEquals(widget, rows.single { it.id == "widget" })
        val pinned = rows.single { it.id != "widget" }
        assertEquals("page_home_2", pinned.pageId)
        assertEquals(2, pinned.gridX)
        assertEquals(3, pinned.gridY)
        assertEquals(app.launcherComponent, pinned.launcherComponent)
        assertEquals(app.launcherUserSerial, pinned.launcherUserSerial)
        assertEquals("EXACT", pinned.launcherIdentityState)
        assertFalse(reopened.pinAppAtGridPosition(app, "page_home_2", 2, 3, 4, false, 6))
        val backup = JSONObject(repo.exportBackupJson())
        val records = backup.getJSONArray("tiles")
        val record = (0 until records.length()).map { records.getJSONObject(it) }
            .single { it.getString("id") == pinned.id }
        assertEquals(app.launcherComponent, record.getString("launcherComponent"))
        assertEquals(10L, record.getLong("launcherUserSerial"))
    }

    @Test fun invalidDropAndLockedLayoutNeverChangeRowsOrFolders() = runBlocking {
        val old = TileEntity(id = "old", title = "Old", pageId = "page_home_2", gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(old)
        val beforeSettings = repo.loadSettings()
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", 0, 0, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app, "page_all_apps", 2, 3, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app, "missing", 2, 3, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", -1, 3, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", 3, 3, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", 2, 61, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", 2, 3, 5, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app.copy(launcherUserSerial = null), "page_home_2", 2, 3, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app.copy(launcherComponent = null), "page_home_2", 2, 3, 4, false, 6))
        assertFalse(repo.pinAppAtGridPosition(app.copy(launcherComponent = "other/.Main"), "page_home_2", 2, 3, 4, false, 6))
        repo.saveSettings(beforeSettings.copy(lockHomeLayout = true))
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", 2, 3, 4, false, 6))
        assertEquals(listOf(old), db.tileDao().getAllTiles())
        assertEquals(beforeSettings.appDrawerFolders, repo.loadSettings().appDrawerFolders)
        assertEquals(beforeSettings.appDrawerCustomOrder, repo.loadSettings().appDrawerCustomOrder)
    }

    @Test fun collisionRetryNeverReplacesExistingTile() = runBlocking {
        val original = TileEntity(id = "taken", title = "Keep", pageId = "page_home_1",
            gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(original)
        val candidates = java.util.ArrayDeque(listOf("taken", "new_unique"))
        val guarded = LauncherRepository(context, db.tileDao(), db,
            newTileIdCandidate = { candidates.removeFirst() },
            exactAppTargetAvailable = { true }, releaseWidgetBinding = {})
        assertTrue(guarded.pinAppAtGridPosition(app, "page_home_2", 2, 3, 4, false, 6))
        assertEquals(original, db.tileDao().getAllTiles().single { it.id == "taken" })
        assertEquals("new_unique", db.tileDao().getAllTiles().single { it.id == "new_unique" }.id)
    }

    @Test fun layoutChangedAfterGeometryMeasurementRejectsStaleDrop() = runBlocking {
        val before = repo.loadSettings()
        repo.saveSettings(before.copy(showMoreTiles = true))
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", 2, 3, 4, false, 6))
        repo.saveSettings(before.copy(tileSpacingDp = 8))
        assertFalse(repo.pinAppAtGridPosition(app, "page_home_2", 2, 3, 4, false, 6))
        assertTrue(db.tileDao().getAllTiles().isEmpty())
    }

    @Test fun failedRoomInsertLeavesExistingWidgetAndSettingsUntouched() = runBlocking {
        val widget = TileEntity(id = "bound", title = "Widget", tileType = TileType.WIDGET,
            pageId = "page_home_1", gridX = 0, gridY = 0,
            appWidgetId = 432, widgetProvider = "com.acme/.Widget")
        db.tileDao().insertOrUpdate(widget)
        val settings = repo.loadSettings()
        db.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER reject_drawer_drop BEFORE INSERT ON tiles
                WHEN NEW.packageName = 'com.acme.demo'
                BEGIN SELECT RAISE(ABORT, 'injected failure'); END"""
        )
        try {
            repo.pinAppAtGridPosition(app, "page_home_2", 2, 3, 4, false, 6)
            fail("Expected Room insert failure")
        } catch (_: android.database.sqlite.SQLiteException) {
            // The transaction rolls back; the ViewModel reports the failure to UI.
        }
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
        assertEquals(settings.appDrawerFolders, repo.loadSettings().appDrawerFolders)
        assertEquals(settings.appDrawerCustomOrder, repo.loadSettings().appDrawerCustomOrder)
    }
}
