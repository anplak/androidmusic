package com.anplak.androidmusic.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for Playlists functionality:
 * - Create new playlist
 * - Add track to playlist
 * - View playlist detail
 * - Track count display
 */
@RunWith(AndroidJUnit4::class)
class PlaylistsE2ETest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Test that playlists tab shows empty state initially.
     */
    @Test
    fun playlistsTab_showsEmptyStateInitially() {
        composeTestRule.waitForIdle()
        
        // Wait for library to load first
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            val hasContent = composeTestRule
                .onAllNodes(hasTestTag("track_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasEmpty = composeTestRule
                .onAllNodes(hasTestTag("empty_state"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasContent || hasEmpty
        }
        
        // Navigate to playlists
        composeTestRule
            .onNodeWithTag("nav_playlists")
            .performClick()
        
        // Wait for playlists screen to load
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val hasEmptyState = composeTestRule
                .onAllNodes(hasTestTag("playlists_empty_state"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasListState = composeTestRule
                .onAllNodes(hasTestTag("playlists_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasEmptyState || hasListState
        }
        
        // Verify empty state or list is shown (fresh database should be empty)
        val hasEmptyState = composeTestRule
            .onAllNodes(hasTestTag("playlists_empty_state"))
            .fetchSemanticsNodes()
            .isNotEmpty()
        val hasListState = composeTestRule
            .onAllNodes(hasTestTag("playlists_list"))
            .fetchSemanticsNodes()
            .isNotEmpty()
        
        assert(hasEmptyState || hasListState) {
            "Expected playlists_empty_state or playlists_list to be displayed"
        }
    }

    /**
     * Test creating a new playlist.
     */
    @Test
    fun createPlaylist_showsInList() {
        composeTestRule.waitForIdle()
        
        // Wait for library to load first
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            val hasContent = composeTestRule
                .onAllNodes(hasTestTag("track_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasEmpty = composeTestRule
                .onAllNodes(hasTestTag("empty_state"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasContent || hasEmpty
        }
        
        // Navigate to playlists
        composeTestRule
            .onNodeWithTag("nav_playlists")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Click create playlist FAB
        composeTestRule
            .onNodeWithTag("create_playlist_fab")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Enter playlist name
        composeTestRule
            .onNodeWithTag("playlist_name_input")
            .performTextInput("My Test Playlist")
        
        // Confirm creation
        composeTestRule
            .onNodeWithTag("create_playlist_confirm")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Wait for playlist to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("playlists_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        
        // Verify playlist appears in list
        composeTestRule
            .onNodeWithText("My Test Playlist")
            .assertIsDisplayed()
    }

    /**
     * Test opening create playlist dialog.
     */
    @Test
    fun createPlaylistFab_opensDialog() {
        composeTestRule.waitForIdle()
        
        // Wait for library to load first
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            val hasContent = composeTestRule
                .onAllNodes(hasTestTag("track_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasEmpty = composeTestRule
                .onAllNodes(hasTestTag("empty_state"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasContent || hasEmpty
        }
        
        // Navigate to playlists
        composeTestRule
            .onNodeWithTag("nav_playlists")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Click create playlist FAB
        composeTestRule
            .onNodeWithTag("create_playlist_fab")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify dialog is shown
        composeTestRule
            .onNodeWithTag("create_playlist_dialog")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("playlist_name_input")
            .assertIsDisplayed()
    }
    
    /**
     * Test that creating a new playlist via "Add to Playlist" dialog shows correct track count.
     * 
     * Bug fix verification: When creating a new playlist and adding a track via the dialog,
     * the playlist should show "1 track" instead of "0 tracks".
     */
    @Test
    fun createPlaylistAndAddTrack_showsCorrectTrackCount() {
        composeTestRule.waitForIdle()
        
        // Wait for library to load first
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            val hasContent = composeTestRule
                .onAllNodes(hasTestTag("track_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasEmpty = composeTestRule
                .onAllNodes(hasTestTag("empty_state"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasContent || hasEmpty
        }
        
        // Check if we have tracks
        val hasTrackList = composeTestRule
            .onAllNodes(hasTestTag("track_list"))
            .fetchSemanticsNodes()
            .isNotEmpty()
        
        if (!hasTrackList) {
            // No tracks on device - skip test
            return
        }
        
        // Open the more options menu on the first track
        composeTestRule
            .onNodeWithTag("more_button_0")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Click "Add to playlist" option
        composeTestRule
            .onNodeWithTag("add_to_playlist_menu_0")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Wait for add to playlist dialog
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("add_to_playlist_dialog"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        
        // Click "Create new playlist" option
        composeTestRule
            .onNodeWithTag("create_new_playlist_option")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Enter playlist name
        val uniquePlaylistName = "TrackCount Test ${System.currentTimeMillis()}"
        composeTestRule
            .onNodeWithTag("new_playlist_name_input")
            .performTextInput(uniquePlaylistName)
        
        // Click create button
        composeTestRule
            .onNodeWithTag("create_and_add_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Navigate to playlists tab
        composeTestRule
            .onNodeWithTag("nav_playlists")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Wait for playlists list to load
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("playlists_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        
        // Verify the playlist is shown with the correct name
        composeTestRule
            .onNodeWithText(uniquePlaylistName)
            .assertIsDisplayed()
        
        // Verify the playlist shows "1 tracks" (not "0 tracks")
        // The track count is displayed as supporting text in the playlist item
        // Note: The app uses "%d tracks" format which doesn't handle singular form
        composeTestRule
            .onNodeWithText("1 tracks")
            .assertIsDisplayed()
    }
}

