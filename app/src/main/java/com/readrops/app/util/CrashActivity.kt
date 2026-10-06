package com.readrops.app.util

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readrops.app.R
import com.readrops.app.util.diagnostics.DiagnosticEntry
import com.readrops.app.util.diagnostics.DiagnosticLevel
import com.readrops.app.util.diagnostics.DiagnosticLog
import com.readrops.app.util.diagnostics.DiagnosticRedaction
import com.readrops.app.util.diagnostics.DiagnosticReport
import com.readrops.app.util.diagnostics.diagnosticEnvironment
import com.readrops.app.util.diagnostics.reportOnGitHub
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.ReadropsTheme
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.VeryLargeSpacer
import com.readrops.app.util.theme.VeryShortSpacer
import com.readrops.app.util.theme.spacing
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))

        val stackTrace = intent.getStringExtra(STACK_TRACE_KEY).orEmpty()

        setContent {
            ReadropsTheme {
                CrashScreen(
                    stackTrace = stackTrace,
                    onReport = { reportOnGitHub(crashReport(stackTrace)) }
                )
            }
        }
    }

    /**
     * The recent diagnostic log, whose newest entry is this crash, so the report also shows what
     * led to it. Servers are hidden: nothing on this screen lets the user choose otherwise.
     */
    private fun crashReport(stackTrace: String): DiagnosticReport {
        val entries = runCatching { get<DiagnosticLog>().entries.value }
            .getOrDefault(emptyList())
            .ifEmpty {
                listOf(
                    DiagnosticEntry(
                        id = 1,
                        timestamp = System.currentTimeMillis(),
                        level = DiagnosticLevel.ERROR,
                        tag = "Crash",
                        message = "Droidrops crashed",
                        details = DiagnosticRedaction.redactSecrets(stackTrace)
                    )
                )
            }

        return DiagnosticReport(
            entries = entries.take(MAX_REPORTED_ENTRIES),
            environment = diagnosticEnvironment(this),
            hideServers = true
        )
    }

    companion object {
        const val STACK_TRACE_KEY = "STACK_TRACE"
        const val MAX_STACK_TRACE_LENGTH = 100_000

        private const val MAX_REPORTED_ENTRIES = 30
    }
}

@Composable
fun CrashScreen(stackTrace: String, onReport: () -> Unit) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val copyStackTrace = {
        coroutineScope.launch {
            clipboard.setClipEntry(
                ClipEntry(ClipData.newPlainText("stack trace", stackTrace))
            )
            displayToast(context)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(MaterialTheme.spacing.mediumSpacing)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            VeryLargeSpacer()

            Icon(
                painter = painterResource(id = R.drawable.ic_bug),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            MediumSpacer()

            Text(
                text = stringResource(R.string.readrops_crashed),
                style = MaterialTheme.typography.titleLarge
            )

            ShortSpacer()

            Text(
                text = stringResource(R.string.crash_message),
                style = MaterialTheme.typography.bodyMedium,
            )

            MediumSpacer()

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stackTrace,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Justify,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .padding(MaterialTheme.spacing.mediumSpacing)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                )
            }

            MediumSpacer()

            Column {
                Button(
                    onClick = onReport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.report_error_github))
                }

                VeryShortSpacer()

                OutlinedButton(
                    onClick = {
                        copyStackTrace()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.copy_error_clipboard))
                }
            }
        }
    }
}

fun displayToast(context: Context) {
    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
}

@DefaultPreview
@Composable
private fun CrashScreenPreview() {
    ReadropsTheme {
        CrashScreen(stackTrace = "", onReport = {})
    }
}
