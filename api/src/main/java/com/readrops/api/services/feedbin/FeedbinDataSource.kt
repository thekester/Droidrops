package com.readrops.api.services.feedbin

import com.readrops.api.services.DataSourceResult
import com.readrops.api.services.SyncType
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Item
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.OffsetDateTime

class FeedbinDataSource(private val service: FeedbinService) {
    suspend fun login(): String {
        val json = org.json.JSONObject(service.authenticate().use { it.string() })
        return json.optString("email")
    }

    suspend fun synchronize(
        syncType: SyncType, lastModified: Long,
        readIds: List<String> = emptyList(), unreadIds: List<String> = emptyList(),
        starredIds: List<String> = emptyList(), unstarredIds: List<String> = emptyList(),
        initialSyncCursor: String? = null
    ): DataSourceResult {
        update(readIds, "unread_entries", service::markRead)
        update(unreadIds, "unread_entries", service::markUnread)
        update(starredIds, "starred_entries", service::markStarred)
        update(unstarredIds, "starred_entries", service::unmarkStarred)
        val feedJson = JSONArray(service.subscriptions().use { it.string() })
        val subscriptionIdsByFeedId = mutableMapOf<Long, String>()
        val feeds = List(feedJson.length()) { i ->
            val value = feedJson.getJSONObject(i)
            val feedId = value.optLong("feed_id")
            val subscriptionId = value.optLong("id").takeIf { it > 0 }?.toString()
            if (feedId > 0 && subscriptionId != null) subscriptionIdsByFeedId[feedId] = subscriptionId
            Feed(
                name = value.optString("title").takeIf(String::isNotBlank),
                url = value.optString("feed_url").takeIf(String::isNotBlank),
                siteUrl = value.optString("site_url").takeIf(String::isNotBlank),
                remoteId = subscriptionId,
            )
        }.filter { it.remoteId != null && it.url != null }
        val iconsJson = JSONArray(service.icons().use { it.string() })
        val iconsByHost = buildMap {
            for (index in 0 until iconsJson.length()) {
                val icon = iconsJson.optJSONObject(index) ?: continue
                val host = icon.optString("host").lowercase()
                val iconUrl = icon.optString("url")
                if (host.isNotBlank() && iconUrl.isNotBlank()) put(host, iconUrl)
            }
        }
        feeds.forEach { feed ->
            val host = runCatching { java.net.URI(feed.siteUrl ?: feed.url).host?.lowercase() }.getOrNull()
            feed.iconUrl = host?.let(iconsByHost::get)
        }

        val unread = idSet(service.unreadIds().use { it.string() })
        val starred = idSet(service.starredIds().use { it.string() })
        val changed = mutableListOf<Item>()
        val cursorParts = initialSyncCursor.orEmpty().split('|')
        var page = cursorParts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        var since = cursorParts.getOrNull(2)?.takeIf(String::isNotBlank)
            ?: initialSyncCursor?.takeIf { it.startsWith("since|") }?.substringAfter("since|")
        val startingPage = page
        var nextCursor: String? = null
        var newestCreatedAt: String? = since
        while (true) {
            val url = service.entries(page, since = if (syncType == SyncType.INITIAL_SYNC) null else since)
            val raw = JSONArray(url.use { it.string() })
            for (i in 0 until raw.length()) {
                val createdAt = raw.optJSONObject(i)?.optString("created_at")?.takeIf(String::isNotBlank)
                if (createdAt != null && (newestCreatedAt == null || createdAt > newestCreatedAt!!)) {
                    newestCreatedAt = createdAt
                }
            }
            changed += List(raw.length()) { i ->
                val value = raw.getJSONObject(i)
                val id = value.optLong("id")
                val published = value.optString("published")
                Item(
                    title = value.optString("title").takeIf(String::isNotBlank),
                    link = value.optString("url").takeIf(String::isNotBlank),
                    author = value.optString("author").takeIf(String::isNotBlank),
                    content = value.optString("content").takeIf(String::isNotBlank)
                        ?: value.optString("summary").takeIf(String::isNotBlank),
                    remoteId = id.takeIf { it > 0 }?.toString(),
                    feedRemoteId = subscriptionIdsByFeedId[value.optLong("feed_id")],
                    pubDate = runCatching { OffsetDateTime.parse(published).toLocalDateTime() }
                        .getOrElse { runCatching { LocalDateTime.parse(published) }.getOrElse { LocalDateTime.now() } },
                    isRead = id.toString() !in unread,
                    isStarred = id.toString() in starred
                )
            }.filter { it.remoteId != null && it.feedRemoteId != null }
            if (raw.length() < PAGE_SIZE) break
            if ((syncType == SyncType.INITIAL_SYNC || syncType == SyncType.CLASSIC_SYNC) && page - startingPage + 1 >= MAX_SYNC_PAGES) {
                nextCursor = "${if (syncType == SyncType.INITIAL_SYNC) "initial" else "recent"}|${page + 1}|${newestCreatedAt.orEmpty()}"
                break
            }
            page++
        }
        if (nextCursor == null) nextCursor = newestCreatedAt?.let { "since|$it" }
        return DataSourceResult(
            items = changed,
            feeds = feeds,
            folders = emptyList(),
            unreadIds = changed.filterNot { it.isRead }.mapNotNull { it.remoteId },
            readIds = changed.filter { it.isRead }.mapNotNull { it.remoteId },
            starredIds = changed.filter { it.isStarred }.mapNotNull { it.remoteId },
            syncCursor = nextCursor
        )
    }

    private fun idSet(value: String): Set<String> {
        val array = JSONArray(value)
        return buildSet(array.length()) { for (i in 0 until array.length()) add(array.optString(i)) }
    }

    suspend fun createSubscription(feedUrl: String): Feed = service.createSubscription(
        mapOf("feed_url" to feedUrl)
    ).use { response ->
        val value = org.json.JSONObject(response.string())
        Feed(
            name = value.optString("title").takeIf(String::isNotBlank) ?: feedUrl,
            url = value.optString("feed_url").takeIf(String::isNotBlank) ?: feedUrl,
            siteUrl = value.optString("site_url").takeIf(String::isNotBlank),
            remoteId = value.optLong("id").takeIf { it > 0 }?.toString()
                ?: throw IllegalStateException("Feedbin did not return a subscription ID")
        )
    }

    suspend fun updateSubscription(subscriptionId: Long, title: String) {
        service.updateSubscription(subscriptionId, mapOf("title" to title)).close()
    }

    suspend fun deleteSubscription(subscriptionId: Long) = service.deleteSubscription(subscriptionId)

    private suspend fun update(
        ids: List<String>, key: String,
        action: suspend (Map<String, List<Long>>) -> Unit
    ) {
        ids.mapNotNull(String::toLongOrNull).chunked(1000).forEach { batch ->
            if (batch.isNotEmpty()) action(mapOf(key to batch))
        }
    }

    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_SYNC_PAGES = 20
    }
}
