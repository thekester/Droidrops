package com.readrops.app.timelime

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.work.workDataOf
import cafe.adriel.voyager.core.model.screenModelScope
import com.readrops.app.R
import com.readrops.app.home.TabScreenModel
import com.readrops.app.repositories.ErrorResult
import com.readrops.app.repositories.GetFoldersWithFeeds
import com.readrops.app.sync.SyncWorker
import com.readrops.app.timelime.components.SwipeAction
import com.readrops.app.timelime.components.TimelineItemSize
import com.readrops.app.util.PAGING_INITIAL_SIZE
import com.readrops.app.util.PAGING_PAGE_SIZE
import com.readrops.app.util.PAGING_PREFETCH_DISTANCE
import com.readrops.app.util.Preferences
import com.readrops.app.util.Utils
import com.readrops.app.util.extensions.clearSerializables
import com.readrops.app.util.extensions.getSerializable
import com.readrops.app.util.extensions.isConnected
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.entities.OpenIn
import com.readrops.db.filters.MainFilter
import com.readrops.db.filters.OrderField
import com.readrops.db.filters.OrderType
import com.readrops.db.filters.QueryFilters
import com.readrops.db.filters.SubFilter
import com.readrops.db.pojo.ItemWithFeed
import com.readrops.db.queries.ItemSelectionQueryBuilder
import com.readrops.db.queries.ItemsQueryBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.readrops.db.entities.Tag
import java.util.Collections
import java.util.LinkedHashSet

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineScreenModel(
    private val database: Database,
    private val getFoldersWithFeeds: GetFoldersWithFeeds,
    private val preferences: Preferences,
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TabScreenModel(database, context) {

    private val _timelineState = MutableStateFlow(TimelineState())
    val timelineState = _timelineState.asStateFlow()

    // separate this from main Timeline state for performances
    // as it will be very often updated
    private val _listIndexState = MutableStateFlow(0)
    val listIndexState = _listIndexState.asStateFlow()

    private val filters = MutableStateFlow(_timelineState.value.filters)
    private val recentlyReadItemIds = Collections.synchronizedSet(LinkedHashSet<Int>())

    private val useCustomShareIntentTpl = preferences.useCustomShareIntentTpl.flow.stateIn(
        screenModelScope, SharingStarted.Eagerly, false
    )
    private val customShareIntentTpl = preferences.customShareIntentTpl.flow.stateIn(
        screenModelScope, SharingStarted.Eagerly, ""
    )

    init {
        screenModelScope.launch(dispatcher) {
            var syncAtLaunch = preferences.synchAtLaunch.flow.first()
            filters.update { it.copy(mainFilter = MainFilter.valueOf(preferences.mainFilter.flow.first())) }

            combine(
                accountEvent,
                filters,
                getTimelinePreferences()
            ) { account, filters, timelinePreferences ->
                Triple(account, filters.copy(accountId = account.id), timelinePreferences)
            }.collectLatest { (account, filters, timelinePreferences) ->
                _timelineState.update {
                    it.copy(
                        preferences = timelinePreferences,
                        filters = filters.copy(
                            showReadItems = timelinePreferences.showReadItems,
                            orderField = timelinePreferences.orderField,
                            orderType = timelinePreferences.orderType,
                            recentlyReadItemIds = if (timelinePreferences.showRecentlyReadItems) {
                                recentlyReadItemIdsSnapshot()
                            } else {
                                emptyList()
                            }
                        )
                    )
                }

                if (syncAtLaunch) {
                    refreshTimeline()
                    syncAtLaunch = false
                } else {
                    buildPager()
                }

                preferences.hideReadFeeds.flow
                    .flatMapLatest { hideReadFeeds ->
                        getFoldersWithFeeds.get(
                            accountId = account.id,
                            mainFilter = filters.mainFilter,
                            useSeparateState = account.config.useSeparateState,
                            hideReadFeeds = hideReadFeeds
                        )
                    }
                    .collect { foldersAndFeeds ->
                        _timelineState.update {
                            it.copy(
                                foldersAndFeeds = foldersAndFeeds
                            )
                        }
                    }
            }
        }

        screenModelScope.launch(dispatcher) {
            accountEvent.collectLatest { account ->
                // tags are listed in the drawer so the timeline can be filtered on one
                database.tagDao().selectAllFlow(account.id)
                    .collect { tags ->
                        _timelineState.update { it.copy(tags = tags) }
                    }
            }
        }

        screenModelScope.launch(dispatcher) {
            accountEvent.flatMapLatest {
                getFoldersWithFeeds.getNewItemsUnreadCount(it.id, it.config.useSeparateState)
            }.collectLatest { count ->
                _timelineState.update {
                    it.copy(unreadNewItemsCount = count)
                }
            }
        }

        screenModelScope.launch(dispatcher) {
            accountEvent.flatMapLatest { database.folderDao().selectFolders(it.id) }
                .collect { folders -> reconcileFolderFilter(folders) }
        }
    }

    /**
     * The selected folder can be renamed or deleted on the server side. Its name is then kept up to
     * date, and the timeline falls back on the default filter when the folder doesn't exist
     * anymore, instead of staying stuck on an empty list.
     */
    private fun reconcileFolderFilter(folders: List<Folder>) {
        val state = _timelineState.value
        if (state.filters.subFilter != SubFilter.FOLDER) {
            return
        }

        val folder = folders.find { it.id == state.filters.folderId }

        when {
            folder == null -> _timelineState.update {
                it.copy(
                    filters = updateFilters {
                        it.filters.copy(subFilter = SubFilter.ALL, folderId = 0)
                    },
                    filterFolderName = ""
                )
            }

            folder.name != state.filterFolderName -> _timelineState.update {
                it.copy(filterFolderName = folder.name.orEmpty())
            }
        }
    }

    private fun getTimelinePreferences(): Flow<TimelinePreferences> = with(preferences) {
        return combine<Any, TimelinePreferences>(
            timelineItemSize.flow,
            showTimelineImages.flow,
            showTimelineSource.flow,
            showTimelineDate.flow,
            showTimelineReadTime.flow,
            scrollRead.flow,
            displayNotificationsPermission.flow,
            showReadItems.flow,
            orderField.flow,
            orderType.flow,
            theme.flow,
            openLinksWith.flow,
            globalOpenInAsk.flow,
            synchAtLaunch.flow,
            swipeToLeft.flow,
            swipeToRight.flow,
            showRecentlyReadItems.flow,
            transform = {
                TimelinePreferences(
                    itemSize = when (it[0]) {
                        "compact" -> TimelineItemSize.COMPACT
                        "regular" -> TimelineItemSize.REGULAR
                        else -> TimelineItemSize.LARGE
                    },
                    showImages = it[1] as Boolean,
                    showSource = it[2] as Boolean,
                    showDate = it[3] as Boolean,
                    showReadTime = it[4] as Boolean,
                    markReadOnScroll = it[5] as Boolean,
                    displayNotificationsPermission = it[6] as Boolean,
                    showReadItems = it[7] as Boolean,
                    orderField = OrderField.valueOf(it[8] as String),
                    orderType = OrderType.valueOf(it[9] as String),
                    theme = it[10] as String,
                    openInExternalBrowser = it[11] as String == "external_navigator",
                    openInAsk = it[12] as Boolean,
                    syncAtLaunch = it[13] as Boolean,
                    swipeToLeft = SwipeAction.valueOf(it[14] as String),
                    swipeToRight = SwipeAction.valueOf(it[15] as String),
                    showRecentlyReadItems = it[16] as Boolean
                )
            }
        )
    }

    private fun buildPager(empty: Boolean = false) {
        val query = ItemsQueryBuilder.buildItemsQuery(
            queryFilters = _timelineState.value.filters,
            separateState = currentAccount!!.config.useSeparateState
        )

        val pager = Pager(
            config = PagingConfig(
                initialLoadSize = PAGING_INITIAL_SIZE,
                pageSize = PAGING_PAGE_SIZE,
                prefetchDistance = PAGING_PREFETCH_DISTANCE
            ),
            pagingSourceFactory = {
                database.itemDao().selectAll(query)
            },
        )
            .flow
            .map { pagingData ->
                pagingData.map { itemWithFeed ->
                    itemWithFeed.item.tags = database.tagDao().selectAllByItem(itemWithFeed.item.id)

                    itemWithFeed
                }
            }
            .cachedIn(screenModelScope)

        _timelineState.update {
            it.copy(
                itemState = if (!empty) {
                    pager
                } else {
                    emptyFlow()
                },
                isAccountLocal = currentAccount!!.isLocal,
                scrollToTop = true,
                hideReadAllFAB = !currentAccount!!.config.canMarkAllItemsAsRead
            )
        }

        _listIndexState.update { 0 }
    }

    @Suppress("UNCHECKED_CAST")
    fun refreshTimeline() {
        if (!context.isConnected()) {
            _timelineState.update { it.copy(syncError = context.getString(R.string.no_network)) }
            return
        }

        screenModelScope.launch(dispatcher) {
            val filterPair = with(_timelineState.value.filters) {
                when (subFilter) {
                    SubFilter.FEED -> SyncWorker.FEED_ID_KEY to feedId
                    SubFilter.FOLDER -> SyncWorker.FOLDER_ID_KEY to folderId
                    else -> null
                }
            }
            val accountPair = SyncWorker.ACCOUNT_ID_KEY to currentAccount!!.id

            val workData = if (filterPair != null) {
                workDataOf(filterPair, accountPair)
            } else {
                workDataOf(accountPair)
            }

            if (!currentAccount!!.isLocal) {
                _timelineState.update {
                    it.copy(
                        isRefreshing = true,
                        hideReadAllFAB = true
                    )
                }
            }

            SyncWorker.startNow(context, workData) { workInfo ->
                when {
                    workInfo.outputData.getBoolean(SyncWorker.END_SYNC_KEY, false) -> {
                        val errors =
                            workInfo.outputData.getSerializable(SyncWorker.LOCAL_SYNC_ERRORS_KEY) as ErrorResult?
                        workInfo.outputData.clearSerializables()

                        _timelineState.update {
                            it.copy(
                                isRefreshing = false,
                                hideReadAllFAB = false,
                                scrollToTop = true,
                                localSyncErrors = errors?.ifEmpty { null }
                            )
                        }

                        buildPager()
                    }

                    workInfo.outputData.getBoolean(SyncWorker.SYNC_FAILURE_KEY, false) -> {
                        val error =
                            workInfo.outputData.getSerializable(SyncWorker.SYNC_FAILURE_EXCEPTION_KEY) as Exception?
                        workInfo.outputData.clearSerializables()

                        _timelineState.update {
                            it.copy(
                                // the exception sits in a process wide map that the first delivery
                                // clears, so a repeated failed state finds nothing: keep the message
                                syncError = error?.let { e -> accountError?.genericMessage(e) }
                                    ?: it.syncError,
                                isRefreshing = false,
                                hideReadAllFAB = false
                            )
                        }

                        buildPager()
                    }

                    workInfo.progress.getString(SyncWorker.FEED_NAME_KEY) != null -> {
                        _timelineState.update {
                            it.copy(
                                isRefreshing = true,
                                currentFeed = workInfo.progress.getString(SyncWorker.FEED_NAME_KEY)
                                    ?: "",
                                feedCount = workInfo.progress.getInt(SyncWorker.FEED_COUNT_KEY, 0),
                                feedMax = workInfo.progress.getInt(SyncWorker.FEED_MAX_KEY, 0)
                            )
                        }
                    }
                }
            }
        }
    }

    fun openDrawer() {
        _timelineState.update { it.copy(isDrawerOpen = true) }
    }

    fun closeDrawer() {
        _timelineState.update { it.copy(isDrawerOpen = false) }
    }

    fun updateSearchQuery(query: String) {
        filters.update { it.copy(searchQuery = query) }
    }

    fun updateDrawerDefaultItem(selection: MainFilter) {
        _timelineState.update {
            it.copy(
                filters = updateFilters {
                    it.filters.copy(
                        mainFilter = selection,
                        subFilter = SubFilter.ALL,
                        feedId = 0,
                        folderId = 0,
                        tagId = 0
                    )
                },
                isDrawerOpen = false
            )
        }
    }

    fun updateDrawerFolderSelection(folder: Folder) {
        _timelineState.update {
            it.copy(
                filters = updateFilters {
                    it.filters.copy(
                        subFilter = SubFilter.FOLDER,
                        folderId = folder.id,
                        feedId = 0,
                        tagId = 0
                    )
                },
                filterFolderName = folder.name!!,
                isDrawerOpen = false
            )
        }
    }

    fun updateDrawerTagSelection(tag: Tag) {
        _timelineState.update {
            it.copy(
                filters = updateFilters {
                    it.filters.copy(
                        subFilter = SubFilter.TAG,
                        tagId = tag.id,
                        feedId = 0,
                        folderId = 0
                    )
                },
                filterTagName = tag.name,
                isDrawerOpen = false
            )
        }
    }

    fun updateDrawerFeedSelection(feed: Feed) {
        _timelineState.update {
            it.copy(
                filters = updateFilters {
                    it.filters.copy(
                        subFilter = SubFilter.FEED,
                        feedId = feed.id,
                        folderId = 0,
                        tagId = 0
                    )
                },
                filterFeedName = feed.name!!,
                isDrawerOpen = false
            )
        }
    }

    private fun updateFilters(block: () -> QueryFilters): QueryFilters {
        val filter = block()
        filters.update { filter }

        return filter
    }

    fun setItemRead(item: Item) {
        item.isRead = true

        screenModelScope.launch(dispatcher) {
            rememberRecentlyReadItem(item.id)
            repository?.setItemReadState(item)
        }
    }

    fun updateItemReadState(item: Item) {
        screenModelScope.launch(dispatcher) {
            with(item) {
                isRead = !isRead
                if (isRead) rememberRecentlyReadItem(id) else forgetRecentlyReadItem(id)
                repository?.setItemReadState(this)
            }
        }
    }

    fun setItemReadState(item: Item, isRead: Boolean) {
        screenModelScope.launch(dispatcher) {
            item.isRead = isRead
            if (isRead) rememberRecentlyReadItem(item.id) else forgetRecentlyReadItem(item.id)
            repository?.setItemReadState(item)
        }
    }

    /** Keep recently opened/read item IDs for this app session only. */
    fun trackRecentlyReadItem(itemId: Int) {
        screenModelScope.launch(dispatcher) { rememberRecentlyReadItem(itemId) }
    }

    private suspend fun rememberRecentlyReadItem(itemId: Int) {
        if (itemId <= 0) return
        synchronized(recentlyReadItemIds) {
            recentlyReadItemIds.remove(itemId)
            recentlyReadItemIds.add(itemId)
            while (recentlyReadItemIds.size > MAX_RECENTLY_READ_ITEMS) {
                recentlyReadItemIds.remove(recentlyReadItemIds.first())
            }
        }
        updateRecentlyReadFilterIfEnabled()
    }

    private suspend fun forgetRecentlyReadItem(itemId: Int) {
        synchronized(recentlyReadItemIds) { recentlyReadItemIds.remove(itemId) }
        updateRecentlyReadFilterIfEnabled()
    }

    private fun recentlyReadItemIdsSnapshot(): List<Int> = synchronized(recentlyReadItemIds) {
        recentlyReadItemIds.toList()
    }

    private suspend fun updateRecentlyReadFilterIfEnabled() {
        if (preferences.showRecentlyReadItems.flow.first() || filters.value.recentlyReadOnly) {
            filters.update { it.copy(recentlyReadItemIds = recentlyReadItemIds.toList()) }
        }
    }

    fun updateStarState(item: Item) {
        screenModelScope.launch(dispatcher) {
            with(item) {
                isStarred = isStarred.not()
                repository?.setItemStarState(this)
            }
        }
    }

    fun setStarState(item: Item, isStarred: Boolean) {
        screenModelScope.launch(dispatcher) {
            item.isStarred = isStarred
            repository?.setItemStarState(item)
        }
    }


    fun setAllItemsRead() {
        screenModelScope.launch(dispatcher) {
            when (_timelineState.value.filters.subFilter) {
                SubFilter.FEED ->
                    repository?.setAllItemsReadByFeed(
                        feedId = _timelineState.value.filters.feedId
                    )

                SubFilter.FOLDER -> repository?.setAllItemsReadByFolder(
                    folderId = _timelineState.value.filters.folderId
                )

                else -> when (_timelineState.value.filters.mainFilter) {
                    MainFilter.STARS -> repository?.setAllStarredItemsRead()
                    MainFilter.ALL -> repository?.setAllItemsRead()
                    MainFilter.NEW -> repository?.setAllNewItemsRead()
                }
            }
        }
    }

    fun openDialog(dialog: DialogState) = _timelineState.update { it.copy(dialog = dialog) }

    fun closeDialog(dialog: DialogState? = null) {
        if (dialog is DialogState.ErrorList) {
            _timelineState.update { it.copy(localSyncErrors = null) }
        }

        _timelineState.update { it.copy(dialog = null) }
    }

    fun setShowReadItemsState(showReadItems: Boolean) {
        screenModelScope.launch {
            preferences.showReadItems.write(showReadItems)

            _timelineState.update {
                it.copy(
                    filters = it.filters.copy(showReadItems = showReadItems)
                )
            }
        }
    }

    fun setShowRecentlyReadItemsState(showRecentlyReadItems: Boolean) {
        screenModelScope.launch {
            preferences.showRecentlyReadItems.write(showRecentlyReadItems)
            filters.update {
                it.copy(
                    recentlyReadItemIds = if (showRecentlyReadItems) {
                        recentlyReadItemIdsSnapshot()
                    } else {
                        emptyList()
                    }
                )
            }
        }
    }

    fun setRecentlyReadOnlyState(recentlyReadOnly: Boolean) {
        screenModelScope.launch(dispatcher) {
            val keepRecentVisible = preferences.showRecentlyReadItems.flow.first()
            filters.update {
                it.copy(
                    recentlyReadOnly = recentlyReadOnly,
                    recentlyReadItemIds = if (recentlyReadOnly || keepRecentVisible) {
                        recentlyReadItemIdsSnapshot()
                    } else {
                        emptyList()
                    }
                )
            }
        }
    }

    fun setOrderFieldState(orderField: OrderField) {
        screenModelScope.launch {
            preferences.orderField.write(orderField.name)

            _timelineState.update {
                it.copy(
                    filters = it.filters.copy(orderField = orderField)
                )
            }
        }
    }

    fun setOrderTypeState(orderType: OrderType) {
        screenModelScope.launch {
            preferences.orderType.write(orderType.name)

            _timelineState.update {
                it.copy(filters = it.filters.copy(orderType = orderType))
            }
        }
    }

    fun resetScrollToTop() {
        _timelineState.update { it.copy(scrollToTop = false) }
    }

    fun resetSyncError() {
        _timelineState.update { it.copy(syncError = null) }
    }

    fun updateLastFirstVisibleItemIndex(index: Int) {
        _listIndexState.update { index }
    }

    fun disableDisplayNotificationsPermission() {
        screenModelScope.launch {
            preferences.displayNotificationsPermission.write(false)
        }
    }

    suspend fun selectItemWithFeed(itemId: Int): ItemWithFeed? {
        val query =
            ItemSelectionQueryBuilder.buildQuery(itemId, currentAccount!!.config.useSeparateState)
        return database.itemDao().selectItemById(query).firstOrNull()
    }

    fun updateOpenInParameter(feedId: Int, openIn: OpenIn, openInAsk: Boolean) {
        screenModelScope.launch(dispatcher) {
            database.feedDao().updateOpenInSetting(feedId, openIn)
            database.feedDao().updateOpenInAsk(feedId, false)
            preferences.globalOpenInAsk.write(openInAsk)
        }
    }

    fun shareItem(itemWithFeed: ItemWithFeed, context: Context) = Utils.shareItem(
        itemWithFeed, context, useCustomShareIntentTpl.value, customShareIntentTpl.value
    )
}

@Stable
data class TimelineState(
    val isRefreshing: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val currentFeed: String = "",
    val unreadNewItemsCount: Int = 0,
    val feedCount: Int = 0,
    val feedMax: Int = 0,
    val scrollToTop: Boolean = false,
    val localSyncErrors: ErrorResult? = null,
    val syncError: String? = null,
    val filters: QueryFilters = QueryFilters(),
    val filterFeedName: String = "",
    val filterTagName: String = "",
    val filterFolderName: String = "",
    val foldersAndFeeds: Map<Folder?, List<Feed>> = emptyMap(),
    val tags: List<Tag> = emptyList(),
    val itemState: Flow<PagingData<ItemWithFeed>> = emptyFlow(),
    val dialog: DialogState? = null,
    val isAccountLocal: Boolean = false,
    val hideReadAllFAB: Boolean = false,
    val preferences: TimelinePreferences = TimelinePreferences()
) {

    val showSubtitle = filters.subFilter != SubFilter.ALL

    val displayRefreshScreen = isRefreshing && isAccountLocal
}

@Stable
data class TimelinePreferences(
    val itemSize: TimelineItemSize = TimelineItemSize.LARGE,
    val showImages: Boolean = true,
    val showSource: Boolean = true,
    val showDate: Boolean = true,
    val showReadTime: Boolean = true,
    val markReadOnScroll: Boolean = false,
    val displayNotificationsPermission: Boolean = false,
    val showReadItems: Boolean = true,
    val orderField: OrderField = OrderField.DATE,
    val orderType: OrderType = OrderType.DESC,
    val theme: String = "light",
    val openInExternalBrowser: Boolean = false,
    val openInAsk: Boolean = true,
    val syncAtLaunch: Boolean = false,
    val swipeToLeft: SwipeAction = SwipeAction.READ,
    val swipeToRight: SwipeAction = SwipeAction.DISABLED,
    val showRecentlyReadItems: Boolean = false
)

private const val MAX_RECENTLY_READ_ITEMS = 100

sealed interface DialogState {
    data object ConfirmDialog : DialogState
    data object FilterSheet : DialogState
    class ErrorList(val errorResult: ErrorResult) : DialogState
    class OpenIn(val itemWithFeed: ItemWithFeed) : DialogState
}
