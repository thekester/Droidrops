package com.readrops.app.feeds.newfeed

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.readrops.app.R
import com.readrops.app.account.selection.adaptiveIconPainterResource
import com.readrops.app.util.components.AndroidScreen
import com.readrops.app.util.components.DropdownBox
import com.readrops.app.util.components.DropdownBoxValue
import com.readrops.app.util.components.LoadingButton
import com.readrops.app.util.components.TextHorizontalDivider
import com.readrops.app.util.theme.LargeSpacer
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.spacing
import org.koin.core.parameter.parametersOf

class NewFeedScreen(val url: String? = null) : AndroidScreen() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<NewFeedScreenModel> { parametersOf(url) }
        val appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        val state by screenModel.state.collectAsStateWithLifecycle()
        if (state.popScreen) navigator.pop()

        Scaffold(topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_feed)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = appBarScrollBehavior
            )
        }) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues)
                    .padding(horizontal = MaterialTheme.spacing.mediumSpacing)
                    .nestedScroll(appBarScrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .animateContentSize()
                    .fillMaxSize()
            ) {
                Text(stringResource(R.string.feed_directory_title), style = MaterialTheme.typography.titleLarge)
                ShortSpacer()
                Text(stringResource(R.string.feed_directory_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MediumSpacer()

                DropdownBox(
                    expanded = state.isAccountDropdownExpanded,
                    text = state.selectedAccount?.name.orEmpty(),
                    label = stringResource(R.string.choose_account),
                    painter = state.selectedAccount?.let { adaptiveIconPainterResource(it.type!!.iconRes) },
                    values = state.accounts.map { DropdownBoxValue(it.id, it.name.orEmpty(), adaptiveIconPainterResource(it.type!!.iconRes)) },
                    onExpandedChange = screenModel::updateAccountDropDownExpandStatus,
                    onValueClick = { id -> screenModel.updateSelectedAccount(state.accounts.first { it.id == id }) },
                    onDismiss = { screenModel.updateAccountDropDownExpandStatus(false) },
                    modifier = Modifier.fillMaxWidth()
                )
                ShortSpacer()
                DropdownBox(
                    expanded = state.isFoldersDropdownExpanded,
                    text = state.selectedFolder?.name.orEmpty(),
                    label = stringResource(R.string.choose_folder),
                    painter = state.selectedFolder?.let { painterResource(R.drawable.ic_folder_grey) },
                    enabled = state.folders.isNotEmpty(),
                    values = state.folders.map { DropdownBoxValue(it.id, it.name.orEmpty(), painterResource(R.drawable.ic_folder_grey)) },
                    onExpandedChange = screenModel::updateFolderDropdownExpandStatus,
                    onValueClick = { id -> screenModel.updateSelectedFolder(state.folders.first { it.id == id }) },
                    onDismiss = { screenModel.updateFolderDropdownExpandStatus(false) },
                    modifier = Modifier.fillMaxWidth()
                )
                MediumSpacer()

                OutlinedTextField(
                    value = state.directorySearch,
                    onValueChange = screenModel::updateDirectorySearch,
                    label = { Text(stringResource(R.string.search_feeds)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ShortSpacer()
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing)
                ) {
                    FeedDirectory.categories.forEach { category ->
                        FilterChip(
                            selected = state.directoryCategory == category,
                            onClick = { screenModel.updateDirectoryCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }
                ShortSpacer()

                if (state.visibleDirectoryFeeds.isEmpty()) {
                    Text(stringResource(R.string.no_matching_feeds), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.visibleDirectoryFeeds.forEach { feed ->
                    val selected = feed.url in state.selectedDirectoryFeedUrls
                    Card(
                        onClick = { screenModel.toggleDirectoryFeed(feed.url) },
                        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(MaterialTheme.spacing.mediumSpacing),
                        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.veryShortSpacing)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selected, onCheckedChange = { screenModel.toggleDirectoryFeed(feed.url) })
                            Column(Modifier.weight(1f).padding(vertical = MaterialTheme.spacing.shortSpacing)) {
                                Text(feed.title, style = MaterialTheme.typography.titleSmall)
                                Text("${feed.publisher} · ${feed.category}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(feed.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!selected) Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_feed))
                            Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                        }
                    }
                }

                TextButton(onClick = screenModel::toggleManualEntry, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (state.isManualEntryExpanded) R.string.hide_manual_feed else R.string.add_custom_feed))
                }
                if (state.isManualEntryExpanded) {
                    TextHorizontalDivider(text = stringResource(R.string.add_custom_feed))
                    ShortSpacer()
                    OutlinedTextField(
                        value = state.actualUrl,
                        label = { Text(stringResource(R.string.enter_url)) },
                        onValueChange = screenModel::updateUrl,
                        singleLine = true,
                        trailingIcon = {
                            if (state.actualUrl.isNotEmpty()) IconButton(onClick = { screenModel.updateUrl("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { screenModel.validate() }),
                        isError = state.isURLError,
                        supportingText = { Text(state.urlError?.errorText() ?: stringResource(R.string.enter_url_helper)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ShortSpacer()
                    TextHorizontalDivider(text = stringResource(R.string.feed_authentication))
                    ShortSpacer()
                    OutlinedTextField(value = state.login, onValueChange = screenModel::updateLogin, label = { Text(stringResource(R.string.feed_login)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    ShortSpacer()
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = screenModel::updatePassword,
                        label = { Text(stringResource(R.string.feed_password)) },
                        singleLine = true,
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { screenModel.setPasswordVisibility(!state.isPasswordVisible) }) {
                                Icon(painterResource(if (state.isPasswordVisible) R.drawable.ic_visible_off else R.drawable.ic_visible), contentDescription = stringResource(if (state.isPasswordVisible) R.string.hide_password else R.string.show_password))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ShortSpacer()
                    if (state.parsingResults.isNotEmpty()) {
                        TextHorizontalDivider(text = stringResource(R.string.feeds) + " " + stringResource(R.string.selected, state.selectedResultsCount))
                        ShortSpacer()
                        state.parsingResults.forEach { result ->
                            ParsingResultItem(
                                parsingResult = result,
                                folders = state.folders,
                                onExpandedChange = { screenModel.updateParsingResultExpandedState(result, it) },
                                onSelectFolder = { screenModel.updateParsingResultFolder(result, it) },
                                onCheckedChange = { screenModel.updateParsingResultCheckedState(result) },
                                onDismiss = { screenModel.updateParsingResultExpandedState(result, false) },
                                error = result.error,
                                modifier = Modifier.fillMaxWidth()
                            )
                            ShortSpacer()
                        }
                    }
                }

                if (state.error != null) {
                    Text(state.error!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.mediumSpacing))
                }
                LargeSpacer()
                LoadingButton(
                    text = if (state.selectedFeedCount > 0) stringResource(R.string.add_selected_feeds, state.selectedFeedCount) else stringResource(R.string.validate),
                    isLoading = state.isLoading,
                    onClick = screenModel::validate,
                    modifier = Modifier.fillMaxWidth()
                )
                MediumSpacer()
            }
        }
    }
}
