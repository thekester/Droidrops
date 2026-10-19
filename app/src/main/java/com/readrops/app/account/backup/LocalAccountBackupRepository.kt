package com.readrops.app.account.backup

import androidx.room.withTransaction
import com.readrops.app.account.backup.LocalAccountBackupFormat.InvalidBackupException
import com.readrops.db.Database
import com.readrops.db.entities.Tag
import com.readrops.db.entities.TagJoin
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream

data class BackupSummary(
    val folderCount: Int,
    val feedCount: Int,
    val itemCount: Int,
)

data class RestoreResult(
    val summary: BackupSummary,
    val accountId: Int,
    /** Ids, in the new account, of the feeds set to always show the full article. */
    val fullContentFeedIds: Set<Int>,
    /** App settings found in the backup. They are not applied by the restore itself. */
    val preferences: List<BackupPreference>,
)

class LocalAccountBackupRepository(private val database: Database) {

    /**
     * Writes the given account to [output]. The stream is closed afterwards.
     *
     * Server accounts can be backed up as well: their articles and states are exported as they
     * are stored on this device, and the backup is restored as a local account.
     */
    suspend fun export(
        account: Account,
        output: OutputStream,
        fullContentFeedIds: Set<Int> = emptySet(),
        preferences: List<BackupPreference> = emptyList(),
    ): BackupSummary {
        val folders = database.folderDao().selectAllFolders(account.id)
        val feeds = database.feedDao().selectFeeds(account.id)
        val tags = database.tagDao().selectAll(account.id)
        var itemCount = 0

        // server accounts may keep read and star states apart from the articles
        val separateStates = if (account.config.useSeparateState) {
            database.itemStateDao().selectAllItemStates(account.id).associateBy { it.remoteId }
        } else {
            null
        }

        LocalAccountBackupFormat.Writer(output.sink().buffer()).use { writer ->
            writer.writeHeader(
                LocalAccountBackupFormat.Header(
                    accountName = account.name,
                    notificationsEnabled = account.isNotificationsEnabled,
                    folders = folders,
                    // server ids mean nothing to a local account
                    feeds = if (account.isLocal) feeds else feeds.map { it.copy(remoteId = null) },
                    sourceAccountType = account.type?.name,
                    fullContentFeedIds = fullContentFeedIds.intersect(feeds.map { it.id }.toSet()),
                    preferences = preferences,
                    tags = tags,
                )
            )

            for (feed in feeds) {
                var lastId = 0
                while (true) {
                    val page = database.itemDao().selectItemsPage(feed.id, lastId, EXPORT_PAGE_SIZE)
                    if (page.isEmpty()) break

                    if (tags.isNotEmpty()) {
                        val itemTags = database.tagJoinDao().selectByItems(page.map { it.id })
                            .groupBy({ it.itemId }, { Tag(id = it.tagId, name = "") })
                        page.forEach { it.tags = itemTags[it.id].orEmpty() }
                    }

                    writer.writeItems(
                        if (account.isLocal) page else page.map { item ->
                            val state = separateStates?.get(item.remoteId)
                            item.copy(
                                isRead = state?.read ?: if (separateStates != null) false else item.isRead,
                                isStarred = state?.starred ?: if (separateStates != null) false else item.isStarred,
                                // local syncs identify entries by guid, else by link: the link is
                                // the closest match to a server item id once restored locally
                                remoteId = item.link ?: item.remoteId,
                            )
                        }
                    )
                    itemCount += page.size
                    lastId = page.last().id
                }
            }

            writer.finish()
        }

        return BackupSummary(folders.size, feeds.size, itemCount)
    }

    /**
     * Restores a backup as a new local account, which becomes the current one.
     *
     * Existing accounts are never modified. Everything happens in a single transaction:
     * an invalid or truncated file leaves the database exactly as it was.
     */
    suspend fun restore(
        input: InputStream,
        defaultAccountName: String,
        restoredAccountName: (String) -> String = { it },
    ): RestoreResult {
        var folderCount = 0
        var feedCount = 0
        var itemCount = 0
        var accountId = 0
        var fullContentFeedIds = emptySet<Int>()
        var preferences = emptyList<BackupPreference>()

        database.withTransaction {
            val folderIds = HashMap<Int, Int>()
            val feedIds = HashMap<Int, Int>()
            val tagIds = HashMap<Int, Int>()

            input.source().buffer().use { source ->
                LocalAccountBackupFormat.read(
                    source = source,
                    onHeader = { header ->
                        accountId = database.accountDao().insert(
                            Account(
                                name = restoredAccountName(
                                    header.accountName?.takeIf { it.isNotBlank() } ?: defaultAccountName
                                ),
                                type = AccountType.LOCAL,
                                isCurrentAccount = true,
                                isNotificationsEnabled = header.notificationsEnabled,
                            )
                        ).toInt()

                        header.folders.forEach { folder ->
                            val backupId = folder.id
                            folderIds[backupId] = database.folderDao().insert(
                                folder.copy(id = 0, remoteId = null, accountId = accountId)
                            ).toInt()
                        }

                        header.tags.forEach { tag ->
                            // names are unique per account: a duplicate in the file is skipped
                            val id = database.tagDao().insertIgnoreConflicts(
                                tag.copy(id = 0, remoteId = null, accountId = accountId)
                            )
                            if (id > 0) tagIds[tag.id] = id.toInt()
                        }

                        header.feeds
                            .filter { !it.url.isNullOrBlank() }
                            .forEach { feed ->
                                val backupId = feed.id
                                feedIds[backupId] = database.feedDao().insert(
                                    feed.copy(
                                        id = 0,
                                        accountId = accountId,
                                        folderId = feed.folderId?.let(folderIds::get),
                                        etag = null,
                                        lastModified = null,
                                        lastUpdated = null,
                                    )
                                ).toInt()
                            }

                        folderCount = folderIds.size
                        feedCount = feedIds.size
                        fullContentFeedIds = header.fullContentFeedIds.mapNotNull(feedIds::get).toSet()
                        preferences = header.preferences
                    },
                    onItems = { items ->
                        val restored = items.mapNotNull { item ->
                            feedIds[item.feedId]?.let { feedId -> item.copy(id = 0, feedId = feedId) }
                        }

                        if (restored.isNotEmpty()) {
                            val itemIds = database.itemDao().insert(restored)
                            itemCount += restored.size

                            val tagJoins = restored.zip(itemIds).flatMap { (item, itemId) ->
                                item.tags.mapNotNull { tagIds[it.id] }
                                    .map { tagId -> TagJoin(tagId = tagId, itemId = itemId.toInt()) }
                            }
                            if (tagJoins.isNotEmpty()) database.tagJoinDao().insertConflict(tagJoins)
                        }
                    }
                )
            }

            if (feedIds.isEmpty() && folderIds.isEmpty()) {
                throw InvalidBackupException("The backup contains no feeds")
            }
        }

        return RestoreResult(
            summary = BackupSummary(folderCount, feedCount, itemCount),
            accountId = accountId,
            fullContentFeedIds = fullContentFeedIds,
            preferences = preferences,
        )
    }

    companion object {
        private const val EXPORT_PAGE_SIZE = 100
    }
}
