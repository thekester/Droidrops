package com.readrops.app.repositories

import com.readrops.api.services.Credentials
import com.readrops.api.services.SyncType
import com.readrops.api.services.feedbin.FeedbinDataSource
import com.readrops.api.utils.AuthInterceptor
import com.readrops.app.util.Utils
import com.readrops.app.util.diagnostics.DiagnosticLog
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.ItemState
import com.readrops.db.entities.account.Account
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class FeedbinRepository(
    database: Database,
    account: Account,
    private val dataSource: FeedbinDataSource
) : BaseRepository(database, account), KoinComponent {
    override suspend fun login(account: Account) {
        get<AuthInterceptor>().credentials = Credentials.toCredentials(account)
        account.displayedName = dataSource.login()
        get<DiagnosticLog>().info("Feedbin", "Login validation succeeded")
    }

    override suspend fun synchronize(selectedFeeds: List<Feed>, onUpdate: suspend (Feed) -> Unit) =
        throw NotImplementedError("Feedbin uses account-wide synchronization")

    override suspend fun synchronize(): SyncResult {
        val pending = database.itemStateChangeDao().selectItemStateChanges(account.id)
        val syncType = if (account.lastModified == 0L) SyncType.INITIAL_SYNC else SyncType.CLASSIC_SYNC
        val startedAt = System.currentTimeMillis() / 1000L
        val result = dataSource.synchronize(
            syncType = syncType,
            lastModified = account.lastModified,
            readIds = pending.filter { it.readChange && it.read }.map { it.remoteId },
            unreadIds = pending.filter { it.readChange && !it.read }.map { it.remoteId },
            starredIds = pending.filter { it.starChange && it.starred }.map { it.remoteId },
            unstarredIds = pending.filter { it.starChange && !it.starred }.map { it.remoteId },
            initialSyncCursor = account.syncCursor
        )
        result.feeds.forEach { it.accountId = account.id }
        database.feedDao().upsertFeeds(result.feeds, account)
        val localFeeds = database.feedDao().selectFeeds(account.id).associate { it.remoteId to it.id }
        val incomingIds = result.items.mapNotNull { it.remoteId }.distinct()
        val existing = incomingIds.chunked(900).flatMap {
            database.itemDao().selectExistingItems(it, account.id)
        }.associateBy { it.remoteId }
        val oldStates = incomingIds.chunked(900).flatMap {
            database.itemStateDao().selectItemStatesByRemoteIds(it, account.id)
        }.associateBy { it.remoteId }
        val fresh = mutableListOf<Item>()
        val states = mutableListOf<ItemState>()
        for (item in result.items) {
            val remoteId = item.remoteId ?: continue
            val feedId = localFeeds[item.feedRemoteId] ?: continue
            item.feedId = feedId
            item.readTime = Utils.readTimeFromString(item.content.orEmpty())
            val old = existing[remoteId]
            if (old == null) fresh += item else {
                item.id = old.id
                database.itemDao().update(item)
            }
            val next = ItemState(read = item.isRead, starred = item.isStarred, remoteId = remoteId, accountId = account.id)
            val previous = oldStates[remoteId]
            if (previous == null) states += next else {
                if (previous.read != next.read) database.itemStateDao().updateItemReadState(next.read, remoteId, account.id)
                if (previous.starred != next.starred) database.itemStateDao().updateItemStarState(next.starred, remoteId, account.id)
            }
        }
        if (states.isNotEmpty()) database.itemStateDao().insertIgnoreConflicts(states)
        if (fresh.isNotEmpty()) database.itemDao().insert(fresh).zip(fresh).forEach { (id, item) -> item.id = id.toInt() }
        account.lastModified = if (syncType == SyncType.INITIAL_SYNC && result.syncCursor?.startsWith("initial|") == true) 0L else startedAt
        account.syncCursor = result.syncCursor
        database.accountDao().updateSyncProgress(account.lastModified, account.syncCursor, account.id)
        database.itemStateChangeDao().resetStateChanges(account.id)
        return SyncResult(items = fresh, feeds = result.feeds, continueSync = result.syncCursor != null)
    }

    override suspend fun insertNewFeeds(newFeeds: List<Feed>, onUpdate: (Feed) -> Unit): ErrorResult {
        val errors = hashMapOf<Feed, Exception>()
        newFeeds.forEach { feed ->
            try {
                val remoteFeed = dataSource.createSubscription(feed.url!!)
                remoteFeed.accountId = account.id
                database.feedDao().upsertFeeds(listOf(remoteFeed), account)
            } catch (exception: Exception) {
                errors[feed] = exception
            } finally {
                onUpdate(feed)
            }
        }
        return errors
    }

    override suspend fun updateFeed(feed: Feed) {
        dataSource.updateSubscription(feed.remoteId!!.toLong(), feed.name!!)
        super.updateFeed(feed)
    }

    override suspend fun deleteFeed(feed: Feed) {
        dataSource.deleteSubscription(feed.remoteId!!.toLong())
        super.deleteFeed(feed)
    }

    override suspend fun addFolder(folder: Folder) {
        throw UnsupportedOperationException("Feedbin does not support folders")
    }
}
