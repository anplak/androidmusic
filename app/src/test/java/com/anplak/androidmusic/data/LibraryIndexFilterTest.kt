package com.anplak.androidmusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIndexFilterTest {
    private val defaultPolicy = LibraryIndexPolicy()
    private val defaultArtist = "Some Artist"

    @Test
    fun `indexes track within duration under allowed path`() {
        assertTrue(
            LibraryIndexFilter.shouldIndex(
                "/storage/emulated/0/Music/song.mp3",
                5 * 60 * 1000L,
                defaultArtist,
                defaultPolicy,
            ),
        )
    }

    @Test
    fun `rejects track longer than max duration`() {
        assertFalse(
            LibraryIndexFilter.shouldIndex(
                "/storage/emulated/0/Music/podcast.mp3",
                15 * 60 * 1000L,
                defaultArtist,
                defaultPolicy,
            ),
        )
    }

    @Test
    fun `rejects path under excluded folder and nested subfolder`() {
        val policy =
            LibraryIndexPolicy(
                folderRules =
                    listOf(
                        FolderRule("/storage/emulated/0/Music/Podcasts", FolderRuleMode.EXCLUDE),
                    ),
            )
        assertFalse(
            LibraryIndexFilter.shouldIndex(
                "/storage/emulated/0/Music/Podcasts/show/episode.mp3",
                180_000L,
                defaultArtist,
                policy,
            ),
        )
    }

    @Test
    fun `whitelist only indexes paths under include roots`() {
        val policy =
            LibraryIndexPolicy(
                folderRules =
                    listOf(
                        FolderRule("/storage/emulated/0/Music", FolderRuleMode.INCLUDE),
                    ),
            )
        assertTrue(
            LibraryIndexFilter.shouldIndex(
                "/storage/emulated/0/Music/album/track.mp3",
                180_000L,
                defaultArtist,
                policy,
            ),
        )
        assertFalse(
            LibraryIndexFilter.shouldIndex(
                "/storage/emulated/0/Download/track.mp3",
                180_000L,
                defaultArtist,
                policy,
            ),
        )
    }

    @Test
    fun `normalizes trailing slashes and case for matching`() {
        val policy =
            LibraryIndexPolicy(
                folderRules =
                    listOf(
                        FolderRule("/Storage/Emulated/0/Music/", FolderRuleMode.EXCLUDE),
                    ),
            )
        assertFalse(
            LibraryIndexFilter.shouldIndex(
                "/storage/emulated/0/music/song.mp3",
                180_000L,
                defaultArtist,
                policy,
            ),
        )
    }

    @Test
    fun `allows track without path when no include rules`() {
        assertTrue(
            LibraryIndexFilter.shouldIndex("", 180_000L, defaultArtist, defaultPolicy),
        )
    }

    @Test
    fun `rejects track without path when include rules exist`() {
        val policy =
            LibraryIndexPolicy(
                folderRules =
                    listOf(
                        FolderRule("/storage/emulated/0/Music", FolderRuleMode.INCLUDE),
                    ),
            )
        assertFalse(
            LibraryIndexFilter.shouldIndex("", 180_000L, defaultArtist, policy),
        )
    }

    @Test
    fun `rejects track by excluded artist case insensitively`() {
        val policy =
            LibraryIndexPolicy(
                artistRules = listOf(ArtistRule("podcast host")),
            )
        assertEquals(
            IndexSkipReason.ARTIST,
            LibraryIndexFilter.skipReason(
                "/storage/emulated/0/Music/ep.mp3",
                180_000L,
                "Podcast Host",
                policy,
            ),
        )
        assertFalse(
            LibraryIndexFilter.shouldIndex(
                "/storage/emulated/0/Music/ep.mp3",
                180_000L,
                "PODCAST HOST",
                policy,
            ),
        )
    }

    @Test
    fun `folder rule wins over artist rule when path blocked`() {
        val policy =
            LibraryIndexPolicy(
                folderRules =
                    listOf(
                        FolderRule("/storage/emulated/0/Music", FolderRuleMode.INCLUDE),
                    ),
                artistRules = listOf(ArtistRule("blocked artist")),
            )
        assertEquals(
            IndexSkipReason.FOLDER,
            LibraryIndexFilter.skipReason(
                "/storage/emulated/0/Download/track.mp3",
                180_000L,
                "Blocked Artist",
                policy,
            ),
        )
    }

    @Test
    fun `duration skip wins when artist also blocklisted`() {
        val policy =
            LibraryIndexPolicy(
                artistRules = listOf(ArtistRule("podcast host")),
            )
        assertEquals(
            IndexSkipReason.DURATION,
            LibraryIndexFilter.skipReason(
                "/storage/emulated/0/Music/ep.mp3",
                15 * 60 * 1000L,
                "Podcast Host",
                policy,
            ),
        )
    }

    @Test
    fun `returns null skip reason for allowed track`() {
        assertNull(
            LibraryIndexFilter.skipReason(
                "/storage/emulated/0/Music/song.mp3",
                180_000L,
                "Artist",
                defaultPolicy,
            ),
        )
    }
}
