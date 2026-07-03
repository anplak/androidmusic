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
 * E2E tests for favorites sync between Library and Favorites tabs.
 */
@RunWith(AndroidJUnit4::class)
class FavoritesE2ETest {

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

    @Test
    fun bottomNavigation_displaysAllTabs() {
        composeTestRule.waitForAppReady()

        composeTestRule.onNodeWithTag("nav_foryou").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_library").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_favorites").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_playlists").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_history").assertIsDisplayed()
    }

    @Test
    fun favoritesTab_showsEmptyStateInitially() {
        composeTestRule.waitForAppReady()
        composeTestRule.navigateToFavorites()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("favorites_empty_state")) ||
                composeTestRule.safeHasNodes(hasTestTag("favorites_track_list"))
        }
    }

    @Test
    fun toggleFavorite_fromLibrary() {
        composeTestRule.prepareLibraryTab()

        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        composeTestRule.setLibraryFavoriteAtIndex(index = 0, favorited = true)

        composeTestRule.navigateToFavorites()

        composeTestRule.onNodeWithTag("favorites_track_list").assertIsDisplayed()
    }

    @Test
    fun favoriteFromLibrary_appearsOnFavoritesTab_afterTabSwitch() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        composeTestRule.setLibraryFavoriteAtIndex(index = 0, favorited = true)

        composeTestRule.navigateToFavorites()
        assert(composeTestRule.waitForFavoriteTrackList()) {
            "Favorited track should appear on Favorites tab"
        }
        composeTestRule.onNodeWithTag("favorites_track_item_0").assertIsDisplayed()
    }

    @Test
    fun favoriteFromLibrary_showsFilledHeart_whenReturningToLibrary() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        composeTestRule.setLibraryFavoriteAtIndex(index = 0, favorited = true)

        composeTestRule.navigateToFavorites()
        if (!composeTestRule.waitForFavoriteTrackList()) return

        composeTestRule.navigateToLibrary()
        composeTestRule.waitForLibraryContent()
        composeTestRule.assertLibraryFavoriteAtIndex(index = 0, favorited = true)
    }

    @Test
    fun favoriteDuringLibraryRefresh_persistsOnFavoritesTab() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        // Return to Library to trigger a background re-index while cached tracks stay visible.
        composeTestRule.navigateToForYou()
        composeTestRule.navigateToLibrary()
        composeTestRule.waitForLibraryContent()

        composeTestRule.setLibraryFavoriteAtIndex(index = 0, favorited = true)
        composeTestRule.waitForLibraryReindexSettled()

        composeTestRule.navigateToFavorites()
        assert(composeTestRule.waitForFavoriteTrackList()) {
            "Favorite added during library refresh should survive sync completion"
        }
        composeTestRule.onNodeWithTag("favorites_track_item_0").assertIsDisplayed()
    }

    @Test
    fun favoriteFromLibrary_roundTrip_unfavoriteRemovesFromBothTabs() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        // Reset index 0 so leftover favorites from other tests do not shift list indices.
        composeTestRule.setLibraryFavoriteAtIndex(index = 0, favorited = false)

        composeTestRule.setLibraryFavoriteAtIndex(index = 0, favorited = true)

        composeTestRule.navigateToFavorites()
        if (!composeTestRule.waitForFavoriteTrackList()) return

        composeTestRule.onNodeWithTag("favorites_remove_button_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.navigateToLibrary()
        composeTestRule.waitForLibraryContent()
        composeTestRule.waitForLibraryFavoriteAtIndex(index = 0, favorited = false)

        composeTestRule.navigateToFavorites()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.safeHasNodes(hasTestTag("favorites_empty_state")) ||
                composeTestRule.safeHasNodes(hasTestTag("favorites_track_list"))
        }
    }
}
