package com.readrops.app.timelime

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.LoadState
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.readrops.app.MainActivity
import com.readrops.app.R
import com.readrops.app.home.HomeScreen
import com.readrops.app.item.ItemPane
import com.readrops.app.item.ItemScreen
import com.readrops.app.item.TwoPaneContainer
import com.readrops.app.item.TwoPaneLayout
import com.readrops.app.item.twoPaneLayout
import com.readrops.app.more.diagnostics.DiagnosticLogScreen
import com.readrops.app.timelime.components.TimelineAppBar
import com.readrops.app.timelime.components.TimelineItem
import com.readrops.app.timelime.withoutSelectedFolderLabel
import com.readrops.app.timelime.components.TimelineItemSize
import com.readrops.app.timelime.dialog.TimelineDialogs
import com.readrops.app.timelime.drawer.TimelineDrawer
import com.readrops.app.util.KeyboardShortcutFocusAnchor
import com.readrops.app.util.KeyboardShortcutHandler
import com.readrops.app.util.Shortcut
import com.readrops.app.util.components.LoadingScreen
import com.readrops.app.util.components.Placeholder
import com.readrops.app.util.components.RefreshBanner
import com.readrops.app.util.components.RefreshScreen
import com.readrops.app.util.extensions.isError
import com.readrops.app.util.extensions.isLoading
import com.readrops.app.util.extensions.isNotEmpty
import com.readrops.app.util.extensions.isTabletUi
import com.readrops.app.util.extensions.MouseWheelRefreshDetector
import com.readrops.app.util.extensions.mouseWheelPullToRefresh
import com.readrops.app.util.extensions.openInCustomTab
import com.readrops.app.util.extensions.openUrl
import com.readrops.app.util.theme.spacing
import com.readrops.db.entities.OpenIn
import com.readrops.db.filters.MainFilter
import com.readrops.db.pojo.ItemWithFeed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.readrops.db.filters.SubFilter


object TimelineTab : Tab {

    private val openItemChannel = Channel<Int>()

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1u,
            title = stringResource(id = R.string.timeline),
        )

    @SuppressLint("InlinedApi")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val isTablet = context.isTabletUi()
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val usePermanentDrawer = isTablet && isLandscape
        val useLandscapeDrawerOverlay = !isTablet && isLandscape

        val screenModel = koinScreenModel<TimelineScreenModel>()
        val state by screenModel.timelineState.collectAsStateWithLifecycle()
        var tabletDrawerOpen by rememberSaveable { mutableStateOf(true) }
        val preferences = state.preferences
        val mouseWheelRefreshDetector = remember { MouseWheelRefreshDetector() }
        val items = state.itemState.collectAsLazyPagingItems()
        val configuration = LocalConfiguration.current
        val paneLayout = twoPaneLayout(
            enabled = preferences.twoPaneReader,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            smallestScreenWidthDp = configuration.smallestScreenWidthDp
        )
        val useTwoPane = paneLayout != TwoPaneLayout.NONE
        var paneItemId by rememberSaveable { mutableStateOf<Int?>(null) }

        // the window became too narrow (rotation, split screen) or the option was turned off:
        // keep the article open as a regular reader screen instead of dropping it
        LaunchedEffect(useTwoPane) {
            val itemId = paneItemId
            if (!useTwoPane && itemId != null) {
                paneItemId = null
                navigator.push(ItemScreen(itemId, -1, state.filters))
            }
        }

        val lazyListState = rememberLazyListState()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val topAppBarState = rememberTopAppBarState()
        val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)

        LaunchedEffect(state.filters) {
            val filters = state.filters
            if (!filters.isDefaultTimelinePositionScope()) {
                lazyListState.scrollToItem(0)
            }
        }

        LaunchedEffect(state.filters, items.loadState.refresh) {
            val filters = state.filters
            if (!filters.isDefaultTimelinePositionScope() ||
                items.loadState.refresh !is LoadState.NotLoading || items.itemCount == 0
            ) return@LaunchedEffect

            val positionStore = TimelineScrollPosition(context, filters.accountId)
            val savedPosition = positionStore.read()
            lazyListState.scrollToItem(savedPosition.itemIndex, savedPosition.itemOffset)
            snapshotFlow {
                lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
            }.distinctUntilChanged().collect { (index, offset) ->
                positionStore.write(index, offset)
            }
        }

        // Keep the article open in the side pane in view, e.g. after J/K. Declared after the
        // position restore above so that, when a reload triggers both, this one runs last.
        LaunchedEffect(paneItemId, state.filters, items.loadState.refresh) {
            val openId = paneItemId
            if (!useTwoPane || openId == null || items.loadState.refresh !is LoadState.NotLoading) {
                return@LaunchedEffect
            }

            val index = (0 until items.itemCount).firstOrNull { items.peek(it)?.item?.id == openId }
                ?: return@LaunchedEffect
            val layout = lazyListState.layoutInfo
            val fullyVisible = layout.visibleItemsInfo.any {
                it.index == index && it.offset >= layout.viewportStartOffset &&
                    it.offset + it.size <= layout.viewportEndOffset
            }
            if (!fullyVisible) lazyListState.animateScrollToItem(index)
        }

        val lazyColumnPadding = if (preferences.itemSize == TimelineItemSize.COMPACT) {
            0.dp
        } else {
            MaterialTheme.spacing.shortSpacing
        }

        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
                screenModel.disableDisplayNotificationsPermission()
            }

        fun openItem(
            itemWithFeed: ItemWithFeed,
            itemIndex: Int,
            openIn: OpenIn? = itemWithFeed.openIn
        ) {
            val item = itemWithFeed.item
            screenModel.trackRecentlyReadItem(item.id)
            when (resolveTimelineOpenAction(openIn, item.link != null, preferences.openInExternalBrowser)) {
                TimelineOpenAction.LOCAL_ARTICLE -> if (useTwoPane) {
                    paneItemId = item.id
                } else {
                    navigator.push(ItemScreen(item.id, itemIndex, state.filters))
                }

                TimelineOpenAction.EXTERNAL_BROWSER -> item.link?.let { url ->
                    screenModel.setItemRead(item)
                    context.openUrl(url)
                }

                TimelineOpenAction.CUSTOM_TAB -> item.link?.let { url ->
                    screenModel.setItemRead(item)
                    context.openInCustomTab(url, preferences.theme, Color(itemWithFeed.color))
                }
            }
        }

        KeyboardShortcutFocusAnchor()

        // with the side pane, J/K walk the timeline itself; the pane's reader handles the rest
        KeyboardShortcutHandler(enabled = useTwoPane) { shortcut ->
            val step = when (shortcut) {
                Shortcut.NEXT_ARTICLE -> 1
                Shortcut.PREVIOUS_ARTICLE -> -1
                else -> return@KeyboardShortcutHandler false
            }

            val count = items.itemCount
            // items not loaded yet peek as null: never match them against "no open article"
            val current = paneItemId?.let { openId ->
                (0 until count).firstOrNull { items.peek(it)?.item?.id == openId }
            } ?: -1
            val target = if (current == -1) 0 else current + step
            if (target !in 0 until count) return@KeyboardShortcutHandler false
            val itemWithFeed = items[target] ?: return@KeyboardShortcutHandler false

            // the list follows through the "keep in view" effect, once its reload settled
            openItem(itemWithFeed, target)
            true
        }

        LaunchedEffect(Unit) {
            openItemChannel.receiveAsFlow()
                .collect { itemId ->
                    screenModel.selectItemWithFeed(itemId)
                        ?.let {
                            openItem(
                                itemWithFeed = it,
                                itemIndex = items.itemSnapshotList
                                    .indexOfFirst { itemWithFeed -> itemWithFeed?.item?.id == itemId },
                            )
                        }
                }
        }

        LaunchedEffect(preferences.displayNotificationsPermission) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && preferences.displayNotificationsPermission
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        LaunchedEffect(state.scrollToTop) {
            if (state.scrollToTop) {
                val filters = state.filters
                if (filters.isDefaultTimelinePositionScope()) {
                    val savedPosition = TimelineScrollPosition(context, filters.accountId).read()
                    lazyListState.scrollToItem(savedPosition.itemIndex, savedPosition.itemOffset)
                } else {
                    lazyListState.scrollToItem(0)
                }
                screenModel.resetScrollToTop()
                topAppBarState.contentOffset = 0f
            }
        }

        // remove splash screen when opening the app
        LaunchedEffect(items.isLoading(), preferences.syncAtLaunch) {
            val activity = (context as MainActivity)

            if (preferences.syncAtLaunch) {
                activity.ready = true
            } else {
                if (!items.isLoading() && !activity.ready) {
                    activity.ready = true
                }
            }
        }

        val drawerState = key(context.resources.configuration.orientation) {
            rememberDrawerState(
                initialValue = DrawerValue.Closed,
                confirmStateChange = {
                    if (it == DrawerValue.Closed) {
                        screenModel.closeDrawer()
                    } else {
                        screenModel.openDrawer()
                    }

                    true
                }
            )
        }

        BackHandler(
            enabled = state.isDrawerOpen,
            onBack = { screenModel.closeDrawer() }
        )

        LaunchedEffect(state.isDrawerOpen, usePermanentDrawer, useLandscapeDrawerOverlay) {
            // Tablets use a permanent drawer, which may not be composed while closed. A
            // DrawerState only gets its density from ModalNavigationDrawer, so don't animate it
            // when the modal drawer isn't part of the composition.
            if (usePermanentDrawer || useLandscapeDrawerOverlay) return@LaunchedEffect

            if (state.isDrawerOpen) {
                drawerState.open()
            } else {
                drawerState.close()
            }
        }

        LaunchedEffect(
            context.resources.configuration.orientation,
            usePermanentDrawer,
            useLandscapeDrawerOverlay
        ) {
            // DrawerState only receives its density while attached to a Material drawer.
            // Permanent tablet navigation and the landscape overlay do not compose
            // ModalNavigationDrawer, so calling snapTo() in either layout can crash with
            // "The density on DrawerState was not set" during an orientation change.
            if (usePermanentDrawer || useLandscapeDrawerOverlay) {
                screenModel.closeDrawer()
                tabletDrawerOpen = usePermanentDrawer
                return@LaunchedEffect
            }

            // A modal drawer can retain an intermediate offset when the phone rotates. Snap it
            // shut after its new layout anchors are placed so the old-width drawer cannot remain
            // as a clipped strip over the timeline.
            withFrameNanos { }
            drawerState.snapTo(DrawerValue.Closed)
            withFrameNanos { }
            drawerState.snapTo(DrawerValue.Closed)
            screenModel.closeDrawer()
            tabletDrawerOpen = usePermanentDrawer
        }

        LaunchedEffect(state.localSyncErrors) {
            if (state.localSyncErrors != null) {
                val action = snackbarHostState.showSnackbar(
                    message = context.resources.getQuantityString(
                        R.plurals.error_occurred,
                        state.localSyncErrors!!.size
                    ),
                    actionLabel = context.getString(R.string.details),
                    duration = SnackbarDuration.Short
                )

                if (action == SnackbarResult.ActionPerformed) {
                    screenModel.openDialog(DialogState.ErrorList(state.localSyncErrors!!))
                } else {
                    // remove errors from state
                    screenModel.closeDialog(DialogState.ErrorList(state.localSyncErrors!!))
                }
            }
        }

        LaunchedEffect(state.syncError) {
            if (state.syncError != null) {
                val action = snackbarHostState.showSnackbar(
                    message = state.syncError!!,
                    actionLabel = context.getString(R.string.view_log),
                    duration = SnackbarDuration.Long
                )
                screenModel.resetSyncError()

                if (action == SnackbarResult.ActionPerformed) {
                    navigator.push(DiagnosticLogScreen())
                }
            }
        }

        TimelineDialogs(
            state = state,
            screenModel = screenModel,
            onOpenItem = { itemWithFeed, openIn ->
                openItem(
                    itemWithFeed = itemWithFeed,
                    itemIndex = items.itemSnapshotList.indexOfFirst { it?.item?.id == itemWithFeed.item.id },
                    openIn = openIn,
                )
            }
        )

        TimelineDrawer(
            state = state,
            drawerState = drawerState,
            usePermanentDrawer = usePermanentDrawer,
            useLandscapeOverlay = useLandscapeDrawerOverlay,
            tabletDrawerOpen = tabletDrawerOpen,
            onToggleTabletDrawer = { tabletDrawerOpen = !tabletDrawerOpen },
            onCloseDrawer = { screenModel.closeDrawer() },
            onClickDefaultItem = { screenModel.updateDrawerDefaultItem(it) },
            onFolderClick = { screenModel.updateDrawerFolderSelection(it) },
            onFeedClick = { screenModel.updateDrawerFeedSelection(it) },
            onTagClick = { screenModel.updateDrawerTagSelection(it) }
        ) {
            TwoPaneContainer(
                layout = paneLayout,
                pane = { paneModifier ->
                    ItemPane(
                        itemId = paneItemId,
                        queryFilters = state.filters,
                        onClose = { paneItemId = null },
                        modifier = paneModifier
                    )
                }
            ) { listModifier ->
                Scaffold(
                    modifier = listModifier,
                    topBar = {
                        TimelineAppBar(
                            state = state,
                            topAppBarScrollBehavior = topAppBarScrollBehavior,
                            onOpenDrawer = { screenModel.openDrawer() },
                            usePermanentDrawer = usePermanentDrawer,
                            tabletDrawerOpen = tabletDrawerOpen,
                            onToggleTabletDrawer = { tabletDrawerOpen = !tabletDrawerOpen },
                            onOpenFilterSheet = { screenModel.openDialog(DialogState.FilterSheet) },
                            onRefreshTimeline = { screenModel.refreshTimeline() },
                            onSearchQueryChange = screenModel::updateSearchQuery
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        // marking everything read is scoped to the active filter, and no per tag
                        // variant exists yet. Rather than silently emptying the whole account, the
                        // action is withdrawn while a tag filter is on.
                        if (!state.hideReadAllFAB && state.filters.subFilter != SubFilter.TAG) {
                            FloatingActionButton(
                                onClick = {
                                    if (state.filters.mainFilter == MainFilter.ALL) {
                                        screenModel.openDialog(DialogState.ConfirmDialog)
                                    } else {
                                        screenModel.setAllItemsRead()
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_done_all),
                                    contentDescription = stringResource(R.string.mark_all_articles_read)
                                )
                            }
                        }
                    },
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    ) {
                        // PullToRefreshBox must stay in composition whatever the state: leaving it
                        // while its indicator animation runs detaches the node, and animateToThreshold
                        // then crashes reading a CompositionLocal.
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { screenModel.refreshTimeline() },
                            modifier = Modifier.mouseWheelPullToRefresh(
                                canScrollBackward = lazyListState.canScrollBackward,
                                isRefreshing = state.isRefreshing,
                                detector = mouseWheelRefreshDetector,
                                onRefresh = { screenModel.refreshTimeline() }
                            )
                        ) {
                            when {
                                state.displayRefreshScreen && items.itemCount == 0 -> RefreshScreen(
                                    currentFeed = state.currentFeed,
                                    feedCount = state.feedCount,
                                    feedMax = state.feedMax
                                )

                                items.isLoading() -> {
                                    LoadingScreen(isRefreshing = state.isRefreshing)
                                }

                                items.isError() -> {
                                    Placeholder(
                                        text = stringResource(R.string.error_occured),
                                        painter = painterResource(id = R.drawable.ic_error)
                                    )
                                }

                                else -> {
                                    if (items.isNotEmpty()) {
                                        MarkItemsRead(
                                            lazyListState = lazyListState,
                                            items = items,
                                            markReadOnScroll = preferences.markReadOnScroll,
                                            screenModel = screenModel
                                        )

                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(top = if (state.displayRefreshScreen) 56.dp else 0.dp),
                                            state = lazyListState,
                                            contentPadding = PaddingValues(vertical = lazyColumnPadding),
                                            verticalArrangement = Arrangement.spacedBy(lazyColumnPadding)
                                        ) {
                                            items(
                                                count = items.itemCount,
                                                key = items.itemKey { it.item.id },
                                            ) { index ->
                                                val itemWithFeed = items[index]

                                                if (itemWithFeed != null) {
                                                    val itemForDisplay = itemWithFeed
                                                        .withoutSelectedFolderLabel(state.filterFolderName)

                                                    TimelineItem(
                                                        itemWithFeed = itemForDisplay,
                                                        swipeToLeft = state.preferences.swipeToLeft,
                                                        swipeToRight = state.preferences.swipeToRight,
                                                        onClick = {
                                                            if (itemWithFeed.openInAsk && preferences.openInAsk) {
                                                                screenModel.openDialog(
                                                                    DialogState.OpenIn(
                                                                        itemWithFeed
                                                                    )
                                                                )
                                                            } else {
                                                                openItem(
                                                                    itemWithFeed = itemWithFeed,
                                                                    itemIndex = index,
                                                                )
                                                            }
                                                        },
                                                        onFavorite = {
                                                            val wasStarred =
                                                                itemWithFeed.item.isStarred

                                                            screenModel.setStarState(
                                                                itemWithFeed.item,
                                                                isStarred = !wasStarred
                                                            )

                                                            // only the removal is offered an undo:
                                                            // adding a favourite is visible and
                                                            // trivially reversible, losing one is not
                                                            if (wasStarred) {
                                                                coroutineScope.launch {
                                                                    val action =
                                                                        snackbarHostState.showSnackbar(
                                                                            message = context.getString(
                                                                                R.string.removed_from_favorites
                                                                            ),
                                                                            actionLabel = context.getString(
                                                                                R.string.undo
                                                                            ),
                                                                            duration = SnackbarDuration.Short
                                                                        )

                                                                    if (action == SnackbarResult.ActionPerformed) {
                                                                        screenModel.setStarState(
                                                                            itemWithFeed.item,
                                                                            isStarred = wasStarred
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onShare = {
                                                            screenModel.shareItem(itemWithFeed, context)
                                                        },
                                                        onSetReadState = {
                                                            val wasRead = itemWithFeed.item.isRead
                                                            screenModel.updateItemReadState(itemWithFeed.item)

                                                            if (!wasRead) {
                                                                coroutineScope.launch {
                                                                    val action = snackbarHostState.showSnackbar(
                                                                        message = context.getString(R.string.marked_as_read),
                                                                        actionLabel = context.getString(R.string.undo),
                                                                        duration = SnackbarDuration.Short
                                                                    )

                                                                    if (action == SnackbarResult.ActionPerformed) {
                                                                        screenModel.setItemReadState(
                                                                            itemWithFeed.item,
                                                                            isRead = false
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        size = preferences.itemSize,
                                                        showImages = preferences.showImages,
                                                        showSource = preferences.showSource,
                                                        showDate = preferences.showDate,
                                                        showReadTime = preferences.showReadTime,
                                                        showPreview = preferences.showPreview,
                                                        isSelected = useTwoPane &&
                                                            itemWithFeed.item.id == paneItemId,
                                                        modifier = Modifier.animateItem()
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Empty lazyColumn to let the pull to refresh be usable
                                        // when no items are displayed
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize()
                                        ) {}

                                        when {
                                            state.filters.searchQuery.isNotBlank() -> Placeholder(
                                                text = stringResource(R.string.no_search_results),
                                                painter = painterResource(R.drawable.ic_timeline)
                                            )

                                            state.filters.mainFilter == MainFilter.ALL &&
                                                    state.filters.subFilter == SubFilter.ALL -> EmptyTimeline(
                                                onAddFeed = {
                                                    coroutineScope.launch {
                                                        HomeScreen.openAddFeedDialog("")
                                                    }
                                                }
                                            )

                                            else -> Placeholder(
                                                text = stringResource(R.string.no_article),
                                                painter = painterResource(R.drawable.ic_timeline),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.displayRefreshScreen && items.itemCount > 0) {
                            RefreshBanner(
                                currentFeed = state.currentFeed,
                                feedCount = state.feedCount,
                                feedMax = state.feedMax,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                            )
                        }

                        val totalItems = lazyListState.layoutInfo.totalItemsCount
                        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo.size
                        val scrollbarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        if (lazyListState.isScrollInProgress && totalItems > visibleItems && visibleItems > 0) {
                            Canvas(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(vertical = 12.dp)
                                    .width(4.dp)
                                    .fillMaxHeight()
                            ) {
                                val thumbHeight = (size.height * visibleItems / totalItems)
                                    .coerceAtLeast(24.dp.toPx())
                                    .coerceAtMost(size.height)
                                val maxScrollIndex = (totalItems - visibleItems).coerceAtLeast(1)
                                val progress = (lazyListState.firstVisibleItemIndex.toFloat() / maxScrollIndex)
                                    .coerceIn(0f, 1f)
                                val top = (size.height - thumbHeight) * progress
                                drawRoundRect(
                                    color = scrollbarColor,
                                    topLeft = Offset(size.width - 3.dp.toPx(), top),
                                    size = Size(3.dp.toPx(), thumbHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyTimeline(onAddFeed: () -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.largeSpacing),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_timeline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.shortSpacing)
            )
            Text(
                text = stringResource(R.string.empty_timeline_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.empty_timeline_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.mediumSpacing)
            )
            Button(onClick = onAddFeed) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.add_feed),
                    modifier = Modifier.padding(start = MaterialTheme.spacing.shortSpacing)
                )
            }
        }
    }

    @Composable
    private fun MarkItemsRead(
        lazyListState: LazyListState,
        items: LazyPagingItems<ItemWithFeed>,
        markReadOnScroll: Boolean,
        screenModel: TimelineScreenModel
    ) {
        val lastFirstVisibleItemIndex by screenModel.listIndexState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            snapshotFlow { lazyListState.firstVisibleItemIndex }
                .filter {
                    if (it < lastFirstVisibleItemIndex) {
                        screenModel.updateLastFirstVisibleItemIndex(it)
                    }

                    it > lastFirstVisibleItemIndex
                }
                .collect { newLastFirstVisibleItemIndex ->
                    if (newLastFirstVisibleItemIndex - lastFirstVisibleItemIndex > 1) {
                        val difference = newLastFirstVisibleItemIndex - lastFirstVisibleItemIndex

                        for (subCount in 0 until difference) {
                            val item = items[lastFirstVisibleItemIndex + subCount]?.item

                            if (item != null && !item.isRead && markReadOnScroll) {
                                screenModel.setItemRead(item)
                            }
                        }
                    } else {
                        val item = items[lastFirstVisibleItemIndex]?.item

                        if (item != null && !item.isRead && markReadOnScroll) {
                            screenModel.setItemRead(item)
                        }
                    }

                    screenModel.updateLastFirstVisibleItemIndex(newLastFirstVisibleItemIndex)
                }
        }
    }

    suspend fun openItem(itemId: Int) {
        openItemChannel.send(itemId)
    }
}
