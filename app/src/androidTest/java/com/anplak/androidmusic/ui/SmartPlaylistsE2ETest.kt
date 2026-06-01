package com.anplak.androidmusic.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.anplak.androidmusic.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for Smart Playlists and Smart Shuffle covering:
 * - Smart playlists display in Playlists tab
 * - Smart playlist detail navigation
 * - Smart shuffle from Now Playing menu
 *
 * Note: These tests require audio files on the device. Tests will skip
 * gracefully if no audio is present.
 */
@RunWith(AndroidJUnit4::class)
class SmartPlaylistsE2ETest {

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
     * Helper function to navigate to the Playlists tab.
     */
    private fun navigateToPlaylistsTab() {
        composeTestRule.preparePlaylistsTab()
    }

    /**
     * Smart playlists should be displayed in the Playlists tab.
     */
    @Test
    fun smartPlaylists_displayedInPlaylistsTab() {
        navigateToPlaylistsTab()

        // Verify smart playlists header exists
        composeTestRule
            .onNodeWithTag("smart_playlists_header")
            .assertIsDisplayed()

        // Verify all three smart playlist items exist
        composeTestRule
            .onNodeWithTag("smart_playlist_most_played")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("smart_playlist_recently_played")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("smart_playlist_recently_added")
            .assertIsDisplayed()
    }

    /**
     * Tapping a smart playlist should open the detail screen.
     */
    @Test
    fun smartPlaylist_tappingOpensDetailScreen() {
        navigateToPlaylistsTab()

        // Tap "Most Played" smart playlist
        composeTestRule
            .onNodeWithTag("smart_playlist_most_played")
            .performClick()

        // Wait for detail screen to load
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val hasContent = composeTestRule
                .onAllNodes(hasTestTag("smart_playlist_track_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasEmpty = composeTestRule
                .onAllNodes(hasTestTag("smart_playlist_empty_state"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasLoading = composeTestRule
                .onAllNodes(hasTestTag("smart_playlist_loading_state"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasContent || hasEmpty || hasLoading
        }

        // Verify back button exists (we're on detail screen)
        composeTestRule
            .onNodeWithTag("smart_playlist_back_button")
            .assertIsDisplayed()
    }

    /**
     * Back button on smart playlist detail should return to playlists.
     */
    @Test
    fun smartPlaylistDetail_backButtonReturnsToPlaylists() {
        navigateToPlaylistsTab()

        // Tap "Recently Added" smart playlist
        composeTestRule
            .onNodeWithTag("smart_playlist_recently_added")
            .performClick()

        // Wait for detail screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("smart_playlist_back_button"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Tap back button
        composeTestRule
            .onNodeWithTag("smart_playlist_back_button")
            .performClick()

        // Wait for playlists screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("smart_playlists_header"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Verify we're back on playlists screen
        composeTestRule
            .onNodeWithTag("smart_playlists_header")
            .assertIsDisplayed()
    }

    /**
     * Helper function to navigate to Now Playing by selecting the first track.
     * Returns true if navigation succeeded (device has audio files).
     */
    private fun navigateToNowPlaying(): Boolean {
        composeTestRule.prepareLibraryTab()

        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) {
            return false
        }

        composeTestRule.onNodeWithTag("track_item_0").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("play_pause_button"))
        }

        return true
    }

    /**
     * Smart shuffle option should be available in Now Playing menu.
     */
    @Test
    fun smartShuffle_availableInNowPlayingMenu() {
        if (!navigateToNowPlaying()) {
            // No tracks on device - skip test
            return
        }

        // Open more menu
        composeTestRule
            .onNodeWithTag("more_button")
            .performClick()

        // Wait for menu to appear
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule
                .onAllNodes(hasTestTag("smart_shuffle_menu"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Verify smart shuffle menu item exists
        composeTestRule
            .onNodeWithTag("smart_shuffle_menu")
            .assertIsDisplayed()
    }

    /**
     * Tapping smart shuffle should start playback (if tracks available).
     */
    @Test
    fun smartShuffle_startsPlayback() {
        if (!navigateToNowPlaying()) {
            // No tracks on device - skip test
            return
        }

        // Open more menu
        composeTestRule
            .onNodeWithTag("more_button")
            .performClick()

        // Wait for menu
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule
                .onAllNodes(hasTestTag("smart_shuffle_menu"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Tap smart shuffle
        composeTestRule
            .onNodeWithTag("smart_shuffle_menu")
            .performClick()

        // Verify we're still on Now Playing screen (playback continues)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("play_pause_button"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithTag("play_pause_button")
            .assertIsDisplayed()
    }
}

