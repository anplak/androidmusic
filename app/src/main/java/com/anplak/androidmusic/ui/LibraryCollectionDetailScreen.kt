@file:Suppress("ktlint:standard:function-naming")

package com.anplak.androidmusic.ui

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.AlbumSummary
import com.anplak.androidmusic.player.TrackInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistDetailScreen(
    artistKey: String,
    displayName: String,
    onBackClick: () -> Unit,
    onPlayAll: (List<TrackInfo>, Int) -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
    modifier: Modifier = Modifier,
    onExcludeArtist: ((String) -> Unit)? = null,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val tracks = remember(artistKey, uiState) { viewModel.tracksForArtist(artistKey) }
    val favoriteIds = (uiState as? LibraryUiState.Content)?.favoriteIds.orEmpty()

    LibraryCollectionDetailContent(
        title = displayName,
        subtitle = pluralStringResource(R.plurals.tracks_count, tracks.size, tracks.size),
        tracks = tracks,
        favoriteIds = favoriteIds,
        onBackClick = onBackClick,
        onPlayAll = onPlayAll,
        onAddToPlaylist = onAddToPlaylist,
        onToggleFavorite = viewModel::toggleFavorite,
        onExcludeArtist = onExcludeArtist?.let { exclude -> { exclude(displayName) } },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAlbumDetailScreen(
    album: AlbumSummary,
    onBackClick: () -> Unit,
    onPlayAll: (List<TrackInfo>, Int) -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val tracks = remember(album, uiState) { viewModel.tracksForAlbum(album) }
    val favoriteIds = (uiState as? LibraryUiState.Content)?.favoriteIds.orEmpty()

    LibraryCollectionDetailContent(
        title = album.displayTitle,
        subtitle = album.displayArtist,
        tracks = tracks,
        favoriteIds = favoriteIds,
        onBackClick = onBackClick,
        onPlayAll = onPlayAll,
        onAddToPlaylist = onAddToPlaylist,
        onToggleFavorite = viewModel::toggleFavorite,
        onExcludeArtist = null,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryCollectionDetailContent(
    title: String,
    subtitle: String,
    tracks: List<TrackInfo>,
    favoriteIds: Set<Long>,
    onBackClick: () -> Unit,
    onPlayAll: (List<TrackInfo>, Int) -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onExcludeArtist: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("library_detail_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.search_back),
                        )
                    }
                },
                actions = {
                    if (onExcludeArtist != null) {
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("library_detail_overflow"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more_options),
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.exclude_artist_from_library))
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onExcludeArtist()
                                    },
                                    modifier = Modifier.testTag("exclude_artist_menu"),
                                )
                            }
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (tracks.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag("library_detail_empty"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.library_no_filter_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledTonalButton(
                        onClick = { onPlayAll(tracks, 0) },
                        modifier = Modifier.testTag("library_detail_play_all"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(text = stringResource(R.string.play_all))
                    }
                }
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag("library_detail_track_list"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(
                        items = tracks,
                        key = { _, track -> track.uri.toString() },
                    ) { index, track ->
                        TrackListItem(
                            track = track,
                            index = index,
                            isFavorite = favoriteIds.contains(track.id),
                            onClick = { onPlayAll(tracks, index) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            testTagPrefix = "library_detail_track",
                        )
                    }
                }
            }
        }
    }
}
