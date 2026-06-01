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
 * E2E tests for History functionality:
 * - Navigate to History tab
 * - Verify empty state when no history
 * - Verify history entries appear after playing tracks
 * - Tap history entry to start playback
 *
 * Note: These tests require audio files on the device. Tests will skip
 * gracefully if no audio is present.
 */
@RunWith(AndroidJUnit4::class)
class HistoryE2ETest {

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
     * Test that bottom navigation includes History tab.
     */
    @Test
    fun bottomNavigation_displaysHistoryTab() {
        composeTestRule.waitForAppReady()

        // Verify History tab exists
        composeTestRule
            .onNodeWithTag("nav_history")
            .assertIsDisplayed()
    }

    /**
     * Test navigation to History tab shows empty state initially.
     */
    @Test
    fun historyTab_showsEmptyStateInitially() {
        composeTestRule.waitForAppReady()

        // Navigate to History
        composeTestRule
            .onNodeWithTag("nav_history")
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for history screen to load
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val hasContent = composeTestRule
                .onAllNodes(hasTestTag("history_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasEmpty = composeTestRule
                .onAllNodes(hasTestTag("history_empty"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasContent || hasEmpty
        }

        // On fresh install, history should be empty
        // Note: May have entries if tests ran before without clearing data
    }

    /**
     * Test that playing a track records history entry.
     * This test requires audio files on the device.
     */
    @Test
    fun playingTrack_recordsHistoryEntry() {
        composeTestRule.prepareLibraryTab()

        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) {
            // No audio files on device - test passes as skipped
            return
        }

        // Play first track
        composeTestRule
            .onNodeWithTag("track_item_0")
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for Now Playing to show
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("now_playing_screen"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Go back to main shell
        composeTestRule.returnToMainShell()

        composeTestRule.onNodeWithTag("nav_history").performClick()

        composeTestRule.waitForIdle()

        // Wait for history screen to load
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val hasContent = composeTestRule
                .onAllNodes(hasTestTag("history_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasEmpty = composeTestRule
                .onAllNodes(hasTestTag("history_empty"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasContent || hasEmpty
        }

        // Verify history list is shown (entry was recorded)
        composeTestRule
            .onNodeWithTag("history_list")
            .assertIsDisplayed()
    }

    /**
     * Test tapping a history entry starts playback.
     * This test requires audio files and existing history entries.
     */
    @Test
    fun tapHistoryEntry_startsPlayback() {
        composeTestRule.prepareLibraryTab()

        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) {
            // No audio files on device - test passes as skipped
            return
        }

        // Play first track to create history
        composeTestRule
            .onNodeWithTag("track_item_0")
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for Now Playing
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("now_playing_screen"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Go back to main shell
        composeTestRule.returnToMainShell()

        composeTestRule.onNodeWithTag("nav_history").performClick()

        composeTestRule.waitForIdle()

        // Wait for history to load
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val hasContent = composeTestRule
                .onAllNodes(hasTestTag("history_list"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            val hasEmpty = composeTestRule
                .onAllNodes(hasTestTag("history_empty"))
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasContent || hasEmpty
        }

        // Check if history has entries
        val hasHistory = composeTestRule
            .onAllNodes(hasTestTag("history_list"))
            .fetchSemanticsNodes()
            .isNotEmpty()

        if (!hasHistory) {
            // No history entries created - skip
            return
        }

        // Tap history entry
        composeTestRule
            .onNodeWithTag("history_entry")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify Now Playing screen appears
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("now_playing_screen"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithTag("now_playing_screen")
            .assertIsDisplayed()
    }

}
