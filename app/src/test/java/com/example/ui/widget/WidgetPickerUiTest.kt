package com.example.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.ui.components.LumiaStartGrid
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class WidgetPickerUiTest {
    @get:Rule val compose = createComposeRule()
    private val providers = (1..224).map {
        WidgetProviderItem(
            providerComponent = "example.widgets/.Widget$it", packageName = "example.widgets",
            className = ".Widget$it", label = "Widget $it", appName = "Widgets"
        )
    }
    private var picked: WidgetProviderItem? = null
    private var pickedPage: String? = null

    private fun showPicker() {
        val page = LauncherPage("page_home_1", "Home")
        compose.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    LumiaStartGrid(
                        currentPage = page, allHomePages = listOf(page), tiles = emptyList(),
                        settings = LauncherSettings(), telemetry = SystemTelemetry(),
                        isEditMode = false, selectedTileId = null, showAddTileSheet = true,
                        onDismissAddTileSheet = {}, onOpenAddTileSheet = {}, onTileClick = {},
                        onSelectTileForEdit = {}, onResizeTile = {}, onUnpinTile = {},
                        onMoveTile = { _, _ -> }, onMoveTileToGrid = { _, _, _, _ -> },
                        onNudgeTile = { _, _, _, _ -> }, onReorderTiles = { _, _ -> },
                        onMoveTileToPage = { _, _ -> }, onOpenWidgetSettings = {},
                        onPinNewTile = {}, onEnterEditMode = {}, installedWidgets = providers,
                        onPickAndBindWidget = { provider, pageId, _ ->
                            picked = provider
                            pickedPage = pageId
                        }
                    )
                }
            }
        }
    }

    @Test fun longProviderListIsLazyAndLastProviderCanBePinned() {
        showPicker()
        val list = compose.onNodeWithTag("add_tile_picker_list")
        list.performScrollToNode(hasTestTag("widget_app_group_example.widgets"))
        compose.onNodeWithTag("widget_app_group_example.widgets").performClick()
        val lastTag = "pin_widget_item_example.widgets/.Widget224"
        compose.onNodeWithTag(lastTag).assertDoesNotExist()
        list.performScrollToNode(hasTestTag(lastTag))
        compose.onNodeWithTag(lastTag).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(providers.last(), picked)
            assertEquals("page_home_1", pickedPage)
        }
    }

    @Test fun tabsSwitchContentAndSearchFindsProviderBeyondOldLimit() {
        showPicker()
        compose.onNodeWithTag("add_sheet_tab_folders").performClick()
        compose.onNodeWithTag("widget_picker_search_input").assertDoesNotExist()
        val list = compose.onNodeWithTag("add_tile_picker_list")
        list.performScrollToNode(hasTestTag("group_page_tiles_by_category_button"))
        compose.onNodeWithTag("group_page_tiles_by_category_button").assertIsDisplayed()
        list.performScrollToNode(hasTestTag("add_sheet_tab_widgets"))
        compose.onNodeWithTag("add_sheet_tab_widgets").performClick()
        compose.onNodeWithTag("widget_picker_search_input").performTextInput("Widget 224")
        list.performScrollToNode(hasTestTag("pin_widget_item_example.widgets/.Widget224"))
        compose.onNodeWithTag("pin_widget_item_example.widgets/.Widget224").assertIsDisplayed()
        compose.onNodeWithTag("group_page_tiles_by_category_button").assertDoesNotExist()
    }
}
