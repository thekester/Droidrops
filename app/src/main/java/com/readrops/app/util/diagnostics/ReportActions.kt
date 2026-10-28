package com.readrops.app.util.diagnostics

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import com.readrops.app.R
import com.readrops.app.util.extensions.openUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Clipboard and share intents travel through Binder, which rejects about a megabyte. */
private const val MAX_SHARED_REPORT_LENGTH = 200_000

fun Context.copyReport(report: DiagnosticReport) {
    getSystemService<ClipboardManager>()?.setPrimaryClip(
        ClipData.newPlainText(report.suggestedTitle(), report.text(MAX_SHARED_REPORT_LENGTH))
    )
}

fun Context.shareReport(report: DiagnosticReport) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, report.suggestedTitle())
        putExtra(Intent.EXTRA_TEXT, report.text(MAX_SHARED_REPORT_LENGTH))
    }

    try {
        startActivity(Intent.createChooser(intent, getString(R.string.share_log)))
    } catch (exception: ActivityNotFoundException) {
        Toast.makeText(this, R.string.no_app_to_open_link, Toast.LENGTH_SHORT).show()
    }
}

/** Opens an email draft with the complete report attached. The user reviews and sends it. */
suspend fun Context.emailReport(report: DiagnosticReport) {
    val attachment = try {
        withContext(Dispatchers.IO) {
            val directory = File(cacheDir, DIAGNOSTIC_ATTACHMENT_DIRECTORY)
                .apply { mkdirs() }
            val file = File(directory, "$DIAGNOSTIC_ATTACHMENT_PREFIX${System.currentTimeMillis()}.txt")
            file.writeText(report.text())
            FileProvider.getUriForFile(this@emailReport, packageName, file)
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        Toast.makeText(this, R.string.diagnostic_attachment_failed, Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(DIAGNOSTIC_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, report.suggestedTitle())
        putExtra(Intent.EXTRA_TEXT, getString(R.string.diagnostic_email_body))
        putExtra(Intent.EXTRA_STREAM, attachment)
        clipData = ClipData.newUri(contentResolver, DIAGNOSTIC_ATTACHMENT_PREFIX, attachment)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        startActivity(Intent.createChooser(intent, getString(R.string.email_diagnostic_report)))
    } catch (exception: ActivityNotFoundException) {
        Toast.makeText(this, R.string.no_app_to_open_link, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens a prefilled issue on the Droidrops repository. The whole log is copied first, since
 * a long one only partly fits in the link and the user then pastes the rest.
 */
fun Context.reportOnGitHub(report: DiagnosticReport) {
    copyReport(report)
    Toast.makeText(this, R.string.log_copied_for_github, Toast.LENGTH_LONG).show()

    openUrl(report.gitHubIssueUrl(getString(R.string.app_issues_url), report.suggestedTitle()))
}

private const val DIAGNOSTIC_EMAIL = "droidrops@proton.me"
private const val DIAGNOSTIC_ATTACHMENT_DIRECTORY = "diagnostics"
private const val DIAGNOSTIC_ATTACHMENT_PREFIX = "droidrops-diagnostic-"
