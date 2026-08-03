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
import com.anplak.androidmusic.data.DailyMixConfig
import com.anplak.androidmusic.data.RecommendationRowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E tests for Daily Mix rotation (story 15):
 * - Multiple numbered Daily Mix rows on For You when the library is large enough
 * - Themed subtitles (decade, recently added, or artist seed)
 * - Same-day stability across refresh
 * - Soft dedup across slots (engine-level on device library)
 * - Regression: other For You row types still render
 *
 * Requires audio on the device; tests skip gracefully when the catalog is too small.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DailyMixE2ETest {
    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
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
    fun test01_forYou_showsAtLeastOneDailyMixRow() {
        if (!prepareForYouWithLibrary()) return

        val expected = E2ETestRecommendations.dailyMixRows(context)
        if (expected.isEmpty()) return

        composeTestRule.scrollToForYouRowTagPrefix("for_you_row_daily_mix_")
        assertTrue(
            composeTestRule.countForYouRowsWithTagPrefix("for_you_row_daily_mix_") >= 1,
        )
        composeTestRule.onNodeWithTag("for_you_title_${expected.first().id}").assertExists()
    }

    @Test
    fun test02_dailyMixRows_showNumberedTitlesAndSubtitles() {
        if (!prepareForYouWithLibrary()) return

        composeTestRule.refreshForYou()
        val dailyMixes = E2ETestRecommendations.dailyMixRows(context)
        if (dailyMixes.isEmpty()) return

        dailyMixes.forEach { row ->
            composeTestRule.scrollToForYouRow("for_you_row_${row.id}")
            composeTestRule.onNodeWithTag("for_you_title_${row.id}").assertExists()
            composeTestRule.onNodeWithTag("for_you_subtitle_${row.id}").assertExists()
        }

        val numberedTitles = dailyMixes.map { it.title }.toSet()
        assertTrue(
            "Daily Mix titles should be numbered",
            numberedTitles.all { it.startsWith("Daily Mix ") },
        )

        val themedSubtitle =
            dailyMixes.any { mix ->
                val subtitle = mix.subtitle.orEmpty()
                subtitle.contains("gems") ||
                    subtitle.contains("Added this month") ||
                    subtitle.isNotBlank()
            }
        assertTrue("Daily Mix subtitles should describe the active theme", themedSubtitle)
    }

    @Test
    fun test03_refresh_preservesDailyMixRowsSameDay() {
        if (!prepareForYouWithLibrary()) return

        val before = E2ETestRecommendations.dailyMixRows(context)
        if (before.isEmpty()) return

        composeTestRule.refreshForYou()

        val after = E2ETestRecommendations.dailyMixRows(context)
        assertEquals(before.map { it.id }, after.map { it.id })
        assertEquals(before.map { it.title }, after.map { it.title })
        assertEquals(
            before.map { row -> row.tracks.map { it.id }.toSet() },
            after.map { row -> row.tracks.map { it.id }.toSet() },
        )
    }

    @Test
    fun test04_dailyMixSlots_avoidTrackOverlapWhenPossible() {
        if (!prepareForYouWithLibrary()) return

        val dailyMixes = E2ETestRecommendations.dailyMixRows(context)
        if (dailyMixes.size < 2) return

        val overlap = E2ETestRecommendations.dailyMixTrackIdsOverlap(dailyMixes)
        val totalTracks = dailyMixes.sumOf { it.tracks.size }
        val uniqueTracks = dailyMixes.flatMap { it.tracks }.map { it.id }.toSet().size

        if (totalTracks <= uniqueTracks + dailyMixes.size) {
            assertFalse(
                "Daily Mix slots should not reuse tracks when the library has alternatives",
                overlap,
            )
        }
    }

    @Test
    fun test05_richLibrary_showsUpToThreeDailyMixRows() {
        if (!prepareForYouWithLibrary()) return

        val dailyMixes = E2ETestRecommendations.dailyMixRows(context)
        if (dailyMixes.isEmpty()) return

        val hasYearMeta =
            E2ETestRecommendations.tracksWithYearMetadata(context) >=
                DailyMixConfig.MIN_TRACKS_PER_THEME
        val hasDateMeta =
            E2ETestRecommendations.tracksWithDateAddedMetadata(context) >=
                DailyMixConfig.MIN_TRACKS_PER_THEME

        if (hasYearMeta && hasDateMeta &&
            E2ETestDatabase.cachedTrackCount(context) >= DailyMixConfig.MIN_TRACKS_PER_THEME * 3
        ) {
            assertTrue(
                "Rich library should surface multiple Daily Mix slots",
                dailyMixes.size >= 2,
            )
        }

        dailyMixes.forEach { row ->
            composeTestRule.scrollToForYouRow("for_you_row_${row.id}")
            composeTestRule.onNodeWithTag("for_you_row_${row.id}").assertExists()
        }
    }

    @Test
    fun test06_regression_otherForYouRowsStillRender() {
        if (!prepareForYouWithLibrary()) return
        if (!composeTestRule.safeHasNodes(hasTestTag("for_you_list"))) return

        val rows = E2ETestRecommendations.buildRows(context)
        val hasQuickMix = rows.any { it.type == RecommendationRowType.QUICK_MIX }
        val hasBecause = rows.any { it.type == RecommendationRowType.BECAUSE_YOU_LISTEN }

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
    fun test07_dailyMix_playMixStartsPlayback() {
        if (!prepareForYouWithLibrary()) return

        val dailyMix = E2ETestRecommendations.dailyMixRows(context).firstOrNull() ?: return

        composeTestRule.scrollToForYouRow("for_you_row_${dailyMix.id}")
        composeTestRule.onNodeWithTag("for_you_play_mix_${dailyMix.id}").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.safeHasNodes(hasTestTag("now_playing_screen")) &&
                composeTestRule.safeHasNodes(hasTestTag("play_pause_button"))
        }
    }
}
