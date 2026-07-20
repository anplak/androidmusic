package com.anplak.androidmusic.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.anplak.androidmusic.ui.theme.Dimens

/**
 * Shared album-art slot: loads [uri] asynchronously with a monogram fallback underneath
 * so null, loading, and decode failure never leave an empty hole.
 */
@Composable
fun MediaArtwork(
    uri: Uri?,
    contentDescription: String?,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.artworkCornerRadius)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .testTag("media_artwork")
    ) {
        ArtworkFallback(
            label = fallbackLabel,
            modifier = Modifier.fillMaxSize()
        )
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ArtworkFallback(
    label: String,
    modifier: Modifier = Modifier
) {
    val initial = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = Dimens.artworkFallbackBorderWidth,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            .testTag("media_artwork_fallback"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}
