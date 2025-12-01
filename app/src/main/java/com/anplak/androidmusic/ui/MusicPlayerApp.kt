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
    viewModel: PlaybackViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val permissionState = rememberAudioPermissionState()
    
    val filePicker = rememberAudioFilePicker(
        onFileSelected = { uri ->
            viewModel.onFileSelected(uri)
        }
    )
    
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
                    EmptyStateScreen(
                        onSelectMusicClick = {
                            filePicker.launch()
                        }
                    )
                }
                
                else -> {
                    NowPlayingScreen(
                        trackTitle = uiState.selectedTrack?.title ?: "",
                        isPlaying = uiState.isPlaying,
                        currentPosition = uiState.currentPosition,
                        duration = uiState.duration,
                        error = uiState.error,
                        onPlayPauseClick = viewModel::onPlayPause,
                        onSeek = viewModel::onSeek,
                        onErrorDismiss = viewModel::onErrorDismissed
                    )
                }
            }
        }
    }
}

