package com.readrops.app.timelime

import com.readrops.db.entities.Folder
import com.readrops.db.entities.Item
import com.readrops.db.pojo.ItemWithFeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TimelinePresentationTest {
    @Test
    fun hidesTheFolderLabelWhenThatFolderIsAlreadySelected() {
        val item = itemWithFolder("Technology")

        val displayedItem = item.withoutSelectedFolderLabel("Technology")

        assertNull(displayedItem.folder)
        assertEquals("Technology", item.folder?.name)
    }

    @Test
    fun keepsFolderLabelWhenAnotherFolderIsSelected() {
        val item = itemWithFolder("Technology")

        val displayedItem = item.withoutSelectedFolderLabel("Science")

        assertSame(item, displayedItem)
        assertEquals("Technology", displayedItem.folder?.name)
    }

    private fun itemWithFolder(folderName: String) = ItemWithFeed(
        item = Item(title = "Article"),
        feedName = "Feed",
        feedId = 1,
        color = 0,
        feedIconUrl = null,
        websiteUrl = null,
        folder = Folder(name = folderName),
        openIn = null
    )
}
