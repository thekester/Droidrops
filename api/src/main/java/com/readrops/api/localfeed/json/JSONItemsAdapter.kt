package com.readrops.api.localfeed.json

import com.readrops.api.localfeed.XmlAdapter.Companion.AUTHORS_MAX
import com.readrops.api.utils.exceptions.ParseException
import com.readrops.api.utils.extensions.nextNonEmptyString
import com.readrops.api.utils.extensions.nextNullableString
import com.readrops.db.entities.Item
import com.readrops.db.entities.Tag
import com.readrops.db.util.DateUtils
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.LocalDateTime
import com.readrops.api.utils.ApiUtils

class JSONItemsAdapter : JsonAdapter<List<Item>>() {

    override fun toJson(writer: JsonWriter, value: List<Item>?) {
        // not useful
    }

    override fun fromJson(reader: JsonReader): List<Item> = with(reader) {
        val items = arrayListOf<Item>()

        beginArray()

        while (hasNext()) {
            beginObject()
            val item = Item()

            var contentText: String? = null
            var contentHtml: String? = null

            while (hasNext()) {
                with(item) {
                    when (selectName(names)) {
                        0 -> remoteId = nextNonEmptyString()
                        1 -> link = nextNonEmptyString()
                        2 -> title = ApiUtils.cleanText(nextNonEmptyString())
                        3 -> contentHtml = nextNullableString()
                        4 -> contentText = nextNullableString()
                        5 -> description = nextNullableString()
                        6 -> imageLink = nextNullableString()
                        7 -> pubDate = DateUtils.parse(nextNullableString())
                        8 -> author = parseAuthor(reader) // jsonfeed 1.0
                        9 -> author = parseAuthors(reader) // jsonfeed 1.1
                        10 -> tags = parseTags(reader)
                        else -> skipValue()
                    }
                }
            }

            validateItem(item)
            item.content = contentHtml ?: contentText
            if (item.pubDate == null) item.pubDate = LocalDateTime.now()

            endObject()
            items += item
        }

        endArray()
        items
    }

    private fun parseAuthor(reader: JsonReader): String? {
        var author: String? = null
        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name" -> author = reader.nextNullableString()
                else -> reader.skipValue()
            }
        }

        reader.endObject()
        return author
    }

    private fun parseAuthors(reader: JsonReader): String? {
        val authors = arrayListOf<String?>()
        reader.beginArray()

        while (reader.hasNext()) {
            authors += parseAuthor(reader)
        }

        reader.endArray()

        return if (authors.filterNotNull().isNotEmpty())
            authors.filterNotNull().joinToString(limit = AUTHORS_MAX) else null
    }

    private fun parseTags(reader: JsonReader): List<Tag> = with(reader) {
        val tags = arrayListOf<Tag>()
        beginArray()

        while (hasNext()) {
            val name = nextNullableString()

            if (!name.isNullOrEmpty()) {
                tags += Tag(name = name)
            }
        }

        endArray()
        tags
    }

    private fun validateItem(item: Item): Boolean = when {
        item.title == null -> throw ParseException("Item title is required")
        item.link == null -> throw ParseException("Item link is required")
        else -> true
    }

    companion object {
        val names: JsonReader.Options = JsonReader.Options.of(
            "id",
            "url",
            "title",
            "content_html",
            "content_text",
            "summary",
            "image",
            "date_published",
            "author",
            "authors",
            "tags"
        )
    }
}