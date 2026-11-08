package com.readrops.api.services.greader.adapters

import com.readrops.api.utils.exceptions.ParseException
import com.readrops.db.entities.Item
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/** One bounded page from the Google Reader compatible stream endpoint. */
data class GReaderItemsPage(
    val items: List<Item>,
    val continuation: String?
)

class GReaderItemsPageAdapter : JsonAdapter<GReaderItemsPage>() {
    override fun toJson(writer: JsonWriter, value: GReaderItemsPage?) = Unit

    override fun fromJson(reader: JsonReader): GReaderItemsPage = with(reader) {
        val items = mutableListOf<Item>()
        var continuation: String? = null

        try {
            beginObject()
            while (hasNext()) {
                when (nextName()) {
                    "items" -> GReaderItemsAdapter().parseItems(reader, items)
                    "continuation" -> continuation = if (peek() == JsonReader.Token.NULL) {
                        nextNull<String>()
                        null
                    } else {
                        nextString().takeIf(String::isNotBlank)
                    }
                    else -> skipValue()
                }
            }
            endObject()

            GReaderItemsPage(items, continuation)
        } catch (exception: Exception) {
            throw ParseException("GReader items page parsing failure", exception)
        }
    }
}
