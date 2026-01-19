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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.PlayHistoryEntry
import com.anplak.androidmusic.player.TrackInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.history)) },
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
                is HistoryUiState.Loading -> {
                    HistoryLoadingState()
                }
                is HistoryUiState.Empty -> {
                    HistoryEmptyState()
                }
                is HistoryUiState.Content -> {
                    HistoryContent(
                        entries = state.entries,
                        hasMore = state.hasMore,
                        onEntryClick = { entry ->
                            // Start playback with just this track
                            onTrackSelected(listOf(entry.track), 0)
                        },
                        onLoadMore = { viewModel.loadMore() },
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_loading"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HistoryEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_empty"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.no_history),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.no_history_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryContent(
    entries: List<PlayHistoryEntry>,
    hasMore: Boolean,
    onEntryClick: (PlayHistoryEntry) -> Unit,
    onLoadMore: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()
    
    // Trigger load more when reaching the end of the list
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            hasMore && lastVisibleItem >= totalItems - 5
        }
    }
    
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_list"),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = entries,
            key = { it.id }
        ) { entry ->
            HistoryEntryItem(
                entry = entry,
                onClick = { onEntryClick(entry) }
            )
        }
        
        if (hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("history_loading_more")
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryItem(
    entry: PlayHistoryEntry,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = entry.track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = entry.track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = TimeFormatter.formatPlayTime(entry.playedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            if (entry.duration > 0) {
                Text(
                    text = TimeFormatter.formatPositionWithTotal(entry.duration, entry.track.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = TimeFormatter.formatDuration(entry.track.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_entry")
    )
}
