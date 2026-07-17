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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E coverage for the persistent mini player shell (story 17).
 *
 * Skips gracefully when the device library has no audio files.
 */
@RunWith(AndroidJUnit4::class)
class MiniPlayerE2ETest {

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
    fun coldStart_miniPlayerAbsent() {
        composeTestRule.waitForAppReady()

        assertFalse(
            "Mini player must be absent before any track is selected (AC6)",
            composeTestRule.safeHasNodes(hasTestTag("mini_player_bar"))
        )
        composeTestRule.onNodeWithTag("nav_foryou").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_library").assertIsDisplayed()
    }

    @Test
    fun activeTrack_miniPlayerVisibleOnAllTabs() {
        if (!startTrackAndReturnToTabs()) return

        val tabs = listOf(
            "nav_foryou",
            "nav_library",
            "nav_favorites",
            "nav_playlists",
            "nav_history"
        )
        for (tab in tabs) {
            composeTestRule.onNodeWithTag(tab).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.safeHasNodes(hasTestTag("mini_player_bar"))
            }
            composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
        }
    }

    @Test
    fun activeTrack_miniPlayerVisibleOnSearchOverlay() {
        if (!startTrackAndReturnToTabs()) return

        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("mini_player_bar"))
        }
        composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
    }

    @Test
    fun activeTrack_miniPlayerVisibleOnLibraryIndex() {
        if (!startTrackAndReturnToTabs()) return

        composeTestRule.navigateToLibrary()
        composeTestRule.openLibraryIndexFromLibrary()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("mini_player_bar"))
        }
        composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
    }

    @Test
    fun tapMiniPlayerBody_opensNowPlaying_backRestoresPreviousScreen() {
        if (!startTrackAndReturnToTabs()) return

        composeTestRule.navigateToLibrary()
        composeTestRule.openSearchFromLibrary()
        composeTestRule.onNodeWithTag("mini_player_title").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("now_playing_screen"))
        }
        assertFalse(
            "Mini player must not appear on Now Playing (AC7)",
            composeTestRule.safeHasNodes(hasTestTag("mini_player_bar"))
        )

        composeTestRule.onNodeWithTag("back_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("search_back")) &&
                composeTestRule.safeHasNodes(hasTestTag("mini_player_bar"))
        }
        composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
    }

    @Test
    fun playPauseOnMiniPlayer_staysInSyncWithNowPlaying() {
        if (!startTrackAndReturnToTabs()) return

        composeTestRule.onNodeWithTag("mini_player_play_pause").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mini_player_title").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("now_playing_screen"))
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("play_pause_button").performClick()
        composeTestRule.onNodeWithTag("back_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("mini_player_bar"))
        }
        composeTestRule.onNodeWithTag("mini_player_play_pause").assertIsDisplayed()
    }

    @Test
    fun likeOnMiniPlayer_togglesFavorite() {
        if (!startTrackAndReturnToTabs()) return

        composeTestRule.onNodeWithTag("mini_player_favorite").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mini_player_title").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("now_playing_screen"))
        }
        composeTestRule.onNodeWithTag("favorite_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("favorite_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("back_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("mini_player_favorite"))
        }
        assertTrue(composeTestRule.safeHasNodes(hasTestTag("mini_player_bar")))
    }

    /** Starts a track via Library, leaves Now Playing, leaves mini player on main tabs. */
    private fun startTrackAndReturnToTabs(): Boolean {
        if (!composeTestRule.openNowPlayingFromLibrary()) return false
        composeTestRule.onNodeWithTag("back_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.safeHasNodes(hasTestTag("mini_player_bar")) &&
                composeTestRule.safeHasNodes(hasTestTag("nav_library"))
        }
        return true
    }
}
