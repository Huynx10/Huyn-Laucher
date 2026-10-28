package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AppDrawerSettingsMutationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun creatingFolderFromStaleUiKeepsUnrelatedLatestSetting() {
        val staleUiSettings = LauncherSettings(lockHomeLayout = false)
        var latest = staleUiSettings.copy(lockHomeLayout = true)

        compose.setContent {
            MaterialTheme {
                AppDrawer(
                    apps = emptyList(), homePages = emptyList(),
                    settings = staleUiSettings, onLaunchApp = {},
                    onPinAppToPage = { _, _ -> },
                    onMutateSettings = { edit -> edit(latest).also { latest = it } }
                )
            }
        }

        compose.onNodeWithText("+ Thư mục").performClick()
        compose.runOnIdle {
            assertTrue(latest.lockHomeLayout)
            assertEquals(1, latest.appDrawerFolders.size)
        }
    }
}
