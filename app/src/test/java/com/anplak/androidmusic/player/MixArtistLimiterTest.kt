package com.anplak.androidmusic.player

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class MixArtistLimiterTest {
    private val zone = ZoneOffset.UTC

    @Test
    fun `twenty five slot mix keeps at most twelve of the dominant artist`() {
        val artistA = (1L..20L).map { track(it, "Artist A") }
        val others = (21L..40L).map { track(it, "Artist $it") }
        val result =
            MixArtistLimiter.limit(
                preferred = artistA,
                candidates = artistA + others,
                limit = 25,
                randomTierOrder = others,
                leadArtist = "Artist A",
            )

        assertEquals(25, result.size)
        assertEquals(12, result.count { it.artist == "Artist A" })
    }

    @Test
    fun `five song preferred list is not a strict majority`() {
        val preferred = (1L..5L).map { track(it, "Artist A") }
        val others = (6L..10L).map { track(it, "Artist $it") }
        val result =
            MixArtistLimiter.limit(
                preferred = preferred,
                candidates = preferred + others,
                limit = 5,
                randomTierOrder = others,
            )

        assertEquals(5, result.size)
        assertTrue(result.count { it.artist == "Artist A" } <= 2)
    }

    @Test
    fun `odd length with one other artist shrinks below a strict majority`() {
        val preferred = (1L..5L).map { track(it, "Artist A") }
        val other = track(6, "Artist B")
        val result =
            MixArtistLimiter.limit(
                preferred = preferred,
                candidates = preferred + other,
                limit = 5,
                randomTierOrder = listOf(other),
            )

        assertEquals(2, result.size)
        assertEquals(1, result.count { it.artist == "Artist A" })
        assertEquals(1, result.count { it.artist == "Artist B" })
    }

    @Test
    fun `target of two does not repeat a named artist`() {
        val first = track(1, "Artist A")
        val second = track(2, "Artist A")
        val other = track(3, "Artist B")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(first, second),
                candidates = listOf(first, second, other),
                limit = 2,
            )

        assertEquals(2, result.size)
        assertEquals(setOf("artist a", "artist b"), result.map { MixArtistLimiter.artistKey(it.artist) }.toSet())
    }

    @Test
    fun `single artist library may fill the mix`() {
        val tracks = (1L..10L).map { track(it, "Artist A") }
        val result = MixArtistLimiter.limit(preferred = tracks, candidates = tracks, limit = 10)

        assertEquals(10, result.size)
        assertTrue(result.all { it.artist == "Artist A" })
    }

    @Test
    fun `limit of one keeps that song`() {
        val only = track(1, "Artist A")
        val result = MixArtistLimiter.limit(preferred = listOf(only), candidates = listOf(only, track(2, "Artist B")), limit = 1)

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `blank artists do not share a bucket`() {
        val named = track(1, "Artist A")
        val blanks = (2L..5L).map { track(it, "   ") }
        val other = track(6, "Artist B")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(named),
                candidates = listOf(named) + blanks + other,
                limit = 4,
                randomTierOrder = blanks + other,
            )

        assertEquals(listOf(1L, 2L, 3L, 4L), result.map { it.id })
        assertTrue(result.none { it.id == 6L })
    }

    @Test
    fun `blank candidate library waives the cap`() {
        val preferred = (1L..5L).map { track(it, "Artist A") }
        val blanks = (6L..8L).map { track(it, "") }
        val result = MixArtistLimiter.limit(preferred = preferred, candidates = blanks, limit = 5)

        assertEquals(5, result.count { it.artist == "Artist A" })
    }

    @Test
    fun `artist names match ignoring case`() {
        val first = track(1, "Abba")
        val second = track(2, "ABBA")
        val other = track(3, "Other")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(first, second),
                candidates = listOf(first, second, other),
                limit = 2,
                randomTierOrder = listOf(other),
            )

        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `unknown artist is one named artist`() {
        val first = track(1, "Unknown Artist")
        val second = track(2, "Unknown Artist")
        val other = track(3, "Someone")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(first, second),
                candidates = listOf(first, second, other),
                limit = 2,
                randomTierOrder = listOf(other),
            )

        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `fillers follow playlist then decade then add time then random`() {
        val seed = track(id = 1, artist = "Artist A", year = 1991, dateAddedSec = epochSec(2020, 5, 10))
        val playlistSibling =
            track(id = 40, artist = "Playlist Artist", year = 2005, dateAddedSec = epochSec(2010, 1, 1))
        val decade = track(id = 30, artist = "Decade Artist", year = 1994)
        val sameMonth = track(id = 50, artist = "Month Artist", dateAddedSec = epochSec(2020, 5, 20))
        val unrelated = track(id = 21, artist = "Unrelated Artist")
        val candidates = listOf(seed, unrelated, decade, playlistSibling, sameMonth)

        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = candidates,
                limit = 5,
                playlistIdsByTrack = mapOf(1L to setOf(7L), 40L to setOf(7L)),
                randomTierOrder = candidates.sortedBy { it.id },
                zone = zone,
            )

        assertEquals(listOf(1L, 40L, 30L, 50L, 21L), result.map { it.id })
    }

    @Test
    fun `decade filler is used when playlist overlap is absent`() {
        val seed = track(id = 1, artist = "Artist A", year = 1991)
        val decade = track(id = 30, artist = "Decade Artist", year = 1994)
        val unrelated = track(id = 21, artist = "Unrelated Artist")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, unrelated, decade),
                limit = 2,
                randomTierOrder = listOf(unrelated, decade),
                zone = zone,
            )

        assertEquals(listOf(1L, 30L), result.map { it.id })
    }

    @Test
    fun `add time filler is used when year is missing`() {
        val seed = track(id = 1, artist = "Artist A", dateAddedSec = epochSec(2020, 5, 10))
        val sameMonth = track(id = 50, artist = "Month Artist", dateAddedSec = epochSec(2020, 5, 20))
        val unrelated = track(id = 21, artist = "Unrelated Artist")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, unrelated, sameMonth),
                limit = 2,
                randomTierOrder = listOf(unrelated, sameMonth),
                zone = zone,
            )

        assertEquals(listOf(1L, 50L), result.map { it.id })
    }

    @Test
    fun `random filler is used when similarity metadata is missing`() {
        val seed = track(1, "Artist A")
        val unrelated = track(40, "Unrelated Artist")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, unrelated),
                limit = 2,
                randomTierOrder = listOf(unrelated),
                zone = zone,
            )

        assertEquals(listOf(1L, 40L), result.map { it.id })
    }

    @Test
    fun `same day does not fall through to the same month`() {
        val seed = track(id = 1, artist = "Artist A", dateAddedSec = epochSec(2020, 5, 10))
        val sameDay = track(id = 2, artist = "Day Artist", dateAddedSec = epochSec(2020, 5, 10))
        val sameMonth = track(id = 3, artist = "Month Artist", dateAddedSec = epochSec(2020, 5, 20))
        val unrelated = track(id = 4, artist = "Unrelated Artist")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, sameDay, sameMonth, unrelated),
                limit = 3,
                randomTierOrder = listOf(unrelated, sameMonth),
                zone = zone,
            )

        assertEquals(listOf(1L, 2L, 4L), result.map { it.id })
    }

    @Test
    fun `same month is used when no same day candidate exists`() {
        val seed = track(id = 1, artist = "Artist A", dateAddedSec = epochSec(2020, 6, 1))
        val sameMonth = track(id = 3, artist = "Month Artist", dateAddedSec = epochSec(2020, 6, 18))
        val unrelated = track(id = 4, artist = "Unrelated Artist")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, sameMonth, unrelated),
                limit = 2,
                randomTierOrder = listOf(unrelated, sameMonth),
                zone = zone,
            )

        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `null year is not chosen for the decade tier`() {
        val seed = track(id = 1, artist = "Artist A", year = 1991)
        val missingYear = track(id = 2, artist = "Missing Year")
        val decade = track(id = 9, artist = "Decade Artist", year = 1998)
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, missingYear, decade),
                limit = 2,
                randomTierOrder = listOf(missingYear, decade),
                zone = zone,
            )

        assertEquals(listOf(1L, 9L), result.map { it.id })
    }

    @Test
    fun `null date added is not chosen for the add time tier`() {
        val seed = track(id = 1, artist = "Artist A", dateAddedSec = epochSec(2020, 5, 10))
        val missingDate = track(id = 2, artist = "Missing Date")
        val sameDay = track(id = 9, artist = "Day Artist", dateAddedSec = epochSec(2020, 5, 10))
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, missingDate, sameDay),
                limit = 2,
                randomTierOrder = listOf(missingDate, sameDay),
                zone = zone,
            )

        assertEquals(listOf(1L, 9L), result.map { it.id })
    }

    @Test
    fun `random tier order is stable and not sorted by id`() {
        val seed = track(1, "Artist A")
        val later = track(8, "Later Artist")
        val earlier = track(3, "Earlier Artist")
        val first =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, earlier, later),
                limit = 3,
                randomTierOrder = listOf(later, earlier),
            )
        val second =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, earlier, later),
                limit = 3,
                randomTierOrder = listOf(later, earlier),
            )

        assertEquals(listOf(1L, 8L, 3L), first.map { it.id })
        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun `shrink drops the latest over cap tracks`() {
        val artistA = (1L..12L).map { track(it, "Artist A") }
        val other = track(13, "Artist B")
        val result =
            MixArtistLimiter.limit(
                preferred = artistA,
                candidates = artistA + other,
                limit = 25,
                randomTierOrder = listOf(other),
            )

        assertEquals(listOf(1L, 13L), result.map { it.id })
    }

    @Test
    fun `preferred walk skips an artist at the cap and keeps a later artist`() {
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(track(1, "Artist A"), track(2, "Artist A"), track(3, "Artist B")),
                candidates = listOf(track(1, "Artist A"), track(2, "Artist A"), track(3, "Artist B")),
                limit = 2,
            )

        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `empty input or non positive limit returns empty`() {
        assertTrue(MixArtistLimiter.limit(emptyList(), emptyList(), 5).isEmpty())
        assertTrue(MixArtistLimiter.limit(listOf(track(1, "Artist A")), emptyList(), 0).isEmpty())
    }

    @Test
    fun `tracks without genre are ordered by the random tier`() {
        val seed = track(1, "Artist A")
        val lowId = track(2, "Low Artist")
        val highId = track(9, "High Artist")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed),
                candidates = listOf(seed, lowId, highId),
                limit = 3,
                randomTierOrder = listOf(highId, lowId),
            )

        assertEquals(listOf(1L, 9L, 2L), result.map { it.id })
    }

    @Test
    fun `lead takes half and the rest are distinct artists interleaved`() {
        val lead = (1L..10L).map { track(it, "Lead") }
        val repeated = (11L..20L).map { track(it, "Repeat") }
        val unique = (21L..30L).map { track(it, "Artist $it") }
        val result =
            MixArtistLimiter.limit(
                preferred = lead,
                candidates = lead + repeated + unique,
                limit = 10,
                randomTierOrder = repeated + unique,
                leadArtist = "Lead",
            )

        assertEquals(listOf(1L, 11L, 2L, 21L, 3L, 22L, 4L, 23L, 5L, 24L), result.map { it.id })
        assertEquals(5, result.count { it.artist == "Lead" })
        assertEquals(1, result.count { it.artist == "Repeat" })
    }

    @Test
    fun `second pass repeats an artist on a different album`() {
        val first = track(1, "A", album = "One")
        val sameAlbum = track(2, "A", album = "One")
        val otherAlbum = track(3, "A", album = "Two")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(first, sameAlbum, otherAlbum, track(4, "B"), track(5, "C")),
                candidates = listOf(first, sameAlbum, otherAlbum, track(4, "B"), track(5, "C")),
                limit = 4,
            )

        assertEquals(listOf(1L, 4L, 5L, 3L), result.map { it.id })
    }

    @Test
    fun `no lead preferred pool does not take half before other artists`() {
        val preferred = (1L..10L).map { track(it, "Artist A") }
        val others = (11L..20L).map { track(it, "Artist $it") }
        val result =
            MixArtistLimiter.limit(
                preferred = preferred,
                candidates = preferred + others,
                limit = 10,
                randomTierOrder = others,
            )

        assertEquals(1, result.count { it.artist == "Artist A" })
        assertEquals(10, result.map { it.artist }.distinct().size)
    }

    @Test
    fun `single artist library spreads across albums`() {
        val tracks =
            listOf(
                track(1, "A", album = "One"),
                track(2, "A", album = "One"),
                track(3, "A", album = "Two"),
                track(4, "A", album = "Unknown Album"),
                track(5, "A", album = "Unknown Album"),
                track(6, "A", album = " "),
                track(7, "A", album = ""),
            )
        val spread = MixArtistLimiter.limit(preferred = tracks, candidates = tracks, limit = 7)
        val byId =
            listOf(
                track(1, "A", album = "X", albumId = 10),
                track(2, "A", album = "Y", albumId = 10),
                track(3, "A", album = "Y", albumId = 11),
            )

        assertEquals(listOf(1L, 3L, 4L, 6L, 7L, 2L, 5L), spread.map { it.id })
        assertEquals(
            listOf(1L, 3L, 2L),
            MixArtistLimiter.limit(preferred = byId, candidates = byId, limit = 3).map { it.id },
        )
    }

    @Test
    fun `unacceptable same day tracks fall through to the same month`() {
        val seed = track(id = 1, artist = "Artist A", dateAddedSec = epochSec(2020, 5, 10))
        val sameDay = track(id = 2, artist = "Artist A", dateAddedSec = epochSec(2020, 5, 10))
        val sameMonth = track(id = 3, artist = "Month Artist", dateAddedSec = epochSec(2020, 5, 20))
        val unrelated = track(id = 4, artist = "Unrelated Artist")
        val result =
            MixArtistLimiter.limit(
                preferred = listOf(seed, sameDay),
                candidates = listOf(seed, sameDay, sameMonth, unrelated),
                limit = 3,
                randomTierOrder = listOf(unrelated, sameMonth),
                zone = zone,
            )

        assertEquals(listOf(1L, 3L, 4L), result.map { it.id })
    }

    private fun track(
        id: Long,
        artist: String,
        year: Int? = null,
        dateAddedSec: Long? = null,
        album: String = "",
        albumId: Long? = null,
    ) = TrackInfo(
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = "Track $id",
        artist = artist,
        album = album,
        duration = 180_000L,
        year = year,
        dateAddedSec = dateAddedSec,
        albumId = albumId,
    )

    private fun epochSec(
        year: Int,
        month: Int,
        day: Int,
    ): Long = LocalDate.of(year, month, day).atStartOfDay(zone).toEpochSecond()
}
