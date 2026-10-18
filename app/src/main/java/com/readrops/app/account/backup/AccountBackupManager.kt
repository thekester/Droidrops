package com.readrops.app.account.backup

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import com.readrops.app.R
import com.readrops.db.Database
import com.readrops.db.entities.account.Account
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import androidx.datastore.preferences.core.Preferences as DataStorePreferences
import com.readrops.app.util.Preferences as AppPreferences

/** Account backups together with what lives outside the database: per-feed and app settings. */
class AccountBackupManager(
    database: Database,
    private val dataStore: DataStore<DataStorePreferences>,
    private val preferences: AppPreferences,
) {

    private val repository = LocalAccountBackupRepository(database)

    suspend fun export(account: Account, output: OutputStream): BackupSummary =
        repository.export(
            account = account,
            output = output,
            fullContentFeedIds = preferences.fullContentFeeds.flow.first()
                .mapNotNull(String::toIntOrNull)
                .toSet(),
            preferences = BackupPreference.fromDataStore(dataStore.data.first()),
        )

    /**
     * Restores the account and its per-feed settings. App settings are returned in the result
     * and only applied through [applyPreferences], once the user agreed to replace theirs.
     */
    suspend fun restore(input: InputStream, context: Context): RestoreResult {
        val result = repository.restore(
            input = input,
            defaultAccountName = context.getString(com.readrops.db.entities.account.AccountType.LOCAL.nameRes),
            // tells it apart from the account it was backed up from, if still present
            restoredAccountName = { context.getString(R.string.backup_restored_account_name, it) }
        )

        result.fullContentFeedIds.forEach { preferences.setFullContentFeed(it, enabled = true) }
        return result
    }

    suspend fun applyPreferences(backup: List<BackupPreference>) {
        dataStore.edit { BackupPreference.applyTo(it, backup) }
    }
}

fun BackupSummary.describe(context: Context, @StringRes message: Int): String = context.getString(
    message,
    context.resources.getQuantityString(R.plurals.backup_feed_count, feedCount, feedCount),
    context.resources.getQuantityString(R.plurals.backup_article_count, itemCount, itemCount),
)
