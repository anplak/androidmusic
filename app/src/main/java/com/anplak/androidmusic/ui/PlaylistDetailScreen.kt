package com.anplak.androidmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.SmartPlaylistType
import com.anplak.androidmusic.player.TrackInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBackClick: () -> Unit,
    onPlayAll: (List<TrackInfo>, Int) -> Unit,
    onSmartShufflePlay: (List<TrackInfo>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = viewModel()
) {
    val detailState by viewModel.detailState.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showAutoMixDialog by remember { mutableStateOf(false) }
    var showAutoMixInfo by rememberSaveable { mutableStateOf(true) }
    var showAutoMixIntroDialog by remember { mutableStateOf(false) }
    var mixName by remember { mutableStateOf("") }
    var mergeTargetId by remember { mutableStateOf<Long?>(null) }
    var mergedName by remember { mutableStateOf("") }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistDetail(playlistId)
    }

    when (val state = detailState) {
        is PlaylistDetailUiState.Loading -> {
            LoadingState()
        }
        is PlaylistDetailUiState.NotFound -> {
            NotFoundState(onBackClick = onBackClick)
        }
        is PlaylistDetailUiState.Content -> {
            val displayTracks = if (editState.isReorderMode) {
                editState.reorderedTracks
            } else {
                state.tracks
            }
            PlaylistDetailContent(
                playlistName = state.playlist.name,
                tracks = displayTracks,
                editState = editState,
                onBackClick = onBackClick,
                onPlayAll = { onPlayAll(state.tracks, 0) },
                onSmartShufflePlay = { onSmartShufflePlay(state.tracks) },
                onTrackSelected = { track ->
                    if (editState.isSelectionMode) {
                        viewModel.toggleTrackSelection(track.id)
                    } else {
                        val index = state.tracks.indexOf(track)
                        onPlayAll(state.tracks, index)
                    }
                },
                onRemoveTrack = { trackId ->
                    viewModel.removeTrackFromPlaylist(playlistId, trackId)
                },
                onStartSelectionMode = { viewModel.startSelectionMode() },
                onExitSelectionMode = { viewModel.exitSelectionMode() },
                onRequestRemoveSelected = { viewModel.requestRemoveSelected() },
                onConfirmRemoveSelected = { viewModel.confirmRemoveSelected(playlistId) },
                onDismissRemoveConfirm = { viewModel.dismissRemoveConfirmation() },
                onStartReorder = { viewModel.startReorderMode(state.tracks) },
                onCancelReorder = { viewModel.cancelReorderMode() },
                onCommitReorder = { viewModel.commitReorder(playlistId) },
                onUpdateReorder = { viewModel.updateReorder(it) },
                onDuplicatePlaylist = { showDuplicateDialog = true },
                onMergePlaylist = {
                    mergedName = "${state.playlist.name} Mix"
                    mergeTargetId = null
                    showMergeDialog = true
                },
                onGenerateMix = {
                    viewModel.loadAutoMixSeeds()
                    if (showAutoMixInfo) {
                        showAutoMixIntroDialog = true
                    } else {
                        showAutoMixDialog = true
                    }
                },
                onToggleMenu = { showMenu = !showMenu },
                menuExpanded = showMenu,
                modifier = modifier
            )

            if (showDuplicateDialog) {
                var playlistName by remember { mutableStateOf("${state.playlist.name} Copy") }
                AlertDialog(
                    onDismissRequest = { showDuplicateDialog = false },
                    title = { Text(text = "Duplicate playlist") },
                    text = {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text(text = "Playlist name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        FilledTonalButton(
                            onClick = {
                                viewModel.duplicatePlaylist(state.playlist.id, playlistName)
                                showDuplicateDialog = false
                            }
                        ) {
                            Text(text = "Duplicate")
                        }
                    },
                    dismissButton = {
                        FilledTonalButton(onClick = { showDuplicateDialog = false }) {
                            Text(text = "Cancel")
                        }
                    }
                )
            }

            if (showMergeDialog) {
                val mergeOptions = playlists.filter { it.id != state.playlist.id }
                AlertDialog(
                    onDismissRequest = {
                        showMergeDialog = false
                        mergeTargetId = null
                        mergedName = ""
                    },
                    title = { Text(text = "Merge playlists") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (mergeOptions.isEmpty()) {
                                Text(text = "No other playlists available to merge.")
                            } else {
                                mergeOptions.forEach { option ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { mergeTargetId = option.id }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (mergeTargetId == option.id) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = option.name,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = mergedName,
                                onValueChange = { mergedName = it },
                                label = { Text(text = "New playlist name") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        FilledTonalButton(
                            onClick = {
                                mergeTargetId?.let { targetId ->
                                    val name = mergedName.ifBlank { "${state.playlist.name} Mix" }
                                    viewModel.mergePlaylists(state.playlist.id, targetId, name)
                                    showMergeDialog = false
                                    mergeTargetId = null
                                    mergedName = ""
                                }
                            },
                            enabled = mergeTargetId != null
                        ) {
                            Text(text = "Merge")
                        }
                    },
                    dismissButton = {
                        FilledTonalButton(
                            onClick = {
                                showMergeDialog = false
                                mergeTargetId = null
                                mergedName = ""
                            }
                        ) {
                            Text(text = "Cancel")
                        }
                    }
                )
            }

            if (showAutoMixIntroDialog) {
                AlertDialog(
                    onDismissRequest = { showAutoMixIntroDialog = false },
                    title = { Text(text = "About auto-mix") },
                    text = { Text(text = "Auto-mix builds a new playlist from a seed and orders it using your listening stats.") },
                    confirmButton = {
                        FilledTonalButton(
                            onClick = {
                                showAutoMixInfo = false
                                showAutoMixIntroDialog = false
                                showAutoMixDialog = true
                            }
                        ) {
                            Text(text = "Continue")
                        }
                    }
                )
            }

            if (showAutoMixDialog) {
                AutoMixSeedDialog(
                    favoriteTracks = editState.favoriteTracks,
                    favoriteArtists = editState.favoriteArtists,
                    onDismiss = { showAutoMixDialog = false },
                    onSeedSelected = { seed ->
                        viewModel.generateAutoMix(seed)
                        showAutoMixDialog = false
                    }
                )
            }

            when (val mixState = editState.autoMixState) {
                is AutoMixState.Preview -> {
                    val seedLabel = seedLabel(mixState.seed)
                    if (mixName.isBlank()) {
                        mixName = "Mix - $seedLabel"
                    }
                    AutoMixPreviewDialog(
                        seedLabel = seedLabel,
                        tracks = mixState.tracks,
                        mixName = mixName,
                        onMixNameChanged = { mixName = it },
                        onDismiss = {
                            viewModel.clearAutoMixPreview()
                            mixName = ""
                        },
                        onSave = {
                            viewModel.saveAutoMixAsPlaylist(mixName)
                            mixName = ""
                        }
                    )
                }
                is AutoMixState.Error -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearAutoMixPreview() },
                        title = { Text(text = "Mix unavailable") },
                        text = { Text(text = mixState.message) },
                        confirmButton = {
                            FilledTonalButton(onClick = { viewModel.clearAutoMixPreview() }) {
                                Text(text = "OK")
                            }
                        }
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlist_detail_loading"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotFoundState(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("playlist_not_found"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Playlist not found",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailContent(
    playlistName: String,
    tracks: List<TrackInfo>,
    editState: PlaylistDetailEditState,
    onBackClick: () -> Unit,
    onPlayAll: () -> Unit,
    onSmartShufflePlay: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onStartSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onRequestRemoveSelected: () -> Unit,
    onConfirmRemoveSelected: () -> Unit,
    onDismissRemoveConfirm: () -> Unit,
    onStartReorder: () -> Unit,
    onCancelReorder: () -> Unit,
    onCommitReorder: () -> Unit,
    onUpdateReorder: (List<TrackInfo>) -> Unit,
    onDuplicatePlaylist: () -> Unit,
    onMergePlaylist: () -> Unit,
    onGenerateMix: () -> Unit,
    onToggleMenu: () -> Unit,
    menuExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = playlistName) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("playlist_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    when {
                        editState.isReorderMode -> {
                            IconButton(onClick = onCommitReorder) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Save order"
                                )
                            }
                            IconButton(onClick = onCancelReorder) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reorder"
                                )
                            }
                        }
                        editState.isSelectionMode -> {
                            IconButton(onClick = onRequestRemoveSelected) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove selected"
                                )
                            }
                            IconButton(onClick = onExitSelectionMode) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit selection"
                                )
                            }
                        }
                        else -> {
                            IconButton(onClick = onToggleMenu) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = onToggleMenu
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = "Reorder tracks") },
                                    onClick = {
                                        onToggleMenu()
                                        onStartReorder()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Select tracks") },
                                    onClick = {
                                        onToggleMenu()
                                        onStartSelectionMode()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Duplicate") },
                                    onClick = {
                                        onToggleMenu()
                                        onDuplicatePlaylist()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Merge into new") },
                                    onClick = {
                                        onToggleMenu()
                                        onMergePlaylist()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Generate mix") },
                                    onClick = {
                                        onToggleMenu()
                                        onGenerateMix()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Smart shuffle play") },
                                    onClick = {
                                        onToggleMenu()
                                        onSmartShufflePlay()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (tracks.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onPlayAll,
                    modifier = Modifier.testTag("play_all_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play_all)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (tracks.isEmpty()) {
                EmptyPlaylistState()
            } else {
                if (editState.isReorderMode) {
                    ReorderablePlaylistTrackList(
                        tracks = tracks,
                        onTrackSelected = onTrackSelected,
                        onReorder = onUpdateReorder
                    )
                } else {
                    PlaylistTrackList(
                        tracks = tracks,
                        selectionMode = editState.isSelectionMode,
                        selectedTrackIds = editState.selectedTrackIds,
                        onTrackSelected = onTrackSelected,
                        onToggleSelected = { onTrackSelected(it) },
                        onRemoveTrack = onRemoveTrack
                    )
                }
            }
        }
    }

    if (editState.showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRemoveConfirm,
            title = { Text(text = "Remove tracks") },
            text = { Text(text = "Remove selected tracks from this playlist?") },
            confirmButton = {
                FilledTonalButton(onClick = onConfirmRemoveSelected) {
                    Text(text = "Remove")
                }
            },
            dismissButton = {
                FilledTonalButton(onClick = onDismissRemoveConfirm) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyPlaylistState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlist_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_playlist),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.empty_playlist_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistTrackList(
    tracks: List<TrackInfo>,
    selectionMode: Boolean,
    selectedTrackIds: Set<Long>,
    onTrackSelected: (TrackInfo) -> Unit,
    onToggleSelected: (TrackInfo) -> Unit,
    onRemoveTrack: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlist_track_list"),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = tracks,
            key = { it.uri.toString() }
        ) { track ->
            val index = tracks.indexOf(track)
            PlaylistTrackItem(
                track = track,
                index = index,
                selectionMode = selectionMode,
                isSelected = track.id in selectedTrackIds,
                onClick = {
                    if (selectionMode) {
                        onToggleSelected(track)
                    } else {
                        onTrackSelected(track)
                    }
                },
                onRemove = { onRemoveTrack(track.id) }
            )
        }
    }
}

@Composable
private fun ReorderablePlaylistTrackList(
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo) -> Unit,
    onReorder: (List<TrackInfo>) -> Unit
) {
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlist_reorder_list"),
        contentPadding = PaddingValues(vertical = 8.dp),
        state = listState
    ) {
        items(
            items = tracks,
            key = { it.uri.toString() }
        ) { track ->
            val index = tracks.indexOf(track)
            val isDragging = draggingIndex == index
            PlaylistTrackItem(
                track = track,
                index = index,
                selectionMode = false,
                isSelected = false,
                showDragHandle = true,
                showRemoveButton = false,
                highlight = isDragging,
                onClick = { onTrackSelected(track) },
                onRemove = {},
                onDragHandle = Modifier.pointerInput(tracks) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingIndex = index
                            dragOffset = 0f
                        },
                        onDrag = { _, dragAmount ->
                            val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                            dragOffset += dragAmount.y
                            val visibleItems = listState.layoutInfo.visibleItemsInfo
                            val currentItem = visibleItems.firstOrNull { it.index == currentIndex } ?: return@detectDragGesturesAfterLongPress
                            val currentCenter = currentItem.offset + currentItem.size / 2 + dragOffset
                            val target = visibleItems.firstOrNull { info ->
                                currentCenter in info.offset.toFloat()..(info.offset + info.size).toFloat()
                            }
                            if (target != null && target.index != currentIndex) {
                                val updated = tracks.toMutableList()
                                val item = updated.removeAt(currentIndex)
                                updated.add(target.index, item)
                                draggingIndex = target.index
                                dragOffset = 0f
                                onReorder(updated)
                            }
                        },
                        onDragEnd = {
                            draggingIndex = null
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragOffset = 0f
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun PlaylistTrackItem(
    track: TrackInfo,
    index: Int,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    showDragHandle: Boolean = false,
    highlight: Boolean = false,
    showRemoveButton: Boolean = true,
    onDragHandle: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showDragHandle) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        modifier = onDragHandle
                            .padding(end = 8.dp)
                            .testTag("playlist_drag_handle_$index"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatDuration(track.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when {
                    selectionMode -> {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.SelectAll,
                            contentDescription = "Select track",
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    showRemoveButton -> {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.testTag("remove_from_playlist_button_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.remove_from_playlist),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("playlist_track_item_$index"),
        colors = ListItemDefaults.colors(
            containerColor = if (highlight) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    )
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun AutoMixSeedDialog(
    favoriteTracks: List<TrackInfo>,
    favoriteArtists: List<String>,
    onDismiss: () -> Unit,
    onSeedSelected: (AutoMixSeed) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Generate mix") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Choose a seed")
                if (favoriteTracks.isNotEmpty()) {
                    Text(text = "Favorite tracks", style = MaterialTheme.typography.titleSmall)
                    favoriteTracks.take(5).forEach { track ->
                        Text(
                            text = "${track.title} • ${track.artist}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeedSelected(AutoMixSeed.FavoriteTrack(track)) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                if (favoriteArtists.isNotEmpty()) {
                    Text(text = "Favorite artists", style = MaterialTheme.typography.titleSmall)
                    favoriteArtists.take(5).forEach { artist ->
                        Text(
                            text = artist,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeedSelected(AutoMixSeed.FavoriteArtist(artist)) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                Text(text = "Smart playlists", style = MaterialTheme.typography.titleSmall)
                SmartPlaylistType.values().forEach { type ->
                    Text(
                        text = when (type) {
                            SmartPlaylistType.MOST_PLAYED -> "Most played"
                            SmartPlaylistType.RECENTLY_PLAYED -> "Recently played"
                            SmartPlaylistType.RECENTLY_ADDED -> "Recently added"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeedSelected(AutoMixSeed.SmartPlaylist(type)) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}

@Composable
private fun AutoMixPreviewDialog(
    seedLabel: String,
    tracks: List<TrackInfo>,
    mixName: String,
    onMixNameChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Preview mix: $seedLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = mixName,
                    onValueChange = onMixNameChanged,
                    label = { Text(text = "Playlist name") },
                    singleLine = true
                )
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(tracks) { track ->
                        Text(
                            text = "${track.title} • ${track.artist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onSave) {
                Text(text = "Save mix")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

private fun seedLabel(seed: AutoMixSeed): String {
    return when (seed) {
        is AutoMixSeed.FavoriteTrack -> seed.track.title
        is AutoMixSeed.FavoriteArtist -> seed.artist
        is AutoMixSeed.SmartPlaylist -> when (seed.type) {
            SmartPlaylistType.MOST_PLAYED -> "Most played"
            SmartPlaylistType.RECENTLY_PLAYED -> "Recently played"
            SmartPlaylistType.RECENTLY_ADDED -> "Recently added"
        }
    }
}

