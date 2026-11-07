package com.readrops.app.repositories

import android.content.Context
import android.net.Uri
import com.readrops.api.localfeed.LocalRSSDataSource
import com.readrops.app.util.Preferences
import com.readrops.app.feeds.newfeed.FeedDirectory
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.account.Account
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.decsync.library.Decsync
import org.decsync.library.generateAppId
import org.koin.core.component.KoinComponent
import java.util.UUID

/** Local RSS reader whose subscriptions and article states are shared through DecSync. */
@OptIn(kotlin.ExperimentalStdlibApi::class)
class DecsyncRepository(
    database: Database,
    account: Account,
    private val dataSource: LocalRSSDataSource,
    private val preferences: Preferences,
    private val context: Context
) : BaseRepository(database, account), KoinComponent {
    private val subscriptions = linkedMapOf<String, Boolean>()
    private val names = linkedMapOf<String, String>()
    private val feedCategories = linkedMapOf<String, String?>()
    private val categoryNames = linkedMapOf<String, String>()
    private val read = linkedMapOf<String, Boolean>()
    private val marked = linkedMapOf<String, Boolean>()

    private fun decsync(): Decsync<Unit> {
        val appId = account.token ?: generateAppId("Droidrops", isRandom = true).also { account.token = it }
        val sync = Decsync<Unit>(context, Uri.parse(account.url), "rss", null, appId)
        sync.addListener(listOf("feeds", "subscriptions")) { _, entry, _ ->
            entry.key.jsonPrimitive.contentOrNull?.let { subscriptions[it] = entry.value.jsonPrimitive.booleanOrNull ?: false }
        }
        sync.addListener(listOf("feeds", "names")) { _, entry, _ ->
            entry.key.jsonPrimitive.contentOrNull?.let { key -> entry.value.jsonPrimitive.contentOrNull?.let { names[key] = it } }
        }
        sync.addListener(listOf("feeds", "categories")) { _, entry, _ ->
            entry.key.jsonPrimitive.contentOrNull?.let { feedCategories[it] = entry.value.jsonPrimitive.contentOrNull }
        }
        sync.addListener(listOf("categories", "names")) { _, entry, _ ->
            entry.key.jsonPrimitive.contentOrNull?.let { key -> entry.value.jsonPrimitive.contentOrNull?.let { categoryNames[key] = it } }
        }
        sync.addMultiListener(listOf("articles", "read")) { _, entries, _ ->
            entries.forEach { it.key.jsonPrimitive.contentOrNull?.let { key -> it.value.jsonPrimitive.booleanOrNull?.let { value -> read[key] = value } } }
        }
        sync.addMultiListener(listOf("articles", "marked")) { _, entries, _ ->
            entries.forEach { it.key.jsonPrimitive.contentOrNull?.let { key -> it.value.jsonPrimitive.booleanOrNull?.let { value -> marked[key] = value } } }
        }
        return sync
    }

    override suspend fun login(account: Account) {
        require(account.url?.startsWith("content://") == true) { "Choose a DecSync folder" }
        account.token = generateAppId("Droidrops", isRandom = true)
        // Validate that the selected SAF tree is readable and writable.
        decsync().apply { initStoredEntries(); executeAllNewEntries(Unit) }
        account.displayedName = "DecSync"
    }

    override suspend fun synchronize(selectedFeeds: List<Feed>, onUpdate: suspend (Feed) -> Unit): Pair<SyncResult, ErrorResult> {
        val sync = decsync()
        sync.initStoredEntries()
        sync.executeAllNewEntries(Unit)
        sync.executeStoredEntriesForPathExact(listOf("feeds", "subscriptions"), Unit)
        sync.executeStoredEntriesForPathExact(listOf("feeds", "names"), Unit)
        sync.executeStoredEntriesForPathExact(listOf("feeds", "categories"), Unit)
        sync.executeStoredEntriesForPathExact(listOf("categories", "names"), Unit)
        sync.executeStoredEntriesForPathPrefix(listOf("articles", "read"), Unit)
        sync.executeStoredEntriesForPathPrefix(listOf("articles", "marked"), Unit)

        val currentFeeds = database.feedDao().selectFeeds(account.id).associateBy { it.url }
        // DecSync does not replay this device's own entries to its listeners. Keep feeds that
        // already exist locally unless another device explicitly marked them unsubscribed.
        val wanted = currentFeeds.keys.filter { subscriptions[it] != false }.toSet() +
            subscriptions.filterValues { it }.keys
        val toAdd = wanted.filterNot { it in currentFeeds }.map { url ->
            Feed(name = names[url] ?: url, url = url, remoteId = url, accountId = account.id)
        }
        val localRepository = LocalRSSRepository(dataSource, database, account, preferences)
        val errors = if (toAdd.isEmpty()) hashMapOf() else localRepository.insertNewFeeds(toAdd) { }
        // Local RSS parsing returns a fresh Feed object. Attach the stable DecSync subscription URL.
        val fetchedFeeds = database.feedDao().selectFeeds(account.id)
        fetchedFeeds.filter { it.url in wanted && it.remoteId != it.url }.forEach {
            it.remoteId = it.url
            names[it.url.orEmpty()]?.takeIf { name -> name.isNotBlank() && name != it.url }?.let { name ->
                it.name = name
            }
            database.feedDao().update(it)
        }
        val toRemove = fetchedFeeds.filter { it.url !in wanted }
        toRemove.forEach { database.feedDao().delete(it) }

        val remoteCategories = (categoryNames.keys + feedCategories.values.filterNotNull()).distinct().map { categoryId ->
            Folder(remoteId = categoryId, name = categoryNames[categoryId] ?: categoryId, accountId = account.id)
        }
        database.folderDao().upsertFolders(remoteCategories, account)
        val foldersByRemoteId = database.folderDao().selectAllFolders(account.id).associateBy { it.remoteId }
        database.feedDao().selectFeeds(account.id).forEach { feed ->
            val categoryId = feed.url?.let(feedCategories::get)
            val folderId = categoryId?.let { foldersByRemoteId[it]?.id }
            if (feed.folderId != folderId) {
                feed.folderId = folderId
                database.feedDao().update(feed)
            }
        }

        val feeds = database.feedDao().selectFeeds(account.id)
        val (downloaded, downloadErrors) = localRepository.synchronize(feeds, onUpdate)
        errors.putAll(downloadErrors)

        // Apply shared state, then publish this device's latest states back to DecSync.
        feeds.forEach { feed ->
            var afterId = 0
            while (true) {
                val batch = database.itemDao().selectItemsPage(feed.id, afterId, ITEM_BATCH)
                if (batch.isEmpty()) break
                batch.forEach { item ->
                    val guid = item.remoteId ?: return@forEach
                    read[guid]?.let { database.itemDao().updateReadState(item.id, it); item.isRead = it }
                    marked[guid]?.let { database.itemDao().updateStarState(item.id, it); item.isStarred = it }
                    val date = item.pubDate ?: return@forEach
                    val key = kotlinx.serialization.json.JsonPrimitive(guid)
                    val year = date.year.toString()
                    val month = date.monthValue.toString().padStart(2, '0')
                    val day = date.dayOfMonth.toString().padStart(2, '0')
                    val updates = buildList {
                        if (read[guid] != item.isRead) add(Decsync.EntryWithPath(
                            listOf("articles", "read", year, month, day), key,
                            kotlinx.serialization.json.JsonPrimitive(item.isRead)
                        ))
                        if (marked[guid] != item.isStarred) add(Decsync.EntryWithPath(
                            listOf("articles", "marked", year, month, day), key,
                            kotlinx.serialization.json.JsonPrimitive(item.isStarred)
                        ))
                    }
                    sync.setEntries(updates)
                    read[guid] = item.isRead
                    marked[guid] = item.isStarred
                }
                afterId = batch.last().id
                if (batch.size < ITEM_BATCH) break
            }
        }
        feeds.forEach { feed ->
            val url = feed.url ?: return@forEach
            val feedName = sharedFeedName(url, feed.name)
            if (subscriptions[url] != true) sync.setEntries(listOf(
                Decsync.EntryWithPath(listOf("feeds", "subscriptions"), kotlinx.serialization.json.JsonPrimitive(url), kotlinx.serialization.json.JsonPrimitive(true))
            ))
            if (names[url] != feedName) sync.setEntries(listOf(
                Decsync.EntryWithPath(listOf("feeds", "names"), kotlinx.serialization.json.JsonPrimitive(url), kotlinx.serialization.json.JsonPrimitive(feedName))
            ))
            subscriptions[url] = true
            names[url] = feedName
        }
        return downloaded.copy(feeds = feeds) to errors
    }

    override suspend fun synchronize(): SyncResult = throw NotImplementedError("DecSync is synchronized with local feeds")

    override suspend fun insertNewFeeds(newFeeds: List<Feed>, onUpdate: (Feed) -> Unit): ErrorResult {
        val error = LocalRSSRepository(dataSource, database, account, preferences).insertNewFeeds(newFeeds, onUpdate)
        if (error.isEmpty()) newFeeds.forEach { feed ->
            feed.url?.let { url ->
                val storedFeed = database.feedDao().selectFeeds(account.id).firstOrNull { it.url == url }?.let { stored ->
                    stored.remoteId = url
                    database.feedDao().update(stored)
                    stored
                }
                val feedName = sharedFeedName(url, storedFeed?.name ?: feed.name)
                decsync().setEntries(listOf(
                    Decsync.EntryWithPath(listOf("feeds", "subscriptions"), kotlinx.serialization.json.JsonPrimitive(url), kotlinx.serialization.json.JsonPrimitive(true)),
                    Decsync.EntryWithPath(listOf("feeds", "names"), kotlinx.serialization.json.JsonPrimitive(url), kotlinx.serialization.json.JsonPrimitive(feedName))
                ))
            }
        }
        return error
    }

    private fun sharedFeedName(url: String, name: String?): String {
        val storedName = name?.takeIf { it.isNotBlank() && it != url }
        return storedName ?: FeedDirectory.items.firstOrNull { it.url == url }?.title ?: name ?: url
    }

    override suspend fun updateFeed(feed: Feed) {
        super.updateFeed(feed)
        feed.url?.let { url -> decsync().setEntries(listOf(
            Decsync.EntryWithPath(listOf("feeds", "names"), JsonPrimitive(url), JsonPrimitive(feed.name.orEmpty())),
            Decsync.EntryWithPath(
                listOf("feeds", "categories"), JsonPrimitive(url),
                feed.folderId?.let { database.folderDao().select(it).remoteId }?.let(::JsonPrimitive) ?: JsonNull
            )
        )) }
    }

    override suspend fun deleteFeed(feed: Feed) {
        feed.url?.let { url -> decsync().setEntries(listOf(
            Decsync.EntryWithPath(listOf("feeds", "subscriptions"), kotlinx.serialization.json.JsonPrimitive(url), kotlinx.serialization.json.JsonPrimitive(false))
        )) }
        super.deleteFeed(feed)
    }

    override suspend fun addFolder(folder: Folder) {
        folder.remoteId = UUID.randomUUID().toString()
        folder.accountId = account.id
        database.folderDao().insert(folder)
        decsync().setEntries(listOf(
            Decsync.EntryWithPath(listOf("categories", "names"), JsonPrimitive(folder.remoteId!!), JsonPrimitive(folder.name.orEmpty()))
        ))
    }

    override suspend fun updateFolder(folder: Folder) {
        super.updateFolder(folder)
        folder.remoteId?.let { id -> decsync().setEntries(listOf(
            Decsync.EntryWithPath(listOf("categories", "names"), JsonPrimitive(id), JsonPrimitive(folder.name.orEmpty()))
        )) }
    }

    override suspend fun deleteFolder(folder: Folder) {
        database.feedDao().selectFeeds(account.id).filter { it.folderId == folder.id }.forEach { feed ->
            feed.url?.let { url -> decsync().setEntries(listOf(
                Decsync.EntryWithPath(listOf("feeds", "categories"), JsonPrimitive(url), JsonNull)
            )) }
        }
        folder.remoteId?.let { id -> decsync().setEntries(listOf(
            Decsync.EntryWithPath(listOf("categories", "names"), JsonPrimitive(id), JsonNull)
        )) }
        super.deleteFolder(folder)
    }

    companion object {
        private const val ITEM_BATCH = 200
    }
}
