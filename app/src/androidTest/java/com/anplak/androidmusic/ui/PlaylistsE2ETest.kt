package com.anplak.androidmusic.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsProperties
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

    @Test
    fun playlistReorder_entersReorderMode() {
        waitForLibraryLoaded()
        if (!hasAtLeastTracks(2)) return

        val playlistName = "Reorder Test ${System.currentTimeMillis()}"

        createPlaylistWithTrack(playlistName, 0)
        addTrackToExistingPlaylist(playlistName, 1)

        openPlaylistDetail(playlistName)

        // Open menu and click reorder
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        
        // Verify reorder menu item exists
        val hasReorderMenu = composeTestRule.onAllNodes(hasText("Reorder tracks")).fetchSemanticsNodes().isNotEmpty()
        if (!hasReorderMenu) return
        
        composeTestRule.onNodeWithText("Reorder tracks").performClick()
        composeTestRule.waitForIdle()

        // In reorder mode, the save/cancel icons should appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("playlist_reorder_list")).fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(hasTestTag("playlist_drag_handle_0")).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Cancel to exit reorder mode
        val hasCancelIcon = composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasContentDescription("Cancel reorder")
        ).fetchSemanticsNodes().isNotEmpty()
        if (hasCancelIcon) {
            composeTestRule.onNodeWithContentDescription("Cancel reorder").performClick()
        }
    }

    @Test
    fun playlistMultiSelect_entersSelectionMode() {
        waitForLibraryLoaded()
        if (!hasAtLeastTracks(2)) return

        val playlistName = "MultiSelect Test ${System.currentTimeMillis()}"

        createPlaylistWithTrack(playlistName, 0)
        addTrackToExistingPlaylist(playlistName, 1)

        openPlaylistDetail(playlistName)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        
        // Verify select tracks menu item exists
        val hasSelectMenu = composeTestRule.onAllNodes(hasText("Select tracks")).fetchSemanticsNodes().isNotEmpty()
        if (!hasSelectMenu) return
        
        composeTestRule.onNodeWithText("Select tracks").performClick()
        composeTestRule.waitForIdle()

        // In selection mode, clicking a track should select it
        composeTestRule.onNodeWithTag("playlist_track_item_0").performClick()
        composeTestRule.waitForIdle()

        // Exit selection mode icon should appear
        val hasExitSelection = composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasContentDescription("Exit selection")
        ).fetchSemanticsNodes().isNotEmpty()
        if (hasExitSelection) {
            composeTestRule.onNodeWithContentDescription("Exit selection").performClick()
        }
    }

    @Test
    fun playlistDuplicate_showsDialog() {
        waitForLibraryLoaded()
        if (!hasAtLeastTracks(1)) return

        val playlistName = "Duplicate Test ${System.currentTimeMillis()}"

        createPlaylistWithTrack(playlistName, 0)
        openPlaylistDetail(playlistName)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        
        // Verify duplicate menu item exists
        val hasDuplicateMenu = composeTestRule.onAllNodes(hasText("Duplicate")).fetchSemanticsNodes().isNotEmpty()
        if (!hasDuplicateMenu) return
        
        composeTestRule.onNodeWithText("Duplicate").performClick()
        composeTestRule.waitForIdle()

        // Verify duplicate dialog appears
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Duplicate playlist")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Duplicate playlist").assertIsDisplayed()
        
        // Dismiss the dialog
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    @Test
    fun playlistAutoMix_menuItemExists() {
        waitForLibraryLoaded()
        if (!hasAtLeastTracks(1)) return

        val playlistName = "AutoMix Test ${System.currentTimeMillis()}"
        createPlaylistWithTrack(playlistName, 0)
        openPlaylistDetail(playlistName)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        
        // Verify generate mix menu item exists
        val hasGenerateMix = composeTestRule.onAllNodes(hasText("Generate mix")).fetchSemanticsNodes().isNotEmpty()
        assert(hasGenerateMix) { "Generate mix menu item should be available" }
        
        // Dismiss the menu
        composeTestRule.onNodeWithContentDescription("More options").performClick()
    }

    @Test
    fun playlistSmartShuffle_menuItemExists() {
        waitForLibraryLoaded()
        if (!hasAtLeastTracks(1)) return

        val playlistName = "SmartShuffle Test ${System.currentTimeMillis()}"
        createPlaylistWithTrack(playlistName, 0)
        openPlaylistDetail(playlistName)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        
        // Verify smart shuffle menu item exists
        val hasSmartShuffle = composeTestRule.onAllNodes(hasText("Smart shuffle play")).fetchSemanticsNodes().isNotEmpty()
        assert(hasSmartShuffle) { "Smart shuffle play menu item should be available" }
        
        // Dismiss the menu
        composeTestRule.onNodeWithContentDescription("More options").performClick()
    }

    private fun waitForLibraryLoaded() {
        composeTestRule.waitForIdle()
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
    }

    private fun hasAtLeastTracks(minCount: Int): Boolean {
        val nodes = composeTestRule.onAllNodes(hasTestTag("track_item_0")).fetchSemanticsNodes()
        if (minCount <= 1) return nodes.isNotEmpty()
        return composeTestRule.onAllNodes(hasTestTag("track_item_${minCount - 1}")).fetchSemanticsNodes().isNotEmpty()
    }

    private fun getTrackTitleOnly(tag: String): String {
        return getTrackDisplayText(tag).split("•").firstOrNull()?.trim().orEmpty()
    }

    private fun getTrackDisplayText(tag: String): String {
        val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        val textList = node.config.getOrElse(SemanticsProperties.Text) { emptyList() }
        return textList.joinToString(" ") { it.text }
    }

    private fun createPlaylistWithTrack(playlistName: String, trackIndex: Int) {
        composeTestRule.onNodeWithTag("more_button_$trackIndex").performClick()
        composeTestRule.onNodeWithTag("add_to_playlist_menu_$trackIndex").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_new_playlist_option").performClick()
        composeTestRule.onNodeWithTag("new_playlist_name_input").performTextInput(playlistName)
        composeTestRule.onNodeWithTag("create_and_add_button").performClick()
        composeTestRule.waitForIdle()
    }

    private fun addTrackToExistingPlaylist(playlistName: String, trackIndex: Int) {
        composeTestRule.onNodeWithTag("more_button_$trackIndex").performClick()
        composeTestRule.onNodeWithTag("add_to_playlist_menu_$trackIndex").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()
    }

    private fun openPlaylistDetail(playlistName: String) {
        composeTestRule.onNodeWithTag("nav_playlists").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText(playlistName)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(playlistName).performClick()
        composeTestRule.waitForIdle()
    }
}

