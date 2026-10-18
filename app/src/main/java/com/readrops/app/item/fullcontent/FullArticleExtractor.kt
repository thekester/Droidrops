package com.readrops.app.item.fullcontent

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Extracts the main article body from a full web page, in the spirit of Readability.
 *
 * Paragraph text is scored and credited to its parent and grandparent; the best scored
 * container, penalised by its link density, is kept. Page chrome (navigation, comments,
 * share bars, ads) is removed first. The result is plain article HTML with absolute URLs,
 * ready to be rendered by the reader like a feed entry, or null when no convincing body
 * is found, so that callers can keep showing the original entry.
 */
object FullArticleExtractor {

    fun extract(html: String, baseUrl: String): String? =
        extract(Jsoup.parse(html, baseUrl))

    fun extract(document: Document): String? {
        val body = document.body()

        resolveLazyImages(body)
        body.select(UNWANTED_TAGS).remove()
        body.select(UNWANTED_ROLES).remove()
        body.getAllElements()
            .filter { it !== body && it.isUnlikelyCandidate() }
            .forEach { it.remove() }

        val candidate = findBestCandidate(body) ?: return null
        clean(candidate)

        if (candidate.text().length < MIN_ARTICLE_LENGTH) return null

        candidate.select("[href]").forEach { it.attr("href", it.absUrl("href")) }
        candidate.select("[src]").forEach { it.attr("src", it.absUrl("src")) }
        candidate.select("[srcset]").removeAttr("srcset")

        return candidate.html().trim().takeIf { it.isNotEmpty() }
    }

    private fun findBestCandidate(body: Element): Element? {
        val scores = HashMap<Element, Double>()

        body.select("p, pre, td, blockquote").forEach { paragraph ->
            val text = paragraph.text()
            if (text.length < MIN_PARAGRAPH_LENGTH) return@forEach

            val score = 1.0 + text.count { it == ',' } + minOf(text.length / 100.0, 3.0)
            val parent = paragraph.parent() ?: return@forEach

            scores[parent] = (scores[parent] ?: parent.initialScore()) + score
            parent.parent()?.let { grandParent ->
                scores[grandParent] = (scores[grandParent] ?: grandParent.initialScore()) + score / 2
            }
        }

        return scores
            .map { (element, score) -> element to score * (1 - element.linkDensity()) }
            .maxByOrNull { it.second }
            ?.first
            ?.let { best ->
                // a container holding several sibling candidates is the real article body
                val parent = best.parent()
                if (parent != null && parent !== body && parent.tagName() == "article") parent else best
            }
    }

    private fun clean(article: Element) {
        article.select("[style]").removeAttr("style")
        article.select("[class]").removeAttr("class")
        article.select("[id]").removeAttr("id")

        // link lists (related articles, tags, menus) carry little text of their own
        article.select("ul, ol, div, section, table").forEach { element ->
            val textLength = element.text().length
            if (element.select("img, video, pre").isEmpty() &&
                (textLength < MIN_PARAGRAPH_LENGTH || element.linkDensity() > MAX_LINK_DENSITY)
            ) {
                element.remove()
            }
        }

        article.select("p, div, span, section").forEach { element ->
            if (element.text().isBlank() && element.select("img, video, iframe, br").isEmpty()) {
                element.remove()
            }
        }
    }

    private fun resolveLazyImages(body: Element) {
        body.select("img").forEach { image ->
            val lazySource = LAZY_SOURCE_ATTRIBUTES
                .map(image::attr)
                .firstOrNull { it.isNotBlank() }

            if (lazySource != null && (image.attr("src").isBlank() || image.attr("src").startsWith("data:"))) {
                image.attr("src", lazySource)
            }
        }
    }

    private fun Element.matchWeight(): Int {
        val names = "${className()} ${id()}"
        var weight = 0
        if (POSITIVE.containsMatchIn(names)) weight += 25
        if (NEGATIVE.containsMatchIn(names)) weight -= 25
        return weight
    }

    private fun Element.initialScore(): Double = matchWeight() + when (tagName()) {
        "article" -> 10.0
        "main" -> 8.0
        "div", "section" -> 5.0
        "pre", "td", "blockquote" -> 3.0
        "form", "ul", "ol", "dl", "li" -> -3.0
        "h1", "h2", "h3", "h4", "h5", "h6", "th" -> -5.0
        else -> 0.0
    }

    private fun Element.isUnlikelyCandidate(): Boolean {
        if (tagName() in setOf("body", "article", "main", "a")) return false
        val names = "${className()} ${id()}"
        return UNLIKELY.containsMatchIn(names) && !MAYBE_CANDIDATE.containsMatchIn(names)
    }

    private fun Element.linkDensity(): Double {
        val textLength = text().length
        if (textLength == 0) return 0.0
        val linkLength = select("a").sumOf { it.text().length }
        return linkLength.toDouble() / textLength
    }

    internal const val MIN_ARTICLE_LENGTH = 250
    private const val MIN_PARAGRAPH_LENGTH = 25
    private const val MAX_LINK_DENSITY = 0.5

    private const val UNWANTED_TAGS =
        "script, style, noscript, template, form, button, input, select, textarea, nav, header, " +
            "footer, aside, svg, canvas, object, embed, iframe, dialog, link, meta"
    private const val UNWANTED_ROLES =
        "[role=navigation], [role=banner], [role=contentinfo], [role=complementary], " +
            "[role=dialog], [role=alert], [aria-hidden=true], [hidden]"

    private val LAZY_SOURCE_ATTRIBUTES = listOf("data-src", "data-original", "data-lazy-src", "data-url")

    private val UNLIKELY = Regex(
        "banner|breadcrumb|combx|comment|community|cookie|disqus|extra|foot|header|legends|menu|" +
            "related|remark|replies|rss|shoutbox|sidebar|skyscraper|social|sponsor|ad-break|agegate|" +
            "pagination|pager|popup|promo|share|newsletter|subscribe|modal|outbrain|taboola",
        RegexOption.IGNORE_CASE
    )
    private val MAYBE_CANDIDATE = Regex("and|article|body|column|content|main|shadow", RegexOption.IGNORE_CASE)
    private val POSITIVE = Regex(
        "article|body|content|entry|hentry|h-entry|main|page|post|text|blog|story",
        RegexOption.IGNORE_CASE
    )
    private val NEGATIVE = Regex(
        "hidden|banner|combx|comment|com-|contact|foot|footer|footnote|masthead|media|meta|outbrain|" +
            "promo|related|scroll|share|shoutbox|sidebar|skyscraper|sponsor|shopping|tags|tool|widget",
        RegexOption.IGNORE_CASE
    )
}
