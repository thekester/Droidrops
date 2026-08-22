package com.readrops.api.localfeed.rss2

import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.Names
import com.gitlab.mvysny.konsumexml.allChildrenAutoIgnore
import com.readrops.api.localfeed.RSSMedia
import com.readrops.api.localfeed.XmlAdapter
import com.readrops.api.localfeed.XmlAdapter.Companion.AUTHORS_MAX
import com.readrops.api.utils.ApiUtils
import com.readrops.api.utils.exceptions.ParseException
import com.readrops.api.utils.extensions.nonNullText
import com.readrops.api.utils.extensions.nullableText
import com.readrops.api.utils.extensions.nullableTextRecursively
import com.readrops.db.entities.Item
import com.readrops.db.entities.Tag
import com.readrops.db.util.DateUtils
import java.time.LocalDateTime

class RSS2ItemAdapter : XmlAdapter<Item> {

    override fun fromXml(konsumer: Konsumer): Item {
        val item = Item()

        val creators = arrayListOf<String?>()
        val tags = arrayListOf<Tag>()

        return item.apply {
            konsumer.allChildrenAutoIgnore(names) {
                when (tagName) {
                    "title" -> title = ApiUtils.cleanText(nonNullText())
                    "link" -> link = nonNullText()
                    "author" -> author = nullableText()
                    "dc:creator" -> creators += nullableText()
                    "pubDate" -> pubDate = DateUtils.parse(nullableText())
                    "dc:date" -> pubDate = DateUtils.parse(nullableText())
                    "guid" -> remoteId = nullableText()
                    "description" -> description = nullableTextRecursively()
                    "content:encoded" -> content = nullableTextRecursively()
                    "enclosure" -> RSSMedia.parseMediaContent(this, item = this@apply)
                    "media:content" -> RSSMedia.parseMediaContent(this, item = this@apply)
                    "media:group" -> RSSMedia.parseMediaGroup(this, item = this@apply)
                    "category" -> {
                        nullableText()?.let {
                            tags += Tag(name = it)
                        }
                    }

                    else -> skipContents() // for example media:description
                }
            }

            this.tags = tags
            finalizeItem(this, creators)
        }
    }

    private fun finalizeItem(item: Item, creators: List<String?>) = with(item) {
        validateItem(this)

        if (pubDate == null) pubDate = LocalDateTime.now()
        if (remoteId == null) remoteId = link
        if (author == null && creators.filterNotNull().isNotEmpty())
            author = creators.filterNotNull().joinToString(limit = AUTHORS_MAX)
    }

    /**
     * RSS 2.0 makes every item element optional but requires at least a title or a
     * description. Microblogging feeds such as Mastodon publish posts without any title,
     * so demanding one rejected otherwise valid feeds entirely. A missing title is now
     * derived from the description instead.
     */
    private fun validateItem(item: Item) {
        when {
            item.title == null && item.description == null ->
                throw ParseException("An item requires at least a title or a description")

            item.link == null -> throw ParseException("Item link is required")
        }

        if (item.title == null) {
            val text = ApiUtils.cleanText(item.description!!)

            item.title = if (text.length > TITLE_MAX_LENGTH) {
                text.take(TITLE_MAX_LENGTH).trimEnd() + "…"
            } else {
                text
            }
        }
    }

    companion object {
        // titles derived from a description are trimmed to stay readable in the timeline
        private const val TITLE_MAX_LENGTH = 100

        val names = Names.of(
            "title", "link", "author", "creator", "pubDate", "date",
            "guid", "description", "encoded", "enclosure", "content", "group", "category"
        )
    }
}