package com.anplak.androidmusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.RecommendationRow
import com.anplak.androidmusic.player.TrackInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouScreen(
    onRowSelected: (RecommendationRow) -> Unit,
    onPlayRow: (RecommendationRow) -> Unit,
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoveryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.for_you)) },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.testTag("for_you_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_recommendations)
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
                is ForYouUiState.Loading -> ForYouLoadingState()
                is ForYouUiState.Empty -> ForYouEmptyState()
                is ForYouUiState.Error -> ForYouErrorState(message = state.message)
                is ForYouUiState.Content -> ForYouContent(
                    rows = state.rows,
                    onRowSelected = onRowSelected,
                    onPlayRow = onPlayRow,
                    onTrackSelected = onTrackSelected
                )
            }
        }
    }
}

@Composable
private fun ForYouLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("for_you_loading"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ForYouEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("for_you_empty"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.for_you_empty_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.for_you_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ForYouErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("for_you_error"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
private fun ForYouContent(
    rows: List<RecommendationRow>,
    onRowSelected: (RecommendationRow) -> Unit,
    onPlayRow: (RecommendationRow) -> Unit,
    onTrackSelected: (List<TrackInfo>, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("for_you_list"),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(items = rows, key = { it.id }) { row ->
            RecommendationRowSection(
                row = row,
                onRowClick = { onRowSelected(row) },
                onPlayMix = { onPlayRow(row) },
                onTrackClick = { track ->
                    val index = row.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                    onTrackSelected(row.tracks, index)
                }
            )
        }
    }
}

@Composable
private fun RecommendationRowSection(
    row: RecommendationRow,
    onRowClick: () -> Unit,
    onPlayMix: () -> Unit,
    onTrackClick: (TrackInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("for_you_row_${row.id}")
    ) {
        Text(
            text = row.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        row.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = row.tracks.take(12),
                key = { it.id }
            ) { track ->
                RecommendationTrackCard(
                    track = track,
                    onClick = { onTrackClick(track) }
                )
            }
        }
        RowActions(
            onSeeAll = onRowClick,
            onPlayMix = onPlayMix
        )
    }
}

@Composable
private fun RowActions(
    onSeeAll: () -> Unit,
    onPlayMix: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextButton(
            onClick = onSeeAll,
            modifier = Modifier.testTag("for_you_see_all")
        ) {
            Text(text = stringResource(R.string.see_all))
        }
        TextButton(
            onClick = onPlayMix,
            modifier = Modifier.testTag("for_you_play_mix")
        ) {
            Text(text = stringResource(R.string.play_mix))
        }
    }
}

@Composable
private fun RecommendationTrackCard(
    track: TrackInfo,
    onClick: () -> Unit
) {
    val initial = track.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("for_you_track_${track.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            text = track.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Text(
            text = track.artist.ifBlank { stringResource(R.string.unknown_artist) },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}
