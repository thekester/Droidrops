package com.readrops.app.more.cleanup

import androidx.room.withTransaction
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CleanupReadFilter { READ, UNREAD, BOTH }

data class ArticleCleanupState(
    val accountId: Int? = null,
    val accountName: String = "",
    val separateState: Boolean = false,
    val feeds: List<Feed> = emptyList(),
    val selectedFeedIds: Set<Int> = emptySet(),
    val periodDays: Int = 30,
    val readFilter: CleanupReadFilter = CleanupReadFilter.READ,
    val keepStarred: Boolean = true,
    val previewCount: Int = 0,
    val isPreviewing: Boolean = true,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val deletedCount: Int? = null,
    val error: Boolean = false
)

class ArticleCleanupScreenModel(
    private val database: Database,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ScreenModel {
    private val mutableState = MutableStateFlow(ArticleCleanupState())
    val cleanupState = mutableState.asStateFlow()
    private var previewJob: Job? = null
    private val ioDispatcher = dispatcher

    init {
        screenModelScope.launch(dispatcher) {
            database.accountDao().selectCurrentAccount().collectLatest { account ->
                if (account == null) {
                    mutableState.update { it.copy(accountId = null, feeds = emptyList(), selectedFeedIds = emptySet(), isLoading = false) }
                    return@collectLatest
                }

                val feeds = database.feedDao().selectFeeds(account.id)
                mutableState.update {
                    it.copy(
                        accountId = account.id,
                        accountName = account.name ?: "",
                        separateState = account.config.useSeparateState,
                        feeds = feeds,
                        selectedFeedIds = feeds.map { feed -> feed.id }.toSet(),
                        isLoading = false,
                        deletedCount = null
                    )
                }
                refreshPreview()
            }
        }
    }

    fun setPeriod(days: Int) = updateCriteria { copy(periodDays = days, deletedCount = null) }

    fun setReadFilter(filter: CleanupReadFilter) = updateCriteria { copy(readFilter = filter, deletedCount = null) }

    fun setKeepStarred(keep: Boolean) = updateCriteria { copy(keepStarred = keep, deletedCount = null) }

    fun toggleFeed(feedId: Int) = updateCriteria {
        copy(
            selectedFeedIds = if (feedId in selectedFeedIds) selectedFeedIds - feedId
            else selectedFeedIds + feedId,
            deletedCount = null
        )
    }

    fun selectAllFeeds(selected: Boolean) = updateCriteria {
        copy(
            selectedFeedIds = if (selected) feeds.map { it.id }.toSet() else emptySet(),
            deletedCount = null
        )
    }

    fun deleteSelected() {
        val snapshot = mutableState.value
        val accountId = snapshot.accountId ?: return
        if (snapshot.selectedFeedIds.isEmpty() || snapshot.previewCount == 0 || snapshot.isDeleting || snapshot.isPreviewing) return

        screenModelScope.launch(ioDispatcher) {
            mutableState.update { it.copy(isDeleting = true, error = false) }
            try {
                val cleanupCutoff = cutoff(snapshot.periodDays)
                val deleted = database.withTransaction {
                    var total = 0
                    for (feedChunk in snapshot.selectedFeedIds.toList().chunked(SQLITE_SAFE_CHUNK_SIZE)) {
                        var afterId = 0
                        while (true) {
                            val ids = database.itemDao().selectCleanupItemIdsPage(
                                accountId = accountId,
                                feedIds = feedChunk,
                                cutoff = cleanupCutoff,
                                includeRead = snapshot.readFilter != CleanupReadFilter.UNREAD,
                                includeUnread = snapshot.readFilter != CleanupReadFilter.READ,
                                keepStarred = snapshot.keepStarred,
                                separateState = snapshot.separateState,
                                afterId = afterId,
                                limit = CLEANUP_PAGE_SIZE
                            )
                            if (ids.isEmpty()) break
                            database.itemDao().deleteByIds(ids)
                            total += ids.size
                            afterId = ids.last()
                        }
                    }
                    total
                }
                mutableState.update { it.copy(isDeleting = false, deletedCount = deleted, previewCount = 0) }
                refreshPreview()
            } catch (_: Exception) {
                mutableState.update { it.copy(isDeleting = false, error = true) }
            }
        }
    }

    private fun updateCriteria(transform: ArticleCleanupState.() -> ArticleCleanupState) {
        mutableState.update { transform(it).copy(error = false) }
        refreshPreview()
    }

    private fun refreshPreview() {
        val snapshot = mutableState.value
        previewJob?.cancel()
        if (snapshot.accountId == null || snapshot.selectedFeedIds.isEmpty()) {
            mutableState.update { it.copy(previewCount = 0, isPreviewing = false) }
            return
        }

        mutableState.update { it.copy(isPreviewing = true) }
        previewJob = screenModelScope.launch(ioDispatcher) {
            val cutoff = cutoff(snapshot.periodDays)
            try {
                val count = snapshot.selectedFeedIds.toList()
                    .chunked(SQLITE_SAFE_CHUNK_SIZE)
                    .sumOf { feedChunk ->
                        database.itemDao().countCleanupItems(
                            accountId = snapshot.accountId,
                            feedIds = feedChunk,
                            cutoff = cutoff,
                            includeRead = snapshot.readFilter != CleanupReadFilter.UNREAD,
                            includeUnread = snapshot.readFilter != CleanupReadFilter.READ,
                            keepStarred = snapshot.keepStarred,
                            separateState = snapshot.separateState
                        )
                    }
                mutableState.update { current ->
                    if (current.accountId == snapshot.accountId && current.periodDays == snapshot.periodDays &&
                        current.selectedFeedIds == snapshot.selectedFeedIds && current.readFilter == snapshot.readFilter &&
                        current.keepStarred == snapshot.keepStarred
                    ) current.copy(previewCount = count, isPreviewing = false) else current
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(error = true, isPreviewing = false) }
            }
        }
    }

    private fun cutoff(days: Int): Long = System.currentTimeMillis() - days.toLong() * MILLIS_PER_DAY

    private companion object {
        const val CLEANUP_PAGE_SIZE = 500
        const val SQLITE_SAFE_CHUNK_SIZE = 500
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
