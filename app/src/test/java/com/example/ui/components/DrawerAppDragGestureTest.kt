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
    @Test fun listRootPointerReleasePersistsOneExactHomeTileWithoutReplacingWidget() {
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
            allAppsViewMode = AllAppsViewMode.METRO_LIST,
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
        // Only the catalog is controlled; pointer handling and persistence are real.
        compose.runOnIdle {
            val field = LauncherViewModel::class.java.getDeclaredField("_apps")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val appsFlow = field.get(viewModel) as MutableStateFlow<List<AppItem>>
            appsFlow.value = listOf(app)
        }

        val pager = compose.onNodeWithTag("launcher_horizontal_pager")
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        val itemTag = "drawer_app_list_${app.drawerKey}"
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

}
