package com.readrops.app.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.readrops.app.R
import com.readrops.app.util.components.SelectableIconText
import com.readrops.app.util.components.dialog.BaseDialog
import com.readrops.app.util.theme.spacing

enum class ItemLinkChoice {
    OPEN,
    COPY,
    SHARE
}

@Composable
fun ItemLinkDialog(
    url: String,
    onChoice: (ItemLinkChoice) -> Unit,
    onDismiss: () -> Unit
) {
    BaseDialog(
        title = stringResource(R.string.link_options),
        icon = painterResource(R.drawable.ic_open_in_browser),
        onDismiss = onDismiss
    ) {
        Column {
            SelectionContainer {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = androidx.compose.ui.Modifier.padding(bottom = MaterialTheme.spacing.shortSpacing)
                )
            }
            SelectableIconText(
                icon = painterResource(R.drawable.ic_open_in_browser),
                text = stringResource(R.string.open_url),
                style = MaterialTheme.typography.titleMedium,
                onClick = { onChoice(ItemLinkChoice.OPEN) }
            )
            SelectableIconText(
                icon = painterResource(R.drawable.ic_copy),
                text = stringResource(R.string.copy_url),
                style = MaterialTheme.typography.titleMedium,
                onClick = { onChoice(ItemLinkChoice.COPY) }
            )
            SelectableIconText(
                icon = rememberVectorPainter(Icons.Default.Share),
                text = stringResource(R.string.share_url),
                style = MaterialTheme.typography.titleMedium,
                onClick = { onChoice(ItemLinkChoice.SHARE) }
            )
        }
    }
}
