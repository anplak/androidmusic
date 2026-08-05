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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.AlbumSummary
import com.anplak.androidmusic.data.ArtistSummary
import com.anplak.androidmusic.data.RecommendationRow
import com.anplak.androidmusic.data.SmartPlaylistType
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

data class MainTabCallbacks(
    val onTabSelected: (NavigationTab) -> Unit,
    val onTrackSelected: (List<TrackInfo>, Int) -> Unit,
    val onAddToPlaylist: (TrackInfo) -> Unit,
    val onPlaylistSelected: (Long) -> Unit,
    val onSmartPlaylistSelected: (SmartPlaylistType) -> Unit,
    val onRecommendationRowSelected: (RecommendationRow) -> Unit,
    val onPlayRecommendationRow: (RecommendationRow) -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenLibraryIndex: () -> Unit,
    val onArtistClick: (ArtistSummary) -> Unit,
    val onAlbumClick: (AlbumSummary) -> Unit,
    val onConsumeLibraryHint: () -> Unit,
    val onOpenNowPlaying: () -> Unit,
    val onPlayPause: () -> Unit,
    val onToggleFavorite: () -> Unit,
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
        if (!permissionState.isGranted) {
            PermissionRationaleScreen(
                onGrantPermissionClick = { permissionState.requestPermission() },
            )
        } else {
            MusicPlayerAppContent(
                uiState = uiState,
                currentTab = currentTab,
                currentScreen = currentScreen,
                screenBeforeNowPlaying = screenBeforeNowPlaying,
                librarySearchHint = librarySearchHint,
                showMiniPlayer = showMiniPlayer,
                miniPlayerSlot = miniPlayerSlot,
                snackbarHostState = snackbarHostState,
                playbackViewModel = playbackViewModel,
                playlistsViewModel = playlistsViewModel,
                discoveryViewModel = discoveryViewModel,
                searchViewModel = searchViewModel,
                libraryViewModel = libraryViewModel,
                libraryIndexViewModel = libraryIndexViewModel,
                openNowPlaying = ::openNowPlaying,
                onCurrentTabChange = { currentTab = it },
                onCurrentScreenChange = { currentScreen = it },
                onLibrarySearchHintChange = { librarySearchHint = it },
                onTrackForPlaylist = { trackForPlaylistDialog = it },
                onCollectionForPlaylist = { collectionForPlaylistDialog = it },
            )
        }

        PlaylistDialogsHost(
            trackForPlaylistDialog = trackForPlaylistDialog,
            collectionForPlaylistDialog = collectionForPlaylistDialog,
            playlistsViewModel = playlistsViewModel,
            onDismissTrack = { trackForPlaylistDialog = null },
            onDismissCollection = { collectionForPlaylistDialog = null },
        )

        PlaylistOperationSnackbarEffect(
            playlistsViewModel = playlistsViewModel,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun MusicPlayerAppContent(
    uiState: PlaybackUiState,
    currentTab: NavigationTab,
    currentScreen: AppScreen,
    screenBeforeNowPlaying: AppScreen,
    librarySearchHint: String?,
    showMiniPlayer: Boolean,
    miniPlayerSlot: @Composable () -> Unit,
    snackbarHostState: SnackbarHostState,
    playbackViewModel: PlaybackViewModel,
    playlistsViewModel: PlaylistsViewModel,
    discoveryViewModel: DiscoveryViewModel,
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    libraryIndexViewModel: LibraryIndexViewModel,
    openNowPlaying: () -> Unit,
    onCurrentTabChange: (NavigationTab) -> Unit,
    onCurrentScreenChange: (AppScreen) -> Unit,
    onLibrarySearchHintChange: (String?) -> Unit,
    onTrackForPlaylist: (TrackInfo) -> Unit,
    onCollectionForPlaylist: (CollectionForPlaylistDialog) -> Unit,
) {
    val goMainTabs = { onCurrentScreenChange(AppScreen.MainTabs) }
    when (currentScreen) {
        is AppScreen.NowPlaying -> {
            if (uiState.selectedTrack != null) {
                NowPlayingRoute(
                    uiState = uiState,
                    playbackViewModel = playbackViewModel,
                    onBackClick = { onCurrentScreenChange(screenBeforeNowPlaying) },
                    onAddToPlaylist = onTrackForPlaylist,
                )
            } else {
                MainTabsHost(
                    currentTab = currentTab,
                    librarySearchHint = librarySearchHint,
                    playlistsViewModel = playlistsViewModel,
                    discoveryViewModel = discoveryViewModel,
                    libraryViewModel = libraryViewModel,
                    playbackUiState = uiState,
                    snackbarHostState = snackbarHostState,
                    playbackViewModel = playbackViewModel,
                    openNowPlaying = openNowPlaying,
                    onCurrentTabChange = onCurrentTabChange,
                    onCurrentScreenChange = onCurrentScreenChange,
                    onLibrarySearchHintChange = onLibrarySearchHintChange,
                    onTrackForPlaylist = onTrackForPlaylist,
                )
            }
        }
        is AppScreen.PlaylistDetail,
        is AppScreen.SmartPlaylistDetail,
        is AppScreen.RecommendationDetail,
        -> {
            PlaybackDetailRoutes(
                currentScreen = currentScreen,
                showMiniPlayer = showMiniPlayer,
                miniPlayerSlot = miniPlayerSlot,
                playlistsViewModel = playlistsViewModel,
                discoveryViewModel = discoveryViewModel,
                playbackViewModel = playbackViewModel,
                onBack = goMainTabs,
                openNowPlaying = openNowPlaying,
            )
        }
        is AppScreen.Search,
        is AppScreen.LibraryIndex,
        is AppScreen.LibraryArtistDetail,
        is AppScreen.LibraryAlbumDetail,
        -> {
            LibraryNavRoutes(
                currentScreen = currentScreen,
                showMiniPlayer = showMiniPlayer,
                miniPlayerSlot = miniPlayerSlot,
                searchViewModel = searchViewModel,
                libraryViewModel = libraryViewModel,
                libraryIndexViewModel = libraryIndexViewModel,
                playbackViewModel = playbackViewModel,
                onBack = goMainTabs,
                onCurrentTabChange = onCurrentTabChange,
                onCurrentScreenChange = onCurrentScreenChange,
                onLibrarySearchHintChange = onLibrarySearchHintChange,
                onTrackForPlaylist = onTrackForPlaylist,
                onCollectionForPlaylist = onCollectionForPlaylist,
                openNowPlaying = openNowPlaying,
            )
        }
        else -> {
            MainTabsHost(
                currentTab = currentTab,
                librarySearchHint = librarySearchHint,
                playlistsViewModel = playlistsViewModel,
                discoveryViewModel = discoveryViewModel,
                libraryViewModel = libraryViewModel,
                playbackUiState = uiState,
                snackbarHostState = snackbarHostState,
                playbackViewModel = playbackViewModel,
                openNowPlaying = openNowPlaying,
                onCurrentTabChange = onCurrentTabChange,
                onCurrentScreenChange = onCurrentScreenChange,
                onLibrarySearchHintChange = onLibrarySearchHintChange,
                onTrackForPlaylist = onTrackForPlaylist,
            )
        }
    }
}

@Composable
private fun PlaybackDetailRoutes(
    currentScreen: AppScreen,
    showMiniPlayer: Boolean,
    miniPlayerSlot: @Composable () -> Unit,
    playlistsViewModel: PlaylistsViewModel,
    discoveryViewModel: DiscoveryViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
) {
    when (currentScreen) {
        is AppScreen.PlaylistDetail -> {
            PlaylistDetailRoute(
                playlistId = currentScreen.playlistId,
                showMiniPlayer = showMiniPlayer,
                miniPlayer = miniPlayerSlot,
                playlistsViewModel = playlistsViewModel,
                playbackViewModel = playbackViewModel,
                onBack = onBack,
                openNowPlaying = openNowPlaying,
            )
        }
        is AppScreen.SmartPlaylistDetail -> {
            SmartPlaylistDetailRoute(
                type = currentScreen.type,
                showMiniPlayer = showMiniPlayer,
                miniPlayer = miniPlayerSlot,
                playbackViewModel = playbackViewModel,
                onBack = onBack,
                openNowPlaying = openNowPlaying,
            )
        }
        is AppScreen.RecommendationDetail -> {
            RecommendationDetailRoute(
                rowId = currentScreen.rowId,
                showMiniPlayer = showMiniPlayer,
                miniPlayer = miniPlayerSlot,
                discoveryViewModel = discoveryViewModel,
                playbackViewModel = playbackViewModel,
                onBack = onBack,
                openNowPlaying = openNowPlaying,
            )
        }
        else -> Unit
    }
}

@Composable
private fun LibraryNavRoutes(
    currentScreen: AppScreen,
    showMiniPlayer: Boolean,
    miniPlayerSlot: @Composable () -> Unit,
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    libraryIndexViewModel: LibraryIndexViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onCurrentTabChange: (NavigationTab) -> Unit,
    onCurrentScreenChange: (AppScreen) -> Unit,
    onLibrarySearchHintChange: (String?) -> Unit,
    onTrackForPlaylist: (TrackInfo) -> Unit,
    onCollectionForPlaylist: (CollectionForPlaylistDialog) -> Unit,
    openNowPlaying: () -> Unit,
) {
    when (currentScreen) {
        is AppScreen.Search -> {
            SearchRoute(
                showMiniPlayer = showMiniPlayer,
                miniPlayer = miniPlayerSlot,
                searchViewModel = searchViewModel,
                playbackViewModel = playbackViewModel,
                onBack = onBack,
                onPlaylistSelected = { onCurrentScreenChange(AppScreen.PlaylistDetail(it)) },
                onNavigateToLibrary = { query ->
                    onLibrarySearchHintChange(query)
                    onCurrentTabChange(NavigationTab.Library)
                    onCurrentScreenChange(AppScreen.MainTabs)
                },
                openNowPlaying = openNowPlaying,
            )
        }
        is AppScreen.LibraryIndex -> {
            LibraryIndexRoute(
                showMiniPlayer = showMiniPlayer,
                miniPlayer = miniPlayerSlot,
                libraryIndexViewModel = libraryIndexViewModel,
                libraryViewModel = libraryViewModel,
                onBack = onBack,
            )
        }
        is AppScreen.LibraryArtistDetail -> {
            LibraryArtistDetailRoute(
                args = currentScreen,
                showMiniPlayer = showMiniPlayer,
                miniPlayer = miniPlayerSlot,
                libraryViewModel = libraryViewModel,
                libraryIndexViewModel = libraryIndexViewModel,
                playbackViewModel = playbackViewModel,
                onBack = onBack,
                onAddToPlaylist = onTrackForPlaylist,
                onAddCollection = onCollectionForPlaylist,
                openNowPlaying = openNowPlaying,
            )
        }
        is AppScreen.LibraryAlbumDetail -> {
            LibraryAlbumDetailRoute(
                album = currentScreen.album,
                showMiniPlayer = showMiniPlayer,
                miniPlayer = miniPlayerSlot,
                libraryViewModel = libraryViewModel,
                playbackViewModel = playbackViewModel,
                onBack = onBack,
                onAddToPlaylist = onTrackForPlaylist,
                onAddCollection = onCollectionForPlaylist,
                openNowPlaying = openNowPlaying,
            )
        }
        else -> Unit
    }
}

@Composable
private fun MainTabsHost(
    currentTab: NavigationTab,
    librarySearchHint: String?,
    playlistsViewModel: PlaylistsViewModel,
    discoveryViewModel: DiscoveryViewModel,
    libraryViewModel: LibraryViewModel,
    playbackUiState: PlaybackUiState,
    snackbarHostState: SnackbarHostState,
    playbackViewModel: PlaybackViewModel,
    openNowPlaying: () -> Unit,
    onCurrentTabChange: (NavigationTab) -> Unit,
    onCurrentScreenChange: (AppScreen) -> Unit,
    onLibrarySearchHintChange: (String?) -> Unit,
    onTrackForPlaylist: (TrackInfo) -> Unit,
) {
    MainTabsContent(
        currentTab = currentTab,
        librarySearchHint = librarySearchHint,
        playlistsViewModel = playlistsViewModel,
        discoveryViewModel = discoveryViewModel,
        libraryViewModel = libraryViewModel,
        playbackUiState = playbackUiState,
        snackbarHostState = snackbarHostState,
        callbacks =
            MainTabCallbacks(
                onTabSelected = onCurrentTabChange,
                onTrackSelected = { tracks, index ->
                    playbackViewModel.onTrackSelected(tracks, index)
                    openNowPlaying()
                },
                onAddToPlaylist = onTrackForPlaylist,
                onPlaylistSelected = {
                    onCurrentScreenChange(AppScreen.PlaylistDetail(it))
                },
                onSmartPlaylistSelected = {
                    onCurrentScreenChange(AppScreen.SmartPlaylistDetail(it))
                },
                onRecommendationRowSelected = {
                    onCurrentScreenChange(AppScreen.RecommendationDetail(it.id))
                },
                onPlayRecommendationRow = { row ->
                    playbackViewModel.startSmartShuffle(row.tracks)
                    openNowPlaying()
                },
                onOpenSearch = { onCurrentScreenChange(AppScreen.Search) },
                onOpenLibraryIndex = { onCurrentScreenChange(AppScreen.LibraryIndex) },
                onArtistClick = { artist ->
                    onCurrentScreenChange(
                        AppScreen.LibraryArtistDetail(
                            artistKey = artist.normalizedKey,
                            displayName = artist.displayName,
                        ),
                    )
                },
                onAlbumClick = { album ->
                    onCurrentScreenChange(AppScreen.LibraryAlbumDetail(album))
                },
                onConsumeLibraryHint = { onLibrarySearchHintChange(null) },
                onOpenNowPlaying = openNowPlaying,
                onPlayPause = playbackViewModel::onPlayPause,
                onToggleFavorite = playbackViewModel::toggleFavorite,
            ),
    )
}

@Composable
private fun NowPlayingRoute(
    uiState: PlaybackUiState,
    playbackViewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
) {
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
        onBackClick = onBackClick,
        onToggleFavorite = playbackViewModel::toggleFavorite,
        onAddToPlaylist = {
            uiState.selectedTrack?.let(onAddToPlaylist)
        },
        onSmartShuffle = playbackViewModel::startSmartShuffle,
        artworkUri = uiState.selectedTrack?.artworkUri,
    )
}

@Composable
private fun PlaylistDetailRoute(
    playlistId: Long,
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    playlistsViewModel: PlaylistsViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
) {
    MiniPlayerOverlayHost(showMiniPlayer = showMiniPlayer, miniPlayer = miniPlayer) {
        PlaylistDetailScreen(
            playlistId = playlistId,
            onBackClick = onBack,
            onPlayAll = { tracks, index ->
                playbackViewModel.onTrackSelected(tracks, index)
                openNowPlaying()
            },
            onSmartShufflePlay = { tracks ->
                playbackViewModel.startSmartShuffle(tracks)
                openNowPlaying()
            },
            viewModel = playlistsViewModel,
        )
    }
}

@Composable
private fun SmartPlaylistDetailRoute(
    type: SmartPlaylistType,
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
) {
    MiniPlayerOverlayHost(showMiniPlayer = showMiniPlayer, miniPlayer = miniPlayer) {
        SmartPlaylistDetailScreen(
            type = type,
            onBackClick = onBack,
            onPlayAll = { tracks, index ->
                playbackViewModel.onTrackSelected(tracks, index)
                openNowPlaying()
            },
        )
    }
}

@Composable
private fun RecommendationDetailRoute(
    rowId: String,
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    discoveryViewModel: DiscoveryViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
) {
    MiniPlayerOverlayHost(showMiniPlayer = showMiniPlayer, miniPlayer = miniPlayer) {
        RecommendationDetailScreen(
            rowId = rowId,
            onBackClick = onBack,
            onPlayAll = { tracks, index ->
                playbackViewModel.onTrackSelected(tracks, index)
                openNowPlaying()
            },
            viewModel = discoveryViewModel,
        )
    }
}

@Composable
private fun SearchRoute(
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    searchViewModel: SearchViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onNavigateToLibrary: (String) -> Unit,
    openNowPlaying: () -> Unit,
) {
    MiniPlayerOverlayHost(showMiniPlayer = showMiniPlayer, miniPlayer = miniPlayer) {
        SearchScreen(
            onBackClick = onBack,
            onTrackSelected = { tracks, index ->
                playbackViewModel.onTrackSelected(tracks, index)
                openNowPlaying()
            },
            onPlaylistSelected = onPlaylistSelected,
            onNavigateToLibrary = onNavigateToLibrary,
            viewModel = searchViewModel,
        )
    }
}

@Composable
private fun LibraryIndexRoute(
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    libraryIndexViewModel: LibraryIndexViewModel,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
) {
    MiniPlayerOverlayHost(showMiniPlayer = showMiniPlayer, miniPlayer = miniPlayer) {
        LibraryIndexScreen(
            onBackClick = {
                if (libraryIndexViewModel.consumeRulesChanged()) {
                    libraryViewModel.refresh()
                }
                onBack()
            },
            viewModel = libraryIndexViewModel,
        )
    }
}

@Composable
private fun LibraryArtistDetailRoute(
    args: AppScreen.LibraryArtistDetail,
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    libraryViewModel: LibraryViewModel,
    libraryIndexViewModel: LibraryIndexViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
    onAddCollection: (CollectionForPlaylistDialog) -> Unit,
    openNowPlaying: () -> Unit,
) {
    MiniPlayerOverlayHost(showMiniPlayer = showMiniPlayer, miniPlayer = miniPlayer) {
        LibraryArtistDetailScreen(
            artistKey = args.artistKey,
            displayName = args.displayName,
            onBackClick = onBack,
            onPlayAll = { tracks, index ->
                playbackViewModel.onTrackSelected(tracks, index)
                openNowPlaying()
            },
            onAddToPlaylist = onAddToPlaylist,
            onAddCollectionToPlaylist = {
                val tracks = libraryViewModel.tracksForArtist(args.artistKey)
                onAddCollection(
                    CollectionForPlaylistDialog(
                        collectionName = args.displayName,
                        trackIds = tracks.map { it.id },
                    ),
                )
            },
            onExcludeArtist = { artistName ->
                libraryIndexViewModel.addArtistRule(artistName)
                libraryViewModel.refresh()
                onBack()
            },
            viewModel = libraryViewModel,
        )
    }
}

@Composable
private fun LibraryAlbumDetailRoute(
    album: AlbumSummary,
    showMiniPlayer: Boolean,
    miniPlayer: @Composable () -> Unit,
    libraryViewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onAddToPlaylist: (TrackInfo) -> Unit,
    onAddCollection: (CollectionForPlaylistDialog) -> Unit,
    openNowPlaying: () -> Unit,
) {
    MiniPlayerOverlayHost(showMiniPlayer = showMiniPlayer, miniPlayer = miniPlayer) {
        LibraryAlbumDetailScreen(
            album = album,
            onBackClick = onBack,
            onPlayAll = { tracks, index ->
                playbackViewModel.onTrackSelected(tracks, index)
                openNowPlaying()
            },
            onAddToPlaylist = onAddToPlaylist,
            onAddCollectionToPlaylist = {
                val tracks = libraryViewModel.tracksForAlbum(album)
                onAddCollection(
                    CollectionForPlaylistDialog(
                        collectionName = album.displayTitle,
                        trackIds = tracks.map { it.id },
                    ),
                )
            },
            viewModel = libraryViewModel,
        )
    }
}

@Composable
private fun PlaylistDialogsHost(
    trackForPlaylistDialog: TrackInfo?,
    collectionForPlaylistDialog: CollectionForPlaylistDialog?,
    playlistsViewModel: PlaylistsViewModel,
    onDismissTrack: () -> Unit,
    onDismissCollection: () -> Unit,
) {
    trackForPlaylistDialog?.let { track ->
        AddToPlaylistDialog(
            track = track,
            onDismiss = onDismissTrack,
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
            onDismiss = onDismissCollection,
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
}

@Composable
private fun PlaylistOperationSnackbarEffect(
    playlistsViewModel: PlaylistsViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val operationState by playlistsViewModel.operationState.collectAsState()
    LaunchedEffect(operationState) {
        when (operationState) {
            is PlaylistOperationState.Success -> {
                val result = (operationState as PlaylistOperationState.Success).result
                val message =
                    if (result.addedCount > 0) {
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
                snackbarHostState.showSnackbar(
                    (operationState as PlaylistOperationState.Error).message,
                )
                playlistsViewModel.clearOperationState()
            }
            else -> Unit
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
    librarySearchHint: String?,
    playlistsViewModel: PlaylistsViewModel,
    discoveryViewModel: DiscoveryViewModel,
    libraryViewModel: LibraryViewModel,
    playbackUiState: PlaybackUiState,
    snackbarHostState: SnackbarHostState,
    callbacks: MainTabCallbacks,
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
                        onOpenNowPlaying = callbacks.onOpenNowPlaying,
                        onPlayPause = callbacks.onPlayPause,
                        onToggleFavorite = callbacks.onToggleFavorite,
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
                                callbacks.onTabSelected(tab)
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
                        onRowSelected = callbacks.onRecommendationRowSelected,
                        onPlayRow = callbacks.onPlayRecommendationRow,
                        onTrackSelected = callbacks.onTrackSelected,
                        viewModel = discoveryViewModel,
                    )
                }
                NavigationTab.Library -> {
                    LibraryScreen(
                        onTrackSelected = callbacks.onTrackSelected,
                        onAddToPlaylist = callbacks.onAddToPlaylist,
                        onArtistClick = callbacks.onArtistClick,
                        onAlbumClick = callbacks.onAlbumClick,
                        onOpenSearch = callbacks.onOpenSearch,
                        onOpenLibraryIndex = callbacks.onOpenLibraryIndex,
                        initialLocalQuery = librarySearchHint,
                        onConsumeLibraryHint = callbacks.onConsumeLibraryHint,
                        viewModel = libraryViewModel,
                    )
                }
                NavigationTab.Favorites -> {
                    FavoritesScreen(
                        onTrackSelected = callbacks.onTrackSelected,
                        onAddToPlaylist = callbacks.onAddToPlaylist,
                    )
                }
                NavigationTab.Playlists -> {
                    PlaylistsScreen(
                        onPlaylistSelected = callbacks.onPlaylistSelected,
                        onSmartPlaylistSelected = callbacks.onSmartPlaylistSelected,
                        onOpenSearch = callbacks.onOpenSearch,
                        viewModel = playlistsViewModel,
                    )
                }
                NavigationTab.History -> {
                    HistoryScreen(
                        onTrackSelected = callbacks.onTrackSelected,
                    )
                }
            }
        }
    }
}
