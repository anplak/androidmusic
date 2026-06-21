package com.anplak.androidmusic.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.anplak.androidmusic.MainActivity

typealias MainActivityComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/**
 * Returns whether matching nodes exist without throwing when Compose is not ready yet.
 */
fun MainActivityComposeRule.safeHasNodes(matcher: SemanticsMatcher): Boolean {
    return try {
        onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    } catch (_: IllegalStateException) {
        false
    }
}

/**
 * Waits until the main app shell is composed (bottom nav or pre-grant permission UI).
 */
fun MainActivityComposeRule.waitForAppReady() {
    waitUntil(timeoutMillis = 60_000) {
        safeHasNodes(hasTestTag("nav_foryou")) ||
            safeHasNodes(hasTestTag("permission_request")) ||
            safeHasNodes(hasTestTag("permission_rationale")) ||
            safeHasNodes(hasText("Grant Permission"))
    }

    if (safeHasNodes(hasText("Grant Permission"))) {
        onNodeWithText("Grant Permission").performClick()
        waitUntil(timeoutMillis = 60_000) {
            safeHasNodes(hasTestTag("nav_foryou")) ||
                safeHasNodes(hasTestTag("permission_rationale"))
        }
    }

    if (safeHasNodes(hasTestTag("permission_rationale"))) {
        onNodeWithTag("permission_rationale_grant").performClick()
        waitUntil(timeoutMillis = 60_000) {
            safeHasNodes(hasTestTag("nav_foryou"))
        }
    }
}

/** Leaves Now Playing / detail screens and waits for bottom navigation. */
fun MainActivityComposeRule.returnToMainShell() {
    if (safeHasNodes(hasTestTag("back_button"))) {
        onNodeWithTag("back_button").performClick()
    }
    waitUntil(timeoutMillis = 15_000) {
        safeHasNodes(hasTestTag("nav_foryou")) || safeHasNodes(hasTestTag("nav_library"))
    }
}

fun MainActivityComposeRule.prepareLibraryTab() {
    waitForAppReady()
    navigateToLibrary()
}

fun MainActivityComposeRule.preparePlaylistsTab() {
    waitForAppReady()
    navigateToPlaylists()
}

fun MainActivityComposeRule.navigateToLibrary() {
    onNodeWithTag("nav_library").performClick()
    waitForLibraryContent()
}

/** Waits until the Library tab has loaded (list, empty, or filter UI). */
fun MainActivityComposeRule.waitForLibraryContent() {
    waitUntil(timeoutMillis = 15_000) {
        safeHasNodes(hasTestTag("track_list")) ||
            safeHasNodes(hasTestTag("empty_state")) ||
            safeHasNodes(hasTestTag("library_search_field"))
    }
}

fun MainActivityComposeRule.navigateToPlaylists() {
    onNodeWithTag("nav_playlists").performClick()
    waitUntil(timeoutMillis = 15_000) {
        safeHasNodes(hasTestTag("playlists_list")) ||
            safeHasNodes(hasTestTag("playlists_search_field"))
    }
}

fun MainActivityComposeRule.openSearchFromLibrary() {
    onNodeWithTag("open_search").performClick()
    waitForSearchScreen()
}

fun MainActivityComposeRule.openSearchFromPlaylists() {
    onNodeWithTag("open_search").performClick()
    waitForSearchScreen()
}

fun MainActivityComposeRule.openLibraryIndexFromLibrary() {
    onNodeWithTag("open_library_index").performClick()
    waitForLibraryIndexScreen()
}

fun MainActivityComposeRule.waitForLibraryIndexScreen() {
    waitUntil(timeoutMillis = 10_000) {
        safeHasNodes(hasTestTag("library_index"))
    }
}

fun MainActivityComposeRule.returnFromLibraryIndex() {
    onNodeWithTag("library_index_back").performClick()
    waitForLibraryContent()
}

/** Scan summary snackbar or settled library list after re-index. */
fun MainActivityComposeRule.waitForLibraryReindexSettled() {
    waitUntil(timeoutMillis = 30_000) {
        safeHasNodes(hasTestTag("scan_summary")) ||
            safeHasNodes(hasTestTag("track_list")) ||
            safeHasNodes(hasTestTag("empty_state"))
    }
}

/** Global search overlay is visible. */
fun MainActivityComposeRule.waitForSearchScreen() {
    waitUntil(timeoutMillis = 10_000) {
        safeHasNodes(hasTestTag("search_field"))
    }
}

/** Search finished: results, no results, or idle suggestions. */
fun MainActivityComposeRule.waitForSearchSettled() {
    waitUntil(timeoutMillis = 15_000) {
        safeHasNodes(hasTestTag("search_results")) ||
            safeHasNodes(hasTestTag("search_no_results")) ||
            safeHasNodes(hasTestTag("search_idle")) ||
            safeHasNodes(hasTestTag("search_error"))
    }
}

fun MainActivityComposeRule.typeInSearchField(text: String) {
    onNodeWithTag("search_field").performClick()
    onNodeWithTag("search_field").performTextClearance()
    onNodeWithTag("search_field").performTextInput(text)
}

fun MainActivityComposeRule.typeInLibrarySearchField(text: String) {
    onNodeWithTag("library_search_field").performClick()
    onNodeWithTag("library_search_field").performTextClearance()
    onNodeWithTag("library_search_field").performTextInput(text)
}

fun MainActivityComposeRule.typeInPlaylistsSearchField(text: String) {
    onNodeWithTag("playlists_search_field").performClick()
    onNodeWithTag("playlists_search_field").performTextClearance()
    onNodeWithTag("playlists_search_field").performTextInput(text)
}

fun MainActivityComposeRule.waitForHistorySettled() {
    waitUntil(timeoutMillis = 15_000) {
        safeHasNodes(hasTestTag("history_list")) ||
            safeHasNodes(hasTestTag("history_empty"))
    }
}

/** Returns true when at least one history entry is visible. */
fun MainActivityComposeRule.waitForHistoryList(timeoutMillis: Long = 15_000): Boolean {
    return try {
        waitUntil(timeoutMillis = timeoutMillis) {
            safeHasNodes(hasTestTag("history_list"))
        }
        true
    } catch (_: Throwable) {
        false
    }
}

fun MainActivityComposeRule.navigateToHistory() {
    onNodeWithTag("nav_history").performClick()
    waitForHistorySettled()
}

fun MainActivityComposeRule.navigateToFavorites() {
    onNodeWithTag("nav_favorites").performClick()
    waitForFavoritesSettled()
}

fun MainActivityComposeRule.waitForFavoritesSettled() {
    waitUntil(timeoutMillis = 15_000) {
        safeHasNodes(hasTestTag("favorites_track_list")) ||
            safeHasNodes(hasTestTag("favorites_empty_state"))
    }
}

fun MainActivityComposeRule.waitForFavoriteTrackList(timeoutMillis: Long = 15_000): Boolean {
    return try {
        waitUntil(timeoutMillis = timeoutMillis) {
            safeHasNodes(hasTestTag("favorites_track_list"))
        }
        true
    } catch (_: Throwable) {
        false
    }
}

private fun favoriteButtonMatcher(index: Int, favorited: Boolean): SemanticsMatcher {
    val description = if (favorited) "Remove from favorites" else "Add to favorites"
    return hasTestTag("favorite_button_$index").and(hasContentDescription(description))
}

/** Scrolls to the track row and taps its favorite button. */
fun MainActivityComposeRule.clickLibraryFavoriteAtIndex(index: Int) {
    onNodeWithTag("track_item_$index").performScrollTo()
    onNodeWithTag("favorite_button_$index").performClick()
    waitForIdle()
}

/** Toggles until the track at [index] matches [favorited] (handles persisted DB state). */
fun MainActivityComposeRule.setLibraryFavoriteAtIndex(index: Int, favorited: Boolean) {
    onNodeWithTag("track_item_$index").performScrollTo()
    waitUntil(timeoutMillis = 15_000) {
        safeHasNodes(hasTestTag("favorite_button_$index"))
    }
    val isFavorited = safeHasNodes(favoriteButtonMatcher(index, favorited = true))
    if (isFavorited != favorited) {
        clickLibraryFavoriteAtIndex(index)
    }
    waitForLibraryFavoriteAtIndex(index, favorited)
}

/** Waits until the library favorite button at [index] shows the expected state. */
fun MainActivityComposeRule.waitForLibraryFavoriteAtIndex(
    index: Int,
    favorited: Boolean,
    timeoutMillis: Long = 15_000
) {
    onNodeWithTag("track_item_$index").performScrollTo()
    waitUntil(timeoutMillis = timeoutMillis) {
        safeHasNodes(favoriteButtonMatcher(index, favorited))
    }
}

fun MainActivityComposeRule.assertLibraryFavoriteAtIndex(index: Int, favorited: Boolean) {
    onNodeWithTag("track_item_$index").performScrollTo()
    onNode(favoriteButtonMatcher(index, favorited)).assertIsDisplayed()
}

fun MainActivityComposeRule.navigateToForYou() {
    onNodeWithTag("nav_foryou").performClick()
    waitForIdle()
}

fun MainActivityComposeRule.waitForForYouSettled() {
    waitUntil(timeoutMillis = 30_000) {
        !safeHasNodes(hasTestTag("for_you_loading")) &&
            (
                safeHasNodes(hasTestTag("for_you_list")) ||
                    safeHasNodes(hasTestTag("for_you_empty")) ||
                    safeHasNodes(hasTestTag("for_you_error"))
                )
    }
}
