package com.readrops.app.repositories

import android.util.Log
import com.readrops.api.services.Credentials
import com.readrops.api.services.SyncType
import com.readrops.api.services.miniflux.MinifluxDataSource
import com.readrops.api.utils.AuthInterceptor
import com.readrops.app.util.Utils
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.ItemState
import com.readrops.db.entities.account.Account
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MinifluxRepository(
    database: Database,
    account: Account,
    private val dataSource: MinifluxDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseRepository(database, account), KoinComponent {

    override suspend fun login(account: Account) {
        get<AuthInterceptor>().credentials = Credentials.toCredentials(account)
        account.displayedName = dataSource.login()
    }

    override suspend fun synchronize(
        selectedFeeds: List<Feed>,
        onUpdate: suspend (Feed) -> Unit
    ): Pair<SyncResult, ErrorResult> = throw NotImplementedError("This method can't be called here")

    override suspend fun synchronize(): SyncResult {
        val changes = database.itemStateChangeDao().selectItemStateChanges(account.id)
        val syncType = if (account.lastModified == 0L) SyncType.INITIAL_SYNC else SyncType.CLASSIC_SYNC
        val syncStartedAt = System.currentTimeMillis() / 1000L
        val result = dataSource.synchronize(
            syncType = syncType,
            lastModified = account.lastModified,
            readIds = changes.filter { it.readChange && it.read }.map { it.remoteId },
            unreadIds = changes.filter { it.readChange && !it.read }.map { it.remoteId },
            starredIds = changes.filter { it.starChange && it.starred }.map { it.remoteId },
            unstarredIds = changes.filter { it.starChange && !it.starred }.map { it.remoteId }
        )

        insertFolders(result.folders)
        val newFeeds = insertFeeds(result.feeds)
        val newItems = insertItems(result.items)

        account.lastModified = syncStartedAt
        database.accountDao().updateLastModified(syncStartedAt, account.id)
        database.itemStateChangeDao().resetStateChanges(account.id)

        return SyncResult(items = newItems, feeds = newFeeds)
    }

    override suspend fun insertNewFeeds(
        newFeeds: List<Feed>,
        onUpdate: (Feed) -> Unit
    ): ErrorResult {
        val errors = hashMapOf<Feed, Exception>()
        var attempted = false

        for (newFeed in newFeeds) {
            onUpdate(newFeed)
            attempted = true
            try {
                val folder = newFeed.folderId?.let { database.folderDao().select(it) }
                dataSource.createFeed(newFeed.url!!, folder?.remoteId)
            } catch (exception: Exception) {
                errors[newFeed] = exception
            }
        }

        if (attempted) {
            try {
                insertFolders(dataSource.getFolders())
                val remoteFeeds = dataSource.getFeeds()
                val remoteUrls = remoteFeeds.mapNotNull { it.url }.toSet()
                errors.keys.filter { it.url in remoteUrls }.forEach(errors::remove)
                insertFeeds(remoteFeeds)
            } catch (exception: Exception) {
                Log.w(TAG, "Unable to refresh Miniflux feeds after subscription", exception)
            }
        }

        return errors
    }

    override suspend fun updateFeed(feed: Feed) {
        val folder = feed.folderId?.let { database.folderDao().select(it) }
        dataSource.updateFeed(feed.remoteId!!.toLong(), feed.name!!, folder?.remoteId)
        super.updateFeed(feed)
    }

    override suspend fun deleteFeed(feed: Feed) {
        dataSource.deleteFeed(feed.remoteId!!.toLong())
        super.deleteFeed(feed)
    }

    override suspend fun addFolder(folder: Folder) {
        database.folderDao().insert(dataSource.createFolder(folder.name!!).apply {
            accountId = account.id
        })
    }

    override suspend fun updateFolder(folder: Folder) {
        dataSource.updateFolder(folder.remoteId!!.toLong(), folder.name!!)
        super.updateFolder(folder)
    }

    override suspend fun deleteFolder(folder: Folder) {
        dataSource.deleteFolder(folder.remoteId!!.toLong())
        super.deleteFolder(folder)
    }

    private suspend fun insertFolders(folders: List<Folder>) {
        folders.forEach { it.accountId = account.id }
        database.folderDao().upsertFolders(folders, account)
    }

    private suspend fun insertFeeds(feeds: List<Feed>): List<Feed> {
        feeds.forEach { it.accountId = account.id }
        return database.feedDao().upsertFeeds(feeds, account)
    }

    private suspend fun insertItems(items: List<Item>): List<Item> {
        if (items.isEmpty()) return emptyList()

        val remoteIds = items.mapNotNull { it.remoteId }.distinct()
        val existing = database.itemDao().selectExistingItems(remoteIds, account.id)
            .associateBy { it.remoteId }
        val localFeedIds = database.feedDao().selectFeeds(account.id)
            .associate { it.remoteId to it.id }
        val existingStates = database.itemStateDao().selectAllItemStates(account.id)
            .associateBy { it.remoteId }
        val newItems = mutableListOf<Item>()
        val newStates = mutableListOf<ItemState>()

        for (item in items) {
            val remoteId = item.remoteId ?: continue
            val remoteFeedId = item.feedRemoteId ?: continue
            val feedId = localFeedIds[remoteFeedId] ?: continue

            item.feedId = feedId
            item.readTime = Utils.readTimeFromString(item.content.orEmpty())

            val old = existing[remoteId]
            if (old == null) {
                newItems += item
            } else {
                item.id = old.id
                database.itemDao().update(item)
            }

            val newState = ItemState(
                read = item.isRead,
                starred = item.isStarred,
                remoteId = remoteId,
                accountId = account.id
            )
            val oldState = existingStates[remoteId]
            if (oldState == null) {
                newStates += newState
            } else {
                if (oldState.read != newState.read) {
                    database.itemStateDao().updateItemReadState(newState.read, remoteId, account.id)
                }
                if (oldState.starred != newState.starred) {
                    database.itemStateDao().updateItemStarState(newState.starred, remoteId, account.id)
                }
            }
        }

        if (newStates.isNotEmpty()) database.itemStateDao().insertIgnoreConflicts(newStates)

        if (newItems.isNotEmpty()) {
            database.itemDao().insert(newItems)
                .zip(newItems)
                .forEach { (id, item) -> item.id = id.toInt() }
        }

        return newItems
    }

    companion object {
        private val TAG = MinifluxRepository::class.java.simpleName
    }
}
