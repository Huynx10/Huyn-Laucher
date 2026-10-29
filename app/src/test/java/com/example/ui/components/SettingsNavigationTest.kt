package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.model.LauncherSettings
import com.example.data.model.SystemTelemetry
import com.example.ui.viewmodel.CloudAuthState
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class SettingsNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cleanSettingsBackReturnsToCategoriesAndCloseExits() {
        var dismissCount = 0
        compose.setContent {
            CleanSettingsScreen(
                settings = LauncherSettings(),
                cloudAuthState = CloudAuthState.Unauthenticated,
                cloudBackups = emptyList(),
                isCloudSyncing = false,
                isSavingSettings = false,
                onSaveSettings = {},
                onUpdateTileAppearance = { _, _ -> },
                onPickCustomWallpaper = {},
                onExportBackup = { "" },
                onRestoreBackup = { false },
                onResetDefaults = {},
                onSignInWithGoogle = {},
                onSignOutCloud = {},
                onSyncToCloudNow = {},
                onRestoreCloudBackup = {},
                onOpenAdvanced = {},
                onDismiss = { dismissCount++ }
            )
        }

        compose.onNodeWithText("Ứng dụng").performClick()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Cài đặt").assertExists()
        compose.onNodeWithText("Ứng dụng").performClick()
        compose.onNodeWithContentDescription("Đóng cài đặt").performClick()
        compose.runOnIdle { assertEquals(1, dismissCount) }
    }

    @Test fun cleanSettingsCannotCloseWhileSettingsAreSaving() {
        var dismissCount = 0
        compose.setContent {
            CleanSettingsScreen(
                settings = LauncherSettings(),
                cloudAuthState = CloudAuthState.Unauthenticated,
                cloudBackups = emptyList(),
                isCloudSyncing = false,
                isSavingSettings = true,
                onSaveSettings = {},
                onUpdateTileAppearance = { _, _ -> },
                onPickCustomWallpaper = {},
                onExportBackup = { "" },
                onRestoreBackup = { false },
                onResetDefaults = {},
                onSignInWithGoogle = {},
                onSignOutCloud = {},
                onSyncToCloudNow = {},
                onRestoreCloudBackup = {},
                onOpenAdvanced = {},
                onDismiss = { dismissCount++ }
            )
        }

        // The loading surface intercepts taps, including the close control.
        compose.onNodeWithTag("settings_loading").assertExists()
        compose.onNodeWithContentDescription("Đóng cài đặt", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(0, dismissCount) }
    }

    @Test fun advancedSettingsSwitchesCategoriesWithoutAnotherLevel() {
        var dismissCount = 0
        compose.setContent {
            SettingsSheet(
                settings = LauncherSettings(),
                telemetry = SystemTelemetry(),
                onSaveSettings = { _, _ -> },
                onUpdateTileAppearance = { _, _ -> },
                onExportBackup = { "" },
                onRestoreBackup = { false },
                onDismiss = { dismissCount++ }
            )
        }

        compose.onNodeWithTag("settings_tab_drawer").performClick()
        compose.onNodeWithText("DANH SÁCH ỨNG DỤNG").assertExists()
        compose.onNodeWithTag("settings_tab_system").performClick()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithContentDescription("Đóng tùy chỉnh nâng cao").performClick()
        compose.runOnIdle { assertEquals(2, dismissCount) }
    }

    @Test fun headerKeepsControlsAtLargeText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                Box(Modifier.width(220.dp).testTag("header_container")) {
                    SettingsHeader("Sao lưu và khôi phục", onBack = {}, onClose = {})
                }
            }
        }

        compose.onNodeWithText("Sao lưu và khôi phục").assertExists()
        compose.onNodeWithContentDescription("Quay lại").assertExists()
        compose.onNodeWithContentDescription("Đóng cài đặt").assertExists()
    }

    @Test fun advancedRestoreBlocksDuplicateRequestAndExitWhileBusy() {
        val restoreResult = CompletableDeferred<Boolean>()
        var restoreCalls = 0
        var dismissCount = 0
        compose.setContent {
            SettingsSheet(
                settings = LauncherSettings(),
                telemetry = SystemTelemetry(),
                onSaveSettings = { _, _ -> },
                onUpdateTileAppearance = { _, _ -> },
                onExportBackup = { "" },
                onRestoreBackup = {
                    restoreCalls++
                    restoreResult.await()
                },
                onDismiss = { dismissCount++ }
            )
        }

        compose.onNodeWithTag("settings_tab_system").performClick()
        compose.onNodeWithTag("settings_advanced_content")
            .performScrollToNode(hasText("Khôi phục hoặc đặt lại"))
        compose.onNodeWithText("Khôi phục hoặc đặt lại").performClick()
        compose.onNodeWithText("Dán dữ liệu sao lưu").performTextInput("payload")
        compose.onNodeWithText("Khôi phục").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, restoreCalls) }
        compose.onNodeWithTag("settings_loading").assertExists()
        compose.onNodeWithText("Khôi phục", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Đóng tùy chỉnh nâng cao", useUnmergedTree = true)
            .performClick()
        compose.runOnIdle {
            assertEquals(1, restoreCalls)
            assertEquals(0, dismissCount)
        }

        restoreResult.complete(false)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Đóng tùy chỉnh nâng cao").performClick()
        compose.runOnIdle { assertEquals(1, dismissCount) }
    }
}
