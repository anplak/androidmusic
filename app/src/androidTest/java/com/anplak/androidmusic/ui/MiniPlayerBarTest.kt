package com.anplak.androidmusic.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anplak.androidmusic.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [MiniPlayerBar] in isolation (no playback engine).
 */
@RunWith(AndroidJUnit4::class)
class MiniPlayerBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsTitleAndArtist_andReflectsPlayingFavoriteState() {
        composeTestRule.setContent {
            MaterialTheme {
                MiniPlayerBar(
                    title = "Fixture Track",
                    artist = "Fixture Artist",
                    isPlaying = true,
                    isFavorite = true,
                    onBarClick = {},
                    onPlayPauseClick = {},
                    onToggleFavorite = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fixture Track").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fixture Artist").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(pauseLabel()).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(removeFavoriteLabel()).assertIsDisplayed()
    }

    @Test
    fun showsPausedAndUnfavoritedIcons() {
        composeTestRule.setContent {
            MaterialTheme {
                MiniPlayerBar(
                    title = "Track",
                    artist = null,
                    isPlaying = false,
                    isFavorite = false,
                    onBarClick = {},
                    onPlayPauseClick = {},
                    onToggleFavorite = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(playLabel()).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(addFavoriteLabel()).assertIsDisplayed()
    }

    @Test
    fun titleClick_opensNowPlaying_actionsDoNot() {
        var barClicks = 0
        var playPauseClicks = 0
        var favoriteClicks = 0

        composeTestRule.setContent {
            MaterialTheme {
                MiniPlayerBar(
                    title = "Click Me",
                    artist = "Artist",
                    isPlaying = false,
                    isFavorite = false,
                    onBarClick = { barClicks++ },
                    onPlayPauseClick = { playPauseClicks++ },
                    onToggleFavorite = { favoriteClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("mini_player_title").performClick()
        assertEquals(1, barClicks)
        assertEquals(0, playPauseClicks)
        assertEquals(0, favoriteClicks)

        composeTestRule.onNodeWithTag("mini_player_play_pause").performClick()
        assertEquals(1, barClicks)
        assertEquals(1, playPauseClicks)
        assertEquals(0, favoriteClicks)

        composeTestRule.onNodeWithTag("mini_player_favorite").performClick()
        assertEquals(1, barClicks)
        assertEquals(1, playPauseClicks)
        assertEquals(1, favoriteClicks)
    }

    @Test
    fun iconStateUpdates_whenPlayingAndFavoriteChange() {
        var isPlaying by mutableStateOf(false)
        var isFavorite by mutableStateOf(false)

        composeTestRule.setContent {
            MaterialTheme {
                MiniPlayerBar(
                    title = "Track",
                    artist = "Artist",
                    isPlaying = isPlaying,
                    isFavorite = isFavorite,
                    onBarClick = {},
                    onPlayPauseClick = {},
                    onToggleFavorite = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(playLabel()).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(addFavoriteLabel()).assertIsDisplayed()

        isPlaying = true
        isFavorite = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(pauseLabel()).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(removeFavoriteLabel()).assertIsDisplayed()
    }

    private fun playLabel(): String = string(R.string.play)
    private fun pauseLabel(): String = string(R.string.pause)
    private fun addFavoriteLabel(): String = string(R.string.add_to_favorites)
    private fun removeFavoriteLabel(): String = string(R.string.remove_from_favorites)

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)
}
