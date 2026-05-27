package com.anplak.androidmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.Playlist
import com.anplak.androidmusic.data.SmartPlaylistType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onPlaylistSelected: (Long) -> Unit,
    onSmartPlaylistSelected: (SmartPlaylistType) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.playlistSearchQuery.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.playlists)) },
                actions = {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("open_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("create_playlist_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_playlist)
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PlaylistsUiState.Loading -> {
                    LoadingState()
                }
                is PlaylistsUiState.Empty,
                is PlaylistsUiState.Content -> {
                    val playlists = when (state) {
                        is PlaylistsUiState.Content -> state.playlists
                        else -> emptyList()
                    }
                    val filtered = playlists.filter { playlist ->
                        searchQuery.isBlank() ||
                            playlist.name.contains(searchQuery, ignoreCase = true)
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::setPlaylistSearchQuery,
                            placeholder = { Text(stringResource(R.string.search_playlists_hint)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("playlists_search_field")
                        )
                        PlaylistListWithSmartPlaylists(
                            playlists = filtered,
                            onPlaylistSelected = onPlaylistSelected,
                            onSmartPlaylistSelected = onSmartPlaylistSelected,
                            onDeletePlaylist = { viewModel.deletePlaylist(it) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlists_loading_state"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PlaylistListWithSmartPlaylists(
    playlists: List<Playlist>,
    onPlaylistSelected: (Long) -> Unit,
    onSmartPlaylistSelected: (SmartPlaylistType) -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlists_list"),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Smart Playlists Section Header
        item {
            SectionHeader(
                title = stringResource(R.string.smart_playlists),
                modifier = Modifier.testTag("smart_playlists_header")
            )
        }
        
        // Smart Playlist Items
        item {
            SmartPlaylistItem(
                title = stringResource(R.string.most_played),
                description = stringResource(R.string.smart_playlist_description),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                onClick = { onSmartPlaylistSelected(SmartPlaylistType.MOST_PLAYED) },
                testTag = "smart_playlist_most_played"
            )
        }
        item {
            SmartPlaylistItem(
                title = stringResource(R.string.recently_played),
                description = stringResource(R.string.smart_playlist_description),
                icon = Icons.Default.History,
                onClick = { onSmartPlaylistSelected(SmartPlaylistType.RECENTLY_PLAYED) },
                testTag = "smart_playlist_recently_played"
            )
        }
        item {
            SmartPlaylistItem(
                title = stringResource(R.string.recently_added),
                description = stringResource(R.string.smart_playlist_description),
                icon = Icons.Default.LibraryAdd,
                onClick = { onSmartPlaylistSelected(SmartPlaylistType.RECENTLY_ADDED) },
                testTag = "smart_playlist_recently_added"
            )
        }
        
        // User Playlists Section Header (only if there are playlists)
        if (playlists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.your_playlists),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .testTag("user_playlists_header")
                )
            }
            
            items(
                items = playlists,
                key = { it.id }
            ) { playlist ->
                val index = playlists.indexOf(playlist)
                PlaylistItem(
                    playlist = playlist,
                    index = index,
                    onClick = { onPlaylistSelected(playlist.id) },
                    onDelete = { onDeletePlaylist(playlist.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SmartPlaylistItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    )
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = playlist.name,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.tracks_count, playlist.trackCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_playlist_button_$index")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_playlist),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("playlist_item_$index")
    )
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.new_playlist))
        },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(playlistName) },
                enabled = playlistName.isNotBlank(),
                modifier = Modifier.testTag("create_playlist_confirm")
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("create_playlist_cancel")
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = Modifier.testTag("create_playlist_dialog")
    )
}

