package com.anplak.androidmusic.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.anplak.androidmusic.MainActivity
import com.anplak.androidmusic.data.MixDimensionConfig
import com.anplak.androidmusic.data.RecommendationRowType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E tests for metadata- and playlist-aware For You rows (story 16).
 * Requires audio on device; tests skip gracefully when the catalog is too small.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MetadataMixE2ETest {

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

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun prepareForYouWithLibrary(): Boolean {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return false
        composeTestRule.navigateToForYou()
        composeTestRule.waitForForYouSettled()
        return E2ETestRecommendations.libraryMeetsDailyMixMinimum(context)
    }

    @Test
    fun test01_forYou_showsGenreMixWhenFolderLibrary() {
        if (!prepareForYouWithLibrary()) return

        val genreMixes = E2ETestRecommendations.genreMixRows(context)
        if (genreMixes.isEmpty()) {
            if (!E2ETestRecommendations.libraryMeetsGenreMixMinimum(context)) return
        }
        if (genreMixes.isEmpty()) return

        val row = genreMixes.first()
        composeTestRule.scrollToForYouRow("for_you_row_${row.id}")
        composeTestRule.onNodeWithTag("for_you_title_${row.id}").assertExists()
        assertTrue(row.title.contains("mix", ignoreCase = true))
    }

    @Test
    fun test02_playlistAffinityRowWhenPlaylistsExist() {
        if (!prepareForYouWithLibrary()) return

        val playlistRows = E2ETestRecommendations.playlistAffinityRows(context)
        if (playlistRows.isEmpty()) return

        val row = playlistRows.first()
        composeTestRule.scrollToForYouRow("for_you_row_${row.id}")
        composeTestRule.onNodeWithTag("for_you_title_${row.id}").assertExists()
        assertTrue(row.title.contains("playlist", ignoreCase = true))
    }

    @Test
    fun test03_dimensionRow_seeAllOpensDetail() {
        if (!prepareForYouWithLibrary()) return
        if (!composeTestRule.safeHasNodes(hasTestTag("for_you_list"))) return

        val dimensionRow = E2ETestRecommendations.buildRows(context).firstOrNull {
            it.type == RecommendationRowType.GENRE_MIX ||
                it.type == RecommendationRowType.PLAYLIST_AFFINITY ||
                it.type == RecommendationRowType.LANGUAGE_MIX
        } ?: return

        composeTestRule.scrollToForYouRow("for_you_row_${dimensionRow.id}")
        composeTestRule.onNodeWithTag("for_you_see_all_${dimensionRow.id}").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.safeHasNodes(hasTestTag("recommendation_detail_track_list")) ||
                composeTestRule.safeHasNodes(hasTestTag("recommendation_detail_empty"))
        }
        composeTestRule.onNodeWithTag("recommendation_detail_back").performClick()
    }

    @Test
    fun test04_noLanguageRowWithoutTags() {
        if (!prepareForYouWithLibrary()) return

        if (E2ETestRecommendations.tracksWithLanguageMetadata(context) >=
            MixDimensionConfig.MIN_TRACKS_PER_BUCKET
        ) {
            return
        }

        val languageRows = E2ETestRecommendations.languageMixRows(context)
        assertTrue(languageRows.isEmpty())
        assertFalse(composeTestRule.safeHasNodes(hasTestTagPrefix("for_you_row_language_mix_")))
    }

    @Test
    fun test05_regression_dailyMixAndQuickMixStillRender() {
        if (!prepareForYouWithLibrary()) return
        if (!composeTestRule.safeHasNodes(hasTestTag("for_you_list"))) return

        val rows = E2ETestRecommendations.buildRows(context)
        val hasDailyMix = rows.any { it.type == RecommendationRowType.DAILY_MIX }
        val hasQuickMix = rows.any { it.type == RecommendationRowType.QUICK_MIX }
        val hasBecause = rows.any { it.type == RecommendationRowType.BECAUSE_YOU_LISTEN }

        if (hasDailyMix) {
            composeTestRule.scrollToForYouRowTagPrefix("for_you_row_daily_mix_")
            assertTrue(composeTestRule.safeHasNodes(hasTestTagPrefix("for_you_row_daily_mix_")))
        }
        if (hasQuickMix) {
            composeTestRule.scrollToForYouRowTagPrefix("for_you_row_quick_mix_")
            assertTrue(composeTestRule.safeHasNodes(hasTestTagPrefix("for_you_row_quick_mix_")))
        }
        if (hasBecause) {
            composeTestRule.scrollToForYouRowTagPrefix("for_you_row_because_")
            assertTrue(composeTestRule.safeHasNodes(hasTestTagPrefix("for_you_row_because_")))
        }
    }

    @Test
    fun test06_dimensionRow_playMixStartsPlayback() {
        if (!prepareForYouWithLibrary()) return

        val dimensionRow = E2ETestRecommendations.buildRows(context).firstOrNull {
            it.type == RecommendationRowType.GENRE_MIX ||
                it.type == RecommendationRowType.PLAYLIST_AFFINITY
        } ?: return

        composeTestRule.scrollToForYouRow("for_you_row_${dimensionRow.id}")
        composeTestRule.onNodeWithTag("for_you_play_mix_${dimensionRow.id}").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.safeHasNodes(hasTestTag("now_playing_screen")) &&
                composeTestRule.safeHasNodes(hasTestTag("play_pause_button"))
        }
    }
}
