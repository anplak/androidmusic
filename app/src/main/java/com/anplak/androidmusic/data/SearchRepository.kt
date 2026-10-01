package com.anplak.androidmusic.data

import com.anplak.androidmusic.data.db.PlayHistoryDao
import com.anplak.androidmusic.data.db.PlaylistDao
import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

interface SearchRepository {
    suspend fun searchAll(query: String): SearchRawResults

    suspend fun getSuggestions(): List<String>
}

class SearchRepositoryImpl(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val playHistoryDao: PlayHistoryDao,
    private val playHistoryRepository: PlayHistoryRepository,
) : SearchRepository {
    override suspend fun searchAll(query: String): SearchRawResults =
        coroutineScope {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                return@coroutineScope SearchRawResults(emptyList(), emptyList(), emptyList())
            }

            val tracksDeferred =
                async {
                    trackDao.searchTracks(trimmed, TRACK_LIMIT).map { it.toTrackInfo() }
                }
            val playlistsDeferred =
                async {
                    playlistDao.searchPlaylists(trimmed, PLAYLIST_LIMIT).map { entity ->
                        Playlist(
                            id = entity.id,
                            name = entity.name,
                            createdAt = entity.createdAt,
                            trackCount = entity.trackCount,
                        )
                    }
                }
            val historyDeferred =
                async {
                    playHistoryDao.searchHistory(trimmed, HISTORY_LIMIT).map { it.toPlayHistoryEntry() }
                }

            val tracks = tracksDeferred.await()
            SearchRawResults(
                tracks = tracks,
                playlists = playlistsDeferred.await(),
                history = historyDeferred.await(),
                collectionMatches =
                    SearchCollectionMatch(
                        artists =
                            LibraryBrowseAggregator.aggregateArtists(tracks)
                                .filter { artist ->
                                    artist.displayName.contains(trimmed, ignoreCase = true)
                                }
                                .map { artist ->
                                    SearchResultItem(
                                        id = "artist:${artist.normalizedKey}",
                                        kind = SearchResultKind.ARTIST,
                                        title = artist.displayName,
                                        subtitle = "${artist.trackCount} tracks",
                                        artistKey = artist.normalizedKey,
                                    )
                                },
                        albums =
                            LibraryBrowseAggregator.aggregateAlbums(tracks)
                                .filter { album ->
                                    album.displayTitle.contains(trimmed, ignoreCase = true) ||
                                        album.displayArtist.contains(trimmed, ignoreCase = true)
                                }
                                .map { album ->
                                    SearchResultItem(
                                        id = "album:${album.normalizedTitle}:${album.normalizedArtist}",
                                        kind = SearchResultKind.ALBUM,
                                        title = album.displayTitle,
                                        subtitle = album.displayArtist,
                                        albumTitle = album.displayTitle,
                                        albumArtist = album.displayArtist,
                                    )
                                },
                    ),
            )
        }

    override suspend fun getSuggestions(): List<String> {
        val since30d = System.currentTimeMillis() - THIRTY_DAYS_MS
        return playHistoryRepository.getTopArtistsSince(since30d, SUGGESTION_LIMIT)
            .first()
            .map { it.artist }
            .filter { it.isNotBlank() }
    }

    companion object {
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
        private const val TRACK_LIMIT = 200
        private const val PLAYLIST_LIMIT = 20
        private const val HISTORY_LIMIT = 30
        private const val SUGGESTION_LIMIT = 8
    }
}

private fun com.anplak.androidmusic.data.db.PlayHistoryWithTrack.toPlayHistoryEntry(): PlayHistoryEntry {
    return PlayHistoryEntry(
        id = id,
        trackId = trackId,
        playedAt = playedAt,
        duration = duration,
        sessionId = sessionId,
        track =
            TrackInfo(
                uri = TrackInfo.uriFromId(trackId),
                title = title,
                artist = artist,
                album = album,
                duration = trackDuration,
            ),
    )
}
