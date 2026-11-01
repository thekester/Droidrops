package com.readrops.app.more.cleanup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.readrops.app.R
import com.readrops.app.util.components.AndroidScreen

class ArticleCleanupScreen : AndroidScreen() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ArticleCleanupScreenModel>()
        val state by screenModel.cleanupState.collectAsStateWithLifecycle()
        var showConfirmation by remember { mutableStateOf(false) }
        var showFeedPicker by remember { mutableStateOf(false) }
        var feedSearch by rememberSaveable { mutableStateOf("") }

        if (showFeedPicker) {
            val filteredFeeds = state.feeds.filter { feed ->
                feed.name.orEmpty().contains(feedSearch, ignoreCase = true)
            }
            AlertDialog(
                onDismissRequest = { showFeedPicker = false },
                title = { Text(stringResource(R.string.cleanup_choose_feeds)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = feedSearch,
                            onValueChange = { feedSearch = it },
                            label = { Text(stringResource(R.string.cleanup_search_feeds)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    screenModel.selectAllFeeds(state.selectedFeedIds.size != state.feeds.size)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.feeds.isNotEmpty() && state.selectedFeedIds.size == state.feeds.size,
                                onCheckedChange = screenModel::selectAllFeeds
                            )
                            Text(stringResource(R.string.cleanup_all_feeds))
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                            items(filteredFeeds, key = { it.id }) { feed ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { screenModel.toggleFeed(feed.id) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = feed.id in state.selectedFeedIds,
                                        onCheckedChange = { screenModel.toggleFeed(feed.id) }
                                    )
                                    Text(feed.name ?: stringResource(R.string.no_feed), maxLines = 1)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFeedPicker = false }) {
                        Text(stringResource(R.string.cleanup_done))
                    }
                }
            )
        }

        if (showConfirmation) {
            AlertDialog(
                onDismissRequest = { showConfirmation = false },
                title = { Text(stringResource(R.string.cleanup_confirm_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.cleanup_confirm_message,
                            state.previewCount
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmation = false
                            screenModel.deleteSelected()
                        }
                    ) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmation = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.cleanup_title)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!state.isLoading && state.accountId != null) {
                    Surface(tonalElevation = 2.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when {
                                state.error -> Text(
                                    stringResource(R.string.cleanup_error),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                state.isDeleting -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Text(stringResource(R.string.cleanup_deleting), Modifier.padding(start = 12.dp))
                                }
                                state.deletedCount != null -> {
                                    val deleted = state.deletedCount ?: 0
                                    Text(pluralStringResource(R.plurals.cleanup_deleted_count, deleted, deleted))
                                }
                                state.isPreviewing -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Text(stringResource(R.string.cleanup_calculating), Modifier.padding(start = 12.dp))
                                }
                                else -> Text(
                                    pluralStringResource(
                                        R.plurals.cleanup_preview_count,
                                        state.previewCount,
                                        state.previewCount
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Button(
                                onClick = { showConfirmation = true },
                                enabled = !state.isDeleting && !state.isPreviewing && state.previewCount > 0 &&
                                    state.selectedFeedIds.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Text(stringResource(R.string.cleanup_button), Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        ) { padding ->
            if (state.isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.accountId == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.cleanup_no_account))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cleanup_account, state.accountName),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = stringResource(R.string.cleanup_local_only_notice),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(stringResource(R.string.cleanup_older_than), style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 30, 90, 180, 365).forEach { days ->
                            FilterChip(
                                selected = state.periodDays == days,
                                onClick = { screenModel.setPeriod(days) },
                                label = { Text(stringResource(R.string.cleanup_period_days, days)) }
                            )
                        }
                    }

                    Text(stringResource(R.string.cleanup_article_state), style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CleanupReadFilter.entries.forEach { filter ->
                            val label = when (filter) {
                                CleanupReadFilter.READ -> stringResource(R.string.cleanup_read)
                                CleanupReadFilter.UNREAD -> stringResource(R.string.cleanup_unread)
                                CleanupReadFilter.BOTH -> stringResource(R.string.cleanup_read_and_unread)
                            }
                            FilterChip(
                                selected = state.readFilter == filter,
                                onClick = { screenModel.setReadFilter(filter) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.cleanup_keep_favorites))
                            Text(
                                stringResource(R.string.cleanup_keep_favorites_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.keepStarred,
                            onCheckedChange = screenModel::setKeepStarred
                        )
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.cleanup_feeds), style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (state.selectedFeedIds.size == state.feeds.size) {
                                    stringResource(R.string.cleanup_all_feeds)
                                } else {
                                    stringResource(
                                        R.string.cleanup_selected_feeds,
                                        state.selectedFeedIds.size,
                                        state.feeds.size
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { feedSearch = ""; showFeedPicker = true }) {
                            Text(stringResource(R.string.cleanup_choose_feeds))
                        }
                    }

                }
            }
        }
    }
}
