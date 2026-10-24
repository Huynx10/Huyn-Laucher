package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.PageType
import com.example.data.model.TileEntity
import com.example.data.model.TileType
import com.example.data.repository.LauncherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PagePersistenceRegressionTest {
    private val mainScheduler = TestCoroutineScheduler()
    private val pageScheduler = TestCoroutineScheduler()
    private val store = ViewModelStore()
    private lateinit var db: LumiaDatabase
    private lateinit var repo: LauncherRepository
    private lateinit var vm: LauncherViewModel

    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher(mainScheduler))
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = LauncherRepository(context, db.tileDao(), db)
        vm = LauncherViewModel(repo, null, StandardTestDispatcher(pageScheduler))
        store.put("launcher", vm)
    }

    @After fun close() {
        store.clear()
        mainScheduler.runCurrent()
        pageScheduler.runCurrent()
        Dispatchers.resetMain()
        db.close()
    }

    @Test fun queuedPageEditCannotOverwriteRestoredPages() = runBlocking {
        val expected = repo.loadPages()
        val backup = repo.exportBackupJson()
        vm.renamePage(expected.first { it.pageType == PageType.HOME }.id, "Stale queued title")
        // Deliberately hold the page writer until restore has completed.
        assertTrue(vm.restoreBackup(backup))
        pageScheduler.runCurrent()
        assertEquals(expected, repo.loadPages())
        assertEquals(expected, vm.pages.value)
    }

    @Test fun rapidPageEditsPersistLatestSnapshotWithUniqueIds() {
        repeat(30) { vm.addHomePage("Page $it") }
        val added = vm.pages.value.last { it.pageType == PageType.HOME }
        vm.renamePage(added.id, "Final title")
        val expected = vm.pages.value
        pageScheduler.runCurrent()
        assertEquals(expected, repo.loadPages())
        assertEquals(expected.size, expected.map { it.id }.distinct().size)
        assertEquals(1, expected.count { it.pageType == PageType.ALL_APPS })
    }

    @Test fun resetDropsStalePageWriteAndRepairsDefaultHomeForBackup() = runBlocking {
        vm.addHomePage("Temporary default")
        val custom = vm.pages.value.last { it.pageType == PageType.HOME }
        repo.saveSettings(repo.loadSettings().copy(defaultHomePageId = custom.id))
        vm.resetDefaultsAwait()
        pageScheduler.runCurrent()
        val pages = repo.loadPages()
        assertFalse(pages.any { it.id == custom.id })
        assertTrue(pages.any { it.id == repo.loadSettings().defaultHomePageId && it.pageType == PageType.HOME })
        assertEquals(repo.loadSettings(), vm.settings.value)
        assertTrue(repo.restoreFromJson(repo.exportBackupJson()))
    }

    @Test fun deletingAllAppsOrUnknownPageDoesNotChangeLayout() {
        val before = vm.pages.value
        vm.deleteHomePage(before.first { it.pageType == PageType.ALL_APPS }.id)
        vm.deleteHomePage("missing-page")
        pageScheduler.runCurrent()
        assertEquals(before, vm.pages.value)
        assertEquals(before, repo.loadPages())
    }

    @Test fun legacyInterleavedAllAppsIsReadWithoutDroppingOrRewritingPages() {
        val original = repo.loadPages()
        val homes = original.filter { it.pageType == PageType.HOME }
        val allApps = original.single { it.pageType == PageType.ALL_APPS }
        repo.savePages(listOf(homes.first(), allApps, homes.last()))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val rawBefore = prefs.getString("launcher_pages_json", null)

        val normalized = repo.loadPages()
        assertEquals(original.map { it.id }, normalized.map { it.id })
        assertEquals(normalized.indices.toList(), normalized.map { it.orderIndex })
        assertEquals(PageType.ALL_APPS, normalized.last().pageType)
        // Normalizing a legacy read must not silently overwrite stored user data.
        assertEquals(rawBefore, prefs.getString("launcher_pages_json", null))
    }

    @Test fun movingHomeAcrossAllAppsKeepsTerminalDestinationAndPersistsIds() {
        val original = vm.pages.value
        val allAppsId = original.single { it.pageType == PageType.ALL_APPS }.id
        vm.movePage(original.lastIndex, 0)
        assertEquals(original, vm.pages.value)

        vm.movePage(0, original.lastIndex)
        vm.addHomePage("Additional Home")
        val expected = vm.pages.value
        pageScheduler.runCurrent()

        assertEquals(allAppsId, expected.last().id)
        assertEquals(PageType.ALL_APPS, expected.last().pageType)
        assertEquals(original.first { it.pageType == PageType.HOME }.id, expected[1].id)
        assertEquals(expected.size, expected.map { it.id }.distinct().size)
        assertEquals(expected, repo.loadPages())
    }

    @Test fun wrongTypePagesPreferenceCannotCrashViewModelStartupOrDeleteWidgetRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val widget = TileEntity(
            id = "existing_widget", title = "Saved widget",
            tileType = TileType.WIDGET, widgetProvider = "example/.Provider",
            appWidgetId = 127, pageId = "page_home_1"
        )
        db.tileDao().insertOrUpdate(widget)
        assertTrue(prefs.edit().putInt("launcher_pages_json", 42).commit())

        // Simulate Home/ViewModel creation with a legacy wrong-type persisted value.
        val restarted = LauncherViewModel(repo, null, StandardTestDispatcher(pageScheduler))
        store.put("launcher", restarted)
        assertEquals(repo.getDefaultPages(), restarted.pages.value)
        assertEquals(42, prefs.getInt("launcher_pages_json", -1))
        assertEquals(listOf(widget), db.tileDao().getAllTiles())
    }

    @Test fun malformedPageJsonFallsBackOnReadWithoutRewritingUserData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
        val raw = "[not-valid-json"
        assertTrue(prefs.edit().putString("launcher_pages_json", raw).commit())

        assertEquals(repo.getDefaultPages(), repo.loadPages())
        assertEquals(raw, prefs.getString("launcher_pages_json", null))
        // Even malformed input must not remove the terminal All Apps destination.
        assertEquals(PageType.ALL_APPS, repo.loadPages().last().pageType)
    }
}
