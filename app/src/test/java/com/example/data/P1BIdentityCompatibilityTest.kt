package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.AppFolder
import com.example.data.model.AppItem
import com.example.data.model.TileEntity
import com.example.data.model.TileType
import com.example.data.repository.LauncherIdentityPolicy
import com.example.data.repository.LauncherRepository
import com.example.ui.components.resolveLinkedAppForTile
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
class P1BIdentityCompatibilityTest {
    private lateinit var context: Context
    private lateinit var db: LumiaDatabase
    private lateinit var repo: LauncherRepository

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = LauncherRepository(context, db.tileDao(), db, releaseWidgetBinding = {})
    }

    @After fun tearDown() = db.close()

    private fun exact(component: String, serial: Long, label: String) = AppItem(
        label = label, packageName = "com.acme.demo",
        launcherComponent = "com.acme.demo/$component", launcherUserSerial = serial
    )

    @Test fun multipleActivitiesAndProfilesHaveFourDistinctTargetsWithoutLegacyFolderExpansion() {
        val apps = listOf(
            exact("com.acme.demo.Main", 0, "Main personal"),
            exact("com.acme.demo.Editor", 0, "Editor personal"),
            exact("com.acme.demo.Main", 10, "Main work"),
            exact("com.acme.demo.Editor", 10, "Editor work")
        )
        assertEquals(4, apps.map { it.drawerKey }.distinct().size)
        assertNotEquals(apps[0].drawerKey, apps[1].drawerKey)
        assertNotEquals(apps[0].drawerKey, apps[2].drawerKey)
        val oneLegacySlot = requireNotNull(LauncherIdentityPolicy.legacyFolderSlot("com.acme.demo", apps))
        assertNull(oneLegacySlot.launcherComponent)
        assertNull(oneLegacySlot.launcherUserSerial)
        assertEquals("app:com.acme.demo", oneLegacySlot.drawerKey)
    }

    @Test fun oldDrawerFolderAndOrderJsonRemainUntouchedAcrossIdentityExport() = runBlocking {
        val folder = AppFolder(id = "folder_old", title = "Original", appPackages = listOf("com.acme.demo"))
        val keys = listOf("folder:folder_old", "app:com.acme.demo")
        repo.saveSettings(repo.loadSettings().copy(
            appDrawerFolders = listOf(folder), appDrawerCustomOrder = keys
        ))
        val before = repo.loadSettings()
        val export = JSONObject(repo.exportBackupJson())
        assertEquals(7, export.getInt("version"))
        val restored = JSONObject(export.toString())
        assertTrue(repo.restoreFromJson(restored.toString()))
        val after = repo.loadSettings()
        assertEquals(before.appDrawerFolders, after.appDrawerFolders)
        assertEquals(keys, after.appDrawerCustomOrder)
    }

    @Test fun versionSevenCrossDeviceIdentityIsUnresolvedAndNeverLaunchesWrongProfile() = runBlocking {
        val app = TileEntity(id = "exact_app", title = "My Work App", packageName = "com.acme.demo",
            tileType = TileType.APP, pageId = "page_home_1", gridX = 0, gridY = 4,
            launcherComponent = "com.acme.demo/com.acme.demo.Main", launcherUserSerial = 10,
            launcherIdentityState = "EXACT")
        val legacy = TileEntity(id = "legacy_app", title = "Old", packageName = "com.acme.old",
            tileType = TileType.APP, pageId = "page_home_1", gridX = 2, gridY = 4)
        val widget = TileEntity(id = "existing_widget", title = "Real widget",
            tileType = TileType.WIDGET, appWidgetId = 213, widgetProvider = "com.acme/.Provider",
            pageId = "page_home_2", gridX = 0, gridY = 8)
        db.tileDao().insertAll(listOf(app, legacy, widget))
        val backup = repo.exportBackupJson()
        val j = JSONObject(backup)
        assertEquals(7, j.getInt("version"))
        val records = j.getJSONArray("tiles")
        val exactRecord = (0 until records.length()).map { records.getJSONObject(it) }
            .single { it.getString("id") == "exact_app" }
        assertEquals("EXACT", exactRecord.getString("launcherIdentityState"))

        // In-place data remains identical until an explicit full restore.
        assertEquals(widget, db.tileDao().getAllTiles().single { it.id == widget.id })
        assertTrue(repo.restoreFromJson(backup))
        val all = db.tileDao().getAllTiles()
        val imported = all.single { it.id == app.id }
        assertEquals("UNRESOLVED", imported.launcherIdentityState)
        assertNull(imported.launcherUserSerial)
        assertEquals(app.launcherComponent, imported.launcherComponent)
        assertEquals(app.gridX, imported.gridX)
        assertEquals(app.gridY, imported.gridY)
        assertFalse(repo.launchTile(imported))
        val restoredLegacy = all.single { it.id == legacy.id }
        assertEquals("LEGACY", restoredLegacy.launcherIdentityState)
        assertEquals(legacy.gridX, restoredLegacy.gridX)
        val importedWidget = all.single { it.id == widget.id }
        assertEquals(-1, importedWidget.appWidgetId) // Existing explicit full-restore rebind rule.
        assertEquals(widget.widgetProvider, importedWidget.widgetProvider)
        assertEquals(widget.gridX, importedWidget.gridX)
        assertEquals(widget.gridY, importedWidget.gridY)
    }

    @Test fun malformedComponentAndProfileRejectBackupBeforeJournalAndRoomWrites() = runBlocking {
        val widget = TileEntity(id = "saved_widget", title = "Keep me", tileType = TileType.WIDGET,
            appWidgetId = 99, widgetProvider = "com.acme/.Widget", pageId = "page_home_1",
            gridX = 0, gridY = 2)
        val app = TileEntity(id = "saved_app", title = "Keep app", packageName = "com.acme.demo",
            gridX = 2, gridY = 2)
        db.tileDao().insertAll(listOf(widget, app))
        val baseline = db.tileDao().getAllTiles()
        val backup = JSONObject(repo.exportBackupJson())
        val target = backup.getJSONArray("tiles").getJSONObject(0)
        target.put("launcherIdentityState", "EXACT")
        target.put("launcherComponent", "com.different/.Main")
        target.put("launcherUserSerial", 11)
        assertFalse(repo.restoreFromJson(backup.toString()))
        assertEquals(baseline, db.tileDao().getAllTiles())
        assertFalse(context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .contains("pending_restore_json"))
    }

    @Test fun allLegacyBackupVersionsOneThroughSixRemainAccepted() = runBlocking {
        db.tileDao().insertOrUpdate(TileEntity(id = "legacy", title = "Package app",
            packageName = "com.acme.demo", gridX = 0, gridY = 3))
        val original = JSONObject(repo.exportBackupJson())
        for (version in 1..6) {
            val source = JSONObject(original.toString())
            source.put("version", version)
            val items = source.getJSONArray("tiles")
            for (i in 0 until items.length()) {
                items.getJSONObject(i).remove("launcherComponent")
                items.getJSONObject(i).remove("launcherUserSerial")
                items.getJSONObject(i).remove("launcherIdentityState")
            }
            assertTrue("backup version $version", repo.restoreFromJson(source.toString()))
            val restored = db.tileDao().getAllTiles().single { it.id == "legacy" }
            assertEquals("LEGACY", restored.launcherIdentityState)
            assertNull(restored.launcherComponent)
            assertNull(restored.launcherUserSerial)
        }
    }

    @Test fun homeAppIconResolverNeverSelectsDifferentComponentOrProfile() {
        val apps = listOf(
            exact("com.acme.demo.Main", 0, "Main personal"),
            exact("com.acme.demo.Editor", 0, "Editor personal"),
            exact("com.acme.demo.Main", 10, "Main work")
        )
        val exactTile = TileEntity(id = "exact", title = "Work", packageName = "com.acme.demo",
            launcherIdentityState = "EXACT", launcherComponent = "com.acme.demo/com.acme.demo.Main",
            launcherUserSerial = 10)
        assertEquals(apps[2], resolveLinkedAppForTile(exactTile, apps))
        assertNull(resolveLinkedAppForTile(exactTile.copy(launcherUserSerial = 12), apps))
        assertNull(resolveLinkedAppForTile(exactTile.copy(
            launcherIdentityState = "UNRESOLVED", launcherUserSerial = null), apps))
        assertNull(resolveLinkedAppForTile(TileEntity(id = "legacy", title = "Old",
            packageName = "com.acme.demo"), apps))
    }

    @Test fun invalidRetargetPreservesTileIdAndEveryOtherField() = runBlocking {
        val existing = TileEntity(id = "old_id", title = "Original", packageName = "com.acme.demo",
            tileType = TileType.APP, pageId = "page_home_2", gridX = 3, gridY = 7,
            colorOverrideHex = "#334455")
        db.tileDao().insertOrUpdate(existing)
        assertFalse(repo.rebindAppTile(existing.id, exact("com.acme.demo.Main", 999999L, "Unavailable")))
        assertEquals(existing, db.tileDao().getAllTiles().single())
    }

    @Test fun missingExactProfileCannotPinOrRewriteOldWidget() = runBlocking {
        val widget = TileEntity(id = "keep_widget", title = "Existing",
            tileType = TileType.WIDGET, appWidgetId = 100, widgetProvider = "com.acme/.W",
            gridX = 0, gridY = 0)
        db.tileDao().insertOrUpdate(widget)
        repo.pinApp(exact("com.acme.demo.Main", 999999, "Unavailable"), "page_home_1")
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }
}
