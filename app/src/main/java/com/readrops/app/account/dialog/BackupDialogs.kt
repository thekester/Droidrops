package com.readrops.app.account.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.readrops.app.R
import com.readrops.app.util.components.dialog.BaseDialog
import com.readrops.app.util.theme.spacing

enum class Backup {
    EXPORT,
    RESTORE
}

@Composable
fun BackupChoiceDialog(
    onChoice: (Backup) -> Unit,
    onDismiss: () -> Unit
) {
    BaseDialog(
        title = stringResource(id = R.string.backup_and_restore),
        icon = painterResource(id = R.drawable.ic_backup),
        onDismiss = onDismiss
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.backup_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(MaterialTheme.spacing.shortSpacing)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChoice(Backup.EXPORT) }
            ) {
                Text(
                    text = stringResource(id = R.string.backup_export),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(MaterialTheme.spacing.shortSpacing)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChoice(Backup.RESTORE) }
            ) {
                Text(
                    text = stringResource(id = R.string.backup_restore),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(MaterialTheme.spacing.shortSpacing)
                )
            }
        }
    }
}

@Composable
fun BackupProgressDialog() {
    BaseDialog(
        title = stringResource(id = R.string.backup_and_restore),
        icon = painterResource(id = R.drawable.ic_backup),
        // the transaction must not be left halfway because the dialog was dismissed
        onDismiss = {}
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.mediumSpacing)
        ) {
            CircularProgressIndicator()
        }
    }
}
