package com.readrops.app.item.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.RelativeLayout
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView

@SuppressLint("ResourceType", "ViewConstructor")
class ItemNestedScrollView(
    context: Context,
    private val articleId: Int,
    useBackgroundTitle: Boolean,
    onGlobalLayoutListener: (viewHeight: Int, contentHeight: Int) -> Unit,
    onUrlClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    onLinkLongPress: (String) -> Unit,
    onShareSelectedText: (String) -> Unit,
    composeViewContent: @Composable () -> Unit
) : NestedScrollView(context) {

    private val scrollPositions = ArticleScrollPositions(context)
    private val scrollSaveHandler = Handler(Looper.getMainLooper())
    private var restorePending = true
    private var restoreAttempts = 0
    private val saveScrollPosition = Runnable {
        scrollPositions.save(articleId, scrollY)
    }
    private val restoreScrollPosition = object : Runnable {
        override fun run() {
            val target = scrollPositions.get(articleId)
            val maxScrollY = (getChildAt(0).height - height).coerceAtLeast(0)

            if (maxScrollY >= target || restoreAttempts >= MAX_RESTORE_ATTEMPTS) {
                scrollTo(0, target.coerceAtMost(maxScrollY))
                restorePending = false
            } else {
                restoreAttempts++
                scrollSaveHandler.postDelayed(this, RESTORE_RETRY_DELAY_MS)
            }
        }
    }

    init {
        addView(
            RelativeLayout(context).apply {
                ViewCompat.setNestedScrollingEnabled(this, true)
                descendantFocusability = FOCUS_BLOCK_DESCENDANTS

                val composeView = ComposeView(context).apply {
                    id = 1

                    setContent {
                        composeViewContent()
                    }
                }

                val composeViewParams = RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                composeViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                composeView.layoutParams = composeViewParams

                val webView = ItemWebView(
                    context = context,
                    onUrlClick = onUrlClick,
                    onImageClick = onImageClick,
                    onImageLongPress = onImageLongPress,
                    onLinkLongPress = onLinkLongPress,
                    onShareSelectedText = onShareSelectedText,
                    onContentLoaded = { restoreArticlePosition() }
                ).apply {
                    id = 2
                    ViewCompat.setNestedScrollingEnabled(this, true)
                }

                val webViewParams = RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )

                webViewParams.addRule(RelativeLayout.BELOW, composeView.id)
                webView.layoutParams = webViewParams

                if (useBackgroundTitle) {
                    val density = resources.displayMetrics.density
                    val dpAsPixels = (8 * density + 0.5f).toInt()
                    composeView.setPadding(0, 0, 0, dpAsPixels)
                }

                addView(composeView)
                addView(webView)
            }
        )

        viewTreeObserver.addOnGlobalLayoutListener {
            val viewHeight = this.measuredHeight
            val contentHeight = getChildAt(0).height

            onGlobalLayoutListener(viewHeight, contentHeight)
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (!restorePending) {
            scrollSaveHandler.removeCallbacks(saveScrollPosition)
            scrollSaveHandler.postDelayed(saveScrollPosition, SCROLL_SAVE_DELAY_MS)
        }
    }

    override fun onDetachedFromWindow() {
        scrollSaveHandler.removeCallbacks(saveScrollPosition)
        scrollSaveHandler.removeCallbacks(restoreScrollPosition)
        if (!restorePending) {
            scrollPositions.save(articleId, scrollY)
        }
        super.onDetachedFromWindow()
    }

    private fun restoreArticlePosition() {
        if (!restorePending) return
        scrollSaveHandler.removeCallbacks(restoreScrollPosition)
        scrollSaveHandler.post(restoreScrollPosition)
    }

    private companion object {
        const val SCROLL_SAVE_DELAY_MS = 250L
        const val RESTORE_RETRY_DELAY_MS = 100L
        const val MAX_RESTORE_ATTEMPTS = 50
    }
}
