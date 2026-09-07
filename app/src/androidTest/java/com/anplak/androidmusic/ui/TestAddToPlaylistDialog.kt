@file:Suppress("ktlint:standard:function-naming", "FunctionName")

package com.anplak.androidmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.Playlist

/**
 * Testable version of AddToPlaylistDialog that doesn't depend on ViewModel.
 * Used for UI testing with injected playlist data.
 */
@Composable
fun TestAddToPlaylistDialog(
    collectionName: String,
    trackCount: Int,
    trackIds: List<Long>,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (playlistId: Long, trackIds: List<Long>) -> Unit,
    onCreatePlaylist: (name: String, trackIds: List<Long>) -> Unit,
) {
    var showCreateNew by remember { mutableStateOf(playlists.isEmpty()) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.add_to_playlist))
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = collectionName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.tracks_count, trackCount, trackCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (showCreateNew || playlists.isEmpty()) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(stringResource(R.string.playlist_name)) },
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("new_playlist_name_input"),
                    )
                }

                if (playlists.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn {
                        items(
                            items = playlists,
                            key = { it.id },
                        ) { playlist ->
                            TestPlaylistOption(
                                playlist = playlist,
                                onClick = {
                                    onPlaylistSelected(playlist.id, trackIds)
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateNew || playlists.isEmpty()) {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName, trackIds)
                            onDismiss()
                        }
                    },
                    enabled = newPlaylistName.isNotBlank() && trackIds.isNotEmpty(),
                    modifier = Modifier.testTag("create_and_add_button"),
                ) {
                    Text(stringResource(R.string.create))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_to_playlist_button"),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = Modifier.testTag("add_to_playlist_dialog"),
    )
}

@Composable
private fun TestPlaylistOption(
    playlist: Playlist,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = playlist.name)
        },
        supportingContent = {
            Text(
                text = pluralStringResource(R.plurals.tracks_count, playlist.trackCount, playlist.trackCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .testTag("playlist_option_${playlist.id}"),
    )
}
