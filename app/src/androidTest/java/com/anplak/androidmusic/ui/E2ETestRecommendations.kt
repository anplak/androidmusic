package com.anplak.androidmusic.ui

import android.content.Context
import com.anplak.androidmusic.data.DailyMixConfig
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.MusicLibraryRepositoryFactory
import com.anplak.androidmusic.data.PlayHistoryRepositoryImpl
import com.anplak.androidmusic.data.PlaylistRepositoryImpl
import com.anplak.androidmusic.data.RecommendationEngine
import com.anplak.androidmusic.data.RecommendationRepositoryImpl
import com.anplak.androidmusic.data.RecommendationRow
import com.anplak.androidmusic.data.RecommendationRowType
import com.anplak.androidmusic.data.TrackStatsRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.AutoMixGenerator
import com.anplak.androidmusic.player.SmartShuffleGenerator
import kotlinx.coroutines.runBlocking

/**
 * Builds recommendation rows from the on-device Room cache (same path as [DiscoveryViewModel]).
 */
object E2ETestRecommendations {
    fun buildRows(context: Context): List<RecommendationRow> =
        runBlocking {
            val db = AppDatabase.getInstance(context)
            val favoritesRepository = FavoritesRepositoryImpl(db.favoriteDao(), db.trackDao())
            val repository =
                RecommendationRepositoryImpl(
                    musicLibraryRepository = MusicLibraryRepositoryFactory.create(context),
                    favoritesRepository = favoritesRepository,
                    playHistoryRepository = PlayHistoryRepositoryImpl(db.playHistoryDao()),
                    playlistRepository = PlaylistRepositoryImpl(db.playlistDao()),
                )
            val engine =
                RecommendationEngine(
                    AutoMixGenerator(
                        SmartShuffleGenerator(
                            favoritesRepository,
                            TrackStatsRepositoryImpl(db.trackStatsDao()),
                        ),
                    ),
                )
            engine.buildRows(repository.loadInputs())
        }

    fun dailyMixRows(context: Context): List<RecommendationRow> = buildRows(context).filter { it.type == RecommendationRowType.DAILY_MIX }

    fun tracksWithYearMetadata(context: Context): Int =
        runBlocking {
            AppDatabase.getInstance(context).trackDao().getAll()
                .count { it.year != null && it.year!! > 0 }
        }

    fun tracksWithDateAddedMetadata(context: Context): Int =
        runBlocking {
            AppDatabase.getInstance(context).trackDao().getAll()
                .count { it.dateAddedSec != null && it.dateAddedSec!! > 0 }
        }

    fun libraryMeetsDailyMixMinimum(context: Context): Boolean =
        E2ETestDatabase.cachedTrackCount(context) >= DailyMixConfig.MIN_TRACKS_PER_THEME

    fun namedLibraryArtistCount(context: Context): Int =
        runBlocking {
            AppDatabase.getInstance(context)
                .trackDao()
                .getAll()
                .mapNotNull { artistKey(it.artist) }
                .distinct()
                .size
        }

    fun artistDiversityViolations(rows: List<RecommendationRow>): List<String> =
        rows
            .filter {
                it.type == RecommendationRowType.DAILY_MIX ||
                    it.type == RecommendationRowType.QUICK_MIX ||
                    it.type == RecommendationRowType.BECAUSE_YOU_LISTEN
            }.filter { it.tracks.size >= 4 }
            .mapNotNull { row ->
                val named = row.tracks.mapNotNull { artistKey(it.artist) }.distinct().size
                if (named >= 3) null else "${row.id} has $named named artists"
            }

    fun artistCapViolations(rows: List<RecommendationRow>): List<String> =
        rows
            .filter {
                it.type == RecommendationRowType.DAILY_MIX ||
                    it.type == RecommendationRowType.QUICK_MIX ||
                    it.type == RecommendationRowType.BECAUSE_YOU_LISTEN
            }.flatMap { row -> violationsFor(row) }

    private fun violationsFor(row: RecommendationRow): List<String> {
        if (row.tracks.size < 2) return emptyList()
        val cap = row.tracks.size / 2
        return row.tracks
            .mapNotNull { artistKey(it.artist) }
            .groupingBy { it }
            .eachCount()
            .filter { (_, count) -> count > cap }
            .map { (artist, count) ->
                "$artist has $count of ${row.tracks.size} in ${row.id}"
            }
    }

    private fun artistKey(artist: String): String? = artist.trim().takeIf { it.isNotEmpty() }?.lowercase()

    fun dailyMixTrackIdsOverlap(rows: List<RecommendationRow>): Boolean {
        val dailyMixes = rows.filter { it.type == RecommendationRowType.DAILY_MIX }
        if (dailyMixes.size < 2) return false
        val seen = mutableSetOf<Long>()
        for (row in dailyMixes) {
            for (trackId in row.tracks.map { it.id }) {
                if (trackId in seen) return true
                seen += trackId
            }
        }
        return false
    }
}
