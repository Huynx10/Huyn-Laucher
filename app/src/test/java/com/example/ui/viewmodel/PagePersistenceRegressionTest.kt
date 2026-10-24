package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiaDatabase
import com.example.data.model.PageType
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
}
