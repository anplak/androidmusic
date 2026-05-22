package com.anplak.androidmusic.ui

import android.Manifest
import android.os.Build
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
 * E2E tests for For You / recommendations discovery tab.
 */
@RunWith(AndroidJUnit4::class)
class ForYouE2ETest {

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
    fun forYouTab_isDefaultAndShowsContentOrEmpty() {
        composeTestRule.waitForAppReady()
        composeTestRule.waitForForYouSettled()

        val hasList = composeTestRule.safeHasNodes(hasTestTag("for_you_list"))
        val hasEmpty = composeTestRule.safeHasNodes(hasTestTag("for_you_empty"))
        assert(hasList || hasEmpty) {
            "For You tab should show recommendations or empty state"
        }
    }

    @Test
    fun forYouTab_refreshControlExists() {
        composeTestRule.waitForAppReady()
        composeTestRule.waitForForYouSettled()

        composeTestRule.onNodeWithTag("for_you_refresh").assertExists()
        composeTestRule.onNodeWithTag("for_you_refresh").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitForForYouSettled()
    }

    @Test
    fun forYouTab_navigateFromLibraryAndBack() {
        composeTestRule.waitForAppReady()
        composeTestRule.waitForForYouSettled()

        composeTestRule.navigateToLibrary()
        composeTestRule.navigateToForYou()
        composeTestRule.waitForForYouSettled()

        val onForYou = composeTestRule.safeHasNodes(hasTestTag("for_you_list")) ||
            composeTestRule.safeHasNodes(hasTestTag("for_you_empty"))
        assert(onForYou) { "Should return to For You tab" }
    }

    @Test
    fun forYouTab_seeAllOpensDetailWhenRowsExist() {
        composeTestRule.waitForAppReady()
        composeTestRule.waitForForYouSettled()

        if (!composeTestRule.safeHasNodes(hasTestTag("for_you_list"))) return
        if (!composeTestRule.safeHasNodes(hasTestTag("for_you_see_all"))) return

        composeTestRule.onNodeWithTag("for_you_see_all").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.safeHasNodes(hasTestTag("recommendation_detail_track_list")) ||
                composeTestRule.safeHasNodes(hasTestTag("recommendation_detail_empty"))
        }

        composeTestRule.onNodeWithTag("recommendation_detail_back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitForForYouSettled()
    }
}
