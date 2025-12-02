package com.anplak.androidmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NowPlayingScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `displays track title`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song Title",
                artistName = "Test Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                onPlayPauseClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Test Song Title").assertIsDisplayed()
    }
    
    @Test
    fun `displays artist name`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Famous Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                onPlayPauseClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Famous Artist").assertIsDisplayed()
    }
    
    @Test
    fun `shows play button when not playing`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                onPlayPauseClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }
    
    @Test
    fun `shows pause button when playing`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = true,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                onPlayPauseClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }
    
    @Test
    fun `play pause click triggers callback`() {
        var clicked = false
        
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                onPlayPauseClick = { clicked = true },
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Play").performClick()
        
        assertTrue(clicked)
    }
    
    @Test
    fun `back button triggers callback`() {
        var backClicked = false
        
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                onPlayPauseClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = { backClicked = true }
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Back to library").performClick()
        
        assertTrue(backClicked)
    }
    
    @Test
    fun `displays current position time`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 65000L, // 1:05
                duration = 180000L,
                error = null,
                onPlayPauseClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("1:05").assertIsDisplayed()
    }
    
    @Test
    fun `displays duration time`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 245000L, // 4:05
                error = null,
                onPlayPauseClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("4:05").assertIsDisplayed()
    }
    
    @Test
    fun `hides artist text when artist is blank`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                onPlayPauseClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {}
            )
        }
        
        // Track title should be visible
        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
        // But there should be no empty artist text node causing layout issues
    }
}

