package com.anplak.androidmusic.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anplak.androidmusic.ui.theme.MusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [CompactTabActions] (story 18 chrome density).
 */
@RunWith(AndroidJUnit4::class)
class CompactTabActionsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsTrailingActions_withoutTitle() {
        composeTestRule.setContent {
            MusicTheme(darkTheme = true) {
                CompactTabActions {
                    IconButton(onClick = {}, modifier = Modifier.testTag("action_a")) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = {}, modifier = Modifier.testTag("action_b")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("compact_tab_actions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_a").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_b").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
        // No large duplicated tab title in the chrome row
        composeTestRule.onNodeWithText("Library").assertDoesNotExist()
        composeTestRule.onNodeWithText("For You").assertDoesNotExist()
    }

    @Test
    fun actionClicks_invokeCallbacks() {
        var searchClicks = 0
        var refreshClicks = 0

        composeTestRule.setContent {
            MusicTheme(darkTheme = true) {
                CompactTabActions {
                    IconButton(
                        onClick = { searchClicks++ },
                        modifier = Modifier.testTag("open_search"),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(
                        onClick = { refreshClicks++ },
                        modifier = Modifier.testTag("for_you_refresh"),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("open_search").performClick()
        composeTestRule.onNodeWithTag("for_you_refresh").performClick()

        assertEquals(1, searchClicks)
        assertEquals(1, refreshClicks)
    }
}
