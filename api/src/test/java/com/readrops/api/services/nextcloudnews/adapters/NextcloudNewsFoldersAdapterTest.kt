package com.readrops.api.services.nextcloudnews.adapters

import com.readrops.api.TestUtils
import com.readrops.db.entities.Folder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import junit.framework.TestCase.assertEquals
import okio.Buffer
import org.junit.Test

class NextcloudNewsFoldersAdapterTest {

    private val adapter = Moshi.Builder()
            .add(NextcloudNewsFoldersAdapter())
            .build()
            .adapter<List<Folder>>(Types.newParameterizedType(List::class.java, Folder::class.java))

    @Test
    fun validFoldersTest() {
        val stream = TestUtils.loadResource("services/nextcloudnews/adapters/valid_folder.json")

        val folder = adapter.fromJson(Buffer().readFrom(stream))!![0]

        assertEquals(folder.remoteId,  "4")
        assertEquals(folder.name, "Media")
    }

    @Test
    fun emptyUnnamedFoldersAreIgnored() {
        val stream = TestUtils.loadResource("services/nextcloudnews/adapters/non_valid_folder.json")

        assertEquals(emptyList<Folder>(), adapter.fromJson(Buffer().readFrom(stream)))
    }
}
