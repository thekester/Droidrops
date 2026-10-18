package com.readrops.app.item

import android.Manifest
import android.content.pm.PackageManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.readrops.app.R
import com.readrops.app.util.KeyboardShortcutHandler
import com.readrops.app.util.Shortcut
import com.readrops.app.util.components.AndroidScreen
import com.readrops.app.util.components.CenteredProgressIndicator
import com.readrops.app.util.components.Placeholder
import com.readrops.app.util.extensions.isError
import com.readrops.app.util.extensions.isLoading
import com.readrops.app.util.extensions.isNotEmpty
import com.readrops.app.util.extensions.openInCustomTab
import com.readrops.app.util.extensions.openUrl
import com.readrops.db.filters.QueryFilters
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class ItemScreen(
    private val itemId: Int,
    private val itemIndex: Int,
    private val queryFilters: QueryFilters
) : AndroidScreen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val closePane = LocalItemPaneClose.current
        val close: () -> Unit = closePane ?: {
            ReaderSession.clear(context)
            navigator.pop()
        }

        LaunchedEffect(itemId) {
            // the side pane is part of the timeline, it is not a reader to restore on launch
            if (closePane == null) ReaderSession.save(context, itemId)
        }

        BackHandler(onBack = close)

        val screenModel =
            koinScreenModel<ItemScreenModel>(parameters = { parametersOf(itemId, itemIndex, queryFilters) })
        val state by screenModel.state.collectAsStateWithLifecycle()
        val items = screenModel.itemState.collectAsLazyPagingItems()

        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val linkAccentColor = MaterialTheme.colorScheme.primary
        var imageViewerUrl by remember { mutableStateOf<String?>(null) }
        var linkDialogUrl by remember { mutableStateOf<String?>(null) }
        var pendingDownloadUrl by remember { mutableStateOf<String?>(null) }
        val downloadPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            val url = pendingDownloadUrl
            pendingDownloadUrl = null
            if (granted && url != null) {
                screenModel.downloadImage(url, context)
            } else if (url != null) {
                screenModel.reportImageDownloadError(context)
            }
        }
        val requestImageDownload: (String) -> Unit = { url ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingDownloadUrl = url
                downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                screenModel.downloadImage(url, context)
            }
        }

        imageViewerUrl?.let { url ->
            ItemImageViewerDialog(
                imageUrl = url,
                onDismiss = { imageViewerUrl = null }
            )
        }

        linkDialogUrl?.let { url ->
            ItemLinkDialog(
                url = url,
                onChoice = { choice ->
                    when (choice) {
                        ItemLinkChoice.OPEN -> {
                            if (state.openInExternalBrowser) context.openUrl(url)
                            else context.openInCustomTab(url, state.theme, linkAccentColor)
                        }
                        ItemLinkChoice.COPY -> {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.link_copied))
                            }
                        }
                        ItemLinkChoice.SHARE -> {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_url)))
                        }
                    }
                    linkDialogUrl = null
                },
                onDismiss = { linkDialogUrl = null }
            )
        }

        if (state.imageDialogUrl != null) {
            ItemImageDialog(
                onChoice = {
                    if (it == ItemImageChoice.SHARE) {
                        screenModel.shareImage(state.imageDialogUrl!!, context)
                    } else {
                        requestImageDownload(state.imageDialogUrl!!)
                    }

                    screenModel.closeImageDialog()
                },
                onDismiss = { screenModel.closeImageDialog() }
            )
        }

        LaunchedEffect(state.fileDownloadedEvent) {
            if (state.fileDownloadedEvent) {
                snackbarHostState.showSnackbar(context.getString(R.string.downloaded_file))
                screenModel.consumeFileDownloadedEvent()
            }
        }

        LaunchedEffect(state.error) {
            if (state.error != null) {
                snackbarHostState.showSnackbar(state.error!!)
            }
        }

        when {
            items.isLoading() -> {
                CenteredProgressIndicator()
            }

            items.isError() -> {
                Placeholder(
                    text = stringResource(R.string.error_occured),
                    painter = painterResource(id = R.drawable.ic_error)
                )
            }

            else -> {
                val pagerState = rememberPagerState(
                    initialPage = state.initialPage.coerceAtLeast(0),
                    pageCount = { items.itemCount }
                )

                LaunchedEffect(pagerState.currentPage) {
                    snapshotFlow { pagerState.currentPage }
                        .distinctUntilChanged { old, new -> old == new }
                        .collect { pageIndex ->
                            if (items.isNotEmpty()) {
                                items[pageIndex]?.let {
                                    screenModel.setItemRead(it)
                                }
                            }
                        }
                }

                KeyboardShortcutHandler { shortcut ->
                    val page = pagerState.currentPage
                    val itemWithFeed = (if (page < items.itemCount) items.peek(page) else null)
                        ?: return@KeyboardShortcutHandler false
                    val item = itemWithFeed.item

                    when (shortcut) {
                        // in the side pane there is a single article: the timeline moves instead
                        Shortcut.NEXT_ARTICLE -> (page + 1 < pagerState.pageCount).also { canMove ->
                            if (canMove) coroutineScope.launch { pagerState.animateScrollToPage(page + 1) }
                        }

                        Shortcut.PREVIOUS_ARTICLE -> (page > 0).also { canMove ->
                            if (canMove) coroutineScope.launch { pagerState.animateScrollToPage(page - 1) }
                        }

                        Shortcut.TOGGLE_READ -> true.also { screenModel.setItemReadState(item) }
                        Shortcut.TOGGLE_STAR -> true.also { screenModel.setItemStarState(item) }
                        Shortcut.FULL_ARTICLE -> (item.link != null).also { hasLink ->
                            if (hasLink) screenModel.toggleFullContent(item, context)
                        }

                        Shortcut.OPEN_IN_BROWSER -> (item.link != null).also { hasLink ->
                            if (hasLink) {
                                if (state.openInExternalBrowser) {
                                    context.openUrl(item.link!!)
                                } else {
                                    context.openInCustomTab(item.link!!, state.theme, linkAccentColor)
                                }
                            }
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = state.swipeBetweenArticles,
                    beyondViewportPageCount = 2,
                    key = items.itemKey { it.item.id }
                ) { page ->
                    val itemWithFeed = items[page]

                    if (itemWithFeed != null) {
                        val accentColor = if (itemWithFeed.color != 0) {
                            Color(itemWithFeed.color)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }

                        val item = itemWithFeed.item

                        val alwaysFullContent = itemWithFeed.feedId in state.fullContentFeedIds
                        LaunchedEffect(item.id, alwaysFullContent) {
                            if (alwaysFullContent) screenModel.loadFullContentAutomatically(item, context)
                        }

                        ItemScreenPage(
                            itemWithFeed = itemWithFeed,
                            snackbarHostState = snackbarHostState,
                            onOpenUrl = { url ->
                                if (state.openInExternalBrowser) {
                                    context.openUrl(url)
                                } else {
                                    context.openInCustomTab(url, state.theme, accentColor)
                                }
                            },
                            openVideosInYoutube = state.openVideosInYoutube,
                            articleFontSize = state.articleFontSize,
                            onShareItem = { screenModel.shareItem(itemWithFeed, context) },
                            onShareSelectedText = { selectedText ->
                                com.readrops.app.util.Utils.shareSelectionWithReference(
                                    selectedText,
                                    itemWithFeed,
                                    context
                                )
                            },
                            onSetReadState = { screenModel.setItemReadState(item) },
                            onSetStarState = { screenModel.setItemStarState(item) },
                            fullContent = state.fullContents[item.id],
                            onToggleFullContent = { screenModel.toggleFullContent(item, context) },
                            onOpenImageViewer = { imageViewerUrl = it },
                            onOpenImageDialog = { screenModel.openImageDialog(it) },
                            onOpenLinkDialog = { linkDialogUrl = it },
                            onPop = close,
                        )
                    }
                }
            }
        }
    }
}
