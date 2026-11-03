package com.example.ui.components

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.ui.viewmodel.LauncherViewModel
import com.example.data.repository.LauncherRepository
import com.example.data.model.TileType
import com.example.data.model.TileEntity
import com.example.data.db.LumiaDatabase
import com.example.LumiaLauncherApp
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.platform.LocalDensity
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.example.data.model.AllAppsViewMode
import com.example.data.model.AppItem
import com.example.data.model.LauncherPage
import com.example.data.model.LauncherSettings
import com.example.data.model.PageType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DrawerAppDragGestureTest {
    @get:Rule val compose = createComposeRule()
    private val app = AppItem(label = "Editor", packageName = "com.acme.demo",
        launcherComponent = "com.acme.demo/.Editor", launcherUserSerial = 0L)
    private val pages = listOf(LauncherPage("page_home_1", "Home", PageType.HOME, 0))

    private fun content(mode: AllAppsViewMode, onLaunch: (AppItem) -> Unit,
                        onStart: () -> Unit, onEnd: () -> Boolean,
                        onPin: () -> Unit = {}) {
        val settings = LauncherSettings(allAppsViewMode = mode, useRealAppIcons = false)
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                AppDrawer(
                    apps = listOf(app), homePages = pages, settings = settings,
                    onLaunchApp = { onLaunch(it) }, onPinAppToPage = { _, _ -> onPin() },
                    onMutateSettings = { transform -> transform(settings) },
                    onAppDragStart = { _, _ -> onStart() },
                    onAppDragEnd = { onEnd() }
                )
            }
        }
    }

    private fun tapLaunchesOnce(mode: AllAppsViewMode) {
            var launches = 0
            var starts = 0
            var launched: AppItem? = null
            content(mode, { launches++; launched = it }, { starts++ }, { false })
            val tag = if (mode == AllAppsViewMode.METRO_LIST) "drawer_app_list_${app.drawerKey}"
                else "drawer_app_grid_${app.drawerKey}"
            compose.onNodeWithTag(tag).performTouchInput { down(center); up() }
            compose.runOnIdle {
                assertEquals(1, launches)
                assertEquals(app, launched)
                assertEquals(0, starts)
            }
    }

    @Test fun listTapLaunchesOnce() = tapLaunchesOnce(AllAppsViewMode.METRO_LIST)
    @Test fun gridTapLaunchesOnce() = tapLaunchesOnce(AllAppsViewMode.ONE_UI_GRID)

    private fun moveBeforeHoldNeverStartsHomeDrag(mode: AllAppsViewMode) {
        var starts = 0
        var pins = 0
        content(mode, { _ -> }, { starts++ }, { false }, { pins++ })
        val tag = if (mode == AllAppsViewMode.METRO_LIST) "drawer_app_list_${app.drawerKey}"
            else "drawer_app_grid_${app.drawerKey}"
        compose.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveTo(center + Offset(0f, 90f))
            up()
        }
        compose.runOnIdle { assertEquals(0, starts); assertEquals(0, pins) }
    }

    @Test fun listMoveBeforeHoldDoesNotPin() = moveBeforeHoldNeverStartsHomeDrag(AllAppsViewMode.METRO_LIST)
    @Test fun gridMoveBeforeHoldDoesNotPin() = moveBeforeHoldNeverStartsHomeDrag(AllAppsViewMode.ONE_UI_GRID)

    private fun holdReleaseOpensOneMenuWithoutPin(mode: AllAppsViewMode) {
        var pins = 0
        var launches = 0
        var starts = 0
        content(mode, { _ -> launches++ }, { starts++ }, { false }, { pins++ })
        val tag = if (mode == AllAppsViewMode.METRO_LIST) "drawer_app_list_${app.drawerKey}"
            else "drawer_app_grid_${app.drawerKey}"
        val item = compose.onNodeWithTag(tag)
        // performTouchInput batches every event in one block. Let Compose's gesture
        // timeout run while the pointer is still down before injecting UP.
        item.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { assertEquals(1, starts) }
        item.performTouchInput { up() }
        compose.onAllNodesWithTag("drawer_app_actions").assertCountEquals(1)
        compose.runOnIdle { assertEquals(0, pins); assertEquals(0, launches) }
    }

    @Test fun listHoldReleaseOpensOneMenu() = holdReleaseOpensOneMenuWithoutPin(AllAppsViewMode.METRO_LIST)
    @Test fun gridHoldReleaseOpensOneMenu() = holdReleaseOpensOneMenuWithoutPin(AllAppsViewMode.ONE_UI_GRID)

    private fun parentConsumedDragDoesNotLaunch(mode: AllAppsViewMode) {
            var launches = 0
            var starts = 0
            var ends = 0
            content(mode, { _ -> launches++ }, { starts++ }, { ends++; true })
            val tag = if (mode == AllAppsViewMode.METRO_LIST) "drawer_app_list_${app.drawerKey}"
                else "drawer_app_grid_${app.drawerKey}"
            compose.onNodeWithTag(tag).performTouchInput {
                down(center)
                advanceEventTime(700)
                moveTo(center + Offset(30f, 0f))
                moveTo(center + Offset(60f, 0f))
                up()
            }
            compose.runOnIdle {
                assertEquals(1, starts)
                assertEquals(1, ends)
                assertEquals(0, launches)
            }
    }

    @Test fun listParentConsumedDragDoesNotLaunch() =
        parentConsumedDragDoesNotLaunch(AllAppsViewMode.METRO_LIST)

    @Test fun gridParentConsumedDragDoesNotLaunch() =
        parentConsumedDragDoesNotLaunch(AllAppsViewMode.ONE_UI_GRID)

    /**
     * G06: exercise the actual List/Grid pointer recognizer, BackHandler and
     * Lazy item disposal. This observes callbacks, not Room or device hardware.
     */
    private fun backDisposesLazySourceAndLateUpCannotPin(mode: AllAppsViewMode) {
        val visible = mutableStateOf(true)
        val settings = LauncherSettings(allAppsViewMode = mode, useRealAppIcons = false)
        var backDispatcher: OnBackPressedDispatcher? = null
        var starts = 0
        var aborts = 0
        var recognizerCancels = 0
        var homeReleaseCallbacks = 0
        var menuPins = 0
        var launches = 0
        compose.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            Box(Modifier.fillMaxSize().testTag("drawer_drag_host")) {
                AppDrawer(
                    apps = if (visible.value) listOf(app) else emptyList(),
                    homePages = pages, settings = settings,
                    onLaunchApp = { launches++ },
                    onPinAppToPage = { _, _ -> menuPins++ },
                    onMutateSettings = { transform -> transform(settings) },
                    onAppDragStart = { _, _ -> starts++ },
                    onAppDragEnd = { homeReleaseCallbacks++; true },
                    onAppDragAbort = { aborts++; visible.value = false },
                    onAppDragRecognizerCancel = { recognizerCancels++ }
                )
            }
        }
        val tag = if (mode == AllAppsViewMode.METRO_LIST)
            "drawer_app_list_${app.drawerKey}" else "drawer_app_grid_${app.drawerKey}"
        val host = compose.onNodeWithTag("drawer_drag_host")
        fun itemPoint(): Offset {
            val itemBounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            val hostBounds = host.fetchSemanticsNode().boundsInRoot
            return itemBounds.center - hostBounds.topLeft
        }

        // One physical DOWN, followed by long press. Back disposes the Lazy item
        // before UP; an old source callback must not be treated as a valid drop.
        host.performTouchInput { down(itemPoint()) }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle {
            assertEquals(1, starts)
            requireNotNull(backDispatcher).onBackPressed()
        }
        compose.onAllNodesWithTag(tag).assertCountEquals(0)
        host.performTouchInput { up() }
        compose.runOnIdle {
            assertEquals(1, aborts)
            assertEquals(0, homeReleaseCallbacks)
            assertEquals(0, menuPins)
            assertEquals(0, launches)
        }
        compose.onAllNodesWithTag("drawer_app_actions").assertCountEquals(0)

        // A genuinely new DOWN after the canceled gesture can still finish.
        compose.runOnIdle { visible.value = true }
        val nextPoint = itemPoint()
        host.performTouchInput { down(nextPoint) }
        compose.mainClock.advanceTimeBy(700)
        host.performTouchInput { moveTo(nextPoint + Offset(60f, 0f)) }
        host.performTouchInput { up() }
        compose.runOnIdle {
            assertEquals(2, starts)
            assertEquals(1, homeReleaseCallbacks)
            assertEquals(0, menuPins)
            assertEquals(0, launches)
            // The disposed recognizer is allowed to report cancel, never release.
            assertEquals(true, recognizerCancels >= 1)
        }
    }

    @Test fun listBackDisposesLazySourceAndLateUpCannotPin() =
        backDisposesLazySourceAndLateUpCannotPin(AllAppsViewMode.METRO_LIST)

    @Test fun gridBackDisposesLazySourceAndLateUpCannotPin() =
        backDisposesLazySourceAndLateUpCannotPin(AllAppsViewMode.ONE_UI_GRID)

    /**
     * G05 root-to-Room integration: real launcher pager/root pointer observer,
     * AppDrawer recognizer, Home geometry, ViewModel and in-memory Room.
     * No fake onAppDragEnd callback. The catalog fixture is injected into the
     * ViewModel StateFlow because Robolectric has no installed work-profile app.
     */
    private fun rootPointerReleasePersistsOneExactHomeTileWithoutReplacingWidget(mode: AllAppsViewMode) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("lumia_first_run", Context.MODE_PRIVATE)
            .edit().putBoolean("guided_v1", true).commit()
        context.getSharedPreferences("lumia_default_widget_setup", Context.MODE_PRIVATE)
            .edit().putBoolean("offered_v1", true).commit()
        val db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java)
            .allowMainThreadQueries().build()
        val widget = TileEntity(
            id = "existing_widget", title = "Keep widget", tileType = TileType.WIDGET,
            pageId = "page_home_1", gridX = 3, gridY = 8,
            appWidgetId = 432, widgetProvider = "com.acme/.Clock"
        )
        runBlocking { db.tileDao().insertOrUpdate(widget) }
        val repo = LauncherRepository(
            context, db.tileDao(), db,
            exactAppTargetAvailable = { it == app },
            releaseWidgetBinding = {}
        )
        repo.savePages(listOf(
            LauncherPage("page_home_1", "Home", PageType.HOME, 0),
            LauncherPage("page_all_apps", "All Apps", PageType.ALL_APPS, 1)
        ))
        repo.saveSettings(repo.loadSettings().copy(
            defaultHomePageId = "page_home_1",
            allAppsViewMode = mode,
            allAppsCategorizedEnabled = false,
            allAppsGroupByCategory = false,
            useRealAppIcons = false
        ))
        val viewModel = LauncherViewModel(repo, null)
        var density = 1f
        compose.setContent {
            density = LocalDensity.current.density
            LumiaLauncherApp(viewModel)
        }
        compose.waitForIdle()
        val pager = compose.onNodeWithTag("launcher_horizontal_pager")
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        // Publish the controlled catalog after pager navigation: the ViewModel's
        // asynchronous startup app scan can otherwise overwrite an early injection.
        // Only the catalog is controlled; pointer handling and persistence are real.
        compose.runOnIdle {
            val field = LauncherViewModel::class.java.getDeclaredField("_apps")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val appsFlow = field.get(viewModel) as MutableStateFlow<List<AppItem>>
            appsFlow.value = listOf(app)
        }

        val itemTag = if (mode == AllAppsViewMode.METRO_LIST) "drawer_app_list_${app.drawerKey}"
            else "drawer_app_grid_${app.drawerKey}"
        if (mode == AllAppsViewMode.ONE_UI_GRID) {
            // Assert the real Grid recognizer/source, not a synthetic callback.
            compose.onNodeWithTag("app_drawer_grid").assertExists()
        }
        compose.onNodeWithTag(itemTag).assertIsDisplayed()
        val pagerBounds = pager.fetchSemanticsNode().boundsInRoot
        val start = compose.onNodeWithTag(itemTag).fetchSemanticsNode().boundsInRoot.center -
            pagerBounds.topLeft

        pager.performTouchInput { down(start) }
        compose.mainClock.advanceTimeBy(700)
        pager.performTouchInput { moveTo(Offset(2f, start.y)) }
        // Dwell at the left edge until the pager navigates from All Apps to Home.
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        val home = compose.onNodeWithTag("tiles_grid_page_home_1")
        home.assertIsDisplayed()
        if (mode == AllAppsViewMode.ONE_UI_GRID) {
            // The Grid item is still composed and the extra label is the root
            // drag preview; the real root session survived edge navigation.
            compose.onNodeWithTag(itemTag).assertExists()
            compose.onAllNodesWithText(app.label, useUnmergedTree = true).assertCountEquals(2)
        }
        val homeBounds = home.fetchSemanticsNode().boundsInRoot
        val stepPx = (homeBounds.width - 24f * density +
            repo.loadSettings().tileSpacingDp * density) / 4f
        // The release finger is centered over the first 2-column Tile footprint.
        val homeFinger = Offset(
            homeBounds.left + 12f * density + stepPx / 2f,
            homeBounds.top + 10f * density + stepPx / 2f
        ) - pagerBounds.topLeft
        pager.performTouchInput { moveTo(homeFinger); up() }
        compose.waitUntil(10_000) {
            runBlocking { db.tileDao().getAllTiles().size == 2 }
        }
        // Give any late Lazy-source completion an opportunity to run.
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle {
            val rows = runBlocking { db.tileDao().getAllTiles() }
            assertEquals(2, rows.size)
            assertEquals(widget, rows.single { it.id == widget.id })
            val pinned = rows.single { it.id != widget.id }
            assertEquals(TileType.APP, pinned.tileType)
            assertEquals("page_home_1", pinned.pageId)
            assertEquals(0, pinned.gridX)
            assertEquals(0, pinned.gridY)
            assertEquals(app.launcherComponent, pinned.launcherComponent)
            assertEquals(app.launcherUserSerial, pinned.launcherUserSerial)
        }
        db.close()
    }


    @Test fun listRootPointerReleasePersistsOneExactHomeTileWithoutReplacingWidget() =
        rootPointerReleasePersistsOneExactHomeTileWithoutReplacingWidget(AllAppsViewMode.METRO_LIST)

    /** G05 Grid valid release: root Grid pointer → Home geometry → ViewModel → Room. */
    @Test fun gridRootPointerReleasePersistsOneExactHomeTileWithoutReplacingWidget() =
        rootPointerReleasePersistsOneExactHomeTileWithoutReplacingWidget(AllAppsViewMode.ONE_UI_GRID)

    /**
     * G06 root-to-Room negative path. Keep the physical pointer DOWN while the
     * Lazy List source disappears on edge dwell, press Back on the real launcher,
     * then deliver the stale MOVE/UP over a valid empty Home cell. The root
     * observer must not pin anything after Back canceled its drag session.
     */
    @Test fun listRootBackAfterLazyDisposalLateUpLeavesRoomUnchanged() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("lumia_first_run", Context.MODE_PRIVATE)
            .edit().putBoolean("guided_v1", true).commit()
        context.getSharedPreferences("lumia_default_widget_setup", Context.MODE_PRIVATE)
            .edit().putBoolean("offered_v1", true).commit()
        val db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java)
            .allowMainThreadQueries().build()
        val widget = TileEntity(
            id = "cancel_keeps_widget", title = "Keep widget", tileType = TileType.WIDGET,
            pageId = "page_home_1", gridX = 3, gridY = 8,
            appWidgetId = 432, widgetProvider = "com.acme/.Clock"
        )
        runBlocking { db.tileDao().insertOrUpdate(widget) }
        val repo = LauncherRepository(
            context, db.tileDao(), db,
            exactAppTargetAvailable = { it == app },
            releaseWidgetBinding = {}
        )
        repo.savePages(listOf(
            LauncherPage("page_home_1", "Home", PageType.HOME, 0),
            LauncherPage("page_all_apps", "All Apps", PageType.ALL_APPS, 1)
        ))
        repo.saveSettings(repo.loadSettings().copy(
            defaultHomePageId = "page_home_1",
            allAppsViewMode = AllAppsViewMode.METRO_LIST,
            allAppsCategorizedEnabled = false,
            allAppsGroupByCategory = false,
            useRealAppIcons = false
        ))
        val viewModel = LauncherViewModel(repo, null)
        var backDispatcher: OnBackPressedDispatcher? = null
        var density = 1f
        compose.setContent {
            density = LocalDensity.current.density
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            LumiaLauncherApp(viewModel)
        }
        compose.waitForIdle()
        val pager = compose.onNodeWithTag("launcher_horizontal_pager")
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        // Inject after pager navigation, matching the stable G05/List Back-only
        // fixtures; startup app scanning may overwrite an earlier catalog value.
        lateinit var appsFlow: MutableStateFlow<List<AppItem>>
        compose.runOnIdle {
            val field = LauncherViewModel::class.java.getDeclaredField("_apps")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            appsFlow = field.get(viewModel) as MutableStateFlow<List<AppItem>>
            appsFlow.value = listOf(app)
        }
        val itemTag = "drawer_app_list_${app.drawerKey}"
        compose.onNodeWithTag(itemTag).assertIsDisplayed()
        val pagerBounds = pager.fetchSemanticsNode().boundsInRoot
        val start = compose.onNodeWithTag(itemTag).fetchSemanticsNode().boundsInRoot.center -
            pagerBounds.topLeft

        pager.performTouchInput { down(start) }
        compose.mainClock.advanceTimeBy(700)
        pager.performTouchInput { moveTo(Offset(2f, start.y)) }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        val home = compose.onNodeWithTag("tiles_grid_page_home_1")
        home.assertIsDisplayed()
        // HorizontalPager retains its adjacent All Apps page (beyondViewport=1),
        // so edge dwell alone does not dispose the Lazy source in this two-page
        // fixture. Simulate a catalog removal while the physical pointer is
        // still DOWN: the actual Lazy item must leave composition before Back.
        compose.runOnIdle { appsFlow.value = emptyList() }
        compose.onAllNodesWithTag(itemTag).assertCountEquals(0)
        val homeBounds = home.fetchSemanticsNode().boundsInRoot
        val stepPx = (homeBounds.width - 24f * density +
            repo.loadSettings().tileSpacingDp * density) / 4f
        val validEmptyCell = Offset(
            homeBounds.left + 12f * density + stepPx / 2f,
            homeBounds.top + 10f * density + stepPx / 2f
        ) - pagerBounds.topLeft
        // Back must cancel the root-owned drag even though the source is gone.
        compose.runOnIdle { requireNotNull(backDispatcher).onBackPressed() }
        assertEquals(listOf(widget), runBlocking { db.tileDao().getAllTiles() })
        // Deliver the old pointer's late release at the same valid cell used
        // by the positive G05 test; it must not commit a Room APP row.
        pager.performTouchInput { moveTo(validEmptyCell); up() }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        compose.runOnIdle {
            val rows = runBlocking { db.tileDao().getAllTiles() }
            assertEquals(listOf(widget), rows)
            assertEquals(0, rows.count { it.tileType == TileType.APP })
        }
        db.close()
    }


    /**
     * G06 Back-only: unlike the catalog-removal fixture, the actual List/Grid
     * source stays composed throughout. The launcher drag preview is a visible
     * witness that the root still owns the drag when Back is dispatched.
     * Back must remove that preview before a stale physical MOVE/UP can
     * reach the otherwise valid empty Home cell.
     */
    private fun rootBackOnlyWhileSourcePresentRejectsLateUpAndKeepsRoomRows(mode: AllAppsViewMode) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("lumia_first_run", Context.MODE_PRIVATE)
            .edit().putBoolean("guided_v1", true).commit()
        context.getSharedPreferences("lumia_default_widget_setup", Context.MODE_PRIVATE)
            .edit().putBoolean("offered_v1", true).commit()
        val db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java)
            .allowMainThreadQueries().build()
        val widget = TileEntity(
            id = "back_only_keeps_widget", title = "Keep widget", tileType = TileType.WIDGET,
            pageId = "page_home_1", gridX = 3, gridY = 8,
            appWidgetId = 432, widgetProvider = "com.acme/.Clock"
        )
        runBlocking { db.tileDao().insertOrUpdate(widget) }
        val repo = LauncherRepository(
            context, db.tileDao(), db,
            exactAppTargetAvailable = { it == app },
            releaseWidgetBinding = {}
        )
        repo.savePages(listOf(
            LauncherPage("page_home_1", "Home", PageType.HOME, 0),
            LauncherPage("page_all_apps", "All Apps", PageType.ALL_APPS, 1)
        ))
        repo.saveSettings(repo.loadSettings().copy(
            defaultHomePageId = "page_home_1",
            allAppsViewMode = mode,
            allAppsCategorizedEnabled = false,
            allAppsGroupByCategory = false,
            useRealAppIcons = false
        ))
        val viewModel = LauncherViewModel(repo, null)
        var backDispatcher: OnBackPressedDispatcher? = null
        var density = 1f
        compose.setContent {
            density = LocalDensity.current.density
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            LumiaLauncherApp(viewModel)
        }
        compose.waitForIdle()
        val pager = compose.onNodeWithTag("launcher_horizontal_pager")
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        lateinit var appsFlow: MutableStateFlow<List<AppItem>>
        compose.runOnIdle {
            val field = LauncherViewModel::class.java.getDeclaredField("_apps")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            appsFlow = field.get(viewModel) as MutableStateFlow<List<AppItem>>
            appsFlow.value = listOf(app)
        }
        val itemTag = if (mode == AllAppsViewMode.METRO_LIST) "drawer_app_list_${app.drawerKey}"
            else "drawer_app_grid_${app.drawerKey}"
        if (mode == AllAppsViewMode.ONE_UI_GRID) {
            compose.onNodeWithTag("app_drawer_grid").assertExists()
        }
        compose.onNodeWithTag(itemTag).assertIsDisplayed()
        val pagerBounds = pager.fetchSemanticsNode().boundsInRoot
        val start = compose.onNodeWithTag(itemTag).fetchSemanticsNode().boundsInRoot.center -
            pagerBounds.topLeft

        pager.performTouchInput { down(start) }
        compose.mainClock.advanceTimeBy(700)
        pager.performTouchInput { moveTo(Offset(2f, start.y)) }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        val home = compose.onNodeWithTag("tiles_grid_page_home_1")
        home.assertIsDisplayed()
        val homeBounds = home.fetchSemanticsNode().boundsInRoot
        val stepPx = (homeBounds.width - 24f * density +
            repo.loadSettings().tileSpacingDp * density) / 4f
        val validEmptyCell = Offset(
            homeBounds.left + 12f * density + stepPx / 2f,
            homeBounds.top + 10f * density + stepPx / 2f
        ) - pagerBounds.topLeft

        // The List/Grid item must still exist, and the extra "Editor" Text is the
        // actual launcher-root drag preview (MainActivity), not a test stub.
        compose.onNodeWithTag(itemTag).assertExists()
        compose.runOnIdle { assertEquals(listOf(app), appsFlow.value) }
        compose.onAllNodesWithText(app.label, useUnmergedTree = true).assertCountEquals(2)
        assertEquals(listOf(widget), runBlocking { db.tileDao().getAllTiles() })

        // Real Back dispatch while pointer is DOWN. The preview's disappearance
        // proves that Back canceled the active root session, not catalog removal.
        compose.runOnIdle { requireNotNull(backDispatcher).onBackPressed() }
        compose.onNodeWithTag(itemTag).assertExists()
        compose.onAllNodesWithText(app.label, useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle { assertEquals(listOf(app), appsFlow.value) }
        assertEquals(listOf(widget), runBlocking { db.tileDao().getAllTiles() })

        pager.performTouchInput { moveTo(validEmptyCell); up() }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        compose.onNodeWithTag(itemTag).assertExists()
        compose.runOnIdle {
            assertEquals(listOf(app), appsFlow.value)
            val rows = runBlocking { db.tileDao().getAllTiles() }
            assertEquals(listOf(widget), rows)
            assertEquals(0, rows.count { it.tileType == TileType.APP })
        }
        db.close()
    }

    @Test fun listRootBackOnlyWhileSourcePresentRejectsLateUpAndKeepsRoomRows() =
        rootBackOnlyWhileSourcePresentRejectsLateUpAndKeepsRoomRows(AllAppsViewMode.METRO_LIST)

    @Test fun gridRootBackOnlyWhileSourcePresentRejectsLateUpAndKeepsRoomRows() =
        rootBackOnlyWhileSourcePresentRejectsLateUpAndKeepsRoomRows(AllAppsViewMode.ONE_UI_GRID)


    /**
     * G06 negative root-to-Room matrix. Unlike repository-only collision tests,
     * these drive the actual List/Grid recognizer and root pointer observer.
     * A locked layout rejects drag initiation; an occupied HOME footprint
     * rejects the completed release after the edge transition.
     */
    private fun rootInvalidDropKeepsRowsAndPageOrder(mode: AllAppsViewMode, locked: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lumia_launcher_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("lumia_first_run", Context.MODE_PRIVATE)
            .edit().putBoolean("guided_v1", true).commit()
        context.getSharedPreferences("lumia_default_widget_setup", Context.MODE_PRIVATE)
            .edit().putBoolean("offered_v1", true).commit()
        val db = Room.inMemoryDatabaseBuilder(context, LumiaDatabase::class.java)
            .allowMainThreadQueries().build()
        val widget = TileEntity(
            id = "occupied_widget", title = "Occupied", tileType = TileType.WIDGET,
            pageId = "page_home_1", gridX = 0, gridY = 0,
            appWidgetId = 432, widgetProvider = "com.acme/.Clock"
        )
        runBlocking { db.tileDao().insertOrUpdate(widget) }
        val repo = LauncherRepository(
            context, db.tileDao(), db,
            exactAppTargetAvailable = { it == app },
            releaseWidgetBinding = {}
        )
        val originalPages = listOf(
            LauncherPage("page_home_1", "Home", PageType.HOME, 0),
            LauncherPage("page_all_apps", "All Apps", PageType.ALL_APPS, 1)
        )
        repo.savePages(originalPages)
        repo.saveSettings(repo.loadSettings().copy(
            defaultHomePageId = "page_home_1",
            allAppsViewMode = mode,
            allAppsCategorizedEnabled = false,
            allAppsGroupByCategory = false,
            lockHomeLayout = locked,
            useRealAppIcons = false
        ))
        val viewModel = LauncherViewModel(repo, null)
        var density = 1f
        compose.setContent {
            density = LocalDensity.current.density
            LumiaLauncherApp(viewModel)
        }
        compose.waitForIdle()
        val pager = compose.onNodeWithTag("launcher_horizontal_pager")
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle {
            val field = LauncherViewModel::class.java.getDeclaredField("_apps")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val appsFlow = field.get(viewModel) as MutableStateFlow<List<AppItem>>
            appsFlow.value = listOf(app)
        }
        val tag = if (mode == AllAppsViewMode.METRO_LIST)
            "drawer_app_list_${app.drawerKey}" else "drawer_app_grid_${app.drawerKey}"
        if (mode == AllAppsViewMode.ONE_UI_GRID) {
            compose.onNodeWithTag("app_drawer_grid").assertExists()
        }
        compose.onNodeWithTag(tag).assertIsDisplayed()
        val pagerBounds = pager.fetchSemanticsNode().boundsInRoot
        val start = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center -
            pagerBounds.topLeft
        pager.performTouchInput { down(start) }
        compose.mainClock.advanceTimeBy(700)
        pager.performTouchInput { moveTo(Offset(2f, start.y)) }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()

        if (locked) {
            // Layout lock prevents even the edge transition while the pointer is down.
            compose.onNodeWithTag(tag).assertExists()
            compose.onAllNodesWithText(app.label, useUnmergedTree = true).assertCountEquals(1)
            pager.performTouchInput { up() }
        } else {
            val home = compose.onNodeWithTag("tiles_grid_page_home_1")
            home.assertIsDisplayed()
            compose.onAllNodesWithText(app.label, useUnmergedTree = true).assertCountEquals(2)
            val bounds = home.fetchSemanticsNode().boundsInRoot
            val stepPx = (bounds.width - 24f * density +
                repo.loadSettings().tileSpacingDp * density) / 4f
            val occupiedCell = Offset(
                bounds.left + 12f * density + stepPx / 2f,
                bounds.top + 10f * density + stepPx / 2f
            ) - pagerBounds.topLeft
            pager.performTouchInput { moveTo(occupiedCell); up() }
        }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf(widget), runBlocking { db.tileDao().getAllTiles() })
            assertEquals(0, runBlocking { db.tileDao().getAllTiles() }.count {
                it.tileType == TileType.APP
            })
            assertEquals(originalPages, repo.loadPages())
        }
        db.close()
    }

    @Test fun listOccupiedHomeCellDoesNotPersistApp() =
        rootInvalidDropKeepsRowsAndPageOrder(AllAppsViewMode.METRO_LIST, false)

    @Test fun gridOccupiedHomeCellDoesNotPersistApp() =
        rootInvalidDropKeepsRowsAndPageOrder(AllAppsViewMode.ONE_UI_GRID, false)

    @Test fun listLockedLayoutDoesNotPersistApp() =
        rootInvalidDropKeepsRowsAndPageOrder(AllAppsViewMode.METRO_LIST, true)

    @Test fun gridLockedLayoutDoesNotPersistApp() =
        rootInvalidDropKeepsRowsAndPageOrder(AllAppsViewMode.ONE_UI_GRID, true)

}
