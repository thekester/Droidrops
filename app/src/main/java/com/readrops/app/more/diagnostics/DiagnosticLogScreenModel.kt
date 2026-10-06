package com.readrops.app.more.diagnostics

import android.content.Context
import cafe.adriel.voyager.core.model.ScreenModel
import com.readrops.app.util.diagnostics.DiagnosticEntry
import com.readrops.app.util.diagnostics.DiagnosticLog
import com.readrops.app.util.diagnostics.DiagnosticReport
import com.readrops.app.util.diagnostics.diagnosticEnvironment
import com.readrops.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URI

class DiagnosticLogScreenModel(
    private val diagnosticLog: DiagnosticLog,
    private val database: Database,
    private val context: Context,
) : ScreenModel {

    val entries: StateFlow<List<DiagnosticEntry>> = diagnosticLog.entries

    /**
     * The report as it will be sent. When servers are hidden, the account servers are masked
     * wherever they appear, including messages whose wording no pattern anticipates.
     */
    suspend fun report(hideServers: Boolean): DiagnosticReport = withContext(Dispatchers.IO) {
        val knownHosts = if (hideServers) {
            database.accountDao().selectAllAccounts().first()
                .mapNotNull { account -> account.url?.let { runCatching { URI(it).host }.getOrNull() } }
                .toSet()
        } else {
            emptySet()
        }

        DiagnosticReport(
            entries = entries.value,
            environment = diagnosticEnvironment(context),
            hideServers = hideServers,
            knownHosts = knownHosts,
        )
    }

    fun clear() = diagnosticLog.clear()
}
