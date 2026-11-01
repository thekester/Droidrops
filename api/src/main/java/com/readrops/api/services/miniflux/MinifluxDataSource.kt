package com.readrops.api.services.miniflux

import com.readrops.api.services.DataSourceResult
import com.readrops.api.services.SyncType
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.OffsetDateTime

class MinifluxDataSource(private val service: MinifluxService) {

    suspend fun login(): String = service.getCurrentUser().use { response ->
        JSONObject(response.string()).optString("username")
    }

    suspend fun synchronize(
        syncType: SyncType,
        lastModified: Long,
        readIds: List<String>,
        unreadIds: List<String>,
        starredIds: List<String>,
        unstarredIds: List<String>,
        initialSyncCursor: String? = null,
        initialSyncBatchSize: Int = DEFAULT_INITIAL_SYNC_BATCH_SIZE,
        initialSyncStartedAt: Long = System.currentTimeMillis() / 1000L,
        onProgress: suspend (String) -> Unit = {}
    ): DataSourceResult {
        onProgress("Applying pending state changes (read=${readIds.size}, unread=${unreadIds.size}, starred=${starredIds.size}, unstarred=${unstarredIds.size})")
        updateReadState("read", readIds)
        updateReadState("unread", unreadIds)
        starredIds.forEach { id -> service.toggleBookmark(id.toLong()) }
        unstarredIds.forEach { id -> service.toggleBookmark(id.toLong()) }

        onProgress("Fetching categories")
        val folders = getFolders()
        onProgress("Fetched ${folders.size} categories")
        onProgress("Fetching feeds")
        val feeds = getFeeds()
        onProgress("Fetched ${feeds.size} feeds")
        val initialBatch = if (syncType == SyncType.INITIAL_SYNC) {
            getInitialEntries(
                cursor = MinifluxInitialSyncCursor.decode(initialSyncCursor)
                    ?: MinifluxInitialSyncCursor("unread", null, initialSyncStartedAt),
                batchSize = initialSyncBatchSize,
                onProgress = onProgress
            )
        } else {
            null
        }
        val items = initialBatch?.items ?: getChangedEntries(lastModified, onProgress)

        onProgress(
            "Miniflux data received: ${folders.size} categories, ${feeds.size} feeds, " +
                "${items.size} entries in this sync batch"
        )
        if (initialBatch != null) {
            onProgress(
                if (initialBatch.nextCursor == null) {
                    "Miniflux initial import complete (${initialBatch.items.size} entries in this batch)"
                } else {
                    "Miniflux initial import paused after ${initialBatch.items.size} entries; " +
                        "the next sync will continue from ${initialBatch.nextCursor.status} " +
                        "before entry ${initialBatch.nextCursor.beforeEntryId}"
                }
            )
        }

        return DataSourceResult(
            items = items,
            feeds = feeds,
            folders = folders,
            unreadIds = items.filterNot { it.isRead }.mapNotNull { it.remoteId },
            readIds = items.filter { it.isRead }.mapNotNull { it.remoteId },
            starredIds = items.filter { it.isStarred }.mapNotNull { it.remoteId },
            syncCursor = initialBatch?.nextCursor?.encode(),
            initialSyncTimestamp = initialBatch?.takeIf { it.nextCursor == null }?.startedAt
        )
    }

    suspend fun getFeeds(): List<Feed> = service.getFeeds().use { response ->
        val json = JSONArray(response.string())
        List(json.length()) { index ->
            val value = json.getJSONObject(index)
            val category = value.optJSONObject("category")
            Feed(
                name = value.optString("title").takeIf(String::isNotBlank),
                url = value.optString("feed_url").takeIf(String::isNotBlank),
                siteUrl = value.optString("site_url").takeIf(String::isNotBlank),
                remoteId = value.optLong("id").takeIf { it > 0 }?.toString(),
                remoteFolderId = category?.optLong("id")
                    ?.takeIf { it > 0 }
                    ?.toString()
            )
        }
    }

    suspend fun getFolders(): List<Folder> = service.getCategories().use { response ->
        val json = JSONArray(response.string())
        List(json.length()) { index ->
            val value = json.getJSONObject(index)
            Folder(
                name = value.optString("title").takeIf(String::isNotBlank),
                remoteId = value.optLong("id").takeIf { it > 0 }?.toString()
            )
        }.filter { it.remoteId != null && it.name != null }
    }

    suspend fun createFeed(url: String, categoryId: String?) {
        val json = JSONObject().put("feed_url", url)
        categoryId?.toLongOrNull()?.let { json.put("category_id", it) }
        service.createFeed(json.toRequestBody(JSON_MEDIA_TYPE)).close()
    }

    suspend fun updateFeed(feedId: Long, name: String, categoryId: String?) {
        val json = JSONObject()
            .put("title", name)
            .put("category_id", categoryId?.toLongOrNull() ?: 0L)
        service.updateFeed(feedId, json.toRequestBody(JSON_MEDIA_TYPE))
    }

    suspend fun deleteFeed(feedId: Long) = service.deleteFeed(feedId)

    suspend fun createFolder(name: String): Folder = service.createCategory(
        JSONObject().put("title", name).toRequestBody(JSON_MEDIA_TYPE)
    ).use { response ->
        val json = JSONObject(response.string())
        Folder(name = json.optString("title", name), remoteId = json.optLong("id").toString())
    }

    suspend fun updateFolder(folderId: Long, name: String) {
        service.updateCategory(
            folderId,
            JSONObject().put("title", name).toRequestBody(JSON_MEDIA_TYPE)
        )
    }

    suspend fun deleteFolder(folderId: Long) = service.deleteCategory(folderId)

    private suspend fun updateReadState(status: String, ids: List<String>) {
        if (ids.isEmpty()) return
        val body = JSONObject()
            .put("entry_ids", JSONArray(ids.map(String::toLong)))
            .put("status", status)
            .toRequestBody(JSON_MEDIA_TYPE)
        service.updateEntries(body)
    }

    private suspend fun getInitialEntries(
        cursor: MinifluxInitialSyncCursor,
        batchSize: Int,
        onProgress: suspend (String) -> Unit
    ): InitialSyncBatch {
        require(batchSize > 0) { "Initial Miniflux sync batch size must be positive" }
        val items = mutableListOf<Item>()
        var status = cursor.status
        var beforeEntryId = cursor.beforeEntryId
        var pageNumber = 0
        var completed = false

        while (items.size < batchSize && !completed) {
            pageNumber++
            val limit = minOf(PAGE_SIZE, batchSize - items.size)
            onProgress(
                "Requesting initial $status entries page $pageNumber " +
                    "(before_entry_id=${beforeEntryId ?: "none"}, batch=${items.size}/$batchSize)"
            )
            val body = service.getEntries(
                status = status,
                limit = limit,
                offset = 0,
                beforeEntryId = beforeEntryId
            ).use { it.string() }
            val json = JSONObject(body)
            val rawEntries = json.optJSONArray("entries") ?: JSONArray()
            val page = parseEntries(body)
            val rawLastId = rawEntries.optJSONObject(rawEntries.length() - 1)
                ?.optLong("id")
                ?.takeIf { it > 0 }

            if (rawEntries.length() > 0 && rawLastId == null) {
                throw IllegalStateException("Miniflux returned an entry page without a usable final entry ID")
            }

            items += page
            val total = json.optInt("total", -1)
            onProgress(
                "Received initial $status entries page $pageNumber " +
                    "(${rawEntries.length()} entries; batch=${items.size}/$batchSize" +
                    (if (total >= 0) "; server_total=$total" else "") + ")"
            )

            if (rawLastId != null) beforeEntryId = rawLastId
            if (rawEntries.length() < limit) {
                if (status == "unread") {
                    status = "read"
                    beforeEntryId = null
                } else {
                    completed = true
                }
            }
        }

        val nextCursor = if (completed) null else {
            MinifluxInitialSyncCursor(status, beforeEntryId, cursor.startedAt)
        }
        return InitialSyncBatch(items, nextCursor, cursor.startedAt)
    }

    private suspend fun getChangedEntries(
        lastModified: Long,
        onProgress: suspend (String) -> Unit
    ): List<Item> = getEntries(changedAfter = lastModified, onProgress = onProgress)

    private suspend fun getEntries(
        status: String? = null,
        changedAfter: Long? = null,
        onProgress: suspend (String) -> Unit
    ): List<Item> {
        val entries = mutableListOf<Item>()
        var offset = 0
        var pageNumber = 0

        while (true) {
            pageNumber++
            val queryDescription = when {
                status != null -> "$status entries"
                changedAfter != null -> "entries changed since $changedAfter"
                else -> "entries"
            }
            onProgress("Requesting $queryDescription page $pageNumber (offset=$offset, collected=${entries.size})")
            val page = service.getEntries(
                status = status,
                changedAfter = changedAfter,
                limit = PAGE_SIZE,
                offset = offset
            ).use { response -> parseEntries(response.string()) }

            entries += page
            onProgress("Received $queryDescription page $pageNumber (${page.size} entries; total=${entries.size})")
            if (page.size < PAGE_SIZE) break
            offset += page.size
        }

        return entries
    }

    private fun parseEntries(json: String): List<Item> {
        val values = JSONObject(json).optJSONArray("entries") ?: JSONArray()
        return List(values.length()) { index ->
            val value = values.getJSONObject(index)
            val feed = value.optJSONObject("feed")
            val publishedAt = value.optString("published_at")
            Item(
                title = value.optString("title").takeIf(String::isNotBlank),
                link = value.optString("url").takeIf(String::isNotBlank),
                author = value.optString("author").takeIf(String::isNotBlank),
                content = value.optString("content").takeIf(String::isNotBlank),
                remoteId = value.optLong("id").takeIf { it > 0 }?.toString(),
                feedRemoteId = feed?.optLong("id")?.takeIf { it > 0 }?.toString(),
                pubDate = runCatching { OffsetDateTime.parse(publishedAt).toLocalDateTime() }
                    .getOrElse { runCatching { LocalDateTime.parse(publishedAt) }.getOrElse { LocalDateTime.now() } },
                isRead = value.optString("status") == "read",
                isStarred = value.optBoolean("starred")
            )
        }.filter { it.remoteId != null && it.feedRemoteId != null }
    }

    private fun JSONObject.toRequestBody(mediaType: okhttp3.MediaType): RequestBody =
        toString().toRequestBody(mediaType)

    companion object {
        private const val PAGE_SIZE = 500
        const val DEFAULT_INITIAL_SYNC_BATCH_SIZE = 2000
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private data class InitialSyncBatch(
        val items: List<Item>,
        val nextCursor: MinifluxInitialSyncCursor?,
        val startedAt: Long
    )
}
