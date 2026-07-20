package com.anplak.androidmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [MusicTheme] design tokens (story 19).
 */
@RunWith(AndroidJUnit4::class)
class MusicThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun darkTheme_backgroundIsInk() {
        var background = Color.Unspecified

        composeTestRule.setContent {
            MusicTheme(darkTheme = true) {
                background = MaterialTheme.colorScheme.background
            }
        }

        assertEquals(Ink, background)
    }

    @Test
    fun darkTheme_primaryIsPhosphor() {
        var primary = Color.Unspecified

        composeTestRule.setContent {
            MusicTheme(darkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
            }
        }

        assertEquals(Phosphor, primary)
    }

    @Test
    fun lightTheme_usesPaperBackground() {
        var background = Color.Unspecified

        composeTestRule.setContent {
            MusicTheme(darkTheme = false) {
                background = MaterialTheme.colorScheme.background
            }
        }

        assertEquals(Paper, background)
    }
}
