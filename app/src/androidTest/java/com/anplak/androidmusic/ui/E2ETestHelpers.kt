package com.anplak.androidmusic.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    waitUntil(timeoutMillis = 30_000) {
        safeHasNodes(hasTestTag("nav_foryou")) ||
            safeHasNodes(hasTestTag("permission_request")) ||
            safeHasNodes(hasTestTag("permission_rationale")) ||
            safeHasNodes(hasText("Grant Permission"))
    }

    if (safeHasNodes(hasText("Grant Permission"))) {
        onNodeWithText("Grant Permission").performClick()
        waitUntil(timeoutMillis = 30_000) {
            safeHasNodes(hasTestTag("nav_foryou")) ||
                safeHasNodes(hasTestTag("permission_rationale"))
        }
    }

    if (safeHasNodes(hasTestTag("permission_rationale"))) {
        onNodeWithTag("permission_rationale_grant").performClick()
        waitUntil(timeoutMillis = 30_000) {
            safeHasNodes(hasTestTag("nav_foryou"))
        }
    }
}

fun MainActivityComposeRule.navigateToLibrary() {
    onNodeWithTag("nav_library").performClick()
    waitForLibraryContent()
}

/** Waits until the Library tab list or empty state is visible. */
fun MainActivityComposeRule.waitForLibraryContent() {
    waitUntil(timeoutMillis = 15_000) {
        safeHasNodes(hasTestTag("track_list")) || safeHasNodes(hasTestTag("empty_state"))
    }
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
