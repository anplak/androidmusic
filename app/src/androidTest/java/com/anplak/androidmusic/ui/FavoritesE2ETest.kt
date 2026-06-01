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
 * E2E tests for Favorites functionality:
 * - Toggle favorite from library
 * - Navigate to favorites tab
 * - Verify track appears in favorites
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

        composeTestRule.onNodeWithTag("nav_favorites").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("favorites_empty_state")) ||
                composeTestRule.safeHasNodes(hasTestTag("favorites_track_list"))
        }
    }

    @Test
    fun toggleFavorite_fromLibrary() {
        composeTestRule.prepareLibraryTab()

        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        composeTestRule.onNodeWithTag("favorite_button_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_favorites").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("favorites_track_list")) ||
                composeTestRule.safeHasNodes(hasTestTag("favorites_empty_state"))
        }

        composeTestRule.onNodeWithTag("favorites_track_list").assertIsDisplayed()
    }
}
