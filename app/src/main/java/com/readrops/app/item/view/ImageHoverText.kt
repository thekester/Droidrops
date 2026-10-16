package com.readrops.app.item.view

import org.jsoup.nodes.Element

/** Add image title text, falling back to alt text for feeds that only provide that field. */
internal fun Element.revealImageHoverText() {
    select("img[title], img[alt]").forEach { image ->
        val hoverText = image.attr("title").trim().ifEmpty { image.attr("alt").trim() }
        if (hoverText.isEmpty()) return@forEach

        // Keep the caption outside a linked image so tapping the caption does not open it.
        val anchor = image.parents().firstOrNull { it.tagName() == "a" } ?: image
        val caption = Element("p")
            .addClass("hover-text")
            .appendElement("em")
            .text(hoverText)
            .parent()!!
        anchor.after(caption)
    }
}
