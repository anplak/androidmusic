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
 * E2E tests for Story 9: search and filter experience.
 *
 * Designed for on-device runs (Wi-Fi adb). Tests are tolerant when the device has no
 * local audio: they assert UI wiring and skip playback-dependent steps when empty.
 *
 * Run on a connected phone:
 *   ./scripts/run-e2e-search-filter-wifi.sh
 */
@RunWith(AndroidJUnit4::class)
class SearchAndFilterE2ETest {

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

    // --- Library filters (FR4) ---

    @Test
    fun libraryTab_showsSearchAndFilterControls() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()

        composeTestRule.onNodeWithTag("open_search").assertExists()
        composeTestRule.onNodeWithTag("library_search_field").assertExists()
        composeTestRule.onNodeWithTag("library_filter_chips").assertExists()
        composeTestRule.onNodeWithTag("library_filter_favorites").assertExists()
    }

    @Test
    fun libraryTab_favoritesFilter_whenTracksExist() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()

        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        composeTestRule.onNodeWithTag("library_filter_favorites").performClick()
        composeTestRule.waitForIdle()

        val hasFilteredList = composeTestRule.safeHasNodes(hasTestTag("track_list"))
        val hasNoMatches = composeTestRule.safeHasNodes(hasTestTag("library_no_filter_results"))
        assert(hasFilteredList || hasNoMatches) {
            "Favorites filter should show tracks or no-match state"
        }
    }

    @Test
    fun libraryTab_localSearch_whenTracksExist() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()

        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        composeTestRule.typeInLibrarySearchField("zzz_no_match_query_zzz")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("library_no_filter_results")) ||
                composeTestRule.safeHasNodes(hasTestTag("track_list"))
        }
        assert(composeTestRule.safeHasNodes(hasTestTag("library_no_filter_results"))) {
            "Nonsense query should yield no matching tracks"
        }
    }

    // --- Global search (FR1–FR3) ---

    @Test
    fun library_openSearch_showsSearchScreen() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        composeTestRule.onNodeWithTag("search_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_back").assertExists()
    }

    @Test
    fun search_backReturnsToLibraryTab() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        composeTestRule.onNodeWithTag("search_back").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_library").assertExists()
        composeTestRule.onNodeWithTag("library_search_field").assertExists()
    }

    @Test
    fun search_idleOrSuggestions_whenOpened() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.safeHasNodes(hasTestTag("search_idle")) ||
                composeTestRule.safeHasNodes(hasTestTag("search_loading"))
        }
    }

    @Test
    fun search_typeQuery_showsResultsOrNoResults() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        composeTestRule.typeInSearchField("a")
        composeTestRule.waitForSearchSettled()

        val hasResults = composeTestRule.safeHasNodes(hasTestTag("search_results"))
        val hasEmpty = composeTestRule.safeHasNodes(hasTestTag("search_no_results"))
        assert(hasResults || hasEmpty) {
            "Search should settle to results or empty state"
        }
    }

    @Test
    fun search_tapTrackResult_startsPlaybackWhenResultsExist() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()

        composeTestRule.typeInSearchField("a")
        composeTestRule.waitForSearchSettled()

        when {
            composeTestRule.safeHasNodes(hasTestTag("search_result_track")) -> {
                composeTestRule.onAllNodes(hasTestTag("search_result_track"))[0].performClick()
            }
            composeTestRule.safeHasNodes(hasTestTag("search_result_history")) -> {
                composeTestRule.onAllNodes(hasTestTag("search_result_history"))[0].performClick()
            }
            else -> return
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.safeHasNodes(hasTestTag("play_pause_button"))
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed()
    }

    // --- Playlists search entry ---

    @Test
    fun playlistsTab_showsSearchFieldAndIcon() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToPlaylists()

        composeTestRule.onNodeWithTag("open_search").assertExists()
        composeTestRule.onNodeWithTag("playlists_search_field").assertExists()
    }

    @Test
    fun playlists_openSearch_showsSearchScreen() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToPlaylists()
        composeTestRule.openSearchFromPlaylists()

        composeTestRule.onNodeWithTag("search_field").assertIsDisplayed()
    }

    @Test
    fun playlists_filterByName_whenUserPlaylistsExist() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToPlaylists()

        if (!composeTestRule.safeHasNodes(hasTestTag("user_playlists_header"))) return

        composeTestRule.typeInPlaylistsSearchField("zzz_no_playlist_zzz")
        composeTestRule.waitForIdle()

        if (!composeTestRule.safeHasNodes(hasTestTag("playlist_item_0"))) return

        assert(!composeTestRule.safeHasNodes(hasTestTag("playlist_item_0"))) {
            "Nonsense playlist filter should hide the first user playlist row"
        }
    }
}
