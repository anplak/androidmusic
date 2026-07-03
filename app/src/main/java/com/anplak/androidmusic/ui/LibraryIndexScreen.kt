package com.anplak.androidmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.ArtistRule
import com.anplak.androidmusic.data.FolderRule
import com.anplak.androidmusic.data.FolderRuleMode
import com.anplak.androidmusic.data.LibraryIndexSuggestions
import com.anplak.androidmusic.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryIndexScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryIndexViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddArtistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_index)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("library_index_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.search_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddFolderDialog = true },
                        modifier = Modifier.testTag("add_folder_rule")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_folder_rule)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.testTag("library_index")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Dimens.screenPadding),
            contentPadding = PaddingValues(vertical = Dimens.listVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.trackItemSpacing)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.max_track_duration)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.max_track_duration_value,
                                uiState.maxDurationMinutes
                            )
                        )
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.folder_rules),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = Dimens.listVerticalPadding)
                )
            }

            if (uiState.includeFolderRules.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.folder_rules_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.includeFolderRules, key = { "include_${it.path}" }) { rule ->
                    FolderRuleItem(
                        rule = rule,
                        onRemove = { viewModel.removeFolderRule(rule.path) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.listVerticalPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.excluded_folders),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("excluded_folders_section")
                    )
                    IconButton(
                        onClick = { showAddFolderDialog = true },
                        modifier = Modifier.testTag("add_excluded_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_folder_rule)
                        )
                    }
                }
            }

            if (uiState.excludedFolders.isEmpty() && uiState.excludedArtists.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_exclusions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("no_exclusions")
                    )
                }
            }

            items(uiState.excludedFolders, key = { "exclude_folder_${it.path}" }) { rule ->
                ExcludedFolderItem(
                    rule = rule,
                    onRemove = { viewModel.removeFolderRule(rule.path) }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.listVerticalPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.excluded_artists),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("excluded_artists_section")
                    )
                    IconButton(
                        onClick = { showAddArtistDialog = true },
                        modifier = Modifier.testTag("add_artist_rule")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_artist_rule)
                        )
                    }
                }
            }

            items(uiState.excludedArtists, key = { "artist_${it.name}" }) { rule ->
                ArtistRuleItem(
                    rule = rule,
                    onRemove = { viewModel.removeArtistRule(rule.name) }
                )
            }
        }
    }

    if (showAddFolderDialog) {
        AddFolderRuleDialog(
            knownFolders = uiState.knownFolders,
            presetFolders = uiState.presetFolders,
            onDismiss = { showAddFolderDialog = false },
            onAdd = { path ->
                viewModel.addFolderRule(path, FolderRuleMode.EXCLUDE)
                showAddFolderDialog = false
            }
        )
    }

    if (showAddArtistDialog) {
        AddArtistRuleDialog(
            knownArtists = uiState.knownArtists,
            onDismiss = { showAddArtistDialog = false },
            onAdd = { name ->
                viewModel.addArtistRule(name)
                showAddArtistDialog = false
            }
        )
    }
}

@Composable
private fun FolderRuleItem(
    rule: FolderRule,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = rule.path,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = when (rule.mode) {
                    FolderRuleMode.INCLUDE -> stringResource(R.string.folder_rule_include)
                    FolderRuleMode.EXCLUDE -> stringResource(R.string.folder_rule_exclude)
                }
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_folder_rule)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ExcludedFolderItem(
    rule: FolderRule,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = rule.path,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(text = stringResource(R.string.excluded_label))
        },
        trailingContent = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.testTag("remove_excluded_folder_${rule.path.hashCode()}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_folder_rule)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ArtistRuleItem(
    rule: ArtistRule,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = rule.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(text = stringResource(R.string.excluded_label))
        },
        trailingContent = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.testTag("remove_artist_rule_${rule.name.hashCode()}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_artist_rule)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("artist_rule_item_${rule.name.hashCode()}")
    )
}

@Composable
private fun AddFolderRuleDialog(
    knownFolders: List<String>,
    presetFolders: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val suggestions = remember(query, knownFolders, presetFolders) {
        val pool = (knownFolders + presetFolders).distinct()
        if (query.isBlank()) {
            pool.take(20)
        } else {
            pool.filter { it.contains(query, ignoreCase = true) }.take(20)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_folder_rule_dialog"),
        title = { Text(stringResource(R.string.add_folder_rule)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.listVerticalPadding)) {
                Text(
                    text = stringResource(R.string.add_folder_rule_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.add_folder_rule_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_rule_input")
                )
                suggestions.forEach { path ->
                    FilledTonalButton(
                        onClick = { onAdd(path) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_exclude_folder")
                    ) {
                        Text(
                            text = stringResource(R.string.exclude_folder, path),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(query.trim()) },
                enabled = query.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_folder_rule")
            ) {
                Text(stringResource(R.string.add_folder_rule))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun AddArtistRuleDialog(
    knownArtists: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val suggestions = remember(query, knownArtists) {
        if (query.isBlank()) {
            knownArtists.take(20)
        } else {
            knownArtists.filter { it.contains(query, ignoreCase = true) }.take(20)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_artist_rule_dialog"),
        title = { Text(stringResource(R.string.add_artist_rule)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.listVerticalPadding)) {
                Text(
                    text = stringResource(R.string.add_artist_rule_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                FilledTonalButton(
                    onClick = { onAdd(LibraryIndexSuggestions.UNKNOWN_ARTIST_LABEL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exclude_unknown_artist")
                ) {
                    Text(stringResource(R.string.exclude_unknown_artist))
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.add_artist_rule_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("artist_rule_input")
                )
                suggestions.forEach { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { query = artist }
                            .padding(vertical = Dimens.listVerticalPadding / 2)
                            .testTag("artist_suggestion_${artist.hashCode()}")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(query.trim()) },
                enabled = query.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_artist_rule")
            ) {
                Text(stringResource(R.string.add_artist_rule))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
