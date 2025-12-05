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

    /**
     * Test that bottom navigation is displayed with three tabs.
     */
    @Test
    fun bottomNavigation_displaysAllTabs() {
        composeTestRule.waitForIdle()
        
        // Wait for library to load
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
        
        // Verify navigation tabs exist
        composeTestRule
            .onNodeWithTag("nav_library")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("nav_favorites")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("nav_playlists")
            .assertIsDisplayed()
    }

    /**
     * Test navigation to favorites tab shows empty state initially.
     */
    @Test
    fun favoritesTab_showsEmptyStateInitially() {
        composeTestRule.waitForIdle()
        
        // Wait for library to load
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
        
        // Navigate to favorites
        composeTestRule
            .onNodeWithTag("nav_favorites")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify empty state is shown
        composeTestRule
            .onNodeWithTag("favorites_empty_state")
            .assertIsDisplayed()
    }

    /**
     * Test toggling favorite on a track from the library.
     * This test requires audio files on the device.
     */
    @Test
    fun toggleFavorite_fromLibrary() {
        composeTestRule.waitForIdle()
        
        // Wait for library to load
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
        
        if (hasTrackList) {
            // Toggle favorite on first track
            composeTestRule
                .onNodeWithTag("favorite_button_0")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Navigate to favorites
            composeTestRule
                .onNodeWithTag("nav_favorites")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Wait for favorites list to load
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                val hasContent = composeTestRule
                    .onAllNodes(hasTestTag("favorites_track_list"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
                val hasEmpty = composeTestRule
                    .onAllNodes(hasTestTag("favorites_empty_state"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
                hasContent || hasEmpty
            }
            
            // Verify track appears in favorites
            composeTestRule
                .onNodeWithTag("favorites_track_list")
                .assertIsDisplayed()
        }
    }
}

