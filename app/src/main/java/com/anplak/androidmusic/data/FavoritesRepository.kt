package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.FavoriteDao
import com.anplak.androidmusic.data.db.FavoriteEntity
import com.anplak.androidmusic.data.db.TrackEntity
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FavoritesRepository {
    suspend fun toggleFavorite(trackId: Long)
    fun isFavorite(trackId: Long): Flow<Boolean>
    fun getAllFavorites(): Flow<List<TrackInfo>>
    fun getAllFavoriteIds(): Flow<Set<Long>>
}

class FavoritesRepositoryImpl(
    private val favoriteDao: FavoriteDao
) : FavoritesRepository {

    override suspend fun toggleFavorite(trackId: Long) {
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
        duration = duration
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
        path = filePath.ifBlank { path }
    )
}

