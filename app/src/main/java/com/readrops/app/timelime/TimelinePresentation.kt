package com.readrops.app.timelime

import com.readrops.db.pojo.ItemWithFeed

internal fun ItemWithFeed.withoutSelectedFolderLabel(selectedFolderName: String): ItemWithFeed =
    if (folder?.name == selectedFolderName) copy(folder = null) else this
