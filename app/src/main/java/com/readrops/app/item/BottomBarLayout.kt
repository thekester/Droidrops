package com.readrops.app.item

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Returns the Scaffold space still occupied by the reader action bar. */
internal fun visibleBottomBarSlotHeight(
    bottomBarHeight: Dp,
    bottomBarOffset: Dp,
    isScrollable: Boolean
): Dp {
    val offset = if (isScrollable) bottomBarOffset else 0.dp
    return (bottomBarHeight + offset).coerceIn(0.dp, bottomBarHeight)
}
