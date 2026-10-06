package com.readrops.app.feeds.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readrops.app.R
import com.readrops.app.feeds.FeedScreenModel
import com.readrops.app.util.components.LoadingTextButton
import com.readrops.app.util.components.dialog.BaseDialog
import com.readrops.app.util.theme.LargeSpacer
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.spacing


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateFeedDialog(
    viewModel: FeedScreenModel,
    onDismissRequest: () -> Unit
) {
    val state by viewModel.updateFeedDialogState.collectAsStateWithLifecycle()

    BaseDialog(
        title = stringResource(R.string.edit_feed),
        icon = painterResource(id = R.drawable.ic_rss_feed_grey),
        onDismiss = onDismissRequest
    ) {
        OutlinedTextField(
            value = state.feedName,
            onValueChange = { viewModel.setUpdateFeedDialogStateFeedName(it) },
            label = { Text(text = stringResource(R.string.feed_name)) },
            singleLine = true,
            isError = state.isFeedNameError,
            supportingText = {
                if (state.isFeedNameError) {
                    Text(
                        text = state.feedNameError?.errorText().orEmpty()
                    )
                }
            }
        )

        MediumSpacer()

        OutlinedTextField(
            value = state.feedUrl,
            onValueChange = { viewModel.setUpdateFeedDialogFeedUrl(it) },
            label = { Text(text = stringResource(R.string.feed_url)) },
            singleLine = true,
            readOnly = state.isFeedUrlReadOnly,
            enabled = !state.isFeedUrlReadOnly,
            isError = state.isFeedUrlError,
            supportingText = {
                if (state.isFeedUrlError) {
                    Text(
                        text = state.feedUrlError?.errorText().orEmpty()
                    )
                } else if (state.isFeedUrlReadOnly) {
                    Text(
                        text = stringResource(id = R.string.feed_url_read_only)
                    )
                }
            }
        )

        MediumSpacer()

        ExposedDropdownMenuBox(
            expanded = state.isFolderDropDownExpanded && state.hasFolders,
            onExpandedChange = { viewModel.setFolderDropDownState(state.isFolderDropDownExpanded.not()) }
        ) {
            ExposedDropdownMenu(
                expanded = state.isFolderDropDownExpanded && state.hasFolders,
                onDismissRequest = { viewModel.setFolderDropDownState(false) }
            ) {
                for (folder in state.folders) {
                    DropdownMenuItem(
                        text = { Text(text = folder.name!!) },
                        onClick = {
                            viewModel.setSelectedFolder(folder)
                            viewModel.setFolderDropDownState(false)
                        },
                        leadingIcon = {
                            Icon(
                                painterResource(id = R.drawable.ic_folder_grey),
                                contentDescription = null,
                            )
                        }
                    )
                }
            }

            OutlinedTextField(
                value = state.selectedFolder?.name.orEmpty(),
                readOnly = true,
                enabled = state.hasFolders,
                onValueChange = {},
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isFolderDropDownExpanded)
                },
                leadingIcon = {
                    if (state.selectedFolder != null) {
                        Icon(
                            painterResource(id = R.drawable.ic_folder_grey),
                            contentDescription = null,
                        )
                    }
                },
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )
        }

        // Credentials are folded away by default. Most feeds are public, and BaseDialog lays its
        // content out in a plain Column with no scrolling, so an always open block would push the
        // validate button off screen on a short viewport.
        if (state.isAuthAvailable) {
            MediumSpacer()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { viewModel.toggleUpdateFeedDialogAuth() }
                    .padding(vertical = MaterialTheme.spacing.shortSpacing)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.feed_authentication_section),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // the collapsed row is the only place the user can tell whether this feed
                    // already carries credentials, so it always states which case applies
                    Text(
                        text = if (state.hasStoredCredentials) {
                            stringResource(R.string.feed_credentials_saved, state.login)
                        } else {
                            stringResource(R.string.feed_credentials_none)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    painter = painterResource(
                        id = if (state.isAuthExpanded) {
                            R.drawable.ic_unfold_less
                        } else R.drawable.ic_unfold_more
                    ),
                    contentDescription = stringResource(
                        if (state.isAuthExpanded) R.string.hide_authentication
                        else R.string.show_authentication
                    )
                )
            }

            AnimatedVisibility(visible = state.isAuthExpanded) {
                Column {
                    ShortSpacer()

                    OutlinedTextField(
                        value = state.login,
                        onValueChange = { viewModel.setUpdateFeedDialogLogin(it) },
                        label = { Text(text = stringResource(R.string.feed_login)) },
                        singleLine = true,
                        isError = state.isLoginError,
                        supportingText = {
                            if (state.isLoginError) {
                                Text(text = state.loginError?.errorText().orEmpty())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ShortSpacer()

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.setUpdateFeedDialogPassword(it) },
                        label = { Text(text = stringResource(R.string.feed_password)) },
                        singleLine = true,
                        isError = state.isPasswordError,
                        visualTransformation = if (state.isPasswordVisible) {
                            VisualTransformation.None
                        } else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    viewModel.setUpdateFeedDialogPasswordVisibility(
                                        !state.isPasswordVisible
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (state.isPasswordVisible) {
                                            R.drawable.ic_visible_off
                                        } else R.drawable.ic_visible
                                    ),
                                    contentDescription = stringResource(
                                        if (state.isPasswordVisible) R.string.hide_password
                                        else R.string.show_password
                                    )
                                )
                            }
                        },
                        supportingText = {
                            when {
                                state.isPasswordError ->
                                    Text(text = state.passwordError?.errorText().orEmpty())
                                // saves the user from retyping a secret just to rename a feed
                                state.hasStoredCredentials ->
                                    Text(
                                        text = stringResource(
                                            R.string.feed_credentials_keep_password
                                        )
                                    )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.hasStoredCredentials) {
                        TextButton(
                            onClick = { viewModel.clearUpdateFeedDialogCredentials() }
                        ) {
                            Text(
                                text = stringResource(R.string.feed_credentials_clear),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        if (state.error != null) {
            MediumSpacer()

            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        LargeSpacer()

        LoadingTextButton(
            text = stringResource(R.string.validate),
            isLoading = state.isLoading,
            onClick = { viewModel.updateFeedDialogValidate() },
        )
    }
}
