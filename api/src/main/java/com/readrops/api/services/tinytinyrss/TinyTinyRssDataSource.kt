package com.readrops.api.services.tinytinyrss

import com.readrops.api.services.DataSourceResult
import com.readrops.api.services.SyncType
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

class TinyTinyRssDataSource(private val service: TinyTinyRssService) {
    private val sequence = AtomicInteger()

    suspend fun login(username: String, password: String): String {
        val content = requestObject("login", null, mapOf("user" to username, "password" to password))
        return content.optString("session_id").takeIf(String::isNotBlank)
            ?: throw IOException("Tiny Tiny RSS did not return a session ID")
    }

    suspend fun synchronize(
        sessionId: String,
        syncType: SyncType,
        lastModified: Long,
        initialSyncCursor: String?,
        readIds: List<String>,
        unreadIds: List<String>,
        starredIds: List<String>,
        unstarredIds: List<String>,
        initialSyncBatchSize: Int = DEFAULT_INITIAL_SYNC_BATCH_SIZE,
        onProgress: suspend (String) -> Unit = {}
    ): DataSourceResult {
        onProgress("Applying pending read/star changes")
        updateArticles(sessionId, readIds, field = FIELD_UNREAD, value = false)
        updateArticles(sessionId, unreadIds, field = FIELD_UNREAD, value = true)
        updateArticles(sessionId, starredIds, field = FIELD_STARRED, value = true)
        updateArticles(sessionId, unstarredIds, field = FIELD_STARRED, value = false)

        onProgress("Fetching Tiny Tiny RSS categories and feeds")
        val folders = getCategories(sessionId)
        val feeds = getFeeds(sessionId)
        val initial = syncType == SyncType.INITIAL_SYNC
        val (items, cursor, highestId) = if (initial) {
            getInitialHeadlines(sessionId, initialSyncCursor, initialSyncBatchSize, onProgress)
        } else {
            val latest = getUpdatedHeadlines(sessionId, lastModified, onProgress)
            Triple(latest, null, latest.mapNotNull { it.remoteId?.toLongOrNull() }.maxOrNull() ?: lastModified)
        }

        return DataSourceResult(
            items = items,
            feeds = feeds,
            folders = folders,
            readIds = items.filter { it.isRead }.mapNotNull { it.remoteId },
            unreadIds = items.filterNot { it.isRead }.mapNotNull { it.remoteId },
            starredIds = items.filter { it.isStarred }.mapNotNull { it.remoteId },
            syncCursor = cursor,
            initialSyncTimestamp = highestId.takeIf { cursor == null }
        )
    }

    suspend fun subscribe(sessionId: String, feedUrl: String, categoryId: String?) {
        val parameters = mutableMapOf<String, Any?>("feed_url" to feedUrl)
        parameters["category_id"] = categoryId?.toLongOrNull() ?: 0L
        request("subscribeToFeed", sessionId, parameters)
    }

    suspend fun unsubscribe(sessionId: String, feedId: Long) {
        request("unsubscribeFeed", sessionId, mapOf("feed_id" to feedId.toString()))
    }

    suspend fun getCategoriesForAccount(sessionId: String): List<Folder> = getCategories(sessionId)

    suspend fun getFeedsForAccount(sessionId: String): List<Feed> = getFeeds(sessionId)

    private suspend fun getCategories(sessionId: String): List<Folder> {
        val values = contentArray(request("getCategories", sessionId, mapOf("include_empty" to true)))
        return List(values.length()) { index ->
            val value = values.getJSONObject(index)
            Folder(
                name = value.optString("title").takeIf(String::isNotBlank),
                remoteId = value.optLong("id").takeIf { it > 0 }?.toString()
            )
        }.filter { it.name != null && it.remoteId != null }
    }

    private suspend fun getFeeds(sessionId: String): List<Feed> {
        val values = contentArray(request("getFeeds", sessionId, mapOf("cat_id" to "-3")))
        return List(values.length()) { index ->
            val value = values.getJSONObject(index)
            Feed(
                name = value.optString("title").takeIf(String::isNotBlank),
                url = value.optString("feed_url").takeIf(String::isNotBlank),
                siteUrl = value.optString("site_url").takeIf(String::isNotBlank),
                remoteId = value.optLong("id").takeIf { it > 0 }?.toString(),
                remoteFolderId = value.optLong("cat_id").takeIf { it > 0 }?.toString()
            )
        }.filter { it.remoteId != null && it.url != null }
    }

    private suspend fun getInitialHeadlines(
        sessionId: String,
        cursor: String?,
        batchSize: Int,
        onProgress: suspend (String) -> Unit
    ): Triple<List<Item>, String?, Long> {
        require(batchSize > 0) { "Tiny Tiny RSS initial import batch size must be positive" }
        val decoded = InitialCursor.decode(cursor)
        val entries = mutableListOf<Item>()
        var skip = decoded.skip
        var highestId = decoded.highestId
        var page = 0
        var complete = false

        while (entries.size < batchSize && !complete) {
            page++
            val limit = minOf(API_PAGE_SIZE, batchSize - entries.size)
            onProgress("Requesting initial articles page $page (skip=$skip, collected=${entries.size}/$batchSize)")
            val values = contentArray(request("getHeadlines", sessionId, mapOf(
                "feed_id" to "-4",
                "view_mode" to "all_articles",
                // Keep offset-based initial-import pages stable while new articles arrive.
                "order_by" to "date_reverse",
                "show_content" to true,
                "limit" to limit.toString(),
                "skip" to skip.toString()
            )))
            val result = parseItems(values)
            entries += result
            highestId = maxOf(highestId, result.mapNotNull { it.remoteId?.toLongOrNull() }.maxOrNull() ?: 0L)
            skip += values.length()
            complete = values.length() < limit
            onProgress("Received ${values.length()} initial articles (total ${entries.size})")
            if (values.length() == 0) complete = true
        }

        val nextCursor = if (complete) null else InitialCursor(skip, highestId).encode()
        return Triple(entries, nextCursor, highestId.takeIf { it > 0 } ?: 1L)
    }

    private suspend fun getUpdatedHeadlines(
        sessionId: String,
        sinceId: Long,
        onProgress: suspend (String) -> Unit
    ): List<Item> {
        val entries = mutableListOf<Item>()
        var skip = 0
        var page = 0
        while (true) {
            page++
            onProgress("Requesting updated articles page $page (since_id=$sinceId, skip=$skip)")
            val values = contentArray(request("getHeadlines", sessionId, mapOf(
                "feed_id" to "-4",
                "view_mode" to "all_articles",
                "show_content" to true,
                "since_id" to sinceId.toString(),
                "limit" to API_PAGE_SIZE.toString(),
                "skip" to skip.toString()
            )))
            entries += parseItems(values)
            if (values.length() < API_PAGE_SIZE) break
            skip += values.length()
        }
        onProgress("Fetched ${entries.size} updated articles")
        return entries
    }

    private fun parseItems(values: JSONArray): List<Item> = List(values.length()) { index ->
        val value = values.getJSONObject(index)
        Item(
            title = value.optString("title").takeIf(String::isNotBlank),
            link = value.optString("link").takeIf(String::isNotBlank),
            author = value.optString("author").takeIf(String::isNotBlank),
            content = value.optString("content").takeIf(String::isNotBlank),
            remoteId = value.optLong("id").takeIf { it > 0 }?.toString(),
            feedRemoteId = value.optLong("feed_id").takeIf { it > 0 }?.toString(),
            pubDate = parseDate(value.optString("updated")),
            isRead = !value.booleanLike("unread"),
            isStarred = value.booleanLike("marked")
        )
    }.filter { it.remoteId != null && it.feedRemoteId != null }

    private suspend fun updateArticles(sessionId: String, ids: List<String>, field: Int, value: Boolean) {
        ids.mapNotNull(String::toLongOrNull).chunked(UPDATE_BATCH_SIZE).forEach { batch ->
            if (batch.isEmpty()) return@forEach
            request("updateArticle", sessionId, mapOf(
                "article_ids" to batch.joinToString(","),
                "field" to field.toString(),
                "mode" to if (value) "1" else "0"
            ))
        }
    }

    private suspend fun request(op: String, sessionId: String?, parameters: Map<String, Any?> = emptyMap()): Any {
        val payload = JSONObject()
            .put("op", op)
            .put("seq", sequence.incrementAndGet())
        sessionId?.let { payload.put("sid", it) }
        parameters.forEach { (key, value) -> payload.put(key, value) }

        val root = service.call(payload.toString().toRequestBody(JSON_MEDIA_TYPE)).use { body ->
            JSONObject(body.string())
        }
        val status = root.optInt("status", 0)
        val error = root.optString("error").takeIf(String::isNotBlank)
        if (status != 0 || error != null) {
            throw IOException("Tiny Tiny RSS API error: ${error ?: "status $status"}")
        }
        return root.opt("content") ?: root
    }

    private suspend fun requestObject(
        op: String,
        sessionId: String?,
        parameters: Map<String, Any?> = emptyMap()
    ): JSONObject = request(op, sessionId, parameters) as? JSONObject
        ?: throw IOException("Tiny Tiny RSS returned an unexpected response for $op")

    private fun contentArray(content: Any): JSONArray = when (content) {
        is JSONArray -> content
        is JSONObject -> when {
            content.has("feeds") -> content.optJSONArray("feeds") ?: JSONArray()
            content.has("categories") -> content.optJSONArray("categories") ?: JSONArray()
            content.has("headlines") -> content.optJSONArray("headlines") ?: JSONArray()
            else -> JSONArray()
        }
        else -> JSONArray()
    }

    private fun JSONObject.booleanLike(name: String): Boolean = when (opt(name)) {
        is Boolean -> optBoolean(name)
        else -> optString(name).let { it == "1" || it.equals("true", ignoreCase = true) }
    }

    private fun parseDate(value: String): LocalDateTime = runCatching {
        OffsetDateTime.parse(value).toLocalDateTime()
    }.recoverCatching {
        LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }.recoverCatching {
        LocalDateTime.parse(value)
    }.getOrElse { LocalDateTime.now() }

    private data class InitialCursor(val skip: Int, val highestId: Long) {
        fun encode() = "initial|$skip|$highestId"

        companion object {
            fun decode(value: String?): InitialCursor {
                val parts = value?.split('|')
                return if (parts?.size == 3 && parts[0] == "initial") {
                    InitialCursor(parts[1].toIntOrNull() ?: 0, parts[2].toLongOrNull() ?: 0L)
                } else InitialCursor(0, 0L)
            }
        }
    }

    companion object {
        private const val API_PAGE_SIZE = 200
        const val DEFAULT_INITIAL_SYNC_BATCH_SIZE = 2_000
        private const val UPDATE_BATCH_SIZE = 500
        private const val FIELD_STARRED = 0
        private const val FIELD_UNREAD = 2
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
