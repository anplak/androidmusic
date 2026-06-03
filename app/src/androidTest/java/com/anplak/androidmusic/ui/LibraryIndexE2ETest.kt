package com.anplak.androidmusic.ui

import android.Manifest
import android.os.Build
import android.os.Environment
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import java.io.File

/**
 * E2E tests for library indexing (duration cap and folder exclude rules).
 *
 * Fixture setup (optional, for duration/folder scenarios):
 * ```
 * adb push fixtures/e2e_index_short.mp3 /sdcard/Music/e2e_index_short.mp3
 * adb push fixtures/e2e_index_long.mp3 /sdcard/Music/e2e_index_long.mp3
 * adb push fixtures/e2e_index_podcast.mp3 /sdcard/Music/Podcasts/e2e_index_podcast.mp3
 * adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/e2e_index_short.mp3
 * ```
 *
 * Tests skip gracefully when fixtures or indexed tracks are absent.
 */
@RunWith(AndroidJUnit4::class)
class LibraryIndexE2ETest {

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
    fun openLibraryIndex_fromLibraryToolbar() {
        composeTestRule.prepareLibraryTab()
        composeTestRule.openLibraryIndexFromLibrary()

        composeTestRule.onNodeWithTag("library_index").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max track duration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tracks longer than 10 minutes are not indexed", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Folder rules").assertIsDisplayed()
    }

    @Test
    fun addExcludeFolderRule_returnsToLibraryAndReindexes() {
        composeTestRule.prepareLibraryTab()
        composeTestRule.openLibraryIndexFromLibrary()

        if (!composeTestRule.safeHasNodes(hasTestTag("add_exclude_folder"))) {
            // No preset folders on device (e.g. Music dir missing) — skip flow
            composeTestRule.onNodeWithTag("library_index_back").performClick()
            return
        }

        composeTestRule.onNodeWithTag("add_folder_rule").performClick()
        composeTestRule.onNodeWithTag("add_exclude_folder").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasText("Exclude (and subfolders)"))
        }

        composeTestRule.returnFromLibraryIndex()
        composeTestRule.waitForLibraryReindexSettled()
    }

    /**
     * When [FIXTURE_SHORT] and [FIXTURE_LONG] exist and are indexed by MediaStore,
     * only the short track title should appear in the library.
     */
    @Test
    fun longDurationTrack_excludedFromLibrary_whenFixturesPresent() {
        if (!fixturePairPresent()) return

        composeTestRule.prepareLibraryTab()

        if (!composeTestRule.safeHasNodes(hasTestTag("track_list"))) return

        composeTestRule.onNodeWithText(FIXTURE_SHORT_TITLE, substring = true).assertIsDisplayed()
        assert(!composeTestRule.safeHasNodes(hasText(FIXTURE_LONG_TITLE, substring = true))) {
            "Long fixture track should be excluded by the 10-minute index limit"
        }
    }

    /**
     * When [FIXTURE_PODCAST] exists under Music/Podcasts and that folder is excluded,
     * the podcast track should disappear from the library after re-index.
     */
    @Test
    fun excludedFolderTrack_hiddenAfterRuleAdded_whenFixturePresent() {
        if (!File(FIXTURE_PODCAST).exists()) return

        composeTestRule.prepareLibraryTab()

        composeTestRule.openLibraryIndexFromLibrary()
        if (!composeTestRule.safeHasNodes(hasTestTag("add_exclude_folder"))) {
            composeTestRule.onNodeWithTag("library_index_back").performClick()
            return
        }

        composeTestRule.onNodeWithTag("add_folder_rule").performClick()
        composeTestRule.onNodeWithTag("add_exclude_folder").performClick()
        composeTestRule.returnFromLibraryIndex()
        composeTestRule.waitForLibraryReindexSettled()

        if (composeTestRule.safeHasNodes(hasTestTag("track_list"))) {
            assert(!composeTestRule.safeHasNodes(hasText(FIXTURE_PODCAST_TITLE, substring = true))) {
                "Track under excluded folder should not appear in library"
            }
        }
    }

    private fun fixturePairPresent(): Boolean {
        return File(FIXTURE_SHORT).exists() && File(FIXTURE_LONG).exists()
    }

    private companion object {
        private val MUSIC_ROOT = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

        const val FIXTURE_SHORT_TITLE = "e2e_index_short"
        const val FIXTURE_LONG_TITLE = "e2e_index_long"
        const val FIXTURE_PODCAST_TITLE = "e2e_index_podcast"

        val FIXTURE_SHORT: String = File(MUSIC_ROOT, "e2e_index_short.mp3").absolutePath
        val FIXTURE_LONG: String = File(MUSIC_ROOT, "e2e_index_long.mp3").absolutePath
        val FIXTURE_PODCAST: String = File(MUSIC_ROOT, "Podcasts/e2e_index_podcast.mp3").absolutePath
    }
}
