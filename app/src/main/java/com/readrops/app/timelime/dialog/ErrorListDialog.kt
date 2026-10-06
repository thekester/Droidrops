package com.readrops.app.timelime.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import com.readrops.app.R
import com.readrops.app.more.diagnostics.DiagnosticLogScreen
import com.readrops.app.repositories.ErrorResult
import com.readrops.app.util.accounterror.AccountError
import com.readrops.app.util.components.dialog.BaseDialog
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.ShortSpacer

@Composable
fun ErrorListDialog(
    errorResult: ErrorResult,
    onDismiss: () -> Unit,
) {
    val scrollableState = rememberScrollState()
    val accountError = AccountError.Companion.DefaultAccountError(LocalContext.current)
    val navigator = LocalNavigator.current

    BaseDialog(
        title = stringResource(R.string.synchronization_errors),
        icon = painterResource(id = R.drawable.ic_error),
        onDismiss = onDismiss,
        modifier = Modifier.heightIn(max = 500.dp)
    ) {
        Text(
            text = pluralStringResource(
                id = R.plurals.error_occurred_feed,
                count = errorResult.size
            )
        )

        MediumSpacer()

        // BaseDialog gives no ColumnScope: this one lets the list shrink so the button stays visible
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollableState)
            ) {
                for (error in errorResult.entries) {
                    // the feed address names the port a network may be blocking
                    Text(text = "${error.key.name}: ${accountError.genericMessage(error.value, error.key.url)}")

                    ShortSpacer()
                }
            }

            if (navigator != null) {
                TextButton(
                    onClick = {
                        onDismiss()
                        navigator.push(DiagnosticLogScreen())
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = stringResource(R.string.view_log))
                }
            }
        }
    }
}