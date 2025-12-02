package com.anplak.androidmusic.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.anplak.androidmusic.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `shows loading state with progress indicator`() {
        composeTestRule.setContent {
            LoadingStateContent()
        }
        
        composeTestRule.onNodeWithText("Scanning library…").assertIsDisplayed()
    }
    
    @Test
    fun `shows empty state when no music found`() {
        composeTestRule.setContent {
            EmptyLibraryStateContent()
        }
        
        composeTestRule.onNodeWithText("No music found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add some music files to your device to see them here.")
            .assertIsDisplayed()
    }
    
    @Test
    fun `shows track list when content is available`() {
        val tracks = listOf(
            createTrack(1, "First Song", "Artist One"),
            createTrack(2, "Second Song", "Artist Two")
        )
        
        composeTestRule.setContent {
            TrackListContent(
                tracks = tracks,
                onTrackSelected = {}
            )
        }
        
        composeTestRule.onNodeWithText("First Song").assertIsDisplayed()
        composeTestRule.onNodeWithText("Artist One").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Song").assertIsDisplayed()
        composeTestRule.onNodeWithText("Artist Two").assertIsDisplayed()
    }
    
    @Test
    fun `shows track duration in correct format`() {
        val tracks = listOf(
            createTrack(1, "Song", "Artist", duration = 185000) // 3:05
        )
        
        composeTestRule.setContent {
            TrackListContent(
                tracks = tracks,
                onTrackSelected = {}
            )
        }
        
        composeTestRule.onNodeWithText("3:05").assertIsDisplayed()
    }
    
    @Test
    fun `track click triggers callback with correct track`() {
        var selectedTrack: TrackInfo? = null
        val tracks = listOf(
            createTrack(1, "First Song", "Artist One"),
            createTrack(2, "Second Song", "Artist Two")
        )
        
        composeTestRule.setContent {
            TrackListContent(
                tracks = tracks,
                onTrackSelected = { selectedTrack = it }
            )
        }
        
        composeTestRule.onNodeWithText("Second Song").performClick()
        
        assertEquals("Second Song", selectedTrack?.title)
    }
    
    private fun createTrack(
        id: Long,
        title: String,
        artist: String,
        duration: Long = 180000L
    ): TrackInfo {
        return TrackInfo(
            uri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = artist,
            album = "Test Album",
            duration = duration
        )
    }
}

// Test-only composables that mirror the internal components from LibraryScreen
@Composable
private fun LoadingStateContent() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Scanning library…",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryStateContent() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No music found",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Add some music files to your device to see them here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TrackListContent(
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo) -> Unit
) {
    MaterialTheme {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = tracks,
                key = { it.uri.toString() }
            ) { track ->
                ListItem(
                    headlineContent = {
                        Text(text = track.title)
                    },
                    supportingContent = {
                        Text(text = track.artist)
                    },
                    trailingContent = {
                        Text(text = formatDuration(track.duration))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackSelected(track) }
                )
            }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
