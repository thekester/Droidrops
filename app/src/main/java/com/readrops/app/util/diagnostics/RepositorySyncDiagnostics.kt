package com.readrops.app.util.diagnostics

import android.os.SystemClock

/** Adds a compact, credential-free trace around a remote account sync. */
class RepositorySyncDiagnostics(
    private val log: DiagnosticLog,
    private val tag: String,
    syncType: String,
    authentication: String,
    pendingChanges: Int,
) {
    private val startedAt = SystemClock.elapsedRealtime()
    private var stage = "starting"

    init {
        log.info(
            tag,
            "Sync started ($syncType, auth=$authentication, pending changes=$pendingChanges)"
        )
    }

    fun progress(message: String) {
        stage = message
        log.info(tag, message)
    }

    fun saving() = progress("Saving downloaded data to the local database")

    fun failed(exception: Exception) {
        log.error(tag, "Sync failed during: $stage", exception)
    }

    fun outOfMemory() {
        // Avoid building an OOM stack trace while the heap is exhausted; the last progress
        // entry tells which remote request/page or local operation was active.
        runCatching {
            log.error(tag, "Sync ran out of memory during: $stage")
            log.flush()
        }
    }

    fun completed(categories: Int, feeds: Int, remoteItems: Int, newItems: Int) {
        log.info(
            tag,
            "Sync completed in ${SystemClock.elapsedRealtime() - startedAt} ms " +
                    "($categories categories, $feeds feeds, $remoteItems remote entries, " +
                    "$newItems new local entries)"
        )
    }
}
