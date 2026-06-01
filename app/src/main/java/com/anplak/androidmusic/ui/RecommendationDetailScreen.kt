package com.anplak.androidmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.RecommendationRow
import com.anplak.androidmusic.player.TrackInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationDetailScreen(
    rowId: String,
    onBackClick: () -> Unit,
    onPlayAll: (List<TrackInfo>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoveryViewModel
) {
    var row by remember { mutableStateOf<RecommendationRow?>(null) }

    LaunchedEffect(rowId) {
        row = viewModel.getRow(rowId)
    }

    val title = row?.title ?: stringResource(R.string.for_you)
    val tracks = row?.tracks.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("recommendation_detail_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.previous)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                row == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recommendation_detail_loading"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                tracks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recommendation_detail_empty"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.for_you_empty_description))
                    }
                }
                else -> {
                    RecommendationDetailContent(
                        tracks = tracks,
                        onTrackClick = { index -> onPlayAll(tracks, index) },
                        onPlayAll = { onPlayAll(tracks, 0) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationDetailContent(
    tracks: List<TrackInfo>,
    onTrackClick: (Int) -> Unit,
    onPlayAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = onPlayAll,
                modifier = Modifier.testTag("recommendation_detail_play_all")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(text = stringResource(R.string.play_all))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("recommendation_detail_track_list"),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.id }
            ) { index, track ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = track.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Text(
                            text = track.artist.ifBlank { stringResource(R.string.unknown_artist) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(index) }
                        .testTag("recommendation_detail_track_$index")
                )
            }
        }
    }
}
