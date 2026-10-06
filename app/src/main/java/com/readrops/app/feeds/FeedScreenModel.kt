package com.readrops.app.feeds

import android.content.Context
import android.content.SharedPreferences
import cafe.adriel.voyager.core.model.screenModelScope
import com.readrops.app.R
import com.readrops.app.home.TabScreenModel
import com.readrops.app.repositories.GetFoldersWithFeeds
import com.readrops.app.util.components.TextFieldError
import com.readrops.app.util.components.dialog.TextFieldDialogState
import com.readrops.app.util.extensions.isConnected
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.OpenIn
import com.readrops.db.filters.MainFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import androidx.work.workDataOf
import com.readrops.app.sync.SyncWorker
import com.readrops.app.util.extensions.isValidFeedUrl

@OptIn(ExperimentalCoroutinesApi::class)
class FeedScreenModel(
    private val database: Database,
    private val getFoldersWithFeeds: GetFoldersWithFeeds,
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TabScreenModel(database, context), KoinComponent {

    private val _feedState = MutableStateFlow(FeedState())
    val feedsState = _feedState.asStateFlow()

    /**
     * The feeds screen shows unread counts, which only a synchronization updates. Pulling on a
     * list is the expected gesture there as much as on the timeline, so the same worker is
     * started. It is enqueued as unique work, so pulling while a sync runs does not stack.
     */
    fun refreshFeeds() {
        if (!context.isConnected()) {
            _feedState.update { it.copy(error = context.getString(R.string.no_network)) }
            return
        }

        val account = currentAccount ?: return

        screenModelScope.launch(dispatcher) {
            _feedState.update { it.copy(isRefreshing = true) }

            SyncWorker.startNow(
                context,
                workDataOf(SyncWorker.ACCOUNT_ID_KEY to account.id)
            ) { workInfo ->
                if (workInfo.state.isFinished) {
                    _feedState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    private val _updateFeedDialogState = MutableStateFlow(UpdateFeedDialogState())
    val updateFeedDialogState = _updateFeedDialogState.asStateFlow()

    private val _folderState = MutableStateFlow(TextFieldDialogState())
    val folderState = _folderState.asStateFlow()

    init {
        screenModelScope.launch(dispatcher) {
            accountEvent.flatMapLatest { account ->
                _feedState.update {
                    it.copy(
                        isAccountNotificationsEnabled = account.isNotificationsEnabled,
                        config = account.config
                    )
                }

                _updateFeedDialogState.update {
                    it.copy(
                        isFeedUrlReadOnly = account.config.isFeedUrlReadOnly,
                        isAuthAvailable = account.isLocal,
                    )
                }

                getFoldersWithFeeds.get(
                    account.id,
                    MainFilter.ALL,
                    account.config.useSeparateState
                )
            }
                .catch { throwable ->
                    _feedState.update {
                        it.copy(foldersAndFeeds = FolderAndFeedsState.ErrorState(Exception(throwable)))
                    }
                }
                .collect { foldersAndFeeds ->
                    _feedState.update { state ->
                        val dialog = when (state.dialog) {
                            is DialogState.FeedSheet -> {
                                val feed = foldersAndFeeds.values.flatten()
                                    .first { it.id == state.dialog.feed.id }
                                state.dialog.copy(feed = feed)
                            }

                            is DialogState.UpdateFeedOpenInSetting -> {
                                val feed = foldersAndFeeds.values.flatten()
                                    .first { it.id == state.dialog.feed.id }
                                state.dialog.copy(feed = feed)
                            }

                            else -> {
                                state.dialog
                            }
                        }

                        state.copy(
                            foldersAndFeeds = FolderAndFeedsState.LoadedState(foldersAndFeeds),
                            dialog = dialog
                        )
                    }
                }
        }

        screenModelScope.launch(dispatcher) {
            accountEvent.flatMapLatest { account ->
                _updateFeedDialogState.update {
                    it.copy(
                        isFeedUrlReadOnly = account.config.isFeedUrlReadOnly,
                        isAuthAvailable = account.isLocal,
                    )
                }

                database.folderDao().selectFolders(account.id)
            }
                .collect { folders ->
                    _updateFeedDialogState.update {
                        it.copy(
                            folders = if (currentAccount!!.config.addNoFolder) {
                                folders + listOf(
                                    Folder(
                                        id = 0,
                                        name = context.resources.getString(R.string.no_folder)
                                    )
                                )
                            } else {
                                folders
                            }
                        )
                    }
                }
        }
    }

    fun setFolderExpandState(isExpanded: Boolean) =
        _feedState.update { it.copy(areFoldersExpanded = isExpanded) }

    fun closeDialog(dialog: DialogState? = null) {
        when (dialog) {
            is DialogState.AddFolder, is DialogState.UpdateFolder -> {
                _folderState.update {
                    it.copy(
                        value = "",
                        textFieldError = null,
                        error = null,
                        isLoading = false
                    )
                }
            }

            is DialogState.UpdateFeed -> {
                _updateFeedDialogState.update { it.copy(error = null, isLoading = false) }
            }

            else -> {}
        }

        if (dialog is DialogState.UpdateFeedOpenInSetting) {
            _feedState.update {
                it.copy(
                    dialog = DialogState.FeedSheet(
                        feed = dialog.feed,
                        folder = null,
                        config = currentAccount!!.config
                    )
                )
            }
        } else {
            _feedState.update { it.copy(dialog = null) }
        }
    }

    fun openDialog(state: DialogState) {
        when (state) {
            is DialogState.UpdateFeed -> {
                _updateFeedDialogState.update {
                    it.copy(
                        feedId = state.feed.id,
                        feedName = state.feed.name!!,
                        feedUrl = state.feed.url!!,
                        selectedFolder = state.folder
                            ?: it.folders.find { folder -> folder.id == 0 },
                        feedRemoteId = state.feed.remoteId,
                        isAuthExpanded = false,
                        isPasswordVisible = false,
                        login = "",
                        loginError = null,
                        // the stored password is never read back into the form: the user
                        // confirms a new one or leaves the field blank to keep the old one
                        password = "",
                        passwordError = null,
                        hasStoredCredentials = false
                    )
                }

                loadFeedCredentials(state.feed)
            }

            is DialogState.UpdateFolder -> {
                _folderState.update {
                    it.copy(
                        value = state.folder.name.orEmpty()
                    )
                }
            }

            else -> {}
        }

        _feedState.update { it.copy(dialog = state) }
    }

    fun deleteFeed(feed: Feed) {
        if (!checkInternetConnection()) {
            return
        }

        screenModelScope.launch(dispatcher) {
            try {
                repository?.deleteFeed(feed)

                // the credentials live in the encrypted preferences, which the database
                // deletion does not reach: without this they would outlive the feed
                get<SharedPreferences>().edit()
                    .remove(feed.loginKey)
                    .remove(feed.passwordKey)
                    .apply()
            } catch (e: Exception) {
                _feedState.update { it.copy(error = accountError?.deleteFeedMessage(e)) }
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        if (!checkInternetConnection()) {
            return
        }

        screenModelScope.launch(dispatcher) {
            try {
                repository?.deleteFolder(folder)
            } catch (e: Exception) {
                _feedState.update { it.copy(error = accountError?.deleteFolderMessage(e)) }
            }
        }
    }

    //region Update feed

    fun setFolderDropDownState(isExpanded: Boolean) {
        _updateFeedDialogState.update {
            it.copy(isFolderDropDownExpanded = isExpanded)
        }
    }

    fun setSelectedFolder(folder: Folder) {
        _updateFeedDialogState.update {
            it.copy(selectedFolder = folder)
        }
    }

    fun setUpdateFeedDialogStateFeedName(feedName: String) {
        _updateFeedDialogState.update {
            it.copy(
                feedName = feedName,
                feedNameError = null,
            )
        }
    }

    fun setUpdateFeedDialogFeedUrl(feedUrl: String) {
        _updateFeedDialogState.update {
            it.copy(
                feedUrl = feedUrl,
                feedUrlError = null,
            )
        }
    }

    fun setUpdateFeedDialogLogin(login: String) {
        _updateFeedDialogState.update {
            it.copy(login = login, loginError = null, error = null)
        }
    }

    fun setUpdateFeedDialogPassword(password: String) {
        _updateFeedDialogState.update {
            it.copy(password = password, passwordError = null, error = null)
        }
    }

    fun setUpdateFeedDialogPasswordVisibility(isVisible: Boolean) {
        _updateFeedDialogState.update { it.copy(isPasswordVisible = isVisible) }
    }

    fun toggleUpdateFeedDialogAuth() {
        _updateFeedDialogState.update { it.copy(isAuthExpanded = !it.isAuthExpanded) }
    }

    /**
     * Only empties the fields. Nothing is written until the user validates, which keeps the
     * whole dialog consistent: no change applies before the validate button.
     */
    fun clearUpdateFeedDialogCredentials() {
        _updateFeedDialogState.update {
            it.copy(login = "", password = "", loginError = null, passwordError = null)
        }
    }

    private fun loadFeedCredentials(feed: Feed) {
        screenModelScope.launch(dispatcher) {
            val preferences = get<SharedPreferences>()
            val login = preferences.getString(feed.loginKey, null).orEmpty()
            val hasPassword = !preferences.getString(feed.passwordKey, null).isNullOrEmpty()

            _updateFeedDialogState.update {
                it.copy(
                    login = login,
                    hasStoredCredentials = login.isNotEmpty() && hasPassword
                )
            }
        }
    }

    /**
     * A blank password on a feed that already has one means "keep the current password", so the
     * user never has to retype a secret just to rename the feed. Clearing the login removes the
     * credentials entirely.
     */
    private fun persistFeedCredentials(feedId: Int, login: String, password: String) {
        val feed = Feed(id = feedId)
        val preferences = get<SharedPreferences>()

        val editor = preferences.edit()

        if (login.isEmpty()) {
            editor.remove(feed.loginKey)
            editor.remove(feed.passwordKey)
        } else {
            editor.putString(feed.loginKey, login)
            if (password.isNotEmpty()) {
                editor.putString(feed.passwordKey, password)
            }
        }

        editor.apply()
    }

    fun updateFeedDialogValidate() {
        val feedName = _updateFeedDialogState.value.feedName
        val feedUrl = _updateFeedDialogState.value.feedUrl
        val isAuthAvailable = _updateFeedDialogState.value.isAuthAvailable
        val login = _updateFeedDialogState.value.login
        val password = _updateFeedDialogState.value.password
        val hasStoredCredentials = _updateFeedDialogState.value.hasStoredCredentials

        when {
            feedName.isEmpty() -> {
                _updateFeedDialogState.update {
                    it.copy(feedNameError = TextFieldError.EmptyField)
                }
                return
            }

            feedUrl.isEmpty() -> {
                _updateFeedDialogState.update {
                    it.copy(feedUrlError = TextFieldError.EmptyField)
                }
                return
            }

            !feedUrl.isValidFeedUrl() -> {
                _updateFeedDialogState.update {
                    it.copy(feedUrlError = TextFieldError.BadUrl)
                }
                return
            }

            // HTTP basic authentication needs both halves, so a lone password is rejected
            isAuthAvailable && login.isEmpty() && password.isNotEmpty() -> {
                _updateFeedDialogState.update {
                    it.copy(loginError = TextFieldError.EmptyField, isAuthExpanded = true)
                }
                return
            }

            // a blank password is only allowed when one is already stored, in which case it
            // means "keep it"
            isAuthAvailable && login.isNotEmpty() && password.isEmpty()
                    && !hasStoredCredentials -> {
                _updateFeedDialogState.update {
                    it.copy(passwordError = TextFieldError.EmptyField, isAuthExpanded = true)
                }
                return
            }

            else -> {
                if (!context.isConnected()) {
                    _updateFeedDialogState.update { it.copy(error = context.getString(R.string.no_network)) }
                    return
                } else {
                    _updateFeedDialogState.update { it.copy(error = null, isLoading = true) }
                }

                screenModelScope.launch(dispatcher) {
                    with(_updateFeedDialogState.value) {
                        try {
                            repository?.updateFeed(
                                Feed(
                                    id = feedId,
                                    name = feedName,
                                    url = feedUrl,
                                    folderId = if (selectedFolder?.id != 0)
                                        selectedFolder?.id
                                    else null,
                                    remoteFolderId = selectedFolder?.remoteId,
                                    remoteId = feedRemoteId
                                )
                            )
                        } catch (e: Exception) {
                            _updateFeedDialogState.update {
                                it.copy(
                                    error = accountError?.updateFeedMessage(e),
                                    isLoading = false
                                )
                            }
                            return@launch
                        }

                        if (isAuthAvailable) {
                            persistFeedCredentials(feedId, login, password)
                        }
                    }

                    closeDialog(_feedState.value.dialog)
                }
            }
        }
    }

    //endregion

    //region Add/Update folder

    fun setFolderName(name: String) = _folderState.update {
        it.copy(
            value = name,
            textFieldError = null,
        )
    }

    fun folderValidate(updateFolder: Boolean = false) {
        val name = _folderState.value.value

        if (name.isEmpty()) {
            _folderState.update {
                it.copy(
                    textFieldError = TextFieldError.EmptyField,
                    isLoading = false
                )
            }
            return
        }

        if (!context.isConnected()) {
            _folderState.update { it.copy(error = context.getString(R.string.no_network)) }
            return
        } else {
            _folderState.update { it.copy(isLoading = true) }
        }

        screenModelScope.launch(dispatcher) {
            try {
                if (updateFolder) {
                    val folder = (_feedState.value.dialog as DialogState.UpdateFolder).folder
                    repository?.updateFolder(folder.copy(name = name))
                } else {
                    repository?.addFolder(Folder(name = name, accountId = currentAccount!!.id))
                }
            } catch (e: Exception) {
                _folderState.update {
                    it.copy(
                        error = if (updateFolder) {
                            accountError?.updateFolderMessage(e)
                        } else {
                            accountError?.newFolderMessage(e)
                        },
                        isLoading = false
                    )
                }
                return@launch
            }

            closeDialog(_feedState.value.dialog)
        }
    }

    //endregion

    fun resetException() = _feedState.update { it.copy(error = null) }

    fun updateFeedNotifications(feedId: Int, isEnabled: Boolean) {
        screenModelScope.launch(dispatcher) {
            database.feedDao().updateFeedNotificationState(feedId, isEnabled)
        }
    }

    fun updateFeedOpenInSetting(feedId: Int, openIn: OpenIn) {
        screenModelScope.launch(dispatcher) {
            database.feedDao().updateOpenInSetting(feedId, openIn)
        }
    }

    private fun checkInternetConnection(): Boolean {
        if (!currentAccount!!.isLocal) {
            return true
        }

        val isConnected = context.isConnected()

        if (!isConnected) {
            _feedState.update { it.copy(error = context.getString(R.string.no_network)) }
        }

        return isConnected
    }
}