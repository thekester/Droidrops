package com.readrops.app.item

import com.readrops.app.util.PAGING_PAGE_SIZE

/** Starts the first bounded database page at the page containing the selected article. */
internal data class ItemPagerAnchor(val initialKey: Int)

internal fun itemPagerAnchor(itemIndex: Int, pageSize: Int = PAGING_PAGE_SIZE): ItemPagerAnchor {
    require(pageSize > 0)

    if (itemIndex < 0) return ItemPagerAnchor(initialKey = 0)

    val initialKey = (itemIndex / pageSize) * pageSize
    return ItemPagerAnchor(initialKey = initialKey)
}
