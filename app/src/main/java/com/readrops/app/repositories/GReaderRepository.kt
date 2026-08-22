package com.readrops.app.repositories

import android.util.Log
import com.readrops.api.services.Credentials
import com.readrops.api.services.SyncType
import com.readrops.api.services.greader.GReaderDataSource
import com.readrops.api.services.greader.GReaderSyncData
import com.readrops.api.utils.AuthInterceptor
import com.readrops.app.util.Utils
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.ItemState
import com.readrops.db.entities.Tag
import com.readrops.db.entities.TagJoin
import com.readrops.db.entities.account.Account
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class GReaderRepository(
    database: Database,
    account: Account,
    private val dataSource: GReaderDataSource,
) : BaseRepository(database, account), KoinComponent {

    override suspend fun login(account: Account) {
        val authInterceptor = get<AuthInterceptor>().apply {
            credentials = Credentials.toCredentials(account)
        }

        account.token = dataSource.login(account.login!!, account.password!!)
        // we got the authToken, time to provide it to make real calls
        authInterceptor.credentials = Credentials.toCredentials(account)

        account.writeToken = dataSource.getWriteToken()

        val userInfo = dataSource.getUserInfo()
        account.displayedName = userInfo.userName
    }

    override suspend fun synchronize(
        selectedFeeds: List<Feed>,
        onUpdate: suspend (Feed) -> Unit
    ): Pair<SyncResult, ErrorResult> = throw NotImplementedError("This method can't be called here")

    override suspend fun synchronize(): SyncResult {
        val itemStateChanges = database.itemStateChangeDao()
            .selectItemStateChanges(account.id)

        val syncData = GReaderSyncData(
            readIds = itemStateChanges.filter { it.readChange && it.read }
                .map { it.remoteId },
            unreadIds = itemStateChanges.filter { it.readChange && !it.read }
                .map { it.remoteId },
            starredIds = itemStateChanges.filter { it.starChange && it.starred }
                .map { it.remoteId },
            unstarredIds = itemStateChanges.filter { it.starChange && !it.starred }
                .map { it.remoteId }
        )

        val syncType: SyncType
        if (account.lastModified != 0L) {
            syncType = SyncType.CLASSIC_SYNC
            syncData.lastModified = account.lastModified
        } else {
            syncType = SyncType.INITIAL_SYNC
        }

        val newLastModified = System.currentTimeMillis() / 1000L

        return dataSource.synchronize(syncType, syncData, account.writeToken!!).run {
            reconcileRenamedFolders(folders, feeds)
            insertFolders(folders)
            val newFeeds = insertFeeds(feeds)
            val tags = insertTags(tags)

            val newItems = insertItems(items, filterStarredItems = true)
            insertItems(starredItems, filterStarredItems = false)

            insertItemsTags(newItems, tags)

            insertItemsIds(unreadIds, readIds, starredIds.toMutableList())

            account.lastModified = newLastModified
            database.accountDao().updateLastModified(newLastModified, account.id)

            database.itemStateChangeDao().resetStateChanges(account.id)

            SyncResult(
                items = newItems,
                feeds = newFeeds
            )
        }
    }

    override suspend fun insertNewFeeds(
        newFeeds: List<Feed>,
        onUpdate: (Feed) -> Unit
    ): ErrorResult {
        val errors = hashMapOf<Feed, Exception>()
        var feedCreated = false

        for (newFeed in newFeeds) {
            onUpdate(newFeed)

            try {
                dataSource.createFeed(account.writeToken!!, newFeed.url!!, newFeed.remoteFolderId)
                feedCreated = true
            } catch (e: Exception) {
                errors[newFeed] = e
            }
        }

        if (feedCreated) {
            // the subscription call returns nothing about the feed it just created, so the
            // subscription list has to be fetched again to make the new feeds locally available
            // instead of waiting for the next synchronization
            try {
                val folderTags = dataSource.getFolders()
                insertFolders(folderTags.folders)
                insertTags(folderTags.tags)
            } catch (e: Exception) {
                Log.e(TAG, "refreshing folders after feed creation: ${e.message}")
            }

            try {
                insertFeeds(dataSource.getFeeds())
                    .forEach { feed -> insertNewFeedItems(feed) }
            } catch (e: Exception) {
                Log.e(TAG, "refreshing feeds after feed creation: ${e.message}")
            }
        }

        return errors
    }

    /**
     * Fetch right away what the feed already holds, so it isn't shown empty until the next
     * synchronization. FreshRSS filters the main item call on the item insertion time, so it would
     * return this backlog too, but only at the next sync and only for servers behaving that way,
     * some filtering on the publication date instead.
     */
    private suspend fun insertNewFeedItems(feed: Feed) {
        val items = dataSource.getFeedItems(feed.remoteId!!)
        val newItems = insertItems(items, filterStarredItems = false)

        // this account type keeps the read/star state in a separate table,
        // an item without any state is considered read and stays hidden from the timeline
        database.itemStateDao().insertIgnoreConflicts(newItems.map { item ->
            ItemState(
                read = item.isRead,
                starred = item.isStarred,
                remoteId = item.remoteId!!,
                accountId = account.id
            )
        })

        insertItemsTags(newItems, database.tagDao().selectAll(account.id))
    }

    /**
     * A GReader folder is identified by its name (user/-/label/<name>), so a folder renamed on the
     * server side looks exactly like a folder deleted and another one created. Recreating it would
     * give it a new local id, and everything pointing to the previous one (timeline filter, drawer
     * selection) would silently refer to a folder which doesn't exist anymore.
     *
     * A rename is detected through the feeds the folder holds: when the feeds of a local folder
     * which disappeared all belong to the same new remote folder, the folder has been renamed and
     * is updated in place instead.
     */
    private suspend fun reconcileRenamedFolders(
        remoteFolders: List<Folder>,
        remoteFeeds: List<Feed>
    ) {
        val localFolders = database.folderDao().selectAllFolders(account.id)
        val remoteIds = remoteFolders.mapNotNull { it.remoteId }.toSet()

        val goneFolders = localFolders.filter { it.remoteId !in remoteIds }
        if (goneFolders.isEmpty()) {
            return
        }

        val localIds = localFolders.mapNotNull { it.remoteId }.toSet()
        val addedFolders = remoteFolders.filter { it.remoteId !in localIds }
            .associateByTo(mutableMapOf()) { it.remoteId!! }

        for (goneFolder in goneFolders) {
            if (addedFolders.isEmpty()) {
                break
            }

            val feedRemoteIds = database.feedDao().selectFeedsByFolder(goneFolder.id)
                .mapNotNull { it.remoteId }
                .toSet()

            if (feedRemoteIds.isEmpty()) {
                continue
            }

            // an ambiguous move, or a move to an already known folder, is a real deletion
            val addedRemoteId = remoteFeeds.filter { it.remoteId in feedRemoteIds }
                .mapNotNull { it.remoteFolderId }
                .distinct()
                .singleOrNull()
                ?.takeIf { addedFolders.containsKey(it) }
                ?: continue

            database.folderDao().updateFolderRemoteIdAndName(
                folderId = goneFolder.id,
                remoteId = addedRemoteId,
                name = addedFolders.getValue(addedRemoteId).name!!
            )

            addedFolders.remove(addedRemoteId)
        }
    }

    override suspend fun updateFeed(feed: Feed) {
        dataSource.updateFeed(account.writeToken!!, feed.url!!, feed.name!!, feed.remoteFolderId!!)
        super.updateFeed(feed)
    }

    override suspend fun deleteFeed(feed: Feed) {
        dataSource.deleteFeed(account.writeToken!!, feed.url!!)
        super.deleteFeed(feed)
    }

    override suspend fun updateFolder(folder: Folder) {
        dataSource.updateFolder(account.writeToken!!, folder.remoteId!!, folder.name!!)
        folder.remoteId = GReaderDataSource.FOLDER_PREFIX + folder.name

        super.updateFolder(folder)
    }

    override suspend fun deleteFolder(folder: Folder) {
        dataSource.deleteFolder(account.writeToken!!, folder.remoteId!!)
        super.deleteFolder(folder)
    }

    private suspend fun insertFeeds(feeds: List<Feed>): List<Feed> {
        feeds.forEach { it.accountId = account.id }
        return database.feedDao().upsertFeeds(feeds, account)
    }

    private suspend fun insertFolders(folders: List<Folder>) {
        folders.forEach { it.accountId = account.id }
        database.folderDao().upsertFolders(folders, account)
    }

    private suspend fun insertTags(tags: List<Tag>): List<Tag> {
        return database.tagDao().upsertTags(tags.map { it.copy(accountId = account.id) }, account)
    }

    /**
     * @param filterStarredItems workaround to avoid inserting starred items coming from the main
     * item call, as the API exclusion filter doesn't seem to work
     */
    private suspend fun insertItems(items: List<Item>, filterStarredItems: Boolean): List<Item> {
        val newItems = arrayListOf<Item>()
        val itemsFeedsIds = mutableMapOf<String?, Int>()

        // an item can be returned by two different calls, typically the items of a feed just added
        // and the next classic synchronization, inserting it twice would duplicate it in the
        // timeline as nothing makes Item.remote_id unique
        val insertedIds = database.itemDao()
            .selectExistingRemoteIds(items.mapNotNull { it.remoteId }, account.id)
            .toMutableSet()

        for (item in items) {
            val remoteId = item.remoteId ?: continue

            if (filterStarredItems && item.isStarred) {
                continue
            }

            if (!insertedIds.add(remoteId)) {
                continue
            }

            val feedId: Int
            if (itemsFeedsIds.containsKey(item.feedRemoteId)) {
                feedId = itemsFeedsIds.getValue(item.feedRemoteId)
            } else {
                feedId =
                    database.feedDao().selectRemoteFeedLocalId(item.feedRemoteId!!, account.id)
                itemsFeedsIds[item.feedRemoteId] = feedId
            }

            item.feedId = feedId

            if (item.text != null) {
                item.readTime = Utils.readTimeFromString(item.text!!)
            }

            newItems.add(item)
        }

        if (newItems.isNotEmpty()) {
            newItems.sortWith(Item::compareTo)
            database.itemDao().insert(newItems)
                .zip(newItems)
                .forEach { (id, item) -> item.id = id.toInt() }
        }

        return newItems
    }

    private suspend fun insertItemsTags(newItems: List<Item>, allTags: List<Tag>) {
        newItems.associate { it to it.tags }
            .flatMap { (item, tags) ->
                tags
                    .filter { tag -> allTags.any { tag.remoteId == it.remoteId } }
                    .map { tag ->
                        TagJoin(
                            itemId = item.id,
                            tagId = allTags.first { tag.remoteId == it.remoteId }.id,
                        )
                    }
            }
            .run {
                database.tagJoinDao().insert(this)
            }
    }

    private suspend fun insertItemsIds(
        unreadIds: List<String>,
        readIds: List<String>,
        starredIds: MutableList<String> // TODO is it performance wise?
    ) {
        database.itemStateDao().deleteItemStates(account.id)

        database.itemStateDao().insert(unreadIds.map { id ->
            val starred = starredIds.any { starredId -> starredId == id }

            if (starred) {
                starredIds.remove(id)
            }

            ItemState(
                id = 0,
                read = false,
                starred = starred,
                remoteId = id,
                accountId = account.id
            )
        })

        database.itemStateDao().insert(readIds.map { id ->
            val starred = starredIds.any { starredId -> starredId == id }
            if (starred) {
                starredIds.remove(id)
            }

            ItemState(
                id = 0,
                read = true,
                starred = starred,
                remoteId = id,
                accountId = account.id
            )
        })

        // insert starred items ids which are read
        if (starredIds.isNotEmpty()) {
            database.itemStateDao().insert(starredIds.map { id ->
                ItemState(
                    0,
                    read = true,
                    starred = true,
                    remoteId = id,
                    accountId = account.id
                )
            })
        }
    }

    companion object {
        private val TAG = GReaderRepository::class.java.simpleName
    }
}
