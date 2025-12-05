package com.anplak.androidmusic.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
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
import com.anplak.androidmusic.player.TrackInfo

enum class NavigationTab(val icon: ImageVector, val labelResId: Int) {
    Library(Icons.Default.LibraryMusic, R.string.your_library),
    Favorites(Icons.Default.Favorite, R.string.favorites),
    Playlists(Icons.AutoMirrored.Filled.QueueMusic, R.string.playlists)
}

sealed class AppScreen {
    data object MainTabs : AppScreen()
    data object NowPlaying : AppScreen()
    data class PlaylistDetail(val playlistId: Long) : AppScreen()
}

@Composable
fun MusicPlayerApp(
    playbackViewModel: PlaybackViewModel = viewModel(),
    playlistsViewModel: PlaylistsViewModel = viewModel()
) {
    val uiState by playbackViewModel.uiState.collectAsState()
    val permissionState = rememberAudioPermissionState()
    
    var currentTab by remember { mutableStateOf(NavigationTab.Library) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var trackForPlaylistDialog by remember { mutableStateOf<TrackInfo?>(null) }

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
                        }
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
                        viewModel = playlistsViewModel
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
                        playlistsViewModel = playlistsViewModel
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
            onCreatePlaylist = { name, _ ->
                playlistsViewModel.createPlaylist(name)
                // Note: Adding track to newly created playlist requires waiting for
                // playlist creation to complete. For simplicity, user can add manually.
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
    playlistsViewModel: PlaylistsViewModel
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelResId)) },
                        label = { Text(stringResource(tab.labelResId)) },
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
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
                NavigationTab.Library -> {
                    LibraryScreen(
                        onTrackSelected = onTrackSelected,
                        onAddToPlaylist = onAddToPlaylist
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
                        viewModel = playlistsViewModel
                    )
                }
            }
        }
    }
}
