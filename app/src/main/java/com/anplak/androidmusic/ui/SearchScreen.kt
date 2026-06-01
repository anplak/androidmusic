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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.SearchEngine
import com.anplak.androidmusic.data.SearchResultItem
import com.anplak.androidmusic.data.SearchResultKind
import com.anplak.androidmusic.data.SearchSection
import com.anplak.androidmusic.player.TrackInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onNavigateToLibrary: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var queryText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("search_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.search_back)
                        )
                    }
                },
                title = {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = {
                            queryText = it
                            viewModel.onQueryChange(it)
                        },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field")
                    )
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
                is SearchUiState.Idle -> SearchIdleContent(
                    recentQueries = state.recentQueries,
                    suggestions = state.suggestions,
                    onRecentClick = { suggestion ->
                        queryText = suggestion
                        viewModel.onSuggestionClicked(suggestion)
                    },
                    onSuggestionClick = { suggestion ->
                        queryText = suggestion
                        viewModel.onSuggestionClicked(suggestion)
                    }
                )
                is SearchUiState.Searching -> SearchLoadingState()
                is SearchUiState.Results -> SearchResultsContent(
                    sections = state.grouped.sections,
                    onItemClick = { item ->
                        viewModel.onSubmit(queryText)
                        handleSearchResultClick(
                            item = item,
                            viewModel = viewModel,
                            onTrackSelected = onTrackSelected,
                            onPlaylistSelected = onPlaylistSelected,
                            onNavigateToLibrary = onNavigateToLibrary
                        )
                    }
                )
                is SearchUiState.NoResults -> SearchNoResultsState()
                is SearchUiState.Error -> SearchErrorState(message = state.message)
            }
        }
    }
}

private fun handleSearchResultClick(
    item: SearchResultItem,
    viewModel: SearchViewModel,
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onNavigateToLibrary: (String) -> Unit
) {
    when (item.kind) {
        SearchResultKind.TRACK,
        SearchResultKind.HISTORY -> {
            val track = viewModel.resolveTrack(item) ?: return
            onTrackSelected(listOf(track), 0)
        }
        SearchResultKind.PLAYLIST -> {
            item.playlistId?.let(onPlaylistSelected)
        }
        SearchResultKind.ARTIST,
        SearchResultKind.ALBUM -> {
            viewModel.libraryQueryForItem(item)?.let(onNavigateToLibrary)
        }
    }
}

@Composable
private fun SearchIdleContent(
    recentQueries: List<String>,
    suggestions: List<String>,
    onRecentClick: (String) -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_idle"),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (recentQueries.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_recent),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(recentQueries, key = { "recent:$it" }) { query ->
                SearchSuggestionRow(
                    label = query,
                    onClick = { onRecentClick(query) },
                    testTag = "search_recent_item"
                )
            }
        }
        if (suggestions.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_suggestions),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(suggestions, key = { "suggestion:$it" }) { artist ->
                SearchSuggestionRow(
                    label = artist,
                    onClick = { onSuggestionClick(artist) },
                    testTag = "search_suggestion_item"
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestionRow(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    ListItem(
        headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    )
}

@Composable
private fun SearchLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_loading"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SearchResultsContent(
    sections: List<SearchSection>,
    onItemClick: (SearchResultItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_results"),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        sections.forEach { section ->
            item(key = "header:${section.header}") {
                Text(
                    text = localizedSectionHeader(section.header),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(section.items, key = { it.id }) { item ->
                ListItem(
                    headlineContent = {
                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = item.subtitle?.let { subtitle ->
                        {
                            Text(
                                text = subtitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .testTag(
                            when (item.kind) {
                                SearchResultKind.TRACK -> "search_result_track"
                                SearchResultKind.HISTORY -> "search_result_history"
                                SearchResultKind.PLAYLIST -> "search_result_playlist"
                                else -> "search_result_${item.id}"
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun localizedSectionHeader(header: String): String {
    return when (header) {
        SearchEngine.SECTION_TRACKS -> stringResource(R.string.search_section_tracks)
        SearchEngine.SECTION_ARTISTS -> stringResource(R.string.search_section_artists)
        SearchEngine.SECTION_ALBUMS -> stringResource(R.string.search_section_albums)
        SearchEngine.SECTION_PLAYLISTS -> stringResource(R.string.search_section_playlists)
        SearchEngine.SECTION_HISTORY -> stringResource(R.string.search_section_history)
        else -> header
    }
}

@Composable
private fun SearchNoResultsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_no_results"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.no_search_results),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.no_search_results_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_error"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(32.dp)
        )
    }
}
