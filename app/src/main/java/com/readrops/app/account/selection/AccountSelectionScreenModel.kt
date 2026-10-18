package com.readrops.app.account.selection

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.readrops.api.opml.OPMLParser
import com.readrops.app.R
import com.readrops.app.account.backup.AccountBackupManager
import com.readrops.app.account.backup.BackupPreference
import com.readrops.app.account.backup.LocalAccountBackupFormat.InvalidBackupException
import com.readrops.app.account.hasImportableFeeds
import com.readrops.app.repositories.BaseRepository
import com.readrops.app.util.accounterror.AccountError
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class AccountSelectionScreenModel(
    private val database: Database,
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : StateScreenModel<AccountSelectionState>(AccountSelectionState()), KoinComponent {

    private val accountError = AccountError.Companion.DefaultAccountError(context)

    fun accountExists(): Boolean {
        val accountCount = runBlocking {
            database.accountDao().selectAccountCount()
        }

        return accountCount > 0
    }

    fun createAccount(accountType: AccountType) {
        if (accountType == AccountType.LOCAL) {
            screenModelScope.launch(dispatcher) {
                createLocalAccount()
                mutableState.update { it.copy(navigation = Navigation.HomeScreen) }
            }
        } else {
            mutableState.update {
                it.copy(navigation = Navigation.AccountCredentialsScreen(accountType))
            }
        }
    }

    fun resetNavigation() {
        mutableState.update { it.copy(navigation = null) }
    }

    private suspend fun createLocalAccount(): Account {
        val context = get<Context>()
        val account = Account(
            url = null,
            name = context.getString(AccountType.LOCAL.nameRes),
            type = AccountType.LOCAL,
            isCurrentAccount = true
        )

        account.id = database.accountDao().insert(account).toInt()
        return account
    }

    fun parseOPMLFile(uri: Uri, context: Context) {
        screenModelScope.launch(dispatcher) {
            val foldersAndFeeds: Map<Folder?, List<Feed>>

            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    mutableState.update { it.copy(error = accountError.genericMessage(NoSuchFileException(uri.toFile()))) }
                    return@launch
                }

                foldersAndFeeds = OPMLParser.read(stream)
            } catch (e: Exception) {
                mutableState.update { it.copy(error = accountError.genericMessage(e)) }
                return@launch
            }

            if (!foldersAndFeeds.hasImportableFeeds()) {
                mutableState.update { it.copy(error = context.getString(R.string.opml_no_feeds)) }
                return@launch
            }

            mutableState.update {
                it.copy(
                    dialog = DialogState.OPMLImport,
                    currentFeed = foldersAndFeeds.values.first().first().name,
                    feedCount = 0,
                    feedMax = foldersAndFeeds.values.flatten().size
                )
            }

            val account = createLocalAccount()
            val repository = get<BaseRepository> { parametersOf(account) }

            repository.insertOPMLFoldersAndFeeds(
                foldersAndFeeds = foldersAndFeeds,
                onUpdate = { feed ->
                    mutableState.update {
                        it.copy(
                            currentFeed = feed.name,
                            feedCount = it.feedCount + 1
                        )
                    }
                }
            )

            mutableState.update {
                it.copy(
                    dialog = null,
                    navigation = Navigation.HomeScreen
                )
            }
        }
    }

    /** Restores a backup as a new local account, typically when setting up a new device. */
    fun restoreBackup(uri: Uri, context: Context) {
        mutableState.update { it.copy(dialog = DialogState.BackupInProgress) }

        screenModelScope.launch(dispatcher) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw NoSuchFileException(uri.toFile())
                val result = get<AccountBackupManager>().restore(stream, context)

                mutableState.update {
                    if (result.preferences.isNotEmpty()) {
                        it.copy(dialog = DialogState.RestorePreferences(result.preferences))
                    } else {
                        it.copy(dialog = null, navigation = Navigation.HomeScreen)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = if (e is InvalidBackupException || e is JsonDataException || e is JsonEncodingException) {
                    context.getString(R.string.backup_invalid_file)
                } else {
                    accountError.genericMessage(e)
                }
                mutableState.update { it.copy(dialog = null, error = message) }
            }
        }
    }

    /** Ends the restore, applying the app settings of the backup if [restore] is true. */
    fun finishRestore(preferences: List<BackupPreference>, restore: Boolean) {
        screenModelScope.launch(dispatcher) {
            if (restore) get<AccountBackupManager>().applyPreferences(preferences)
            mutableState.update { it.copy(dialog = null, navigation = Navigation.HomeScreen) }
        }
    }

    fun resetException() = mutableState.update { it.copy(error = null) }

    fun openDialog(dialog: DialogState) = mutableState.update { it.copy(dialog = dialog) }

    fun closeDialog() = mutableState.update { it.copy(dialog = null) }
}

data class AccountSelectionState(
    val error: String? = null,
    val currentFeed: String? = null,
    val feedCount: Int = 0,
    val feedMax: Int = 0,
    val dialog: DialogState? = null,
    val navigation: Navigation? = null
)

sealed class Navigation {
    data object HomeScreen : Navigation()
    data class AccountCredentialsScreen(val type: AccountType) : Navigation()
}

sealed interface DialogState {
    data object OPMLImport : DialogState
    data class AccountWarning(val type: AccountType) : DialogState
    data object BackupInProgress : DialogState
    data class RestorePreferences(val preferences: List<BackupPreference>) : DialogState
}
