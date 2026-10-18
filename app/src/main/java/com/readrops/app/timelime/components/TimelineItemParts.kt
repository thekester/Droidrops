package com.readrops.app.timelime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.readrops.app.R
import com.readrops.app.item.components.TagSurface
import com.readrops.app.util.components.FeedIcon
import com.readrops.app.util.extensions.bestForegroundOn
import com.readrops.app.util.extensions.displayColor
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.spacing
import com.readrops.db.pojo.ItemWithFeed
import com.readrops.db.util.DateUtils
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Composable
fun RegularTimelineItem(
    itemWithFeed: ItemWithFeed,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    showImages: Boolean = true,
    showSource: Boolean = true,
    showDate: Boolean = true,
    showReadTime: Boolean = true
) {
    val displayColor = itemWithFeed.displayColor(CardDefaults.cardColors().containerColor.toArgb())

    TimelineItemContainer(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.mediumSpacing)
        ) {
            TimelineItemHeader(
                feedName = itemWithFeed.feedName,
                feedIconUrl = itemWithFeed.feedIconUrl,
                feedColor = displayColor,
                folderName = itemWithFeed.folder?.name,
                date = itemWithFeed.item.pubDate!!,
                duration = itemWithFeed.item.readTime,
                isStarred = itemWithFeed.isStarred,
                onFavorite = onFavorite,
                onShare = onShare,
                showSource = showSource,
                showDate = showDate,
                showReadTime = showReadTime
            )

            ShortSpacer()

            TimelineItemTitle(
                title = itemWithFeed.item.title!!,
                isRead = itemWithFeed.isRead,
                thumbnailUrl = itemWithFeed.item.imageLink.takeIf { showImages }
            )

            ShortSpacer()

            TimelineItemBadge(
                date = itemWithFeed.item.pubDate!!,
                duration = itemWithFeed.item.readTime,
                color = displayColor,
                showDate = showDate,
                showReadTime = showReadTime
            )
        }
    }
}

@Composable
fun CompactTimelineItem(
    itemWithFeed: ItemWithFeed,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    showImages: Boolean = true,
    showSource: Boolean = true,
    showDate: Boolean = true,
    showReadTime: Boolean = true
) {
    val containerColor = MaterialTheme.colorScheme.background
    val displayColor = itemWithFeed.displayColor(CardDefaults.cardColors().containerColor.toArgb())

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // if some alpha is applied to the card, the swipe to dismiss background appears behind it
                // so we draw a rect with the current screen background color behind the card but in front of the dismiss background
                drawRect(containerColor)
            }
    ) {
        Column(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.shortSpacing,
                end = MaterialTheme.spacing.shortSpacing,
                top = MaterialTheme.spacing.shortSpacing
            )
        ) {
            TimelineItemHeader(
                feedName = itemWithFeed.feedName,
                feedIconUrl = itemWithFeed.feedIconUrl,
                feedColor = displayColor,
                folderName = itemWithFeed.folder?.name,
                onFavorite = onFavorite,
                onShare = onShare,
                date = itemWithFeed.item.pubDate!!,
                duration = itemWithFeed.item.readTime,
                isStarred = itemWithFeed.isStarred,
                displayActions = false,
                showSource = showSource,
                showDate = showDate,
                showReadTime = showReadTime
            )

            ShortSpacer()

            TimelineItemTitle(
                title = itemWithFeed.item.title!!,
                isRead = itemWithFeed.isRead,
                thumbnailUrl = itemWithFeed.item.imageLink.takeIf { showImages }
            )

            ShortSpacer()

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.shortSpacing)
            )
        }
    }
}

@Composable
fun LargeTimelineItem(
    itemWithFeed: ItemWithFeed,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    showImages: Boolean = true,
    showSource: Boolean = true,
    showDate: Boolean = true,
    showReadTime: Boolean = true
) {
    val displayColor = itemWithFeed.displayColor(CardDefaults.cardColors().containerColor.toArgb())
    var imageLoadFailed by remember(itemWithFeed.item.imageLink) { mutableStateOf(false) }
    var imageLoaded by remember(itemWithFeed.item.imageLink) { mutableStateOf(false) }

    if (itemWithFeed.item.cleanDescription.isNullOrBlank() && !itemWithFeed.item.hasImage) {
        RegularTimelineItem(
            itemWithFeed = itemWithFeed,
            onClick = onClick,
            onFavorite = onFavorite,
            onShare = onShare,
            modifier = modifier,
            showImages = showImages,
            showSource = showSource,
            showDate = showDate,
            showReadTime = showReadTime
        )
    } else {
        TimelineItemContainer(
            onClick = onClick,
            modifier = modifier
        ) {
            Column {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.mediumSpacing)
                ) {
                    TimelineItemHeader(
                        feedName = itemWithFeed.feedName,
                        feedIconUrl = itemWithFeed.feedIconUrl,
                        feedColor = displayColor,
                        folderName = itemWithFeed.folder?.name,
                        date = itemWithFeed.item.pubDate!!,
                        duration = itemWithFeed.item.readTime,
                        isStarred = itemWithFeed.isStarred,
                        onFavorite = onFavorite,
                        onShare = onShare,
                        showSource = showSource,
                        showDate = showDate,
                        showReadTime = showReadTime
                    )

                    ShortSpacer()

                    TimelineItemBadge(
                        date = itemWithFeed.item.pubDate!!,
                        duration = itemWithFeed.item.readTime,
                        color = displayColor,
                        showDate = showDate,
                        showReadTime = showReadTime
                    )

                    ShortSpacer()

                    TimelineItemTitle(
                        title = itemWithFeed.item.title!!,
                        isRead = itemWithFeed.isRead
                    )

                    if (!itemWithFeed.item.cleanDescription.isNullOrBlank()) {
                        ShortSpacer()

                        Text(
                            text = itemWithFeed.item.cleanDescription!!,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (itemWithFeed.item.tags.isNotEmpty()) {
                        ShortSpacer()

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing)
                        ) {
                            for (tag in itemWithFeed.item.tags) {
                                TagSurface(
                                    name = tag.name,
                                    backgroundColor = displayColor,
                                    truncateName = true
                                )
                            }
                        }

                    }
                }

                if (showImages && itemWithFeed.item.hasImage && !imageLoadFailed) {
                    AsyncImage(
                        model = if (!LocalInspectionMode.current) {
                            itemWithFeed.item.imageLink
                        } else {
                            ImageRequest.Builder(LocalContext.current)
                                .data(R.drawable.ic_broken_image)
                                .build()
                        },
                        contentDescription = itemWithFeed.item.title!!,
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            when (state) {
                                is coil3.compose.AsyncImagePainter.State.Success -> imageLoaded = true
                                is coil3.compose.AsyncImagePainter.State.Error -> imageLoadFailed = true
                                else -> Unit
                            }
                        },
                        modifier = if (imageLoaded) {
                            Modifier
                                .aspectRatio(16f / 9f)
                                .fillMaxWidth()
                                .alpha(if (itemWithFeed.isRead) readAlpha else 1f)
                        } else {
                            Modifier.size(1.dp)
                        }
                    )
                }
            }
        }
    }

}

@Composable
fun TimelineItemContainer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = MaterialTheme.spacing.shortSpacing),
    content: @Composable () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.background

    Card(
        onClick = onClick,
        modifier = modifier
            .padding(padding)
            .fillMaxWidth()
            .drawBehind {
                // if some alpha is applied to the card, the swipe to dismiss background appears behind it
                // so we draw a rect with the current screen background color behind the card but in front of the dismiss background
                drawRoundRect(
                    color = containerColor,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
    ) {
        content()
    }
}

@Composable
fun TimelineItemHeader(
    feedName: String,
    feedIconUrl: String?,
    feedColor: Color,
    folderName: String?,
    date: LocalDateTime,
    duration: Double,
    isStarred: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    displayActions: Boolean = true,
    showSource: Boolean = true,
    showDate: Boolean = true,
    showReadTime: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (showSource) {
                FeedIcon(
                    iconUrl = feedIconUrl,
                    name = feedName
                )

                ShortSpacer()

                Column {
                    Text(
                        text = feedName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = feedColor,
                    )

                    if (!folderName.isNullOrEmpty()) {
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        ShortSpacer()

        if (displayActions) {
            Row {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    IconButton(
                        onClick = onFavorite
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isStarred) R.drawable.ic_star
                                else R.drawable.ic_star_outline
                            ),
                            contentDescription = stringResource(
                                if (isStarred) R.string.remove_from_favorite
                                else R.string.add_to_favorite
                            ),
                        )
                    }
                }

                ShortSpacer()

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    IconButton(
                        onClick = onShare
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share_article),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        } else {
            TimelineItemBadge(
                date = date,
                duration = duration,
                color = feedColor,
                showDate = showDate,
                showReadTime = showReadTime
            )
        }
    }
}


@Composable
fun TimelineItemTitle(
    title: String,
    isRead: Boolean,
    thumbnailUrl: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            // unread items are emphasized by weight, not opacity, so read items keep full contrast
            fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
        )

        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = stringResource(R.string.article_thumbnail, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 64.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun TimelineItemBadge(
    date: LocalDateTime,
    duration: Double,
    color: Color,
    showDate: Boolean = true,
    showReadTime: Boolean = true,
) {
    if (!showDate && !showReadTime) return
    val onAccentColor = bestForegroundOn(color.toArgb())

    Surface(
        color = color,
        shape = RoundedCornerShape(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.shortSpacing,
                vertical = MaterialTheme.spacing.veryShortSpacing
            )
        ) {
            if (showDate) Text(
                text = DateUtils.formattedDateTimeByLocal(date),
                style = MaterialTheme.typography.labelSmall,
                color = onAccentColor
            )

            if (showDate && showReadTime) Text(
                text = stringResource(id = R.string.interpoint),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.veryShortSpacing),
                color = onAccentColor
            )

            if (showReadTime) Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = onAccentColor
            )

            if (showDate && showReadTime) ShortSpacer()

            if (showReadTime) Text(
                text = if (duration > 1) {
                    stringResource(id = R.string.read_time, duration.roundToInt())
                } else {
                    stringResource(id = R.string.read_time_lower_than_1)
                },
                style = MaterialTheme.typography.labelSmall,
                color = onAccentColor
            )
        }
    }
}
