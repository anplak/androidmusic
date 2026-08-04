@file:Suppress("ktlint:standard:function-naming", "FunctionName")

package com.anplak.androidmusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.AlbumSummary
import com.anplak.androidmusic.data.ArtistSummary
import com.anplak.androidmusic.data.PlaylistOperationResult
import com.anplak.androidmusic.data.RecommendationRow
import com.anplak.androidmusic.data.SmartPlaylistType
import com.anplak.androidmusic.ui.PlaylistOperationState
import com.anplak.androidmusic.player.TrackInfo

enum class NavigationTab(val icon: ImageVector, val labelResId: Int) {
    ForYou(Icons.Default.Explore, R.string.for_you),
    Library(Icons.Default.LibraryMusic, R.string.library),
    Favorites(Icons.Default.Favorite, R.string.favorites),
    Playlists(Icons.AutoMirrored.Filled.QueueMusic, R.string.playlists),
    History(Icons.Default.History, R.string.history),
}

sealed class AppScreen {
    data object MainTabs : AppScreen()

    data object NowPlaying : AppScreen()

    data class PlaylistDetail(val playlistId: Long) : AppScreen()

    data class SmartPlaylistDetail(val type: SmartPlaylistType) : AppScreen()

    data class RecommendationDetail(val rowId: String) : AppScreen()

    data object Insights : AppScreen()

    data object Search : AppScreen()

    data object LibraryIndex : AppScreen()

    data class LibraryArtistDetail(
        val artistKey: String,
        val displayName: String,
    ) : AppScreen()

    data class LibraryAlbumDetail(val album: AlbumSummary) : AppScreen()
}

data class CollectionForPlaylistDialog(
    val collectionName: String,
    val trackIds: List<Long>,
)

@Composable
fun MusicPlayerApp(
    playbackViewModel: PlaybackViewModel = viewModel(),
    playlistsViewModel: PlaylistsViewModel = viewModel(),
    discoveryViewModel: DiscoveryViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    libraryIndexViewModel: LibraryIndexViewModel = viewModel(),
) {
    val uiState by playbackViewModel.uiState.collectAsState()
    val permissionState = rememberAudioPermissionState()

    var currentTab by remember { mutableStateOf(NavigationTab.ForYou) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var screenBeforeNowPlaying by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var trackForPlaylistDialog by remember { mutableStateOf<TrackInfo?>(null) }
    var collectionForPlaylistDialog by remember {
        mutableStateOf<CollectionForPlaylistDialog?>(null)
    }
    var librarySearchHint by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun openNowPlaying() {
        if (currentScreen !is AppScreen.NowPlaying) {
            screenBeforeNowPlaying = currentScreen
        }
        currentScreen = AppScreen.NowPlaying
    }

    val miniPlayerSlot: @Composable () -> Unit = {
        BoundMiniPlayerBar(
            uiState = uiState,
            onOpenNowPlaying = { openNowPlaying() },
            onPlayPause = playbackViewModel::onPlayPause,
            onToggleFavorite = playbackViewModel::toggleFavorite,
        )
    }
    val showMiniPlayer = uiState.selectedTrack != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when {
            !permissionState.isGranted -> {
                PermissionRationaleScreen(
                    onGrantPermissionClick = {
                        permissionState.requestPermission()
                    },
                )
            }

            currentScreen is AppScreen.NowPlaying && uiState.selectedTrack != null -> {
                NowPlayingScreen(
                    trackTitle = uiState.selectedTrack?.title ?: "",
                    artistName = uiState.selectedTrack?.artist ?: "",
                    isPlaying = uiState.isPlaying,
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    error = uiState.error,
                    queuePosition = uiState.queuePosition,
                    queueSize = uiState.queueSize,
                    hasNext = uiState.hasNext,
                    hasPrevious = uiState.hasPrevious,
                    isFavorite = uiState.isFavorite,
                    onPlayPauseClick = playbackViewModel::onPlayPause,
                    onNextClick = playbackViewModel::onNext,
                    onPreviousClick = playbackViewModel::onPrevious,
                    onSeek = playbackViewModel::onSeek,
                    onErrorDismiss = playbackViewModel::onErrorDismissed,
                    onBackClick = { currentScreen = screenBeforeNowPlaying },
                    onToggleFavorite = playbackViewModel::toggleFavorite,
                    onAddToPlaylist = {
                        uiState.selectedTrack?.let { trackForPlaylistDialog = it }
                    },
                    onSmartShuffle = playbackViewModel::startSmartShuffle,
                    artworkUri = uiState.selectedTrack?.artworkUri,
                )
            }

            currentScreen is AppScreen.PlaylistDetail -> {
                val playlistId = (currentScreen as AppScreen.PlaylistDetail).playlistId
                MiniPlayerOverlayHost(
                    showMiniPlayer = showMiniPlayer,
                    miniPlayer = miniPlayerSlot,
                ) {
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            openNowPlaying()
                        },
                        onSmartShufflePlay = { tracks ->
                            playbackViewModel.startSmartShuffleFromPlaylist(tracks)
                            openNowPlaying()
                        },
                        viewModel = playlistsViewModel,
                    )
                }
            }

            currentScreen is AppScreen.SmartPlaylistDetail -> {
                val type = (currentScreen as AppScreen.SmartPlaylistDetail).type
                MiniPlayerOverlayHost(
                    showMiniPlayer = showMiniPlayer,
                    miniPlayer = miniPlayerSlot,
                ) {
                    SmartPlaylistDetailScreen(
                        type = type,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            openNowPlaying()
                        },
                    )
                }
            }

            currentScreen is AppScreen.RecommendationDetail -> {
                val rowId = (currentScreen as AppScreen.RecommendationDetail).rowId
                MiniPlayerOverlayHost(
                    showMiniPlayer = showMiniPlayer,
                    miniPlayer = miniPlayerSlot,
                ) {
                    RecommendationDetailScreen(
                        rowId = rowId,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            openNowPlaying()
                        },
                        viewModel = discoveryViewModel,
                    )
                }
            }

            currentScreen is AppScreen.Search -> {
                MiniPlayerOverlayHost(
                    showMiniPlayer = showMiniPlayer,
                    miniPlayer = miniPlayerSlot,
                ) {
                    SearchScreen(
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onTrackSelected = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            openNowPlaying()
                        },
                        onPlaylistSelected = { playlistId ->
                            currentScreen = AppScreen.PlaylistDetail(playlistId)
                        },
                        onNavigateToLibrary = { query ->
                            librarySearchHint = query
                            currentTab = NavigationTab.Library
                            currentScreen = AppScreen.MainTabs
                        },
                        viewModel = searchViewModel,
                    )
                }
            }

            currentScreen is AppScreen.LibraryIndex -> {
                MiniPlayerOverlayHost(
                    showMiniPlayer = showMiniPlayer,
                    miniPlayer = miniPlayerSlot,
                ) {
                    LibraryIndexScreen(
                        onBackClick = {
                            if (libraryIndexViewModel.consumeRulesChanged()) {
                                libraryViewModel.refresh()
                            }
                            currentScreen = AppScreen.MainTabs
                        },
                        viewModel = libraryIndexViewModel,
                    )
                }
            }

            currentScreen is AppScreen.LibraryArtistDetail -> {
                val args = currentScreen as AppScreen.LibraryArtistDetail
                MiniPlayerOverlayHost(
                    showMiniPlayer = showMiniPlayer,
                    miniPlayer = miniPlayerSlot,
                ) {
                    LibraryArtistDetailScreen(
                        artistKey = args.artistKey,
                        displayName = args.displayName,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            openNowPlaying()
                        },
                        onAddToPlaylist = { track ->
                            trackForPlaylistDialog = track
                        },
                        onAddCollectionToPlaylist = {
                            val tracks = libraryViewModel.tracksForArtist(args.artistKey)
                            collectionForPlaylistDialog = CollectionForPlaylistDialog(
                                collectionName = args.displayName,
                                trackIds = tracks.map { it.id },
                            )
                        },
                        onExcludeArtist = { artistName ->
                            libraryIndexViewModel.addArtistRule(artistName)
                            libraryViewModel.refresh()
                            currentScreen = AppScreen.MainTabs
                        },
                        viewModel = libraryViewModel,
                    )
                }
            }

            currentScreen is AppScreen.LibraryAlbumDetail -> {
                val args = currentScreen as AppScreen.LibraryAlbumDetail
                MiniPlayerOverlayHost(
                    showMiniPlayer = showMiniPlayer,
                    miniPlayer = miniPlayerSlot,
                ) {
                    LibraryAlbumDetailScreen(
                        album = args.album,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            openNowPlaying()
                        },
                        onAddToPlaylist = { track ->
                            trackForPlaylistDialog = track
                        },
                        onAddCollectionToPlaylist = {
                            val tracks = libraryViewModel.tracksForAlbum(args.album)
                            collectionForPlaylistDialog = CollectionForPlaylistDialog(
                                collectionName = args.album.displayTitle,
                                trackIds = tracks.map { it.id },
                            )
                        },
                        viewModel = libraryViewModel,
                    )
                }
            }

            else -> {
                MainTabsContent(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    onTrackSelected = { tracks, index ->
                        playbackViewModel.onTrackSelected(tracks, index)
                        openNowPlaying()
                    },
                    onAddToPlaylist = { track ->
                        trackForPlaylistDialog = track
                    },
                    onPlaylistSelected = { playlistId ->
                        currentScreen = AppScreen.PlaylistDetail(playlistId)
                    },
                    onSmartPlaylistSelected = { type ->
                        currentScreen = AppScreen.SmartPlaylistDetail(type)
                    },
                    onRecommendationRowSelected = { row ->
                        currentScreen = AppScreen.RecommendationDetail(row.id)
                    },
                    onPlayRecommendationRow = { row ->
                        playbackViewModel.startSmartShuffleFromPlaylist(row.tracks)
                        openNowPlaying()
                    },
                    onOpenSearch = { currentScreen = AppScreen.Search },
                    onOpenLibraryIndex = { currentScreen = AppScreen.LibraryIndex },
                    onArtistClick = { artist ->
                        currentScreen =
                            AppScreen.LibraryArtistDetail(
                                artistKey = artist.normalizedKey,
                                displayName = artist.displayName,
                            )
                    },
                    onAlbumClick = { album ->
                        currentScreen = AppScreen.LibraryAlbumDetail(album)
                    },
                    librarySearchHint = librarySearchHint,
                    onConsumeLibraryHint = { librarySearchHint = null },
                    playlistsViewModel = playlistsViewModel,
                    discoveryViewModel = discoveryViewModel,
                    libraryViewModel = libraryViewModel,
                    playbackUiState = uiState,
                    onOpenNowPlaying = { openNowPlaying() },
                    onPlayPause = playbackViewModel::onPlayPause,
                    onToggleFavorite = playbackViewModel::toggleFavorite,
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        trackForPlaylistDialog?.let { track ->
            AddToPlaylistDialog(
                track = track,
                onDismiss = { trackForPlaylistDialog = null },
                onPlaylistSelected = { playlistId, trackId ->
                    playlistsViewModel.addTrackToPlaylist(playlistId, trackId)
                },
                onCreatePlaylist = { name, trackId ->
                    playlistsViewModel.createPlaylistAndAddTrack(name, trackId)
                },
                viewModel = playlistsViewModel,
            )
        }

        collectionForPlaylistDialog?.let { collection ->
            AddToPlaylistDialog(
                collectionName = collection.collectionName,
                trackCount = collection.trackIds.size,
                trackIds = collection.trackIds,
                onDismiss = { collectionForPlaylistDialog = null },
                onPlaylistSelected = { playlistId, trackIds ->
                    playlistsViewModel.addCollectionToPlaylist(
                        playlistId = playlistId,
                        collectionName = collection.collectionName,
                        trackIds = trackIds,
                    )
                },
                onCreatePlaylist = { name, trackIds ->
                    playlistsViewModel.addCollectionToPlaylist(
                        playlistId = null,
                        collectionName = name,
                        trackIds = trackIds,
                    )
                },
                viewModel = playlistsViewModel,
            )
        }

        val context = LocalContext.current
        val operationState by playlistsViewModel.operationState.collectAsState()
        LaunchedEffect(operationState) {
            when (operationState) {
                is PlaylistOperationState.Success -> {
                    val result = (operationState as PlaylistOperationState.Success).result
                    val message = if (result.addedCount > 0) {
                        if (result.skippedCount > 0) {
                            "${result.addedCount} added · ${result.skippedCount} already there"
                        } else {
                            context.resources.getQuantityString(
                                R.plurals.tracks_added,
                                result.addedCount,
                                result.addedCount,
                            )
                        }
                    } else {
                        if (result.skippedCount > 0) {
                            "Already in playlist"
                        } else {
                            "No tracks to add"
                        }
                    }
                    snackbarHostState.showSnackbar(message)
                    playlistsViewModel.clearOperationState()
                }
                is PlaylistOperationState.Error -> {
                    snackbarHostState.showSnackbar((operationState as PlaylistOperationState.Error).message)
                    playlistsViewModel.clearOperationState()
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun BoundMiniPlayerBar(
    uiState: PlaybackUiState,
    onOpenNowPlaying: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val track = uiState.selectedTrack ?: return
    MiniPlayerBar(
        title = track.title,
        artist = track.artist,
        artworkUri = track.artworkUri,
        isPlaying = uiState.isPlaying,
        isFavorite = uiState.isFavorite,
        onBarClick = onOpenNowPlaying,
        onPlayPauseClick = onPlayPause,
        onToggleFavorite = onToggleFavorite,
    )
}

@Composable
private fun MiniPlayerOverlayHost(
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        bottomBar = {
            if (showMiniPlayer) {
                miniPlayer()
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            content()
        }
    }
}

@Composable
private fun MainTabsContent(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onSmartPlaylistSelected: (SmartPlaylistType) -> Unit,
    onRecommendationRowSelected: (RecommendationRow) -> Unit,
    onPlayRecommendationRow: (RecommendationRow) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLibraryIndex: () -> Unit,
    onArtistClick: (ArtistSummary) -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
    librarySearchHint: String?,
    onConsumeLibraryHint: () -> Unit,
    playlistsViewModel: PlaylistsViewModel,
    discoveryViewModel: DiscoveryViewModel,
    libraryViewModel: LibraryViewModel,
    playbackUiState: PlaybackUiState,
    onOpenNowPlaying: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val showMiniPlayer = playbackUiState.selectedTrack != null

    Scaffold(
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    BoundMiniPlayerBar(
                        uiState = playbackUiState,
                        onOpenNowPlaying = onOpenNowPlaying,
                        onPlayPause = onPlayPause,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
                NavigationBar {
                    NavigationTab.entries.forEach { tab ->
                        NavigationBarItem(
                            icon = {
                                Icon(tab.icon, contentDescription = stringResource(tab.labelResId))
                            },
                            label = { Text(stringResource(tab.labelResId)) },
                            selected = currentTab == tab,
                            onClick = {
                                onTabSelected(tab)
                                when (tab) {
                                    NavigationTab.ForYou -> discoveryViewModel.onForYouVisible()
                                    NavigationTab.Library -> libraryViewModel.onLibraryVisible()
                                    else -> Unit
                                }
                            },
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when (currentTab) {
                NavigationTab.ForYou -> {
                    ForYouScreen(
                        onRowSelected = onRecommendationRowSelected,
                        onPlayRow = onPlayRecommendationRow,
                        onTrackSelected = onTrackSelected,
                        viewModel = discoveryViewModel,
                    )
                }
                NavigationTab.Library -> {
                    LibraryScreen(
                        onTrackSelected = onTrackSelected,
                        onAddToPlaylist = onAddToPlaylist,
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick,
                        onOpenSearch = onOpenSearch,
                        onOpenLibraryIndex = onOpenLibraryIndex,
                        initialLocalQuery = librarySearchHint,
                        onConsumeLibraryHint = onConsumeLibraryHint,
                        viewModel = libraryViewModel,
                    )
                }
                NavigationTab.Favorites -> {
                    FavoritesScreen(
                        onTrackSelected = onTrackSelected,
                        onAddToPlaylist = onAddToPlaylist,
                    )
                }
                NavigationTab.Playlists -> {
                    PlaylistsScreen(
                        onPlaylistSelected = onPlaylistSelected,
                        onSmartPlaylistSelected = onSmartPlaylistSelected,
                        onOpenSearch = onOpenSearch,
                        viewModel = playlistsViewModel,
                    )
                }
                NavigationTab.History -> {
                    HistoryScreen(
                        onTrackSelected = onTrackSelected,
                    )
                }
            }
        }
    }
}
