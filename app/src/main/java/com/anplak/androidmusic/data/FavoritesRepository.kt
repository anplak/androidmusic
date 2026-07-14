package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.FavoriteDao
import com.anplak.androidmusic.data.db.FavoriteEntity
import com.anplak.androidmusic.data.db.FavoriteTimestamp
import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.data.db.TrackEntity
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FavoritesRepository {
    suspend fun toggleFavorite(track: TrackInfo)
    suspend fun toggleFavorite(trackId: Long)
    fun isFavorite(trackId: Long): Flow<Boolean>
    fun getAllFavorites(): Flow<List<TrackInfo>>
    fun getAllFavoriteIds(): Flow<Set<Long>>
    fun getFavoriteTimestamps(): Flow<Map<Long, Long>>
}

class FavoritesRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val trackDao: TrackDao
) : FavoritesRepository {

    override suspend fun toggleFavorite(track: TrackInfo) {
        trackDao.insert(track.toEntity())
        toggleFavoriteInternal(track.id)
    }

    override suspend fun toggleFavorite(trackId: Long) {
        val cached = trackDao.getById(trackId)?.toTrackInfo()
        if (cached != null) {
            toggleFavorite(cached)
            return
        }
        if (favoriteDao.isFavoriteSync(trackId)) {
            favoriteDao.removeFavorite(trackId)
        }
    }

    private suspend fun toggleFavoriteInternal(trackId: Long) {
        if (favoriteDao.isFavoriteSync(trackId)) {
            favoriteDao.removeFavorite(trackId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(trackId = trackId))
        }
    }

    override fun isFavorite(trackId: Long): Flow<Boolean> {
        return favoriteDao.isFavorite(trackId)
    }

    override fun getAllFavorites(): Flow<List<TrackInfo>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toTrackInfo() }
        }
    }

    override fun getAllFavoriteIds(): Flow<Set<Long>> {
        return favoriteDao.getAllFavoriteIds().map { it.toSet() }
    }

    override fun getFavoriteTimestamps(): Flow<Map<Long, Long>> {
        return favoriteDao.getFavoriteTimestamps().map { rows ->
            rows.associate { it.trackId to it.addedAt }
        }
    }
}

/**
 * Extension function to convert TrackEntity to TrackInfo.
 */
fun TrackEntity.toTrackInfo(): TrackInfo {
    return TrackInfo(
        uri = TrackInfo.uriFromId(id),
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        path = path,
        year = year,
        dateAddedSec = dateAddedSec ?: (firstSeenAt / 1000).takeIf { firstSeenAt > 0 }
    )
}

/**
 * Extension function to convert TrackInfo to TrackEntity.
 */
fun TrackInfo.toEntity(filePath: String = path): TrackEntity {
    return TrackEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        path = filePath.ifBlank { path },
        year = year,
        dateAddedSec = dateAddedSec
    )
}

