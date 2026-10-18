package com.readrops.app.item.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.readrops.app.R
import com.readrops.app.timelime.components.itemWithFeed
import com.readrops.app.util.DefaultPreview
import com.readrops.app.util.components.FeedIcon
import com.readrops.app.util.components.IconText
import com.readrops.app.util.extensions.displayColor
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.ReadropsTheme
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.spacing
import com.readrops.db.pojo.ItemWithFeed
import com.readrops.db.util.DateUtils
import kotlin.math.roundToInt

@Composable
fun SimpleTitle(
    itemWithFeed: ItemWithFeed,
    titleColor: Color,
    onBackgroundColor: Color,
    bottomPadding: Boolean,
) {
    val item = itemWithFeed.item
    val spacing = MaterialTheme.spacing.mediumSpacing
    val accentColor = itemWithFeed.displayColor(MaterialTheme.colorScheme.background.toArgb())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing,
                end = spacing,
                top = spacing,
                bottom = if (bottomPadding) spacing else 0.dp
            )
    ) {
        FeedIcon(
            iconUrl = itemWithFeed.feedIconUrl,
            name = itemWithFeed.feedName,
            size = 48.dp,
            modifier = Modifier.clip(CircleShape)
        )

        ShortSpacer()

        Text(
            text = itemWithFeed.feedName,
            style = MaterialTheme.typography.labelLarge,
            color = onBackgroundColor,
            textAlign = TextAlign.Center
        )

        ShortSpacer()

        Text(
            text = item.title!!,
            style = MaterialTheme.typography.headlineMedium,
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (item.author != null) {
            ShortSpacer()

            IconText(
                icon = painterResource(id = R.drawable.ic_person),
                text = itemWithFeed.item.author!!,
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor,
                tint = itemWithFeed.displayColor(MaterialTheme.colorScheme.background.toArgb())
            )
        }

        ShortSpacer()

        val readTime = if (item.readTime > 1) {
            stringResource(id = R.string.read_time, item.readTime.roundToInt())
        } else {
            stringResource(id = R.string.read_time_lower_than_1)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.veryShortSpacing),
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateUtils.formattedDateTimeByLocal(item.pubDate!!),
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor
            )
            Text(
                text = stringResource(id = R.string.interpoint),
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor
            )
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = onBackgroundColor
            )
            Text(
                text = readTime,
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor
            )
        }

        if (itemWithFeed.item.tags.isNotEmpty()) {
            MediumSpacer()

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    MaterialTheme.spacing.shortSpacing,
                    Alignment.CenterHorizontally
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing)
            ) {
                for (tag in itemWithFeed.item.tags) {
                    TagSurface(
                        name = tag.name,
                        backgroundColor = accentColor
                    )

                }
            }
        }
    }
}

@DefaultPreview
@Composable
private fun SimpleTitlePreview() {
    ReadropsTheme {
        SimpleTitle(
            itemWithFeed = itemWithFeed,
            titleColor = MaterialTheme.colorScheme.primary,
            onBackgroundColor = MaterialTheme.colorScheme.onBackground,
            bottomPadding = true
        )
    }
}
