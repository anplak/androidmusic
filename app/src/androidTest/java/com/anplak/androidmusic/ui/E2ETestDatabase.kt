package com.anplak.androidmusic.ui

import android.content.Context
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.data.db.TrackStatsEntity
import kotlinx.coroutines.runBlocking

/**
 * Read-only Room access for instrumentation tests verifying playback stats side effects.
 */
object E2ETestDatabase {

    fun allTrackStats(context: Context): List<TrackStatsEntity> = runBlocking {
        AppDatabase.getInstance(context).trackStatsDao().getAllStatsOrderedByPlayCount()
    }

    fun trackStats(context: Context, trackId: Long): TrackStatsEntity? = runBlocking {
        AppDatabase.getInstance(context).trackStatsDao().getStatsForTrack(trackId)
    }

    fun historyCount(context: Context): Int = runBlocking {
        AppDatabase.getInstance(context).playHistoryDao().getHistoryCount()
    }

    fun cachedTrackCount(context: Context): Int = runBlocking {
        AppDatabase.getInstance(context).trackDao().getAll().size
    }
}

fun Map<Long, TrackStatsEntity>.playCountByTrack(): Map<Long, Int> =
    mapValues { it.value.playCount }

fun Map<Long, TrackStatsEntity>.skipCountByTrack(): Map<Long, Int> =
    mapValues { it.value.skipCount }

fun trackStatsSnapshot(context: Context): Map<Long, TrackStatsEntity> =
    E2ETestDatabase.allTrackStats(context).associateBy { it.trackId }
