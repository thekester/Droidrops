package com.readrops.app.repositories

import com.readrops.api.services.Credentials
import com.readrops.api.services.SyncType
import com.readrops.api.services.tinytinyrss.TinyTinyRssDataSource
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

class TinyTinyRssRepository(
    database: Database,
    account: Account,
    private val dataSource: TinyTinyRssDataSource
) : BaseRepository(database, account), KoinComponent {
    override suspend fun login(account: Account) {
        val diagnostics = get<DiagnosticLog>()
        get<AuthInterceptor>().credentials = Credentials.toCredentials(account)
        try {
            account.token = dataSource.login(account.login.orEmpty(), account.password.orEmpty())
            get<AuthInterceptor>().credentials = Credentials.toCredentials(account)
            account.displayedName = account.login?.takeIf(String::isNotBlank) ?: "Tiny Tiny RSS"
            diagnostics.info(TAG, "Login validation succeeded (native Tiny Tiny RSS API)")
        } catch (exception: Exception) {
            diagnostics.error(TAG, "Login validation failed (native Tiny Tiny RSS API)", exception)
            throw exception
        }
    }

    override suspend fun synchronize(
        selectedFeeds: List<Feed>,
        onUpdate: suspend (Feed) -> Unit
    ): Pair<SyncResult, ErrorResult> = throw NotImplementedError("Tiny Tiny RSS uses account-wide synchronization")

    override suspend fun synchronize(): SyncResult {
        val changes = database.itemStateChangeDao().selectItemStateChanges(account.id)
        val type = if (account.lastModified == 0L) SyncType.INITIAL_SYNC else SyncType.CLASSIC_SYNC
        val startedAt = System.currentTimeMillis() / 1000
        val initialBatchSize = get<com.readrops.app.util.Preferences>().minifluxInitialSyncBatchSize.flow.first()
        val diagnostics = get<DiagnosticLog>()
        var progress = "starting"

        diagnostics.info(
            TAG,
            "Sync started (${type.name.lowercase()}, pending changes=${changes.size})"
        )
        val result = try {
            dataSource.synchronize(
                sessionId = account.token ?: throw IllegalStateException("Tiny Tiny RSS session has expired; reconnect the account"),
                syncType = type,
                lastModified = account.lastModified,
                initialSyncCursor = account.syncCursor,
                readIds = changes.filter { it.readChange && it.read }.map { it.remoteId },
                unreadIds = changes.filter { it.readChange && !it.read }.map { it.remoteId },
                starredIds = changes.filter { it.starChange && it.starred }.map { it.remoteId },
                unstarredIds = changes.filter { it.starChange && !it.starred }.map { it.remoteId },
                initialSyncBatchSize = initialBatchSize,
                onProgress = { message -> progress = message; diagnostics.info(TAG, message) }
            )
        } catch (exception: Exception) {
            diagnostics.error(TAG, "Sync failed during: $progress", exception)
            throw exception
        }

        insertFolders(result.folders)
        val feeds = insertFeeds(result.feeds)
        val items = insertItems(result.items)
        val nextLastModified = when {
            result.syncCursor != null -> 0L
            type == SyncType.INITIAL_SYNC -> result.initialSyncTimestamp ?: startedAt
            else -> maxOf(account.lastModified, result.initialSyncTimestamp ?: account.lastModified)
        }
        account.lastModified = nextLastModified
        account.syncCursor = result.syncCursor
        database.accountDao().updateSyncProgress(nextLastModified, result.syncCursor, account.id)
        database.itemStateChangeDao().resetStateChanges(account.id)

        diagnostics.info(
            TAG,
            "Sync batch complete (${feeds.size} feeds, ${result.folders.size} categories, " +
                "${result.items.size} remote articles, ${items.size} new local articles, " +
                "initial import ${if (result.syncCursor == null) "complete" else "continues next sync"})"
        )
        return SyncResult(items = items, feeds = feeds, continueSync = result.syncCursor != null)
    }

    override suspend fun insertNewFeeds(newFeeds: List<Feed>, onUpdate: (Feed) -> Unit): ErrorResult {
        val errors = hashMapOf<Feed, Exception>()
        newFeeds.forEach { feed ->
            onUpdate(feed)
            try {
                val folder = feed.folderId?.let { database.folderDao().select(it) }
                dataSource.subscribe(
                    sessionId = account.token ?: throw IllegalStateException("Tiny Tiny RSS session has expired"),
                    feedUrl = feed.url ?: throw IllegalArgumentException("Missing feed URL"),
                    categoryId = folder?.remoteId
                )
            } catch (exception: Exception) {
                errors[feed] = exception
            }
        }
        if (newFeeds.isNotEmpty()) {
            runCatching {
                val session = account.token ?: return@runCatching
                insertFolders(dataSource.getCategoriesForAccount(session))
                val remoteFeeds = dataSource.getFeedsForAccount(session)
                val remoteUrls = remoteFeeds.mapNotNull { it.url }.toSet()
                errors.keys.filter { it.url in remoteUrls }.forEach { errors.remove(it) }
                insertFeeds(remoteFeeds)
            }.onFailure { get<DiagnosticLog>().warning(TAG, "Unable to refresh Tiny Tiny RSS subscriptions", it) }
        }
        return errors
    }

    override suspend fun deleteFeed(feed: Feed) {
        dataSource.unsubscribe(
            account.token ?: throw IllegalStateException("Tiny Tiny RSS session has expired"),
            feed.remoteId?.toLongOrNull() ?: throw IllegalArgumentException("Missing Tiny Tiny RSS feed ID")
        )
        super.deleteFeed(feed)
    }

    private suspend fun insertFolders(folders: List<Folder>) {
        folders.forEach { it.accountId = account.id }
        if (folders.isNotEmpty()) database.folderDao().upsertFolders(folders, account)
    }

    private suspend fun insertFeeds(feeds: List<Feed>): List<Feed> {
        feeds.forEach { it.accountId = account.id }
        return if (feeds.isEmpty()) emptyList() else database.feedDao().upsertFeeds(feeds, account)
    }

    private suspend fun insertItems(items: List<Item>): List<Item> {
        if (items.isEmpty()) return emptyList()
        val remoteIds = items.mapNotNull { it.remoteId }.distinct()
        val existing = remoteIds.chunked(SQLITE_MAX_VARIABLES)
            .flatMap { database.itemDao().selectExistingItems(it, account.id) }
            .associateBy { it.remoteId }
        val states = remoteIds.chunked(SQLITE_MAX_VARIABLES)
            .flatMap { database.itemStateDao().selectItemStatesByRemoteIds(it, account.id) }
            .associateBy { it.remoteId }
        val feeds = database.feedDao().selectFeeds(account.id).associateBy { it.remoteId }
        val fresh = mutableListOf<Item>()
        val newStates = mutableListOf<ItemState>()

        items.forEach { item ->
            val remoteId = item.remoteId ?: return@forEach
            val feed = feeds[item.feedRemoteId] ?: return@forEach
            item.feedId = feed.id
            item.readTime = Utils.readTimeFromString(item.content.orEmpty())
            val old = existing[remoteId]
            if (old == null) fresh += item else {
                item.id = old.id
                database.itemDao().update(item)
            }
            val nextState = ItemState(read = item.isRead, starred = item.isStarred, remoteId = remoteId, accountId = account.id)
            val previous = states[remoteId]
            if (previous == null) newStates += nextState else {
                if (previous.read != nextState.read) database.itemStateDao().updateItemReadState(nextState.read, remoteId, account.id)
                if (previous.starred != nextState.starred) database.itemStateDao().updateItemStarState(nextState.starred, remoteId, account.id)
            }
        }
        if (newStates.isNotEmpty()) database.itemStateDao().insertIgnoreConflicts(newStates)
        if (fresh.isNotEmpty()) database.itemDao().insert(fresh).zip(fresh).forEach { (id, item) -> item.id = id.toInt() }
        return fresh
    }

    companion object {
        private const val TAG = "TinyTinyRSS"
        private const val SQLITE_MAX_VARIABLES = 900
    }
}
