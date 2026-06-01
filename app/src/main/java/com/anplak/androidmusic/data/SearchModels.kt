package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo

enum class SearchResultKind {
    TRACK,
    ARTIST,
    ALBUM,
    PLAYLIST,
    HISTORY
}

data class SearchResultItem(
    val id: String,
    val kind: SearchResultKind,
    val title: String,
    val subtitle: String? = null,
    val trackId: Long? = null,
    val playlistId: Long? = null,
    val historyId: Long? = null
)

data class SearchSection(
    val header: String,
    val items: List<SearchResultItem>
)

data class GroupedSearchResults(
    val sections: List<SearchSection>,
    val totalCount: Int
)

data class SearchRawResults(
    val tracks: List<TrackInfo>,
    val playlists: List<Playlist>,
    val history: List<PlayHistoryEntry>
)
