package com.anplak.androidmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun SmartPlaylistDetailScreen(
    type: SmartPlaylistType,
    onBackClick: () -> Unit,
    onPlayAll: (List<TrackInfo>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmartPlaylistsViewModel = viewModel()
) {
    val uiState by viewModel.detailState.collectAsState()
    
    LaunchedEffect(type) {
        viewModel.loadSmartPlaylist(type)
    }
    
    val title = when (type) {
        SmartPlaylistType.MOST_PLAYED -> stringResource(R.string.most_played)
        SmartPlaylistType.RECENTLY_PLAYED -> stringResource(R.string.recently_played)
        SmartPlaylistType.RECENTLY_ADDED -> stringResource(R.string.recently_added)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("smart_playlist_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.previous)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SmartPlaylistDetailUiState.Loading -> {
                    SmartPlaylistLoadingState()
                }
                is SmartPlaylistDetailUiState.Empty -> {
                    SmartPlaylistEmptyState(type)
                }
                is SmartPlaylistDetailUiState.Content -> {
                    SmartPlaylistContent(
                        tracks = state.tracks,
                        onTrackClick = { index -> onPlayAll(state.tracks, index) },
                        onPlayAll = { onPlayAll(state.tracks, 0) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_playlist_loading_state"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SmartPlaylistEmptyState(type: SmartPlaylistType) {
    val message = when (type) {
        SmartPlaylistType.MOST_PLAYED -> "Play some tracks to see your most played here."
        SmartPlaylistType.RECENTLY_PLAYED -> "Your recently played tracks will appear here."
        SmartPlaylistType.RECENTLY_ADDED -> "Add music to your device to see it here."
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_playlist_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmartPlaylistContent(
    tracks: List<TrackInfo>,
    onTrackClick: (Int) -> Unit,
    onPlayAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Play All button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = onPlayAll,
                modifier = Modifier.testTag("smart_playlist_play_all")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(text = stringResource(R.string.play_all))
            }
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("smart_playlist_track_list"),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.id }
            ) { index, track ->
                SmartPlaylistTrackItem(
                    track = track,
                    index = index,
                    onClick = { onTrackClick(index) }
                )
            }
        }
    }
}

@Composable
private fun SmartPlaylistTrackItem(
    track: TrackInfo,
    index: Int,
    onClick: () -> Unit
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
                text = track.artist.ifBlank { "Unknown Artist" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Text(
                text = formatDuration(track.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("smart_playlist_track_item_$index")
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

