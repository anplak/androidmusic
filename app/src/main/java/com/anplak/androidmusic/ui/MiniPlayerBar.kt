package com.anplak.androidmusic.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anplak.androidmusic.R
import com.anplak.androidmusic.ui.theme.Dimens

/**
 * Compact global playback chrome: title (+ artist), Like, Play/Pause.
 * Only the title area opens Now Playing; action buttons do not.
 */
@Composable
fun MiniPlayerBar(
    title: String,
    artist: String?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onBarClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    artworkUri: Uri? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mini_player_bar")
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.miniPlayerHeight)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaArtwork(
                uri = artworkUri,
                contentDescription = title,
                fallbackLabel = title,
                modifier = Modifier.size(Dimens.miniPlayerArtworkSize)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onBarClick)
                    .padding(horizontal = 8.dp)
                    .testTag("mini_player_title")
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!artist.isNullOrBlank()) {
                    Text(
                        text = artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("mini_player_favorite")
            ) {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = stringResource(
                        if (isFavorite) {
                            R.string.remove_from_favorites
                        } else {
                            R.string.add_to_favorites
                        }
                    ),
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.pause else R.string.play
                    )
                )
            }
        }
    }
}
