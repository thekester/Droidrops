package com.readrops.app.account.backup

import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.OpenIn
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.BufferedSink
import okio.BufferedSource
import java.io.IOException
import java.time.LocalDateTime

/**
 * A plain JSON backup of an account: folders, feeds with their per-feed settings, articles with
 * their read and favourite state and, optionally, the app settings. It is always restored as a
 * local account, which also makes it a way to leave a server account with one's history.
 *
 * Secrets are deliberately left out: no account token, no server credentials and no feed HTTP
 * credentials (those are only kept in the encrypted preferences). Feed cache headers are dropped
 * as well, so the first sync after a restore fetches every feed again.
 *
 * The document is written and read as a stream, articles last, so that large histories do
 * not have to fit in memory. Fields added after version 1 are optional, so that older and newer
 * readers ignore what they do not know.
 */
object LocalAccountBackupFormat {

    const val FORMAT = "droidrops-local-account-backup"
    const val VERSION = 1
    const val MIME_TYPE = "application/json"

    data class Header(
        val accountName: String?,
        val notificationsEnabled: Boolean,
        val folders: List<Folder>,
        val feeds: List<Feed>,
        /** The type of the account the backup was made from, null for older backups. */
        val sourceAccountType: String? = null,
        /** Backup ids of the feeds set to always show the full article. */
        val fullContentFeedIds: Set<Int> = emptySet(),
        val preferences: List<BackupPreference> = emptyList(),
    )

    class InvalidBackupException(message: String) : IOException(message)

    class Writer(sink: BufferedSink) : AutoCloseable {

        private val json = JsonWriter.of(sink).apply { indent = "" }
        private var itemsStarted = false

        fun writeHeader(header: Header, exportedAt: LocalDateTime = LocalDateTime.now()) {
            json.beginObject()
            json.name("format").value(FORMAT)
            json.name("version").value(VERSION.toLong())
            json.name("exportedAt").value(exportedAt.toString())

            json.name("account").beginObject()
            json.name("name").value(header.accountName)
            json.name("type").value(header.sourceAccountType)
            json.name("notificationsEnabled").value(header.notificationsEnabled)
            json.endObject()

            json.name("folders").beginArray()
            header.folders.forEach { folder ->
                json.beginObject()
                json.name("id").value(folder.id.toLong())
                json.name("name").value(folder.name)
                json.endObject()
            }
            json.endArray()

            json.name("feeds").beginArray()
            header.feeds.forEach { writeFeed(it, it.id in header.fullContentFeedIds) }
            json.endArray()

            json.name("preferences").beginArray()
            header.preferences.forEach(::writePreference)
            json.endArray()
        }

        private fun writeFeed(feed: Feed, fullContent: Boolean) = with(json) {
            beginObject()
            name("id").value(feed.id.toLong())
            name("name").value(feed.name)
            name("description").value(feed.description)
            name("url").value(feed.url)
            name("siteUrl").value(feed.siteUrl)
            name("imageUrl").value(feed.imageUrl)
            name("iconUrl").value(feed.iconUrl)
            name("color").value(feed.color.toLong())
            name("folderId").value(feed.folderId?.toLong())
            name("remoteId").value(feed.remoteId)
            name("notificationEnabled").value(feed.isNotificationEnabled)
            name("openIn").value(feed.openIn.name)
            name("openInAsk").value(feed.openInAsk)
            name("fullContent").value(fullContent)
            endObject()
        }

        private fun writePreference(preference: BackupPreference) = with(json) {
            beginObject()
            name("key").value(preference.key)
            name("type").value(preference.type.name)
            name("value")
            when (val value = preference.value) {
                is Boolean -> value(value)
                is Int -> value(value.toLong())
                is Long -> value(value)
                is Float -> value(value.toDouble())
                is Double -> value(value)
                is String -> value(value)
                is Set<*> -> {
                    beginArray()
                    value.forEach { value(it as String) }
                    endArray()
                }

                else -> nullValue()
            }
            endObject()
        }

        fun writeItems(items: List<Item>) {
            if (!itemsStarted) {
                json.name("items").beginArray()
                itemsStarted = true
            }

            items.forEach { item ->
                with(json) {
                    beginObject()
                    name("feedId").value(item.feedId.toLong())
                    name("title").value(item.title)
                    name("description").value(item.description)
                    name("cleanDescription").value(item.cleanDescription)
                    name("content").value(item.content)
                    name("link").value(item.link)
                    name("imageLink").value(item.imageLink)
                    name("author").value(item.author)
                    name("pubDate").value(item.pubDate?.toString())
                    name("readTime").value(item.readTime)
                    name("read").value(item.isRead)
                    name("starred").value(item.isStarred)
                    name("remoteId").value(item.remoteId)
                    endObject()
                }
            }
        }

        /** Ends the document. Must be called for the backup to be readable. */
        fun finish() {
            if (!itemsStarted) writeItems(emptyList())
            json.endArray()
            json.endObject()
            json.flush()
        }

        override fun close() = json.close()
    }

    /**
     * Reads a backup. [onHeader] is called once, before any [onItems] batch; item feed ids
     * are those of the backup, which callers map to the feeds they inserted.
     */
    suspend fun read(
        source: BufferedSource,
        onHeader: suspend (Header) -> Unit,
        onItems: suspend (List<Item>) -> Unit,
        batchSize: Int = 200,
    ) {
        val json = JsonReader.of(source)
        var format: String? = null
        var version: Int? = null
        var accountName: String? = null
        var accountType: String? = null
        var notificationsEnabled = false
        var folders: List<Folder>? = null
        var feeds: List<Pair<Feed, Boolean>>? = null
        var preferences: List<BackupPreference> = emptyList()
        var headerSent = false

        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "format" -> format = json.nextString()
                "version" -> version = json.nextInt()
                "account" -> {
                    json.beginObject()
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "name" -> accountName = json.nullableString()
                            "type" -> accountType = json.nullableString()
                            "notificationsEnabled" -> notificationsEnabled = json.nextBoolean()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                }

                "folders" -> folders = json.readArray { readFolder() }
                "feeds" -> feeds = json.readArray { readFeed() }
                "preferences" -> preferences = json.readArray { readPreference() }.filterNotNull()
                "items" -> {
                    if (format != FORMAT) throw InvalidBackupException("Not a Droidrops account backup")
                    if (version == null || version > VERSION) {
                        throw InvalidBackupException("Unsupported backup version $version")
                    }
                    if (folders == null || feeds == null) {
                        throw InvalidBackupException("Folders and feeds must come before articles")
                    }

                    onHeader(
                        Header(
                            accountName = accountName,
                            notificationsEnabled = notificationsEnabled,
                            folders = folders,
                            feeds = feeds.map { it.first },
                            sourceAccountType = accountType,
                            fullContentFeedIds = feeds.filter { it.second }.map { it.first.id }.toSet(),
                            preferences = preferences,
                        )
                    )
                    headerSent = true

                    val batch = ArrayList<Item>(batchSize)
                    json.beginArray()
                    while (json.hasNext()) {
                        batch += json.readItem()
                        if (batch.size >= batchSize) {
                            onItems(batch.toList())
                            batch.clear()
                        }
                    }
                    json.endArray()
                    if (batch.isNotEmpty()) onItems(batch.toList())
                }

                else -> json.skipValue()
            }
        }
        json.endObject()

        if (!headerSent) throw InvalidBackupException("The backup has no article section")
    }

    private fun JsonReader.readFolder(): Folder {
        val folder = Folder()
        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "id" -> folder.id = nextInt()
                "name" -> folder.name = nullableString()
                else -> skipValue()
            }
        }
        endObject()
        return folder
    }

    private fun JsonReader.readFeed(): Pair<Feed, Boolean> {
        val feed = Feed()
        var fullContent = false
        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "id" -> feed.id = nextInt()
                "name" -> feed.name = nullableString()
                "description" -> feed.description = nullableString()
                "url" -> feed.url = nullableString()
                "siteUrl" -> feed.siteUrl = nullableString()
                "imageUrl" -> feed.imageUrl = nullableString()
                "iconUrl" -> feed.iconUrl = nullableString()
                "color" -> feed.color = nextLong().toInt()
                "folderId" -> feed.folderId = nullableInt()
                "remoteId" -> feed.remoteId = nullableString()
                "notificationEnabled" -> feed.isNotificationEnabled = nextBoolean()
                "openIn" -> feed.openIn = nullableString()
                    ?.let { name -> OpenIn.entries.firstOrNull { it.name == name } }
                    ?: OpenIn.LOCAL_VIEW
                "openInAsk" -> feed.openInAsk = nextBoolean()
                "fullContent" -> fullContent = nextBoolean()
                else -> skipValue()
            }
        }
        endObject()
        return feed to fullContent
    }

    /** Unknown types or malformed values are dropped rather than failing the whole restore. */
    private fun JsonReader.readPreference(): BackupPreference? {
        var key: String? = null
        var type: BackupPreference.Type? = null
        var value: Any? = null
        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "key" -> key = nullableString()
                "type" -> type = nullableString()?.let { name ->
                    BackupPreference.Type.entries.firstOrNull { it.name == name }
                }
                "value" -> value = readJsonValue()
                else -> skipValue()
            }
        }
        endObject()

        return if (key != null && type != null && value != null) {
            BackupPreference.of(key, type, value)
        } else {
            null
        }
    }

    private fun JsonReader.readItem(): Item {
        val item = Item()
        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "feedId" -> item.feedId = nextInt()
                "title" -> item.title = nullableString()
                "description" -> item.description = nullableString()
                "cleanDescription" -> item.cleanDescription = nullableString()
                "content" -> item.content = nullableString()
                "link" -> item.link = nullableString()
                "imageLink" -> item.imageLink = nullableString()
                "author" -> item.author = nullableString()
                "pubDate" -> item.pubDate = nullableString()?.let {
                    runCatching { LocalDateTime.parse(it) }.getOrNull()
                }
                "readTime" -> item.readTime = nextDouble()
                "read" -> item.isRead = nextBoolean()
                "starred" -> item.isStarred = nextBoolean()
                "remoteId" -> item.remoteId = nullableString()
                else -> skipValue()
            }
        }
        endObject()
        return item
    }

    private fun <T> JsonReader.readArray(readElement: JsonReader.() -> T): List<T> {
        val elements = mutableListOf<T>()
        beginArray()
        while (hasNext()) elements += readElement()
        endArray()
        return elements
    }

    private fun JsonReader.nullableString(): String? =
        if (peek() == JsonReader.Token.NULL) nextNull() else nextString()

    private fun JsonReader.nullableInt(): Int? =
        if (peek() == JsonReader.Token.NULL) nextNull() else nextInt()
}
