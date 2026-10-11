package com.readrops.api.services.nextcloudnews.adapters

import android.annotation.SuppressLint
import com.readrops.api.utils.exceptions.ParseException
import com.readrops.api.utils.extensions.nextNullableString
import com.readrops.db.entities.Folder
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson

class NextcloudNewsFoldersAdapter {

    @ToJson
    fun toJson(folders: List<Folder>): String = ""

    @SuppressLint("CheckResult")
    @FromJson
    fun fromJson(reader: JsonReader): List<Folder> {
        val folders = mutableListOf<Folder>()

        return try {
            reader.beginObject()
            reader.nextName() // "folders", beginning of folders array
            reader.beginArray()

            while (reader.hasNext()) {
                val folder = Folder()
                reader.beginObject()
                var hasName = false

                while (reader.hasNext()) {
                    with(folder) {
                        when (reader.selectName(NAMES)) {
                            0 -> remoteId = reader.nextInt().toString()
                            1 -> {
                                name = reader.nextNullableString()?.takeIf { it.isNotBlank() }
                                hasName = name != null
                            }
                            else -> reader.skipValue()
                        }
                    }
                }

                // Nextcloud News permits nameless empty folders. They have no useful
                // representation in Droidrops, but must not invalidate the whole response.
                if (hasName) folders += folder
                reader.endObject()
            }

            reader.endArray()
            reader.endObject()

            folders
        } catch (e: Exception) {
            throw ParseException("Nextcloud News folders parsing failure", e)
        }
    }

    companion object {
        val NAMES: JsonReader.Options = JsonReader.Options.of("id", "name")
    }
}
