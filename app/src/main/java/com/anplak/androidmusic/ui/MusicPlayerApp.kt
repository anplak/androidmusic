package com.anplak.androidmusic.ui

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    Library(Icons.Default.LibraryMusic, R.string.your_library),
    Favorites(Icons.Default.Favorite, R.string.favorites),
    Playlists(Icons.AutoMirrored.Filled.QueueMusic, R.string.playlists),
    History(Icons.Default.History, R.string.history)
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
        val displayName: String
    ) : AppScreen()
    data class LibraryAlbumDetail(val album: AlbumSummary) : AppScreen()
}

@Composable
fun MusicPlayerApp(
    playbackViewModel: PlaybackViewModel = viewModel(),
    playlistsViewModel: PlaylistsViewModel = viewModel(),
    discoveryViewModel: DiscoveryViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    libraryIndexViewModel: LibraryIndexViewModel = viewModel()
) {
    val uiState by playbackViewModel.uiState.collectAsState()
    val permissionState = rememberAudioPermissionState()
    
    var currentTab by remember { mutableStateOf(NavigationTab.ForYou) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var trackForPlaylistDialog by remember { mutableStateOf<TrackInfo?>(null) }
    var librarySearchHint by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                !permissionState.isGranted -> {
                    PermissionRationaleScreen(
                        onGrantPermissionClick = {
                            permissionState.requestPermission()
                        }
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
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onToggleFavorite = playbackViewModel::toggleFavorite,
                        onAddToPlaylist = { 
                            uiState.selectedTrack?.let { trackForPlaylistDialog = it }
                        },
                        onSmartShuffle = playbackViewModel::startSmartShuffle
                    )
                }

                currentScreen is AppScreen.PlaylistDetail -> {
                    val playlistId = (currentScreen as AppScreen.PlaylistDetail).playlistId
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            currentScreen = AppScreen.NowPlaying
                        },
                        onSmartShufflePlay = { tracks ->
                            playbackViewModel.startSmartShuffleFromPlaylist(tracks)
                            currentScreen = AppScreen.NowPlaying
                        },
                        viewModel = playlistsViewModel
                    )
                }

                currentScreen is AppScreen.SmartPlaylistDetail -> {
                    val type = (currentScreen as AppScreen.SmartPlaylistDetail).type
                    SmartPlaylistDetailScreen(
                        type = type,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            currentScreen = AppScreen.NowPlaying
                        }
                    )
                }

                currentScreen is AppScreen.RecommendationDetail -> {
                    val rowId = (currentScreen as AppScreen.RecommendationDetail).rowId
                    RecommendationDetailScreen(
                        rowId = rowId,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            currentScreen = AppScreen.NowPlaying
                        },
                        viewModel = discoveryViewModel
                    )
                }

                currentScreen is AppScreen.Search -> {
                    SearchScreen(
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onTrackSelected = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            currentScreen = AppScreen.NowPlaying
                        },
                        onPlaylistSelected = { playlistId ->
                            currentScreen = AppScreen.PlaylistDetail(playlistId)
                        },
                        onNavigateToLibrary = { query ->
                            librarySearchHint = query
                            currentTab = NavigationTab.Library
                            currentScreen = AppScreen.MainTabs
                        },
                        viewModel = searchViewModel
                    )
                }

                currentScreen is AppScreen.LibraryIndex -> {
                    LibraryIndexScreen(
                        onBackClick = {
                            if (libraryIndexViewModel.consumeRulesChanged()) {
                                libraryViewModel.refresh()
                            }
                            currentScreen = AppScreen.MainTabs
                        },
                        viewModel = libraryIndexViewModel
                    )
                }

                currentScreen is AppScreen.LibraryArtistDetail -> {
                    val args = currentScreen as AppScreen.LibraryArtistDetail
                    LibraryArtistDetailScreen(
                        artistKey = args.artistKey,
                        displayName = args.displayName,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            currentScreen = AppScreen.NowPlaying
                        },
                        onAddToPlaylist = { track ->
                            trackForPlaylistDialog = track
                        },
                        onExcludeArtist = { artistName ->
                            libraryIndexViewModel.addArtistRule(artistName)
                            libraryViewModel.refresh()
                            currentScreen = AppScreen.MainTabs
                        },
                        viewModel = libraryViewModel
                    )
                }

                currentScreen is AppScreen.LibraryAlbumDetail -> {
                    val args = currentScreen as AppScreen.LibraryAlbumDetail
                    LibraryAlbumDetailScreen(
                        album = args.album,
                        onBackClick = { currentScreen = AppScreen.MainTabs },
                        onPlayAll = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            currentScreen = AppScreen.NowPlaying
                        },
                        onAddToPlaylist = { track ->
                            trackForPlaylistDialog = track
                        },
                        viewModel = libraryViewModel
                    )
                }

                else -> {
                    MainTabsContent(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        onTrackSelected = { tracks, index ->
                            playbackViewModel.onTrackSelected(tracks, index)
                            currentScreen = AppScreen.NowPlaying
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
                            currentScreen = AppScreen.NowPlaying
                        },
                        onOpenSearch = { currentScreen = AppScreen.Search },
                        onOpenLibraryIndex = { currentScreen = AppScreen.LibraryIndex },
                        onArtistClick = { artist ->
                            currentScreen = AppScreen.LibraryArtistDetail(
                                artistKey = artist.normalizedKey,
                                displayName = artist.displayName
                            )
                        },
                        onAlbumClick = { album ->
                            currentScreen = AppScreen.LibraryAlbumDetail(album)
                        },
                        librarySearchHint = librarySearchHint,
                        onConsumeLibraryHint = { librarySearchHint = null },
                        playlistsViewModel = playlistsViewModel,
                        discoveryViewModel = discoveryViewModel,
                        libraryViewModel = libraryViewModel
                    )
                }
            }
        }
    }

    // Add to playlist dialog
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
            viewModel = playlistsViewModel
        )
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
    libraryViewModel: LibraryViewModel
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelResId)) },
                        label = { Text(stringResource(tab.labelResId)) },
                        selected = currentTab == tab,
                        onClick = {
                            onTabSelected(tab)
                            if (tab == NavigationTab.Library) {
                                libraryViewModel.onLibraryVisible()
                            }
                        },
                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                NavigationTab.ForYou -> {
                    ForYouScreen(
                        onRowSelected = onRecommendationRowSelected,
                        onPlayRow = onPlayRecommendationRow,
                        onTrackSelected = onTrackSelected,
                        viewModel = discoveryViewModel
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
                        viewModel = libraryViewModel
                    )
                }
                NavigationTab.Favorites -> {
                    FavoritesScreen(
                        onTrackSelected = onTrackSelected,
                        onAddToPlaylist = onAddToPlaylist
                    )
                }
                NavigationTab.Playlists -> {
                    PlaylistsScreen(
                        onPlaylistSelected = onPlaylistSelected,
                        onSmartPlaylistSelected = onSmartPlaylistSelected,
                        onOpenSearch = onOpenSearch,
                        viewModel = playlistsViewModel
                    )
                }
                NavigationTab.History -> {
                    HistoryScreen(
                        onTrackSelected = onTrackSelected
                    )
                }
            }
        }
    }
}
