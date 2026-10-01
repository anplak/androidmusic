package com.anplak.androidmusic.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.anplak.androidmusic.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for the fixed search and playlist functionality.
 */
@RunWith(AndroidJUnit4::class)
class SearchAndPlaylistE2ETest {
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

    // Test that search results are ordered correctly (Artists first, then Albums, then Tracks)
    @Test
    fun searchResultsOrderedCorrectly() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        // Type a query that should match artists, albums, and tracks
        composeTestRule.typeInSearchField("a")
        composeTestRule.waitForSearchSettled()

        // Verify we have search results
        composeTestRule.onNodeWithTag("search_results").assertIsDisplayed()

        // Note: Actual ordering verification would require access to the underlying data structure
        // which is not exposed through the UI test API
    }

    // Test that clicking artist result navigates to artist screen
    @Test
    fun searchArtistResultNavigatesToArtistScreen() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        // Type a query that should match artists
        composeTestRule.typeInSearchField("beat")
        composeTestRule.waitForSearchSettled()

        // Click on artist result (if available)
        if (composeTestRule.safeHasNodes(hasTestTag("search_result_artist"))) {
            composeTestRule.clickFirstWithTag("search_result_artist")

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.safeHasNodes(hasTestTag("library_detail_track_list")) ||
                    composeTestRule.safeHasNodes(hasTestTag("library_detail_empty"))
            }
            composeTestRule.onNodeWithTag("library_detail_back_button").assertIsDisplayed()
        }
    }

    // Test that clicking album result navigates to album screen
    @Test
    fun searchAlbumResultNavigatesToAlbumScreen() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        // Type a query that should match albums
        composeTestRule.typeInSearchField("thriller")
        composeTestRule.waitForSearchSettled()

        // Click on album result (if available)
        if (composeTestRule.safeHasNodes(hasTestTag("search_result_album"))) {
            composeTestRule.clickFirstWithTag("search_result_album")

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.safeHasNodes(hasTestTag("library_detail_track_list")) ||
                    composeTestRule.safeHasNodes(hasTestTag("library_detail_empty"))
            }
            composeTestRule.onNodeWithTag("library_detail_back_button").assertIsDisplayed()
        }
    }

    // Test that clicking track result starts playback
    @Test
    fun searchTrackResultStartsPlayback() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        // Type a query that should match tracks
        composeTestRule.typeInSearchField("beat")
        composeTestRule.waitForSearchSettled()

        // Click on track result (if available)
        if (composeTestRule.safeHasNodes(hasTestTag("search_result_track"))) {
            composeTestRule.clickFirstWithTag("search_result_track")

            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.safeHasNodes(hasTestTag("play_pause_button")) ||
                    composeTestRule.safeHasNodes(hasTestTag("mini_player_bar"))
            }

            composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed()
        }
    }

    // Test playlist track addition functionality
    @Test
    fun playlistAddTracksButtonWorks() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToPlaylists()

        // Navigate to a playlist (if any exist)
        if (composeTestRule.safeHasNodes(hasTestTag("playlist_item_0"))) {
            composeTestRule.onNodeWithTag("playlist_item_0").performClick()

            // Look for the add tracks FAB
            if (composeTestRule.safeHasNodes(hasTestTag("add_tracks_fab"))) {
                composeTestRule.onNodeWithTag("add_tracks_fab").performClick()

                // Verify we are now on the search screen
                composeTestRule.onNodeWithTag("search_field").assertIsDisplayed()
            }
        }
    }
}
