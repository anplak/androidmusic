package com.anplak.androidmusic.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MusicPlayerApp(
    playbackViewModel: PlaybackViewModel = viewModel()
) {
    val uiState by playbackViewModel.uiState.collectAsState()
    
    val permissionState = rememberAudioPermissionState()
    
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
                
                uiState.selectedTrack == null -> {
                    LibraryScreen(
                        onTrackSelected = { track ->
                            playbackViewModel.onTrackSelected(track)
                        }
                    )
                }
                
                else -> {
                    NowPlayingScreen(
                        trackTitle = uiState.selectedTrack?.title ?: "",
                        artistName = uiState.selectedTrack?.artist ?: "",
                        isPlaying = uiState.isPlaying,
                        currentPosition = uiState.currentPosition,
                        duration = uiState.duration,
                        error = uiState.error,
                        onPlayPauseClick = playbackViewModel::onPlayPause,
                        onSeek = playbackViewModel::onSeek,
                        onErrorDismiss = playbackViewModel::onErrorDismissed,
                        onBackClick = playbackViewModel::clearTrack
                    )
                }
            }
        }
    }
}

