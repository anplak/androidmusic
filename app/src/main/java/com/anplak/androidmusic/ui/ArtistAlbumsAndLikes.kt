@file:Suppress("ktlint:standard:function-naming")

package com.anplak.androidmusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.AlbumSummary
import com.anplak.androidmusic.player.TrackInfo

@Composable
internal fun ArtistAlbumsAndLikes(
    albums: List<AlbumSummary>,
    likedTracks: List<TrackInfo>,
    onAlbumClick: (AlbumSummary) -> Unit,
    onPlayAll: (List<TrackInfo>, Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
) {
    if (albums.isEmpty() && likedTracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().testTag("library_detail_empty"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.library_artist_no_albums_or_likes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("artist_album_list"),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(albums, key = { "album:${it.normalizedTitle}:${it.normalizedArtist}" }) { album ->
            AlbumListItem(album, onAlbumClick)
        }
        if (likedTracks.isNotEmpty()) {
            item(key = "liked_heading") {
                Text(
                    text = stringResource(R.string.library_liked_songs),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp).semantics { heading() }.testTag("artist_liked_heading"),
                )
            }
            itemsIndexed(likedTracks, key = { _, track -> "like:${track.id}" }) { index, track ->
                TrackListItem(
                    track = track,
                    index = index,
                    isFavorite = true,
                    onClick = { onPlayAll(likedTracks, index) },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    testTagPrefix = "artist_liked_songs",
                )
            }
        }
    }
}
