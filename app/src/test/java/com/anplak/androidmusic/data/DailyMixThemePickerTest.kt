package com.anplak.androidmusic.data

import android.net.Uri
import com.anplak.androidmusic.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class DailyMixThemePickerTest {

    private val epochDay = 20_000L
    private val zoneId = ZoneId.systemDefault()

    @Test
    fun `pickAll is stable for same epoch day`() {
        val library = richLibrary()
        val first = DailyMixThemePicker.pickAll(library, epochDay)
        val second = DailyMixThemePicker.pickAll(library, epochDay)
        assertEquals(first, second)
    }

    @Test
    fun `pickAll returns three slots by default`() {
        val selections = DailyMixThemePicker.pickAll(richLibrary(), epochDay)
        assertEquals(3, selections.size)
        assertEquals(listOf(0, 1, 2), selections.map { it.slot })
    }

    @Test
    fun `pickAll supports custom slot count`() {
        val selections = DailyMixThemePicker.pickAll(
            library = richLibrary(),
            epochDay = epochDay,
            slotCount = 4
        )
        assertEquals(4, selections.size)
    }

    @Test
    fun `decade bucket below minimum is not eligible`() {
        val library = (1L..9L).map { track(it, year = 1985) }
        assertTrue(DailyMixThemePicker.eligibleDecades(library).isEmpty())
    }

    @Test
    fun `decade pool contains only selected decade`() {
        val library = (1L..12L).map { track(it, year = 1985) }
        val selection = DailyMixThemePicker.pickAll(library, epochDay).first {
            it.theme == DailyMixTheme.DECADE
        }
        val pool = DailyMixThemePicker.poolFor(selection, library, epochDay)
        assertEquals(12, pool.size)
        assertTrue(pool.all { DailyMixThemePicker.decadeOf(it.year!!) == selection.decadeStart })
    }

    @Test
    fun `recent pool uses dateAddedSec not play metadata`() {
        val monthStart = DailyMixConfig.monthStartEpochDay(epochDay)
        val inMonth = monthStartEpochSecond(epochDay)
        val beforeMonth = LocalDate.ofEpochDay(monthStart - 1)
            .atStartOfDay(zoneId)
            .toEpochSecond()
        val library = listOf(
            track(1, dateAddedSec = inMonth),
            track(2, dateAddedSec = beforeMonth)
        )
        val pool = DailyMixThemePicker.recentPool(library, epochDay)
        assertEquals(1, pool.size)
        assertEquals(1L, pool.first().id)
    }

    @Test
    fun `different epoch days may change selections`() {
        val library = richLibrary()
        val dayA = DailyMixThemePicker.pickAll(library, epochDay)
        val dayB = DailyMixThemePicker.pickAll(library, epochDay + 1)
        assertNotEquals(dayA, dayB)
    }

    @Test
    fun `decade label formats short centuries`() {
        assertEquals("80s", DailyMixThemePicker.decadeLabel(1980))
        assertEquals("00s", DailyMixThemePicker.decadeLabel(2000))
        assertEquals("10s", DailyMixThemePicker.decadeLabel(2010))
    }

    private fun richLibrary(): List<TrackInfo> {
        val decadeTracks = (1L..12L).map { track(it, year = 1985, artist = "Decade Artist") }
        val recentTracks = (13L..24L).map {
            track(it, year = 2020, artist = "Recent Artist", dateAddedSec = monthStartEpochSecond(epochDay))
        }
        val artistTracks = (25L..36L).map {
            track(it, year = 1995, artist = "Top Artist A")
        }
        return decadeTracks + recentTracks + artistTracks
    }

    private fun monthStartEpochSecond(epochDay: Long): Long =
        LocalDate.ofEpochDay(DailyMixConfig.monthStartEpochDay(epochDay))
            .atStartOfDay(zoneId)
            .toEpochSecond()

    private fun track(
        id: Long,
        year: Int? = null,
        artist: String = "Artist",
        dateAddedSec: Long? = null
    ) = TrackInfo(
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = "Track $id",
        artist = artist,
        album = "Album",
        duration = 180_000L,
        year = year,
        dateAddedSec = dateAddedSec
    )
}
