package com.anplak.androidmusic.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class PermissionState {
    GRANTED,
    DENIED,
    SHOULD_SHOW_RATIONALE
}

@Composable
fun rememberAudioPermissionState(
    onPermissionResult: (Boolean) -> Unit = {}
): AudioPermissionState {
    val context = LocalContext.current
    
    var permissionState by remember {
        mutableStateOf(checkAudioPermission(context))
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState = if (isGranted) {
            PermissionState.GRANTED
        } else {
            PermissionState.DENIED
        }
        onPermissionResult(isGranted)
    }
    
    return remember(permissionState) {
        AudioPermissionState(
            state = permissionState,
            requestPermission = {
                val permission = getAudioPermission()
                permissionLauncher.launch(permission)
            }
        )
    }
}

data class AudioPermissionState(
    val state: PermissionState,
    val requestPermission: () -> Unit
) {
    val isGranted: Boolean
        get() = state == PermissionState.GRANTED
}

private fun checkAudioPermission(context: Context): PermissionState {
    val permission = getAudioPermission()
    return when {
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED -> PermissionState.GRANTED
        else -> PermissionState.DENIED
    }
}

private fun getAudioPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

