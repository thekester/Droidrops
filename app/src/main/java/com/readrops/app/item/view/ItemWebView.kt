package com.readrops.app.item.view

import android.annotation.SuppressLint
import android.view.ActionMode
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.text.SpannedString
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.net.Uri
import androidx.webkit.WebViewAssetLoader
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.text.HtmlCompat
import androidx.core.text.layoutDirection
import com.readrops.app.R
import com.readrops.app.util.Utils
import com.readrops.db.pojo.ItemWithFeed
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.json.JSONTokener
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class ItemWebView(
    context: Context,
    private val onUrlClick: (String) -> Unit,
    private val onImageClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    private val onLinkLongPress: (String) -> Unit,
    private val onShareSelectedText: (String) -> Unit,
    private val onContentLoaded: () -> Unit,
    attrs: AttributeSet? = null,
) : WebView(context, attrs) {

    // Serving the bundled assets over https gives the page a real web origin.
    // A file:// origin makes embedded players refuse to start: YouTube answers
    // "Error 153, video player configuration error" and nothing ever plays.
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    init {
        settings.javaScriptEnabled = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(false)
        isVerticalScrollBarEnabled = true
        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                onContentLoaded()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let(::handleUrl)
                return true
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.toString()?.let(::handleUrl)
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
        }

        setOnLongClickListener {
            val result = hitTestResult
            val type = result.type
            if (type == HitTestResult.IMAGE_TYPE || type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                result.extra?.let { onImageLongPress(it) }
                true
            } else if (type == HitTestResult.ANCHOR_TYPE || type == HitTestResult.SRC_ANCHOR_TYPE) {
                result.extra?.let { onLinkLongPress(it) }
                true
            } else {
                false
            }
        }
    }

    override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode? {
        if (type != ActionMode.TYPE_FLOATING) return super.startActionMode(callback, type)

        val wrappedCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                if (!callback.onCreateActionMode(mode, menu)) return false
                menu.add(Menu.NONE, SHARE_SELECTION_ACTION_ID, menu.size(),
                    context.getString(R.string.share_selection_with_reference))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean =
                callback.onPrepareActionMode(mode, menu)

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (item.itemId != SHARE_SELECTION_ACTION_ID) {
                    return callback.onActionItemClicked(mode, item)
                }

                evaluateJavascript("window.getSelection().toString()") { encodedText ->
                    val selectedText = runCatching {
                        JSONTokener(encodedText).nextValue() as? String
                    }.getOrNull()
                    selectedText?.takeIf(String::isNotBlank)?.let(onShareSelectedText)
                }
                mode.finish()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) = callback.onDestroyActionMode(mode)
        }

        return super.startActionMode(wrappedCallback, type)
    }

    private fun handleUrl(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme == IMAGE_VIEWER_SCHEME && uri.host == IMAGE_VIEWER_HOST) {
            val imageUrl = uri.getQueryParameter(IMAGE_VIEWER_QUERY)
            if (imageUrl?.let { Uri.parse(it).scheme in setOf("http", "https") } == true) {
                onImageClick(imageUrl)
            }
        } else {
            onUrlClick(url)
        }
    }

    fun loadText(
        itemWithFeed: ItemWithFeed,
        accentColor: Color,
        backgroundColor: Color,
        onBackgroundColor: Color,
        openVideosInYoutube: Boolean = false
    ) {
        val direction = if (Locale.getDefault().layoutDirection == LAYOUT_DIRECTION_LTR) {
            "ltr"
        } else {
            "rtl"
        }

        val string = context.getString(
            R.string.webview_html_template,
            Utils.getCssColor(accentColor.toArgb()),
            Utils.getCssColor(onBackgroundColor.toArgb()),
            Utils.getCssColor(backgroundColor.toArgb()),
            direction,
            formatText(itemWithFeed, openVideosInYoutube)
        )

        loadDataWithBaseURL(
            ASSETS_BASE_URL,
            string,
            "text/html; charset=utf-8",
            "UTF-8",
            null
        )
    }

    /**
     * Some feeds, Hacker News for instance, ship entries whose body is nothing but a link.
     * Rendering them as is gives a blank looking page, so a hint towards the real article
     * is appended. The hint is only ever added, never replacing what the feed did provide.
     */
    /**
     * The title attribute of an image is hover text, which webcomics use to carry a second
     * punchline. A touch screen has no hover, so it was simply unreachable. It is surfaced
     * as a caption under the image, styled apart from the body text.
     */
    private fun revealImageHoverText(body: Element) = body.revealImageHoverText()

    /**
     * Swaps the embedded player for a thumbnail linking to YouTube. Readers with the app
     * installed, or a Premium account, get their own player instead of the web one.
     */
    private fun replaceVideoEmbeds(body: Element) {
        body.select("iframe").forEach { iframe ->
            val videoId = YOUTUBE_EMBED.find(iframe.attr("src"))?.groupValues?.get(1)
                ?: return@forEach

            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val label = context.getString(R.string.watch_on_youtube)
            val thumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

            iframe.replaceWith(
                Jsoup.parseBodyFragment(
                    "<p><a href=\"$watchUrl\"><img src=\"$thumbnail\" alt=\"$label\"></a>" +
                            "<br><a href=\"$watchUrl\">$label</a></p>"
                ).body().child(0)
            )
        }
    }

    private fun hasNoProse(body: Element): Boolean {
        // an image or a player is content of its own: a webcomic entry is not an empty entry
        if (body.selectFirst("img, video, iframe, audio") != null) return false

        val wholeText = body.text().trim()
        if (wholeText.isEmpty()) return true

        val textInsideLinks = body.select("a").sumOf { it.text().length }
        return wholeText.length - textInsideLinks < PROSE_THRESHOLD
    }

    private fun emptyContentNotice(itemWithFeed: ItemWithFeed): String {
        val link = itemWithFeed.item.link.orEmpty()
        val notice = context.getString(R.string.feed_provides_no_content)

        return if (link.isEmpty()) {
            "<p><em>$notice</em></p>"
        } else {
            val label = context.getString(R.string.open_original_article)
            "<p><em>$notice</em><br><a href=\"$link\">$label</a></p>"
        }
    }

    private fun formatText(itemWithFeed: ItemWithFeed, openVideosInYoutube: Boolean): String {
        val text = itemWithFeed.item.text ?: return emptyContentNotice(itemWithFeed)
        val unescapedText = Parser.unescapeEntities(text, false)
        val document = if (itemWithFeed.websiteUrl != null) {
            Jsoup.parse(unescapedText, itemWithFeed.websiteUrl!!)
        } else {
            Jsoup.parse(unescapedText)
        }
        // If body has no tags or all tags are unknown (and therefore likely not HTML tags at all),
        // treat the whole thing as plain text and convert it to HTML turning newlines into <br>/<p> tags
        val body = document.body()
        revealImageHoverText(body)
        makeImagesZoomable(body)
        if (openVideosInYoutube) replaceVideoEmbeds(body)
        val isPlainText = body.stream().skip(1).allMatch { !it.tag().isKnownTag }
        val html = if (isPlainText) {
            HtmlCompat.toHtml(SpannedString(unescapedText), HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            body.select("div,span").forEach { it.clearAttributes() }
            body.html()
        }

        return if (hasNoProse(body)) html + emptyContentNotice(itemWithFeed) else html
    }

    /** Route image taps to the standalone image viewer instead of zooming the whole article. */
    private fun makeImagesZoomable(body: Element) {
        body.select("img[src]").forEach { image ->
            image.attr(
                "onclick",
                "event.preventDefault(); event.stopPropagation(); " +
                    "window.location.href='$IMAGE_VIEWER_URI?url=' + " +
                    "encodeURIComponent(this.currentSrc || this.src); return false;"
            )
            image.attr("style", (image.attr("style") + ";cursor:zoom-in").trimStart(';'))
        }
    }

    companion object {
        // characters of text outside links below which an entry is considered link-only
        private const val PROSE_THRESHOLD = 20

        // matches the path handler above, so the relative asset urls of the template resolve here
        private const val ASSETS_BASE_URL = "https://appassets.androidplatform.net/assets/"
        private const val IMAGE_VIEWER_SCHEME = "droidrops-image"
        private const val IMAGE_VIEWER_HOST = "open"
        private const val IMAGE_VIEWER_QUERY = "url"
        private const val SHARE_SELECTION_ACTION_ID = 0xD701
        private const val IMAGE_VIEWER_URI = "$IMAGE_VIEWER_SCHEME://$IMAGE_VIEWER_HOST"

        private val YOUTUBE_EMBED =
            Regex("""(?:youtube(?:-nocookie)?\.com/embed/|youtu\.be/)([A-Za-z0-9_-]{6,})""")
    }
}
