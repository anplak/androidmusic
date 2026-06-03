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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.DurationBucket
import com.anplak.androidmusic.data.LibraryFilter
import com.anplak.androidmusic.player.TrackInfo
import com.anplak.androidmusic.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenLibraryIndex: () -> Unit = {},
    initialLocalQuery: String? = null,
    onConsumeLibraryHint: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scanSummary by viewModel.scanSummary.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadLibrary()
    }

    LaunchedEffect(initialLocalQuery) {
        initialLocalQuery?.let { hint ->
            viewModel.applyLibraryHint(hint)
            onConsumeLibraryHint()
        }
    }

    LaunchedEffect(scanSummary) {
        scanSummary?.let { result ->
            val message = context.getString(
                R.string.scan_summary,
                result.indexedCount,
                result.skippedDurationCount,
                result.skippedFolderCount
            )
            snackbarHostState.showSnackbar(message)
            viewModel.clearScanSummary()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.your_library)) },
                actions = {
                    IconButton(
                        onClick = onOpenLibraryIndex,
                        modifier = Modifier.testTag("open_library_index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.library_index)
                        )
                    }
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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("scan_summary")
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
                is LibraryUiState.Loading -> LoadingState()
                is LibraryUiState.Empty -> EmptyLibraryState()
                is LibraryUiState.Content -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LibraryFilterBar(
                            filter = state.filter,
                            localQuery = state.localQuery,
                            onFilterChange = viewModel::setFilter,
                            onLocalQueryChange = viewModel::setLocalQuery
                        )
                        if (state.showNoFilterResults) {
                            NoFilterResultsState()
                        } else {
                            TrackList(
                                tracks = state.tracks,
                                favoriteIds = state.favoriteIds,
                                onTrackSelected = { track ->
                                    val index = state.tracks.indexOf(track)
                                    onTrackSelected(state.tracks, index)
                                },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onAddToPlaylist = onAddToPlaylist
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryFilterBar(
    filter: LibraryFilter,
    localQuery: String,
    onFilterChange: (LibraryFilter) -> Unit,
    onLocalQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = localQuery,
            onValueChange = onLocalQueryChange,
            placeholder = { Text(stringResource(R.string.search_library_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_field")
        )
        LazyRow(
            modifier = Modifier.testTag("library_filter_chips"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filter.favoritesOnly,
                    onClick = {
                        onFilterChange(filter.copy(favoritesOnly = !filter.favoritesOnly))
                    },
                    label = { Text(stringResource(R.string.filter_favorites)) },
                    modifier = Modifier.testTag("library_filter_favorites")
                )
            }
            item {
                FilterChip(
                    selected = filter.recentlyAdded,
                    onClick = {
                        onFilterChange(filter.copy(recentlyAdded = !filter.recentlyAdded))
                    },
                    label = { Text(stringResource(R.string.filter_recently_added)) },
                    modifier = Modifier.testTag("library_filter_recently_added")
                )
            }
            item {
                FilterChip(
                    selected = filter.durationBucket == DurationBucket.SHORT,
                    onClick = {
                        onFilterChange(
                            filter.copy(
                                durationBucket = toggleDuration(filter.durationBucket, DurationBucket.SHORT)
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.filter_duration_short)) },
                    modifier = Modifier.testTag("library_filter_duration_short")
                )
            }
            item {
                FilterChip(
                    selected = filter.durationBucket == DurationBucket.MEDIUM,
                    onClick = {
                        onFilterChange(
                            filter.copy(
                                durationBucket = toggleDuration(filter.durationBucket, DurationBucket.MEDIUM)
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.filter_duration_medium)) },
                    modifier = Modifier.testTag("library_filter_duration_medium")
                )
            }
            item {
                FilterChip(
                    selected = filter.durationBucket == DurationBucket.LONG,
                    onClick = {
                        onFilterChange(
                            filter.copy(
                                durationBucket = toggleDuration(filter.durationBucket, DurationBucket.LONG)
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.filter_duration_long)) },
                    modifier = Modifier.testTag("library_filter_duration_long")
                )
            }
        }
    }
}

private fun toggleDuration(current: DurationBucket?, bucket: DurationBucket): DurationBucket? {
    return if (current == bucket) null else bucket
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loading_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
            Text(
                text = stringResource(R.string.scanning_library),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLibraryState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.no_music_found),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("empty_state_title")
            )
            Text(
                text = stringResource(R.string.no_music_found_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoFilterResultsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_no_filter_results"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.library_no_filter_results),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.library_no_filter_results_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<TrackInfo>,
    favoriteIds: Set<Long>,
    onTrackSelected: (TrackInfo) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("track_list"),
        contentPadding = PaddingValues(vertical = Dimens.listVerticalPadding)
    ) {
        items(
            items = tracks,
            key = { it.uri.toString() }
        ) { track ->
            val index = tracks.indexOf(track)
            TrackListItem(
                track = track,
                index = index,
                isFavorite = favoriteIds.contains(track.id),
                onClick = { onTrackSelected(track) },
                onToggleFavorite = { onToggleFavorite(track.id) },
                onAddToPlaylist = { onAddToPlaylist(track) }
            )
        }
    }
}
