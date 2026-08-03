package com.anplak.androidmusic.ui

import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anplak.androidmusic.ui.theme.MusicTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [MediaArtwork] fallback behavior (story 20).
 */
@RunWith(AndroidJUnit4::class)
class MediaArtworkTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun nullUri_showsFallbackMonogram() {
        composeTestRule.setContent {
            MusicTheme(darkTheme = true) {
                MediaArtwork(
                    uri = null,
                    contentDescription = "Album",
                    fallbackLabel = "Neon",
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        composeTestRule.onNodeWithTag("media_artwork").assertIsDisplayed()
        composeTestRule.onNodeWithTag("media_artwork_fallback").assertIsDisplayed()
        composeTestRule.onNodeWithText("N").assertIsDisplayed()
    }

    @Test
    fun unreadableUri_keepsFallbackVisible() {
        composeTestRule.setContent {
            MusicTheme(darkTheme = true) {
                MediaArtwork(
                    uri = Uri.parse("content://media/external/audio/albumart/999999"),
                    contentDescription = "Missing art",
                    fallbackLabel = "Ghost",
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        composeTestRule.onNodeWithTag("media_artwork").assertIsDisplayed()
        composeTestRule.onNodeWithTag("media_artwork_fallback").assertIsDisplayed()
        composeTestRule.onNodeWithText("G").assertIsDisplayed()
    }

    @Test
    fun blankFallbackLabel_usesQuestionMark() {
        composeTestRule.setContent {
            MusicTheme(darkTheme = true) {
                MediaArtwork(
                    uri = null,
                    contentDescription = null,
                    fallbackLabel = "   ",
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        composeTestRule.onNodeWithText("?").assertIsDisplayed()
    }
}
