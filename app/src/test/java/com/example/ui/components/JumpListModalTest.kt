package com.example.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], qualifiers = "w320dp-h480dp")
class JumpListModalTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disabledLetterIsAnnouncedAndCannotSelect() {
        val selected = mutableListOf<Char>()
        var dismissCount = 0
        compose.setContent {
            JumpListModal(setOf('A'), "#0078D7", selected::add) { dismissCount++ }
        }

        compose.onNodeWithTag("jump_letter_B")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription, "Không có ứng dụng"
            ))
        compose.onNodeWithTag("jump_letter_B").performClick()
        compose.runOnIdle {
            assertEquals(emptyList<Char>(), selected)
            assertEquals(0, dismissCount)
        }
    }

    @Test fun selectionCallsEachCallbackOnlyOnce() {
        val selected = mutableListOf<Char>()
        var dismissCount = 0
        compose.setContent {
            JumpListModal(setOf('#', 'A'), "#FFC700", selected::add) { dismissCount++ }
        }

        compose.onNodeWithTag("jump_letter_A").performClick()
        compose.onNodeWithTag("jump_letter_A").performClick()
        compose.runOnIdle {
            assertEquals(listOf('A'), selected)
            assertEquals(1, dismissCount)
        }
    }

    @Test fun fallbackGroupExplainsItContainsNamesOutsideAlphabet() {
        compose.setContent {
            JumpListModal(setOf('#'), "#0078D7", {}, {})
        }

        compose.onNodeWithTag("jump_letter_#")
            .assert(SemanticsMatcher.expectValue(ContentDescription, listOf("Khác A đến Z")))
    }

    @Test fun narrowShortWindowScrollsToLastLetterAndOutsideTapDismisses() {
        var dismissCount = 0
        compose.setContent {
            JumpListModal(setOf('Z'), "#0078D7", {}) { dismissCount++ }
        }

        compose.onNodeWithTag("jump_list_grid")
            .performScrollToNode(hasTestTag("jump_letter_Z"))
        compose.onNodeWithTag("jump_letter_Z").assertExists()
        compose.onNodeWithTag("jump_list_scrim")
            .performTouchInput { click(Offset(2f, 2f)) }
        compose.runOnIdle { assertEquals(1, dismissCount) }
    }

    @Test
    @Config(qualifiers = "w320dp-h240dp")
    fun largeTextInLandscapeStaysInsideItsTileAndCanScroll() {
        compose.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale = 2f)
            ) {
                JumpListModal(setOf('A', 'Z'), "#0078D7", {}, {})
            }
        }

        val tile = compose.onNodeWithTag("jump_letter_A").getUnclippedBoundsInRoot()
        val text = compose.onNodeWithText("A", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(text.bottom - text.top >= 35.dp)
        assertTrue(tile.left <= text.left && tile.top <= text.top)
        assertTrue(tile.right >= text.right && tile.bottom >= text.bottom)
        compose.onNodeWithTag("jump_list_grid")
            .performScrollToNode(hasTestTag("jump_letter_Z"))
        compose.onNodeWithTag("jump_letter_Z").assertExists()
    }
}
