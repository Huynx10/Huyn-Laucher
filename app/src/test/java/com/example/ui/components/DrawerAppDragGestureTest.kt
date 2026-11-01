package com.example.ui.components

import androidx.compose.foundation.layout.Box
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
        content(mode, { _ -> launches++ }, {}, { false }, { pins++ })
        val tag = if (mode == AllAppsViewMode.METRO_LIST) "drawer_app_list_${app.drawerKey}"
            else "drawer_app_grid_${app.drawerKey}"
        compose.onNodeWithTag(tag).performTouchInput {
            down(center)
            advanceEventTime(700)
            up()
        }
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
}
