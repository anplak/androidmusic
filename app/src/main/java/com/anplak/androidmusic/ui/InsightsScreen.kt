package com.anplak.androidmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.ArtistPlayCount
import com.anplak.androidmusic.player.TrackInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.insights)) },
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
            when {
                uiState.isLoading -> {
                    InsightsLoadingState()
                }
                !uiState.hasData -> {
                    InsightsEmptyState()
                }
                else -> {
                    InsightsContent(
                        state = uiState,
                        onTrackClick = { track ->
                            onTrackSelected(listOf(track), 0)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Compact insights section that can be embedded in other screens (like HistoryScreen).
 */
@Composable
fun InsightsSection(
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (uiState.hasData) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            // Today's stats card
            if (uiState.todayPlayTime > 0) {
                PlayTimeCard(
                    title = stringResource(R.string.today),
                    playTime = uiState.todayPlayTime,
                    modifier = Modifier.testTag("insights_today_time")
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Week's stats card
            if (uiState.weekPlayTime > 0) {
                PlayTimeCard(
                    title = stringResource(R.string.this_week),
                    playTime = uiState.weekPlayTime,
                    modifier = Modifier.testTag("insights_week_time")
                )
            }
        }
    }
}

@Composable
private fun InsightsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_loading"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun InsightsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_empty"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.no_insights),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.no_insights_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightsContent(
    state: InsightsUiState,
    onTrackClick: (TrackInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today section
        item {
            Text(
                text = stringResource(R.string.today),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            PlayTimeCard(
                title = stringResource(R.string.total_play_time),
                playTime = state.todayPlayTime,
                modifier = Modifier.testTag("insights_today_time")
            )
        }

        if (state.todayTopTracks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.top_tracks),
                    icon = Icons.Default.MusicNote
                )
            }

            items(
                items = state.todayTopTracks,
                key = { "today_track_${it.track.id}" }
            ) { trackWithCount ->
                TopTrackItem(
                    track = trackWithCount.track,
                    playCount = trackWithCount.playCount,
                    onClick = { onTrackClick(trackWithCount.track) }
                )
            }
        }

        if (state.todayTopArtists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.top_artists),
                    icon = Icons.Default.Person
                )
            }

            items(
                items = state.todayTopArtists,
                key = { "today_artist_${it.artist}" }
            ) { artistCount ->
                TopArtistItem(artistCount = artistCount)
            }
        }

        // This Week section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.this_week),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            PlayTimeCard(
                title = stringResource(R.string.total_play_time),
                playTime = state.weekPlayTime,
                modifier = Modifier.testTag("insights_week_time")
            )
        }

        if (state.weekTopTracks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.top_tracks),
                    icon = Icons.Default.MusicNote,
                    modifier = Modifier.testTag("insights_top_tracks")
                )
            }

            items(
                items = state.weekTopTracks,
                key = { "week_track_${it.track.id}" }
            ) { trackWithCount ->
                TopTrackItem(
                    track = trackWithCount.track,
                    playCount = trackWithCount.playCount,
                    onClick = { onTrackClick(trackWithCount.track) }
                )
            }
        }

        if (state.weekTopArtists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.top_artists),
                    icon = Icons.Default.Person,
                    modifier = Modifier.testTag("insights_top_artists")
                )
            }

            items(
                items = state.weekTopArtists,
                key = { "week_artist_${it.artist}" }
            ) { artistCount ->
                TopArtistItem(artistCount = artistCount)
            }
        }
    }
}

@Composable
private fun PlayTimeCard(
    title: String,
    playTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = TimeFormatter.formatTotalPlayTime(playTime),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TopTrackItem(
    track: TrackInfo,
    playCount: Int,
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
                text = track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Text(
                text = stringResource(R.string.play_count, playCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun TopArtistItem(
    artistCount: ArtistPlayCount
) {
    ListItem(
        headlineContent = {
            Text(
                text = artistCount.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Text(
                text = stringResource(R.string.play_count, artistCount.playCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}
