package com.readrops.app.item.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import com.readrops.app.R
import com.readrops.app.util.DefaultPreview
import com.readrops.app.util.extensions.bestForegroundOn
import com.readrops.app.util.theme.ReadropsTheme
import com.readrops.app.util.theme.spacing
import androidx.compose.ui.res.stringResource

data class BottomBarState(
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val isOpenUrlVisible: Boolean = true,
    val isFullContentShown: Boolean = false,
    val isFullContentLoading: Boolean = false
)

@Composable
fun ItemScreenBottomBar(
    state: BottomBarState,
    accentColor: Color,
    onShare: () -> Unit,
    onOpenUrl: () -> Unit,
    onChangeReadState: (Boolean) -> Unit,
    onChangeStarState: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFullContent: () -> Unit = {}
) {
    val onAccentColor = bestForegroundOn(accentColor.toArgb())

    Surface(
        color = accentColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.padding(MaterialTheme.spacing.shortSpacing)
        ) {
            IconButton(
                onClick = { onChangeReadState(!state.isRead) }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (state.isRead) {
                            R.drawable.ic_state_read
                        } else {
                            R.drawable.ic_state_unread
                        }
                    ),
                    tint = onAccentColor,
                    contentDescription = stringResource(
                        if (state.isRead) R.string.mark_unread else R.string.mark_read
                    )
                )
            }

            IconButton(
                onClick = { onChangeStarState(!state.isStarred) }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (state.isStarred)
                            R.drawable.ic_star
                        else R.drawable.ic_star_outline
                    ),
                    tint = onAccentColor,
                    contentDescription = stringResource(
                        if (state.isStarred) R.string.remove_from_favorite
                        else R.string.add_to_favorite
                    )
                )
            }

            IconButton(
                onClick = onShare
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    tint = onAccentColor,
                    contentDescription = stringResource(R.string.share_article)
                )
            }

            if (state.isOpenUrlVisible) {
                IconButton(
                    onClick = onToggleFullContent,
                    enabled = !state.isFullContentLoading
                ) {
                    if (state.isFullContentLoading) {
                        CircularProgressIndicator(
                            color = onAccentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_full_article),
                            tint = onAccentColor,
                            contentDescription = stringResource(
                                if (state.isFullContentShown) R.string.show_feed_content
                                else R.string.load_full_article
                            ),
                            // dimmed while the feed entry is shown, like the outlined/filled star
                            modifier = Modifier.alpha(if (state.isFullContentShown) 1f else 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenUrl
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_browser),
                        tint = onAccentColor,
                        contentDescription = stringResource(R.string.open_url)
                    )
                }
            }
        }
    }
}

@DefaultPreview
@Composable
private fun ItemScreenBottomBarPreview() {
    ReadropsTheme {
        ItemScreenBottomBar(
            state = BottomBarState(
                isRead = false,
                isStarred = false
            ),
            accentColor = MaterialTheme.colorScheme.primary,
            onShare = {},
            onOpenUrl = {},
            onChangeReadState = {},
            onChangeStarState = {},
        )
    }
}
