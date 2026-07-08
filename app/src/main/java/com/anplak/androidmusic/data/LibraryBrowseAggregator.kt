package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo

data class ArtistSummary(
    val displayName: String,
    val normalizedKey: String,
    val trackCount: Int
)

data class AlbumSummary(
    val displayTitle: String,
    val displayArtist: String,
    val normalizedTitle: String,
    val normalizedArtist: String,
    val trackCount: Int
)

object LibraryBrowseAggregator {

    const val UNKNOWN_ALBUM = "Unknown Album"

    fun aggregateArtists(tracks: List<TrackInfo>): List<ArtistSummary> {
        return tracks
            .groupBy { normalizeArtistKey(it.artist) }
            .map { (key, group) ->
                ArtistSummary(
                    displayName = displayArtistName(group.first().artist),
                    normalizedKey = key,
                    trackCount = group.size
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }

    fun aggregateAlbums(tracks: List<TrackInfo>): List<AlbumSummary> {
        val homonyms = findHomonymAlbumTitles(tracks)
        return tracks
            .groupBy { albumGroupKey(it, homonyms) }
            .map { (_, group) ->
                val first = group.first()
                val title = albumTitle(first)
                val artist = displayArtistName(first.artist)
                val needsArtist = homonyms.contains(title.lowercase())
                AlbumSummary(
                    displayTitle = title,
                    displayArtist = artist,
                    normalizedTitle = title.lowercase(),
                    normalizedArtist = if (needsArtist) normalizeArtistKey(first.artist) else "",
                    trackCount = group.size
                )
            }
            .sortedWith(compareBy({ it.displayTitle.lowercase() }, { it.displayArtist.lowercase() }))
    }

    fun tracksForArtist(tracks: List<TrackInfo>, normalizedKey: String): List<TrackInfo> =
        tracks
            .filter { normalizeArtistKey(it.artist) == normalizedKey }
            .sortedBy { it.title.lowercase() }

    fun tracksForAlbum(
        tracks: List<TrackInfo>,
        normalizedTitle: String,
        normalizedArtist: String
    ): List<TrackInfo> =
        tracks
            .filter { track ->
                val title = albumTitle(track).lowercase()
                val artist = normalizeArtistKey(track.artist)
                title == normalizedTitle &&
                    (normalizedArtist.isEmpty() || artist == normalizedArtist)
            }
            .sortedBy { it.title.lowercase() }

    private data class AlbumGroupKey(val title: String, val artist: String)

    private fun albumTitle(track: TrackInfo): String =
        track.album.trim().ifBlank { UNKNOWN_ALBUM }

    private fun displayArtistName(artist: String): String =
        artist.trim().ifBlank { LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL }

    private fun normalizeArtistKey(artist: String): String =
        LibraryIndexFilter.normalizeArtist(
            artist.ifBlank { LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL }
        )

    private fun findHomonymAlbumTitles(tracks: List<TrackInfo>): Set<String> =
        tracks
            .groupBy { albumTitle(it).lowercase() }
            .filter { (_, albumTracks) ->
                albumTracks.map { normalizeArtistKey(it.artist) }.toSet().size > 1
            }
            .keys

    private fun albumGroupKey(track: TrackInfo, homonyms: Set<String>): AlbumGroupKey {
        val title = albumTitle(track).lowercase()
        val artist = normalizeArtistKey(track.artist)
        return if (title in homonyms) {
            AlbumGroupKey(title, artist)
        } else {
            AlbumGroupKey(title, "")
        }
    }
}
