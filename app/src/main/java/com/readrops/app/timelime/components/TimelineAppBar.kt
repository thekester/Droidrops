package com.readrops.app.timelime.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.readrops.app.R
import com.readrops.app.timelime.TimelineState
import com.readrops.db.filters.MainFilter
import com.readrops.db.filters.SubFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineAppBar(
    state: TimelineState,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    onOpenDrawer: () -> Unit,
    usePermanentDrawer: Boolean,
    tabletDrawerOpen: Boolean,
    onToggleTabletDrawer: () -> Unit,
    onOpenFilterSheet:  () -> Unit,
    onRefreshTimeline: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    var searchActive by rememberSaveable {
        mutableStateOf(state.filters.searchQuery.isNotBlank())
    }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            if (searchActive) {
                TextField(
                    value = state.filters.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_articles)) },
                    trailingIcon = {
                        if (state.filters.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    }
                )
            } else {
                Column {
                    Text(
                        text = when (state.filters.mainFilter) {
                            MainFilter.STARS -> stringResource(R.string.favorites)
                            MainFilter.ALL -> stringResource(R.string.articles)
                            MainFilter.NEW -> stringResource(R.string.last_24_hours)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (state.showSubtitle) {
                        Text(
                            text = when (state.filters.subFilter) {
                                SubFilter.FEED -> state.filterFeedName
                                SubFilter.FOLDER -> state.filterFolderName
                                SubFilter.TAG -> state.filterTagName
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (!usePermanentDrawer || !tabletDrawerOpen) {
                IconButton(
                    onClick = {
                        if (usePermanentDrawer) onToggleTabletDrawer() else onOpenDrawer()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.open_navigation_menu)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    if (searchActive) {
                        onSearchQueryChange("")
                        searchActive = false
                    } else {
                        searchActive = true
                    }
                }
            ) {
                Icon(
                    imageVector = if (searchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = stringResource(
                        if (searchActive) R.string.close_search else R.string.search_articles
                    )
                )
            }

            IconButton(
                onClick = onOpenFilterSheet
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter_list),
                    contentDescription = stringResource(R.string.filters)
                )
            }

            IconButton(
                onClick = onRefreshTimeline
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sync),
                    contentDescription = stringResource(R.string.synchronize)
                )
            }
        },
        scrollBehavior = topAppBarScrollBehavior
    )

}
