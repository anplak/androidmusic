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
 * E2E tests for Library browse tabs (Tracks / Artists / Albums) and detail screens.
 *
 * Requires audio files on the device. Tests skip gracefully when the library is empty.
 */
@RunWith(AndroidJUnit4::class)
class LibraryBrowseE2ETest {
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

    @Test
    fun libraryBrowseTabs_allThreeVisibleWhenCatalogLoaded() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.onNodeWithTag("library_tab_tracks").assertIsDisplayed()
        composeTestRule.onNodeWithTag("library_tab_artists").assertIsDisplayed()
        composeTestRule.onNodeWithTag("library_tab_albums").assertIsDisplayed()
    }

    @Test
    fun libraryBrowseTabs_artistsTabShowsArtistListWithoutTrackFilters() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryArtistsTab()

        composeTestRule.onNodeWithTag("artist_list").assertIsDisplayed()
        assert(!composeTestRule.safeHasNodes(hasTestTag("library_search_field"))) {
            "Track filter bar should not appear on Artists tab"
        }
    }

    @Test
    fun libraryBrowseTabs_albumsTabShowsAlbumList() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryAlbumsTab()

        composeTestRule.onNodeWithTag("album_list").assertIsDisplayed()
        assert(!composeTestRule.safeHasNodes(hasTestTag("library_search_field"))) {
            "Track filter bar should not appear on Albums tab"
        }
    }

    @Test
    fun libraryBrowseTabs_tracksTabRestoresFilterBar() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryArtistsTab()
        composeTestRule.switchLibraryBrowseTab(LibraryBrowseTab.Tracks)

        composeTestRule.onNodeWithTag("library_search_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("track_list").assertIsDisplayed()
    }

    @Test
    fun libraryBrowseTabs_rapidSwitch_noCrash() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        repeat(5) {
            composeTestRule.switchLibraryBrowseTab(LibraryBrowseTab.Tracks)
            composeTestRule.switchLibraryBrowseTab(LibraryBrowseTab.Artists)
            composeTestRule.switchLibraryBrowseTab(LibraryBrowseTab.Albums)
        }

        composeTestRule.onNodeWithTag("library_tab_albums").assertIsDisplayed()
        composeTestRule.onNodeWithTag("album_list").assertIsDisplayed()
    }

    @Test
    fun artistDetail_openFromList_showsTracksAndPlayAll() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryArtistsTab()
        composeTestRule.clickFirstWithTagPrefix("artist_item_")

        composeTestRule.waitForLibraryDetailSettled()
        composeTestRule.onNodeWithTag("library_detail_play_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("library_detail_track_list").assertIsDisplayed()
    }

    @Test
    fun artistDetail_backReturnsToArtistsTab() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryArtistsTab()
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        composeTestRule.returnFromLibraryDetail()

        composeTestRule.onNodeWithTag("artist_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("library_tab_artists").assertIsDisplayed()
    }

    @Test
    fun artistDetail_tapTrack_navigatesToNowPlaying() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryArtistsTab()
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        if (!composeTestRule.safeHasNodes(hasTestTag("library_detail_track_item_0"))) return

        composeTestRule.onNodeWithTag("library_detail_track_item_0").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("play_pause_button"))
        }

        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("track_title").assertIsDisplayed()
    }

    @Test
    fun artistDetail_playAll_navigatesToNowPlaying() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryArtistsTab()
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        if (!composeTestRule.safeHasNodes(hasTestTag("library_detail_play_all"))) return

        composeTestRule.onNodeWithTag("library_detail_play_all").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("play_pause_button"))
        }

        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed()
    }

    @Test
    fun artistDetail_excludeMenuIsAvailable() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryArtistsTab()
        composeTestRule.clickFirstWithTagPrefix("artist_item_")
        composeTestRule.waitForLibraryDetailSettled()

        composeTestRule.onNodeWithTag("library_detail_overflow").performClick()
        composeTestRule.onNodeWithTag("exclude_artist_menu").assertIsDisplayed()
    }

    @Test
    fun albumDetail_openFromList_showsTracksAndPlayAll() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryAlbumsTab()
        composeTestRule.clickFirstWithTagPrefix("album_item_")

        composeTestRule.waitForLibraryDetailSettled()
        composeTestRule.onNodeWithTag("library_detail_play_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("library_detail_track_list").assertIsDisplayed()
    }

    @Test
    fun albumDetail_backReturnsToAlbumsTab() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryAlbumsTab()
        composeTestRule.clickFirstWithTagPrefix("album_item_")
        composeTestRule.waitForLibraryDetailSettled()

        composeTestRule.returnFromLibraryDetail()

        composeTestRule.onNodeWithTag("album_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("library_tab_albums").assertIsDisplayed()
    }

    @Test
    fun albumDetail_tapTrack_navigatesToNowPlaying() {
        composeTestRule.prepareLibraryTab()
        if (!composeTestRule.hasLibraryTracks()) return

        composeTestRule.navigateToLibraryAlbumsTab()
        composeTestRule.clickFirstWithTagPrefix("album_item_")
        composeTestRule.waitForLibraryDetailSettled()

        if (!composeTestRule.safeHasNodes(hasTestTag("library_detail_track_item_0"))) return

        composeTestRule.onNodeWithTag("library_detail_track_item_0").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.safeHasNodes(hasTestTag("play_pause_button"))
        }

        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed()
    }
}
