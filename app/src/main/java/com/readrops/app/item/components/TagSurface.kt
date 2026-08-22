package com.readrops.app.item.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.readrops.app.util.Utils
import com.readrops.app.util.extensions.bestForegroundOn
import com.readrops.app.util.theme.spacing

@Composable
fun TagSurface(
    name: String,
    backgroundColor: Color,
    truncateName: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(48.dp),
        color = backgroundColor,
        contentColor = bestForegroundOn(backgroundColor.toArgb())
    ) {
        Text(
            text = if (truncateName) {
                Utils.truncateString(name, 30)
            } else {
                name
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.shortSpacing,
                vertical = MaterialTheme.spacing.veryShortSpacing
            )
        )
    }
}