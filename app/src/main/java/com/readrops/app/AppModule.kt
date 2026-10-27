@file:Suppress("DEPRECATION")

package com.readrops.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.security.keystore.KeyPermanentlyInvalidatedException
import com.readrops.api.services.Credentials
import com.readrops.api.utils.NetworkFailureListener
import com.readrops.app.account.AccountScreenModel
import com.readrops.app.account.backup.AccountBackupManager
import com.readrops.app.account.credentials.AccountCredentialsScreenMode
import com.readrops.app.account.credentials.AccountCredentialsScreenModel
import com.readrops.app.account.selection.AccountSelectionScreenModel
import com.readrops.app.feeds.FeedScreenModel
import com.readrops.app.feeds.color.FeedColorScreenModel
import com.readrops.app.feeds.newfeed.NewFeedScreenModel
import com.readrops.app.item.ItemScreenModel
import com.readrops.app.more.diagnostics.DiagnosticLogScreenModel
import com.readrops.app.more.preferences.PreferencesScreenModel
import com.readrops.app.notifications.NotificationsScreenModel
import com.readrops.app.repositories.BaseRepository
import com.readrops.app.repositories.FeverRepository
import com.readrops.app.repositories.GReaderRepository
import com.readrops.app.repositories.GetFoldersWithFeeds
import com.readrops.app.repositories.LocalRSSRepository
import com.readrops.app.repositories.MinifluxRepository
import com.readrops.app.repositories.NextcloudNewsRepository
import com.readrops.app.sync.SyncAnalyzer
import com.readrops.app.sync.Synchronizer
import com.readrops.app.timelime.TimelineScreenModel
import com.readrops.app.util.DataStorePreferences
import com.readrops.app.util.Preferences
import com.readrops.app.util.diagnostics.DiagnosticLog
import com.readrops.app.util.diagnostics.diagnosticNetworkListener
import com.readrops.db.entities.Feed
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import com.readrops.db.filters.QueryFilters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException
import java.io.File

val appModule = module {

    factory { TimelineScreenModel(get(), get(), get(), androidContext()) }

    factory { FeedScreenModel(get(), get(), androidContext(), get()) }

    factory { (url: String?) -> NewFeedScreenModel(get(), get(), androidContext(), url) }

    factory { AccountSelectionScreenModel(get(), get()) }

    factory { AccountScreenModel(get(), androidContext()) }

    factory { (itemId: Int, itemIndex: Int, queryFilters: QueryFilters) ->
        ItemScreenModel(
            itemId = itemId,
            itemIndex = itemIndex,
            queryFilters = queryFilters,
            database = get(),
            preferences = get()
        )
    }

    factory { (accountType: Account, mode: AccountCredentialsScreenMode) ->
        AccountCredentialsScreenModel(accountType, mode, get(), context = androidContext())
    }

    factory { (account: Account) -> NotificationsScreenModel(account, get(), get(), get()) }

    factory { PreferencesScreenModel(get(), get(), get()) }

    factory { (feed: Feed) -> FeedColorScreenModel(feed, get()) }

    single { GetFoldersWithFeeds(get()) }

    factory { AccountBackupManager(get(), get(), get()) }

    factory<BaseRepository> { (account: Account) ->
        when (account.type) {
            AccountType.LOCAL, AccountType.FEEDLY -> LocalRSSRepository(get(), get(), account, get())
            AccountType.FRESHRSS, AccountType.GREADER -> GReaderRepository(
                database = get(),
                account = account,
                dataSource = get(parameters = { parametersOf(Credentials.toCredentials(account)) })
            )

            AccountType.NEXTCLOUD_NEWS -> NextcloudNewsRepository(
                database = get(),
                account = account,
                dataSource = get(parameters = { parametersOf(Credentials.toCredentials(account)) })
            )

            AccountType.MINIFLUX -> MinifluxRepository(
                database = get(),
                account = account,
                dataSource = get(parameters = { parametersOf(Credentials.toCredentials(account)) })
            )

            AccountType.FEVER -> FeverRepository(
                database = get(),
                account = account,
                feverDataSource = get(parameters = { parametersOf(Credentials.toCredentials(account)) })
            )

            else -> throw IllegalArgumentException("Unknown account type")
        }
    }

    single {
        createEncryptedPreferences(androidContext())
    }

    single {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(SharedPreferencesMigration(get(), "settings")),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { get<Context>().preferencesDataStoreFile("settings") }
        )
    }

    single { DataStorePreferences(get()) }

    single { Preferences(get()) }

    single { NotificationManagerCompat.from(get()) }

    single { Synchronizer(get(), get(), get(), get()) }

    single { SyncAnalyzer(get(), get()) }

    single { DiagnosticLog(File(androidContext().filesDir, DiagnosticLog.FILE_NAME)) }

    single<NetworkFailureListener> { diagnosticNetworkListener(get(), androidContext()) }

    factory { DiagnosticLogScreenModel(get(), get(), androidContext()) }
}

// security-crypto 1.1 currently has no non-deprecated replacement for this
// encrypted SharedPreferences implementation. Keep the suppression isolated
// so the rest of the app remains warning-clean without weakening encryption.
@Suppress("DEPRECATION")
internal fun createEncryptedPreferences(
    context: Context,
    preferencesName: String = ACCOUNT_CREDENTIALS_PREFERENCES,
    masterKeyAlias: String = MasterKey.DEFAULT_MASTER_KEY_ALIAS
): SharedPreferences = try {
    openEncryptedPreferences(context, preferencesName, masterKeyAlias)
} catch (exception: Exception) {
    if (!exception.hasEncryptedPreferencesKeyFailure()) throw exception

    // An old device backup can restore the ciphertext without its Keystore key.
    // Those values cannot be recovered, but removing the stale file lets the app
    // start and create a fresh encrypted store instead of crashing at launch.
    context.deleteSharedPreferences(preferencesName)
    runCatching {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            deleteEntry(masterKeyAlias)
        }
    }

    openEncryptedPreferences(context, preferencesName, masterKeyAlias)
}

@Suppress("DEPRECATION")
private fun openEncryptedPreferences(
    context: Context,
    preferencesName: String,
    masterKeyAlias: String
): SharedPreferences {
    val masterKey = MasterKey.Builder(context, masterKeyAlias)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        preferencesName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

private fun Throwable.hasEncryptedPreferencesKeyFailure(): Boolean =
    generateSequence(this) { it.cause }.any {
        it is AEADBadTagException ||
            it is KeyPermanentlyInvalidatedException ||
            it is UnrecoverableKeyException
    }

private const val ACCOUNT_CREDENTIALS_PREFERENCES = "account_credentials"
