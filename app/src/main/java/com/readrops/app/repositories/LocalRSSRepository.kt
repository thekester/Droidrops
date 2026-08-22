package com.readrops.app.repositories

import android.util.Log
import com.readrops.api.localfeed.LocalRSSDataSource
import com.readrops.api.utils.ApiUtils
import com.readrops.api.utils.HtmlParser
import com.readrops.app.util.FeedColors
import com.readrops.app.util.Utils
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Item
import com.readrops.db.entities.Tag
import com.readrops.db.entities.TagJoin
import com.readrops.db.entities.account.Account
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Headers
import org.jsoup.Jsoup
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class FeedExistException : Exception()

class LocalRSSRepository(
    private val dataSource: LocalRSSDataSource,
    database: Database,
    account: Account,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseRepository(database, account), KoinComponent {

    override suspend fun login(account: Account) { /* useless here */
    }

    override suspend fun synchronize(
        selectedFeeds: List<Feed>,
        onUpdate: suspend (Feed) -> Unit
    ): Pair<SyncResult, ErrorResult> = withContext(dispatcher) {
        val errors = hashMapOf<Feed, Exception>()
        val newItems = mutableListOf<Item>()

        val feeds = selectedFeeds.ifEmpty {
            database.feedDao().selectFeeds(account.id)
        }

        feeds.chunked(MAX_PARALLEL_REQUESTS)
            .map { mapFeeds ->
                mapFeeds.map { feed ->
                    async {
                        ensureActive()

                        val headers = Headers.Builder()
                        if (feed.etag != null) {
                            headers[ApiUtils.IF_NONE_MATCH_HEADER] = feed.etag!!
                        }
                        if (feed.lastModified != null) {
                            headers[ApiUtils.IF_MODIFIED_HEADER] = feed.lastModified!!
                        }

                        try {
                            val pair = dataSource.queryRSSResource(feed.url!!, headers.build())

                            pair?.let { (_, pairItems) ->
                                val itemsTags = pairItems.flatMap { it.tags }
                                    .distinct()
                                val allTags = insertTags(itemsTags)

                                val pairNewItems = insertNewItems(pairItems, feed)

                                if (itemsTags.isNotEmpty()) {
                                    insertItemsTags(pairNewItems, allTags)
                                }

                                newItems.addAll(pairNewItems)
                            }
                        } catch (e: Exception) {
                            errors[feed] = e
                        }

                        onUpdate(feed)
                    }
                }
                    .awaitAll()
            }

        SyncResult(items = newItems) to errors
    }

    override suspend fun synchronize(): SyncResult =
        throw NotImplementedError("This method can't be called here")


    override suspend fun insertNewFeeds(
        newFeeds: List<Feed>,
        onUpdate: (Feed) -> Unit
    ): ErrorResult = withContext(dispatcher) {
        val errors = hashMapOf<Feed, Exception>()

        newFeeds.chunked(MAX_PARALLEL_REQUESTS)
            .map { newFeedsMap ->
                newFeedsMap.map { newFeed ->
                    async {
                        ensureActive()

                        try {
                            val result = dataSource.queryRSSResource(newFeed.url!!, null)!!
                            insertFeed(result.first.also { it.folderId = newFeed.folderId })
                        } catch (e: Exception) {
                            errors[newFeed] = e
                        }

                        onUpdate(newFeed)
                    }
                }
                    .awaitAll()
            }

        errors
    }

    private suspend fun insertTags(tags: List<Tag>): List<Tag> {
        database.tagDao().insertConflict(tags.map { it.copy(accountId = account.id) })
        return database.tagDao().selectAll(account.id)
    }

    private suspend fun insertItemsTags(newItems: List<Item>, allTags: List<Tag>) {
        newItems.associate { it to it.tags }
            .flatMap { (item, tags) ->
                tags.map { tag ->
                    TagJoin(
                        itemId = item.id,
                        tagId = allTags.first { tag.name == it.name }.id,
                    )
                }
            }
            .run {
                database.tagJoinDao().insertConflict(this)
            }
    }

    private suspend fun insertNewItems(items: List<Item>, feed: Feed): List<Item> {
        val newItems = mutableListOf<Item>()

        for (item in items) {
            if (!database.itemDao().itemExists(item.remoteId!!, feed.accountId)) {
                if (item.description != null) {
                    // a description made only of markup, a lone image for instance, strips down
                    // to an empty string: storing null keeps it out of the layout entirely
                    item.cleanDescription = Jsoup.parse(item.description!!).text().ifBlank { null }
                }

                if (item.content != null) {
                    item.readTime = Utils.readTimeFromString(item.content!!)
                } else {
                    item.cleanDescription?.let { item.readTime = Utils.readTimeFromString(it) }
                }

                item.feedId = feed.id
                newItems += item
            }
        }

        // sort by date
        newItems.sort()
        database.itemDao().insert(newItems)
            .zip(newItems)
            .forEach { (id, item) -> item.id = id.toInt() }

        return newItems
    }

    private suspend fun insertFeed(feed: Feed): Feed {
        if (database.feedDao().feedExists(feed.url!!, account.id)) {
            throw FeedExistException()
        }

        return feed.apply {
            accountId = account.id
            // we need empty headers to query the feed just after, without any 304 result
            etag = null
            lastModified = null

            try {
                val document = HtmlParser.getHTMLHeadFromUrl(siteUrl!!, get())
                if (imageUrl == null) {
                    imageUrl = HtmlParser.getFeedImage(document)
                }

                if (description == null) {
                    description = HtmlParser.getFeedDescription(document)
                }

                iconUrl = HtmlParser.getFaviconLink(document).also { feedUrl ->
                    feedUrl?.let { color = FeedColors.getFeedColor(it) }
                }
            } catch (e: Exception) {
                Log.e("LocalRSSRepository", "getFaviconLink: ${e.message}")
            }

            id = database.feedDao().insert(this).toInt()
        }
    }

    companion object {
        const val MAX_PARALLEL_REQUESTS = 30
    }
}