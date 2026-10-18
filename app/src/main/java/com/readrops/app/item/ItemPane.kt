package com.readrops.app.item

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.readrops.app.R
import com.readrops.app.util.components.Placeholder
import com.readrops.app.util.extensions.isTabletWindow
import com.readrops.db.filters.QueryFilters

/**
 * Set while an [ItemScreen] is shown in the timeline's side pane rather than as a full
 * screen. Back and the reader's back arrow then close the pane instead of popping the
 * app navigator.
 */
val LocalItemPaneClose = staticCompositionLocalOf<(() -> Unit)?> { null }

enum class TwoPaneLayout {
    /** The reader opens as a full screen. */
    NONE,

    /** Timeline on the left, article on the right. */
    SIDE_BY_SIDE,

    /** Timeline above, article below, for tablets held in portrait. */
    STACKED
}

/**
 * Material 3's expanded window class starts at 840 dp, enough for a list and an article side by
 * side; a portrait tablet is that tall instead, so the panes are stacked. A phone in landscape is
 * that wide too but far too short, so a tablet-sized smallest width is always required.
 */
internal fun twoPaneLayout(
    enabled: Boolean,
    screenWidthDp: Int,
    screenHeightDp: Int,
    smallestScreenWidthDp: Int
): TwoPaneLayout = when {
    !enabled || !isTabletWindow(smallestScreenWidthDp) -> TwoPaneLayout.NONE
    screenWidthDp >= TWO_PANE_MIN_SIZE_DP -> TwoPaneLayout.SIDE_BY_SIDE
    screenHeightDp >= TWO_PANE_MIN_SIZE_DP -> TwoPaneLayout.STACKED
    else -> TwoPaneLayout.NONE
}

private const val TWO_PANE_MIN_SIZE_DP = 840
private const val LIST_WEIGHT = 0.42f

/** Lays out the timeline ([list]) and the article [pane] for the given [layout]. */
@Composable
fun TwoPaneContainer(
    layout: TwoPaneLayout,
    pane: @Composable (Modifier) -> Unit,
    list: @Composable (Modifier) -> Unit
) {
    when (layout) {
        TwoPaneLayout.NONE -> list(Modifier.fillMaxSize())

        TwoPaneLayout.SIDE_BY_SIDE -> Row(modifier = Modifier.fillMaxSize()) {
            list(Modifier.weight(LIST_WEIGHT))
            VerticalDivider()
            pane(Modifier.weight(1f - LIST_WEIGHT).fillMaxHeight())
        }

        TwoPaneLayout.STACKED -> Column(modifier = Modifier.fillMaxSize()) {
            list(Modifier.weight(LIST_WEIGHT))
            HorizontalDivider()
            // the lower pane does not reach the status bar: no room to leave for it
            pane(
                Modifier
                    .weight(1f - LIST_WEIGHT)
                    .fillMaxWidth()
                    .consumeWindowInsets(WindowInsets.statusBars)
            )
        }
    }
}

@Composable
fun ItemPane(
    itemId: Int?,
    queryFilters: QueryFilters,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (itemId == null) {
            Placeholder(
                text = stringResource(R.string.select_an_article),
                painter = painterResource(R.drawable.ic_timeline)
            )
        } else {
            // A new navigator per article: the reader model is created for it and disposed
            // with it, which is when its pending read and star changes are written.
            key(itemId) {
                Navigator(ItemScreen(itemId, itemIndex = -1, queryFilters = queryFilters)) { navigator ->
                    // the app navigator does not dispose nested navigators (see MainActivity),
                    // so release this screen's model explicitly when the pane changes article.
                    // dispose() is what Navigator itself calls on pop; it is only marked internal.
                    @OptIn(InternalVoyagerApi::class)
                    DisposableEffect(navigator) {
                        onDispose { navigator.items.forEach(navigator::dispose) }
                    }

                    CompositionLocalProvider(LocalItemPaneClose provides onClose) {
                        CurrentScreen()
                    }
                }
            }
        }
    }
}
