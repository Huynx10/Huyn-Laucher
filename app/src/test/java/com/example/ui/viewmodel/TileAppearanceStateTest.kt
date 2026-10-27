package com.example.ui.viewmodel

import com.example.data.model.LauncherSettings
import com.example.data.model.Wp10TileStyle
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class TileAppearanceStateTest {
    @Test
    fun unrelatedSettingChangeThenTransparentKeepsLatestLayoutAndWidgetPreferences() {
        val state = MutableStateFlow(LauncherSettings())
        val staleComposeSnapshot = state.value
        val latest = staleComposeSnapshot.copy(
            defaultHomePageId = "page_home_2",
            showMoreTiles = true,
            autoArrangeTiles = true,
            lockHomeLayout = true,
            wp10RevealBorder = false,
            tileCornerRadiusDp = 8,
            tileSpacingDp = 10,
            customAppCategories = mapOf("com.example.clock" to "TOOLS")
        )
        state.value = latest

        // Simulate a delayed click in a screen still holding staleComposeSnapshot.
        state.updateTileAppearanceFromLatest { Wp10TileStyle.CRYSTAL_CLEAR to 0f }

        assertEquals(
            latest.copy(wp10TileStyle = Wp10TileStyle.CRYSTAL_CLEAR, tileOpacity = 0f),
            state.value
        )
    }

    @Test
    fun repeatedAppearanceUpdatesUseCurrentStateAndClampOpacity() {
        val state = MutableStateFlow(LauncherSettings())
        state.updateTileAppearanceFromLatest { current -> current.wp10TileStyle.next() to 0.38f }
        state.value = state.value.copy(defaultHomePageId = "page_home_3")
        state.updateTileAppearanceFromLatest { current -> current.wp10TileStyle.next() to -1f }

        assertEquals(Wp10TileStyle.FLUENT_ACRYLIC, state.value.wp10TileStyle)
        assertEquals(0f, state.value.tileOpacity)
        assertEquals("page_home_3", state.value.defaultHomePageId)
    }
}
