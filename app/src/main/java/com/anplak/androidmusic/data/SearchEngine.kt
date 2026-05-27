package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo

class SearchEngine {

    fun buildGrouped(query: String, raw: SearchRawResults): GroupedSearchResults {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return GroupedSearchResults(emptyList(), 0)

        val artists = raw.tracks
            .map { it.artist.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(MAX_ARTISTS)
            .map { artist ->
                SearchResultItem(
                    id = "artist:$artist",
                    kind = SearchResultKind.ARTIST,
                    title = artist
                )
            }

        val albums = raw.tracks
            .filter { it.album.isNotBlank() }
            .distinctBy { "${it.artist}|${it.album}".lowercase() }
            .take(MAX_ALBUMS)
            .map { track ->
                SearchResultItem(
                    id = "album:${track.artist}|${track.album}",
                    kind = SearchResultKind.ALBUM,
                    title = track.album,
                    subtitle = track.artist
                )
            }

        val sections = listOfNotNull(
            section(SECTION_TRACKS, raw.tracks.take(MAX_TRACKS).map { trackItem(it) }),
            section(SECTION_ARTISTS, artists).takeIf { it.items.isNotEmpty() },
            section(SECTION_ALBUMS, albums).takeIf { it.items.isNotEmpty() },
            section(SECTION_PLAYLISTS, raw.playlists.take(MAX_PLAYLISTS).map { playlistItem(it) })
                .takeIf { it.items.isNotEmpty() },
            section(SECTION_HISTORY, raw.history.take(MAX_HISTORY).map { historyItem(it) })
                .takeIf { it.items.isNotEmpty() }
        )

        return GroupedSearchResults(
            sections = sections,
            totalCount = sections.sumOf { it.items.size }
        )
    }

    private fun section(header: String, items: List<SearchResultItem>): SearchSection {
        return SearchSection(header = header, items = items)
    }

    private fun trackItem(track: TrackInfo): SearchResultItem {
        return SearchResultItem(
            id = "track:${track.id}",
            kind = SearchResultKind.TRACK,
            title = track.title,
            subtitle = track.artist.ifBlank { null },
            trackId = track.id
        )
    }

    private fun playlistItem(playlist: Playlist): SearchResultItem {
        return SearchResultItem(
            id = "playlist:${playlist.id}",
            kind = SearchResultKind.PLAYLIST,
            title = playlist.name,
            subtitle = "${playlist.trackCount} tracks",
            playlistId = playlist.id
        )
    }

    private fun historyItem(entry: PlayHistoryEntry): SearchResultItem {
        return SearchResultItem(
            id = "history:${entry.id}",
            kind = SearchResultKind.HISTORY,
            title = entry.track.title,
            subtitle = entry.track.artist.ifBlank { null },
            trackId = entry.trackId,
            historyId = entry.id
        )
    }

    companion object {
        const val SECTION_TRACKS = "Tracks"
        const val SECTION_ARTISTS = "Artists"
        const val SECTION_ALBUMS = "Albums"
        const val SECTION_PLAYLISTS = "Playlists"
        const val SECTION_HISTORY = "History"

        private const val MAX_TRACKS = 50
        private const val MAX_ARTISTS = 8
        private const val MAX_ALBUMS = 8
        private const val MAX_PLAYLISTS = 10
        private const val MAX_HISTORY = 15
    }
}
