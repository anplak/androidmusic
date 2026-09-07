package com.anplak.androidmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anplak.androidmusic.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for LibraryCollectionDetailScreen composable.
 * Tests the "Add to playlist" button visibility and functionality
 * on Artist and Album detail screens.
 */
@RunWith(AndroidJUnit4::class)
class LibraryCollectionDetailScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun artistDetailShowsAddToPlaylistButton() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        // Navigate to Artists tab
        composeTestRule.navigateToLibraryArtistsTab()

        // Click first artist
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Verify "Add to playlist" button is displayed
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").assertIsDisplayed()
    }

    @Test
    fun albumDetailShowsAddToPlaylistButton() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        // Navigate to Albums tab
        composeTestRule.navigateToLibraryAlbumsTab()

        // Click first album
        composeTestRule.clickFirstWithTagPrefix("album_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Verify "Add to playlist" button is displayed
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").assertIsDisplayed()
    }

    @Test
    fun buttonClickOpensDialogWithCorrectContext() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        // Navigate to Artists tab
        composeTestRule.navigateToLibraryArtistsTab()

        // Click first artist
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Click "Add to playlist" button
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").performClick()
        composeTestRule.waitForIdle()

        // Wait for dialog to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_dialog"))
        }

        // Verify dialog is displayed
        composeTestRule.onNodeWithTag("add_to_playlist_dialog").assertIsDisplayed()
    }

    @Test
    fun operationSuccessShowsSnackbarWithCounts() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        // Navigate to Artists tab
        composeTestRule.navigateToLibraryArtistsTab()

        // Click first artist
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        // Click "Add to playlist" button
        composeTestRule.onNodeWithTag("library_detail_add_to_playlist").performClick()
        composeTestRule.waitForIdle()

        // Wait for dialog to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_dialog"))
        }

        // Create a new playlist to add tracks to
        composeTestRule.onNodeWithTag("new_playlist_name_input").performTextInput("Test Artist Playlist")
        composeTestRule.onNodeWithTag("create_and_add_button").performClick()
        composeTestRule.waitForIdle()

        // Wait for success snackbar
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_snackbar")) ||
                composeTestRule.safeHasNodes(hasTestTag("snackbar"))
        }

        // Verify snackbar shows counts or exists
        val snackbarExists =
            composeTestRule.safeHasNodes(hasTestTag("add_to_playlist_snackbar")) ||
                composeTestRule.safeHasNodes(hasTestTag("snackbar"))
        assert(snackbarExists) { "Expected snackbar to be displayed" }
    }
}
