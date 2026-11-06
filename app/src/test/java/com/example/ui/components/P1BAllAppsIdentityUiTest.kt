package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.AllAppsViewMode
import com.example.data.model.AppItem
import com.example.data.model.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Actual All Apps composable: click forwarding must preserve exact component and profile. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class P1BAllAppsIdentityUiTest {
    @get:Rule val compose = createComposeRule()

    private val activities = listOf(
        AppItem("One personal", "com.example.two", launcherComponent = "com.example.two/.One", launcherUserSerial = 0),
        AppItem("Two personal", "com.example.two", launcherComponent = "com.example.two/.Two", launcherUserSerial = 0),
        AppItem("One work", "com.example.two", launcherComponent = "com.example.two/.One", launcherUserSerial = 12),
        AppItem("Two work", "com.example.two", launcherComponent = "com.example.two/.Two", launcherUserSerial = 12)
    )

    private fun verifyMode(viewMode: AllAppsViewMode) {
        val launched = mutableListOf<AppItem>()
        var latest = LauncherSettings(
            allAppsViewMode = viewMode, allAppsCategorizedEnabled = false,
            allAppsGroupByCategory = false
        )
        compose.setContent {
            MaterialTheme {
                AppDrawer(apps = activities, homePages = emptyList(), settings = latest,
                    onLaunchApp = { launched += it },
                    onPinAppToPage = { _, _ -> },
                    onMutateSettings = { edit -> edit(latest).also { latest = it } })
            }
        }
        compose.onNodeWithText("One personal").performClick()
        compose.onNodeWithText("Two work").performClick()
        compose.runOnIdle {
            assertEquals(listOf(activities[0], activities[3]), launched)
            assertNotEquals(launched[0].drawerKey, launched[1].drawerKey)
        }
    }

    @Test fun exactTargetListTapRetainsComponentAndProfile() =
        verifyMode(AllAppsViewMode.METRO_LIST)

    @Test fun exactTargetGridTapRetainsComponentAndProfile() =
        verifyMode(AllAppsViewMode.ONE_UI_GRID)
}
