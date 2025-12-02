package com.anplak.androidmusic.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberAudioFilePicker(
    onFileSelected: (Uri) -> Unit
): AudioFilePicker {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onFileSelected(it) }
    }
    
    return remember {
        AudioFilePicker { launcher.launch(arrayOf("audio/*")) }
    }
}

class AudioFilePicker(
    private val launchPicker: () -> Unit
) {
    fun launch() {
        launchPicker()
    }
}

