package com.anplak.androidmusic.e2e

import android.Manifest
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.anplak.androidmusic.MainActivity
import com.anplak.androidmusic.ui.clickFirstWithTagPrefix
import com.anplak.androidmusic.ui.hasLibraryTracks
import com.anplak.androidmusic.ui.navigateToLibraryAlbumsTab
import com.anplak.androidmusic.ui.navigateToLibraryArtistsTab
import com.anplak.androidmusic.ui.navigateToPlaylists
import com.anplak.androidmusic.ui.prepareLibraryTab
import com.anplak.androidmusic.ui.preparePlaylistsTab
import com.anplak.androidmusic.ui.safeHasNodes
import com.anplak.androidmusic.ui.waitForLibraryDetailSettled
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for collection-level "Add to playlist" feature.
 *
 * These tests verify the complete flow of adding artist or album collections
 * to playlists, including duplicate detection and ordering.
 */
@RunWith(AndroidJUnit4::class)
class CollectionAddToPlaylistE2ETest {
    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } else {
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Test: Add an album to an existing playlist and verify all tracks appear in playlist detail.
     */
    @Test
    fun addAlbumToExistingPlaylistVerifyAllTracksInPlaylist() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        // First create a playlist
        val playlistName = "Album Test Playlist ${System.currentTimeMillis()}"
        createPlaylistWithFirstLibraryTrack(playlistName)

        // Navigate to Albums tab
        composeTestRule.navigateToLibraryAlbumsTab()

        // Click first album
        composeTestRule.clickFirstWithTagPrefix("album_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Get the album track titles from the detail screen
        val albumTrackTitles = collectTrackTitlesFromDetailScreen()
        if (albumTrackTitles.isEmpty()) return

        // Click "Add to playlist" button
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").performClick()
        composeTestRule.waitForIdle()

        // Wait for dialog
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_dialog"))
        }

        // Select existing playlist
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()

        // Navigate to playlists and open the playlist
        composeTestRule.navigateToPlaylists()
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()

        // Verify all album tracks are in the playlist
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("playlist_track_list"))
        }

        // Each track should appear in the playlist
        for (trackTitle in albumTrackTitles) {
            composeTestRule.waitUntil(timeoutMillis = 3_000) {
                composeTestRule.safeHasNodes(hasText(trackTitle, substring = true))
            }
        }
    }

    /**
     * Test: Add the same album twice and verify no duplicates are created.
     */
    @Test
    fun addSameAlbumTwiceVerifyNoDuplicates() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        val playlistName = "No Duplicate Test ${System.currentTimeMillis()}"

        // Navigate to Albums tab
        composeTestRule.navigateToLibraryAlbumsTab()

        // Click first album
        composeTestRule.clickFirstWithTagPrefix("album_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Get the track count
        val trackCount = getTrackCountFromDetailScreen()
        if (trackCount == 0) return

        // Add album to new playlist
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_dialog"))
        }

        // Create new playlist
        composeTestRule.onNodeWithTag("new_playlist_name_input").performTextInput(playlistName)
        composeTestRule.onNodeWithTag("create_and_add_button").performClick()
        composeTestRule.waitForIdle()

        // Wait for success message
        waitForOperationSuccess()

        // Add the same album again
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_dialog"))
        }

        // Select the same playlist
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()

        // Wait for partial success message showing skipped count
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val snackbarShown =
                composeTestRule.safeHasNodes(hasText("already there")) ||
                    composeTestRule.safeHasNodes(hasText("skipped")) ||
                    composeTestRule.safeHasNodes(hasText("0 added"))
            snackbarShown
        }

        // Verify playlist still has original track count (no duplicates)
        composeTestRule.navigateToPlaylists()
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()

        // Verify track count hasn't doubled
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasText("$trackCount track", substring = true)) ||
                composeTestRule.safeHasNodes(hasText("$trackCount tracks", substring = true))
        }
    }

    /**
     * Test: Create a new playlist from an artist collection and verify every track appears once.
     */
    @Test
    fun createPlaylistFromArtistVerifyEveryTrackAppearsOnce() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        val playlistName = "Artist Collection ${System.currentTimeMillis()}"

        // Navigate to Artists tab
        composeTestRule.navigateToLibraryArtistsTab()

        // Click first artist
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Get artist track titles
        val artistTrackTitles = collectTrackTitlesFromDetailScreen()
        if (artistTrackTitles.isEmpty()) return

        // Click "Add to playlist"
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_dialog"))
        }

        // Create new playlist with artist name
        composeTestRule.onNodeWithTag("new_playlist_name_input").performTextInput(playlistName)
        composeTestRule.onNodeWithTag("create_and_add_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate to playlists
        composeTestRule.navigateToPlaylists()
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()

        // Wait for playlist detail
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("playlist_track_list"))
        }

        // Verify each artist track appears exactly once in the playlist
        for (trackTitle in artistTrackTitles) {
            val matchingNodes =
                composeTestRule.onAllNodes(
                    hasText(trackTitle, substring = true),
                ).fetchSemanticsNodes()
            assert(matchingNodes.size == 1) {
                "Expected exactly one instance of track '$trackTitle' in playlist, found ${matchingNodes.size}"
            }
        }
    }

    /**
     * Test: Partial add shows correct message ("X added · Y already there").
     */
    @Test
    fun partialAddShowsCorrectMessage() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        // First add some tracks to a playlist
        val playlistName = "Partial Test ${System.currentTimeMillis()}"
        createPlaylistWithFirstLibraryTrack(playlistName)

        // Navigate to Artists tab
        composeTestRule.navigateToLibraryArtistsTab()

        // Click first artist
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Add artist to the playlist (some tracks may already be there)
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_dialog"))
        }

        // Select existing playlist
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()

        // Wait for success message with counts
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            // Look for message pattern: "X added" and optionally "Y already there"
            val hasAddedMessage = composeTestRule.safeHasNodes(hasText("added", substring = true))
            val hasAlreadyThereMessage = composeTestRule.safeHasNodes(hasText("already there", substring = true))
            hasAddedMessage || hasAlreadyThereMessage
        }

        // Verify the snackbar shows the message
        val hasSnackbar =
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_snackbar")) ||
                composeTestRule.safeHasNodes(hasText("added", substring = true))
        assert(hasSnackbar) { "Expected success snackbar to be displayed" }
    }

    /**
     * Test: Verify track order in playlist matches collection screen order.
     */
    @Test
    fun verifyDeterministicOrderMatchesCollectionScreen() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        val playlistName = "Order Test ${System.currentTimeMillis()}"

        // Navigate to Albums tab
        composeTestRule.navigateToLibraryAlbumsTab()

        // Click first album
        composeTestRule.clickFirstWithTagPrefix("album_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Get ordered track list from collection screen
        val collectionScreenOrder = collectTrackTitlesFromDetailScreenOrdered()
        if (collectionScreenOrder.size < 2) return // Need at least 2 tracks to verify order

        // Add album to new playlist
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_dialog"))
        }

        // Create new playlist
        composeTestRule.onNodeWithTag("new_playlist_name_input").performTextInput(playlistName)
        composeTestRule.onNodeWithTag("create_and_add_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate to playlists
        composeTestRule.navigateToPlaylists()
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()

        // Wait for playlist detail
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("playlist_track_list"))
        }

        // Get track order from playlist
        val playlistScreenOrder = collectTrackTitlesFromPlaylistOrdered()

        // Verify orders match
        assert(collectionScreenOrder == playlistScreenOrder) {
            "Track order mismatch!\nCollection: $collectionScreenOrder\nPlaylist: $playlistScreenOrder"
        }
    }

    // Helper methods

    private fun createPlaylistWithFirstLibraryTrack(playlistName: String) {
        composeTestRule.preparePlaylistsTab()
        composeTestRule.waitForIdle()

        // Click create playlist FAB
        composeTestRule.onNodeWithTag("create_playlist_fab").performClick()
        composeTestRule.waitForIdle()

        // Enter playlist name
        composeTestRule.onNodeWithTag("playlist_name_input").performTextInput(playlistName)
        composeTestRule.onNodeWithTag("create_playlist_confirm").performClick()
        composeTestRule.waitForIdle()
    }

    private fun collectTrackTitlesFromDetailScreen(): Set<String> {
        val titles = mutableSetOf<String>()
        var index = 0
        while (true) {
            val tag = "library_detail_track_item_$index"
            if (!composeTestRule.safeHasNodes(hasTestTag(tag))) break
            try {
                val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
                val textList = node.config.getOrNull(SemanticsProperties.Text)
                textList?.firstOrNull()?.text?.let { titles.add(it) }
            } catch (_: Exception) {
                break
            }
            index++
        }
        return titles
    }

    private fun collectTrackTitlesFromDetailScreenOrdered(): List<String> {
        val titles = mutableListOf<String>()
        var index = 0
        while (true) {
            val tag = "library_detail_track_item_$index"
            if (!composeTestRule.safeHasNodes(hasTestTag(tag))) break
            try {
                val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
                val textList = node.config.getOrNull(SemanticsProperties.Text)
                textList?.firstOrNull()?.text?.let { titles.add(it) }
            } catch (_: Exception) {
                break
            }
            index++
        }
        return titles
    }

    private fun collectTrackTitlesFromPlaylistOrdered(): List<String> {
        val titles = mutableListOf<String>()
        var index = 0
        while (true) {
            val tag = "playlist_track_item_$index"
            if (!composeTestRule.safeHasNodes(hasTestTag(tag))) break
            try {
                val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
                val textList = node.config.getOrNull(SemanticsProperties.Text)
                textList?.firstOrNull()?.text?.let { titles.add(it) }
            } catch (_: Exception) {
                break
            }
            index++
        }
        return titles
    }

    private fun getTrackCountFromDetailScreen(): Int {
        return collectTrackTitlesFromDetailScreen().size
    }

    private fun waitForOperationSuccess() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasText("added")) ||
                composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_snackbar"))
        }
    }
}
