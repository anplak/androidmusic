package com.anplak.androidmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anplak.androidmusic.R
import com.anplak.androidmusic.data.FolderRule
import com.anplak.androidmusic.data.FolderRuleMode
import com.anplak.androidmusic.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryIndexScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryIndexViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

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
                        onClick = { showAddDialog = true },
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

            if (uiState.folderRules.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.folder_rules_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.folderRules, key = { it.path }) { rule ->
                    FolderRuleItem(
                        rule = rule,
                        onRemove = { viewModel.removeFolderRule(rule.path) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddFolderRuleDialog(
            presetFolders = uiState.presetFolders,
            onDismiss = { showAddDialog = false },
            onAdd = { path, mode ->
                viewModel.addFolderRule(path, mode)
                showAddDialog = false
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
private fun AddFolderRuleDialog(
    presetFolders: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, FolderRuleMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_folder_rule)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.listVerticalPadding)) {
                Text(
                    text = stringResource(R.string.add_folder_rule_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                presetFolders.forEach { path ->
                    FilledTonalButton(
                        onClick = { onAdd(path, FolderRuleMode.EXCLUDE) },
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
