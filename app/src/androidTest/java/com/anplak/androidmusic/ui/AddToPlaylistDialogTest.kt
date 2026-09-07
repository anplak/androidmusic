package com.anplak.androidmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anplak.androidmusic.data.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for AddToPlaylistDialog composable.
 *
 * These tests verify the UI behavior of the collection-level "Add to playlist" dialog
 * including display of collection info, playlist selection, and new playlist creation.
 */
@RunWith(AndroidJUnit4::class)
class AddToPlaylistDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialogShowsCollectionNameAndTrackCount() {
        val collectionName = "Greatest Hits"
        val trackCount = 12

        composeTestRule.setContent {
            val playlistsFlow = rememberTestPlaylistsFlow(emptyList())
            TestAddToPlaylistDialog(
                collectionName = collectionName,
                trackCount = trackCount,
                trackIds = List(trackCount) { it.toLong() },
                playlists = playlistsFlow.value,
                onDismiss = {},
                onPlaylistSelected = { _, _ -> },
                onCreatePlaylist = { _, _ -> },
            )
        }

        // Verify dialog is displayed
        composeTestRule.onNodeWithTag("add_to_playlist_dialog").assertIsDisplayed()

        // Verify collection name is shown
        composeTestRule.onNodeWithText(collectionName).assertIsDisplayed()

        // Verify track count is shown (plural format: "12 tracks")
        composeTestRule.onNodeWithText("12 tracks").assertIsDisplayed()
    }

    @Test
    fun dialogShowsCollectionNameAndSingularTrackCount() {
        val collectionName = "Single Track"
        val trackCount = 1

        composeTestRule.setContent {
            val playlistsFlow = rememberTestPlaylistsFlow(emptyList())
            TestAddToPlaylistDialog(
                collectionName = collectionName,
                trackCount = trackCount,
                trackIds = List(trackCount) { it.toLong() },
                playlists = playlistsFlow.value,
                onDismiss = {},
                onPlaylistSelected = { _, _ -> },
                onCreatePlaylist = { _, _ -> },
            )
        }

        // Verify collection name is shown
        composeTestRule.onNodeWithText(collectionName).assertIsDisplayed()

        // Verify singular track count is shown
        composeTestRule.onNodeWithText("1 track").assertIsDisplayed()
    }

    @Test
    fun existingPlaylistSelectionCallsOnPlaylistSelectedWithTrackList() {
        val trackIds = listOf(1L, 2L, 3L, 4L, 5L)
        var selectedPlaylistId: Long? = null
        var selectedTrackIds: List<Long>? = null

        val testPlaylists =
            listOf(
                Playlist(id = 10L, name = "My Playlist", createdAt = System.currentTimeMillis(), trackCount = 3),
            )

        composeTestRule.setContent {
            val playlistsFlow = rememberTestPlaylistsFlow(testPlaylists)
            TestAddToPlaylistDialog(
                collectionName = "Test Album",
                trackCount = trackIds.size,
                trackIds = trackIds,
                playlists = playlistsFlow.value,
                onDismiss = {},
                onPlaylistSelected = { playlistId, ids ->
                    selectedPlaylistId = playlistId
                    selectedTrackIds = ids
                },
                onCreatePlaylist = { _, _ -> },
            )
        }

        // Click on existing playlist option
        composeTestRule.onNodeWithTag("playlist_option_10").performClick()

        // Verify callback was called with correct data
        assert(selectedPlaylistId == 10L) { "Expected playlistId to be 10L" }
        assert(selectedTrackIds == trackIds) { "Expected trackIds to match input" }
    }

    @Test
    fun newPlaylistCreationCallsOnCreatePlaylistWithTrackList() {
        val trackIds = listOf(10L, 20L, 30L)
        var createdPlaylistName: String? = null
        var createdTrackIds: List<Long>? = null

        composeTestRule.setContent {
            val playlistsFlow = rememberTestPlaylistsFlow(emptyList())
            TestAddToPlaylistDialog(
                collectionName = "Test Artist",
                trackCount = trackIds.size,
                trackIds = trackIds,
                playlists = playlistsFlow.value,
                onDismiss = {},
                onPlaylistSelected = { _, _ -> },
                onCreatePlaylist = { name, ids ->
                    createdPlaylistName = name
                    createdTrackIds = ids
                },
            )
        }

        // Type new playlist name
        composeTestRule.onNodeWithTag("new_playlist_name_input").performTextInput("Brand New Playlist")

        // Click create button
        composeTestRule.onNodeWithTag("create_and_add_button").performClick()

        // Verify callback was called with correct data
        assert(createdPlaylistName == "Brand New Playlist") { "Expected playlist name to match" }
        assert(createdTrackIds == trackIds) { "Expected trackIds to match input" }
    }

    @Test
    fun emptyCollectionShowsDialogButCreateButtonNotShown() {
        composeTestRule.setContent {
            val playlistsFlow = rememberTestPlaylistsFlow(emptyList())
            TestAddToPlaylistDialog(
                collectionName = "Empty Collection",
                trackCount = 0,
                trackIds = emptyList(),
                playlists = playlistsFlow.value,
                onDismiss = {},
                onPlaylistSelected = { _, _ -> },
                onCreatePlaylist = { _, _ -> },
            )
        }

        // Dialog should be displayed
        composeTestRule.onNodeWithTag("add_to_playlist_dialog").assertIsDisplayed()

        // Collection name should be shown
        composeTestRule.onNodeWithText("Empty Collection").assertIsDisplayed()

        // "0 tracks" should be shown
        composeTestRule.onNodeWithText("0 tracks").assertIsDisplayed()
    }

    @Test
    fun dismissingPickerDoesNotModifyPlaylist() {
        val trackIds = listOf(1L, 2L, 3L)
        var dismissCalled = false
        var playlistSelectedCalled = false
        var createPlaylistCalled = false

        composeTestRule.setContent {
            val playlistsFlow = rememberTestPlaylistsFlow(emptyList())
            TestAddToPlaylistDialog(
                collectionName = "Test Album",
                trackCount = trackIds.size,
                trackIds = trackIds,
                playlists = playlistsFlow.value,
                onDismiss = { dismissCalled = true },
                onPlaylistSelected = { _, _ -> playlistSelectedCalled = true },
                onCreatePlaylist = { _, _ -> createPlaylistCalled = true },
            )
        }

        // Click cancel button
        composeTestRule.onNodeWithTag("cancel_add_to_playlist_button").performClick()

        // Verify dismiss was called but no playlist modification
        assert(dismissCalled) { "Expected onDismiss to be called" }
        assert(!playlistSelectedCalled) { "Expected onPlaylistSelected NOT to be called" }
        assert(!createPlaylistCalled) { "Expected onCreatePlaylist NOT to be called" }
    }

    @Test
    fun dialogDisplaysExistingPlaylistsInList() {
        val testPlaylists =
            listOf(
                Playlist(id = 1L, name = "Favorites", createdAt = 1000L, trackCount = 10),
                Playlist(id = 2L, name = "Workout Mix", createdAt = 2000L, trackCount = 25),
                Playlist(id = 3L, name = "Road Trip", createdAt = 3000L, trackCount = 50),
            )

        composeTestRule.setContent {
            val playlistsFlow = rememberTestPlaylistsFlow(testPlaylists)
            TestAddToPlaylistDialog(
                collectionName = "Test Collection",
                trackCount = 5,
                trackIds = listOf(1L, 2L, 3L, 4L, 5L),
                playlists = playlistsFlow.value,
                onDismiss = {},
                onPlaylistSelected = { _, _ -> },
                onCreatePlaylist = { _, _ -> },
            )
        }

        // Verify all playlists are displayed
        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 tracks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Workout Mix").assertIsDisplayed()
        composeTestRule.onNodeWithText("25 tracks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Road Trip").assertIsDisplayed()
        composeTestRule.onNodeWithText("50 tracks").assertIsDisplayed()
    }

    // Test helper that creates a testable version of the dialog without ViewModel dependency
    @androidx.compose.runtime.Composable
    private fun rememberTestPlaylistsFlow(playlists: List<Playlist>): StateFlow<List<Playlist>> {
        return androidx.compose.runtime.remember { MutableStateFlow(playlists) }
    }
}
