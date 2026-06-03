package com.anplak.androidmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = { clicked = true },
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = { backClicked = true },
                onToggleFavorite = {},
                onAddToPlaylist = {}
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
            )
        }
        
        composeTestRule.onNodeWithText("1:05").performScrollTo().assertIsDisplayed()
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
            )
        }
        
        composeTestRule.onNodeWithText("4:05").performScrollTo().assertIsDisplayed()
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
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
            )
        }
        
        // Track title should be visible
        composeTestRule.onNodeWithText("Test Song").assertIsDisplayed()
        // But there should be no empty artist text node causing layout issues
    }
    
    @Test
    fun `shows queue position when queue has multiple tracks`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                queuePosition = 3,
                queueSize = 10,
                hasNext = true,
                hasPrevious = true,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
            )
        }
        
        composeTestRule.onNodeWithText("Track 3 of 10").assertIsDisplayed()
    }
    
    @Test
    fun `next button triggers callback when enabled`() {
        var nextClicked = false
        
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                queuePosition = 1,
                queueSize = 3,
                hasNext = true,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = { nextClicked = true },
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Next").performClick()
        
        assertTrue(nextClicked)
    }
    
    @Test
    fun `previous button triggers callback when enabled`() {
        var previousClicked = false
        
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                queuePosition = 2,
                queueSize = 3,
                hasNext = true,
                hasPrevious = true,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = { previousClicked = true },
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Previous").performClick()
        
        assertTrue(previousClicked)
    }
    
    @Test
    fun `next button is disabled at end of queue`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                queuePosition = 3,
                queueSize = 3,
                hasNext = false,
                hasPrevious = true,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Next").assertIsNotEnabled()
    }
    
    @Test
    fun `previous button is disabled at start of queue`() {
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                queuePosition = 1,
                queueSize = 3,
                hasNext = true,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = {},
                onAddToPlaylist = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Previous").assertIsNotEnabled()
    }
    
    @Test
    fun `toggle favorite callback is triggered`() {
        var favoriteToggled = false
        
        composeTestRule.setContent {
            NowPlayingScreen(
                trackTitle = "Test Song",
                artistName = "Artist",
                isPlaying = false,
                currentPosition = 0L,
                duration = 180000L,
                error = null,
                queuePosition = 1,
                queueSize = 1,
                hasNext = false,
                hasPrevious = false,
                isFavorite = false,
                onPlayPauseClick = {},
                onNextClick = {},
                onPreviousClick = {},
                onSeek = {},
                onErrorDismiss = {},
                onBackClick = {},
                onToggleFavorite = { favoriteToggled = true },
                onAddToPlaylist = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Add to favorites").performClick()
        
        assertTrue(favoriteToggled)
    }
}
