package com.anplak.androidmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.Playlist
import com.anplak.androidmusic.player.TrackInfo

@Composable
fun AddToPlaylistDialog(
    track: TrackInfo,
    onDismiss: () -> Unit,
    onPlaylistSelected: (playlistId: Long, trackId: Long) -> Unit,
    onCreatePlaylist: (name: String, trackId: Long) -> Unit,
    viewModel: PlaylistsViewModel = viewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.add_to_playlist))
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                // Create new playlist option
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.new_playlist),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .clickable { showCreateNew = true }
                        .testTag("create_new_playlist_option")
                )

                if (showCreateNew) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(stringResource(R.string.playlist_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("new_playlist_name_input")
                    )
                }

                if (playlists.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn {
                        items(
                            items = playlists,
                            key = { it.id }
                        ) { playlist ->
                            PlaylistOption(
                                playlist = playlist,
                                onClick = {
                                    onPlaylistSelected(playlist.id, track.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateNew) {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName, track.id)
                            onDismiss()
                        }
                    },
                    enabled = newPlaylistName.isNotBlank(),
                    modifier = Modifier.testTag("create_and_add_button")
                ) {
                    Text(stringResource(R.string.create))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_to_playlist_button")
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = Modifier.testTag("add_to_playlist_dialog")
    )
}

@Composable
private fun PlaylistOption(
    playlist: Playlist,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = playlist.name)
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.tracks_count, playlist.trackCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("playlist_option_${playlist.id}")
    )
}

