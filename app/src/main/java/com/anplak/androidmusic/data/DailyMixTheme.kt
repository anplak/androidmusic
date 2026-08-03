package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class DailyMixTheme {
    DECADE,
    RECENTLY_ADDED,
    TOP_ARTIST,
}

object DailyMixConfig {
    const val SLOT_COUNT = 3
    const val MIN_TRACKS_PER_THEME = 10
    const val DAILY_MIX_LIMIT = 15

    fun monthStartEpochDay(epochDay: Long): Long = LocalDate.ofEpochDay(epochDay).withDayOfMonth(1).toEpochDay()

    fun slotSeed(
        epochDay: Long,
        slot: Int,
    ): Long = epochDay * 31L + slot
}

data class DailyMixSelection(
    val slot: Int,
    val theme: DailyMixTheme,
    val decadeStart: Int? = null,
    val recentSinceEpochDay: Long? = null,
    val artistSeed: String? = null,
) {
    fun rowTitle(): String = "Daily Mix ${slot + 1}"

    fun subtitle(): String =
        when (theme) {
            DailyMixTheme.DECADE -> "${DailyMixThemePicker.decadeLabel(decadeStart!!)} gems"
            DailyMixTheme.RECENTLY_ADDED -> "Added this month"
            DailyMixTheme.TOP_ARTIST -> artistSeed.orEmpty()
        }
}

object DailyMixThemePicker {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun pickAll(
        library: List<TrackInfo>,
        epochDay: Long,
        topArtists: List<String> = emptyList(),
        slotCount: Int = DailyMixConfig.SLOT_COUNT,
    ): List<DailyMixSelection> {
        if (library.isEmpty()) return emptyList()

        val eligibleThemes = buildEligibleThemes(library, epochDay)
        val usedThemes = mutableSetOf<DailyMixTheme>()
        val usedDecades = mutableSetOf<Int>()
        val usedArtists = mutableSetOf<String>()

        return (0 until slotCount).map { slot ->
            val seed = DailyMixConfig.slotSeed(epochDay, slot)
            pickForSlot(
                library = library,
                seed = seed,
                epochDay = epochDay,
                slot = slot,
                eligibleThemes = eligibleThemes,
                topArtists = topArtists,
                usedThemes = usedThemes,
                usedDecades = usedDecades,
                usedArtists = usedArtists,
            )
        }
    }

    fun poolFor(
        selection: DailyMixSelection,
        library: List<TrackInfo>,
        epochDay: Long,
    ): List<TrackInfo> =
        when (selection.theme) {
            DailyMixTheme.DECADE ->
                library.filter {
                    it.year?.let { year -> decadeOf(year) == selection.decadeStart } == true
                }
            DailyMixTheme.RECENTLY_ADDED -> recentPool(library, epochDay)
            DailyMixTheme.TOP_ARTIST -> {
                val artist = selection.artistSeed.orEmpty()
                library.filter { it.artist.equals(artist, ignoreCase = true) }
                    .ifEmpty { library }
            }
        }

    internal fun decadeOf(year: Int): Int = (year / 10) * 10

    internal fun decadeLabel(decadeStart: Int): String {
        val short = decadeStart % 100
        val label = if (short < 10) "0$short" else "$short"
        return "${label}s"
    }

    internal fun eligibleDecades(library: List<TrackInfo>): List<Int> =
        library.mapNotNull { track -> track.year?.let(::decadeOf) }
            .groupingBy { it }
            .eachCount()
            .filter { (_, count) -> count >= DailyMixConfig.MIN_TRACKS_PER_THEME }
            .keys
            .sorted()

    internal fun recentPool(
        library: List<TrackInfo>,
        epochDay: Long,
    ): List<TrackInfo> {
        val since = DailyMixConfig.monthStartEpochDay(epochDay)
        return library.filter { track ->
            track.dateAddedSec?.let { sec ->
                Instant.ofEpochSecond(sec).atZone(zoneId).toLocalDate().toEpochDay() >= since
            } == true
        }
    }

    private fun buildEligibleThemes(
        library: List<TrackInfo>,
        epochDay: Long,
    ): List<DailyMixTheme> =
        buildList {
            if (eligibleDecades(library).isNotEmpty()) add(DailyMixTheme.DECADE)
            if (recentPool(library, epochDay).size >= DailyMixConfig.MIN_TRACKS_PER_THEME) {
                add(DailyMixTheme.RECENTLY_ADDED)
            }
            add(DailyMixTheme.TOP_ARTIST)
        }

    private fun pickForSlot(
        library: List<TrackInfo>,
        seed: Long,
        epochDay: Long,
        slot: Int,
        eligibleThemes: List<DailyMixTheme>,
        topArtists: List<String>,
        usedThemes: MutableSet<DailyMixTheme>,
        usedDecades: MutableSet<Int>,
        usedArtists: MutableSet<String>,
    ): DailyMixSelection {
        val themePool = eligibleThemes.filter { it !in usedThemes }.ifEmpty { eligibleThemes }
        val theme = themePool[(seed % themePool.size).toInt()]
        usedThemes += theme

        return when (theme) {
            DailyMixTheme.DECADE -> {
                val decades =
                    eligibleDecades(library).filter { it !in usedDecades }
                        .ifEmpty { eligibleDecades(library) }
                val decadeIndex = if (decades.isEmpty()) 0 else (seed / themePool.size % decades.size).toInt()
                val decade = decades[decadeIndex]
                usedDecades += decade
                DailyMixSelection(slot = slot, theme = theme, decadeStart = decade)
            }
            DailyMixTheme.RECENTLY_ADDED ->
                DailyMixSelection(
                    slot = slot,
                    theme = theme,
                    recentSinceEpochDay = DailyMixConfig.monthStartEpochDay(epochDay),
                )
            DailyMixTheme.TOP_ARTIST -> {
                val artist = resolveArtistSeed(seed, topArtists, library, usedArtists)
                usedArtists += artist.lowercase()
                DailyMixSelection(slot = slot, theme = theme, artistSeed = artist)
            }
        }
    }

    private fun resolveArtistSeed(
        seed: Long,
        topArtists: List<String>,
        library: List<TrackInfo>,
        usedArtists: Set<String>,
    ): String {
        val libraryArtists =
            library
                .map { it.artist }
                .filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
                .distinct()
        val candidates =
            buildList {
                addAll(topArtists.filter { it.isNotBlank() && it.lowercase() !in usedArtists })
                addAll(libraryArtists.filter { it.lowercase() !in usedArtists })
            }.distinct()

        val pool =
            candidates.ifEmpty {
                (topArtists.filter { it.isNotBlank() } + libraryArtists).distinct()
            }
        if (pool.isEmpty()) return "Unknown Artist"
        return pool[(seed % pool.size).toInt()]
    }
}
