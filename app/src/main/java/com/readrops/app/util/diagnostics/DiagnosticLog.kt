package com.readrops.app.util.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

enum class DiagnosticLevel { INFO, WARNING, ERROR }

data class DiagnosticEntry(
    val id: Long,
    val timestamp: Long,
    val level: DiagnosticLevel,
    val tag: String,
    val message: String,
    /** Stack trace of the failure, secrets already removed. */
    val details: String? = null,
    val repeatCount: Int = 1,
) {
    /** Exception type and message, e.g. `java.net.SocketTimeoutException: timeout`. */
    val summary: String?
        get() = details?.lineSequence()?.firstOrNull()

    private val exceptionType: String?
        get() = summary?.substringBefore(':')

    /**
     * Messages of the same failure often differ by an ephemeral local port, so the exception
     * type is compared rather than its full text.
     */
    fun isSameFailureAs(other: DiagnosticEntry): Boolean =
        level == other.level && tag == other.tag && message == other.message &&
                exceptionType == other.exceptionType
}

/**
 * On device record of what went wrong, so a user can read it and choose to send it.
 *
 * Nothing leaves the device from here. Entries are kept newest first, bounded in number and
 * size, and written to [file] off the calling thread; [flush] writes synchronously for the
 * crash handler, which runs just before the process exits. Recording never throws: a broken
 * log must not become a second failure.
 */
class DiagnosticLog(
    private val file: File,
    private val clock: () -> Long = System::currentTimeMillis,
    private val persistExecutor: Executor = Executors.newSingleThreadExecutor(),
    private val maxEntries: Int = MAX_ENTRIES,
) {

    private val lock = Any()
    private val fileLock = Any()

    private val state by lazy { MutableStateFlow(load()) }

    /** Newest first. */
    val entries: StateFlow<List<DiagnosticEntry>>
        get() = state

    fun error(tag: String, message: String, throwable: Throwable? = null) =
        record(DiagnosticLevel.ERROR, tag, message, throwable)

    fun warning(tag: String, message: String, throwable: Throwable? = null) =
        record(DiagnosticLevel.WARNING, tag, message, throwable)

    fun info(tag: String, message: String) = record(DiagnosticLevel.INFO, tag, message, null)

    fun record(level: DiagnosticLevel, tag: String, message: String, throwable: Throwable?) {
        runCatching {
            val details = throwable?.let { describe(it) }
            val cleanMessage = DiagnosticRedaction.redactSecrets(message)

            synchronized(lock) {
                val current = state.value
                val latest = current.firstOrNull()
                val candidate = DiagnosticEntry(
                    id = (latest?.id ?: 0L) + 1,
                    timestamp = clock(),
                    level = level,
                    tag = tag,
                    message = cleanMessage,
                    details = details,
                )

                state.value = if (latest != null && latest.isSameFailureAs(candidate) &&
                    candidate.timestamp - latest.timestamp <= COLLAPSE_WINDOW_MS
                ) {
                    listOf(
                        latest.copy(
                            timestamp = candidate.timestamp,
                            details = details,
                            repeatCount = latest.repeatCount + 1
                        )
                    ) + current.drop(1)
                } else {
                    (listOf(candidate) + current).take(maxEntries)
                }
            }

            persistExecutor.execute { persist() }
        }
    }

    fun clear() {
        synchronized(lock) { state.value = emptyList() }
        persistExecutor.execute { persist() }
    }

    /** Writes pending entries now, on the calling thread. */
    fun flush() = persist()

    private fun describe(throwable: Throwable): String {
        val trace = DiagnosticRedaction.redactSecrets(throwable.stackTraceToString())

        return if (trace.length > MAX_DETAILS_LENGTH) {
            trace.take(MAX_DETAILS_LENGTH) + "\n\t... truncated"
        } else {
            trace.trimEnd()
        }
    }

    private fun persist() {
        runCatching {
            synchronized(fileLock) {
                val snapshot = state.value
                file.parentFile?.mkdirs()

                // oldest first, so the file reads in chronological order
                val temp = File(file.parentFile, "${file.name}.tmp")
                temp.writeText(snapshot.asReversed().joinToString("") { encode(it) + "\n" })

                if (!temp.renameTo(file)) {
                    file.delete()
                    temp.renameTo(file)
                }
            }
        }
    }

    private fun load(): List<DiagnosticEntry> = runCatching {
        if (!file.exists()) return@runCatching emptyList()

        file.readLines()
            .mapNotNull { decode(it) }
            .asReversed()
            .take(maxEntries)
    }.getOrDefault(emptyList())

    private fun encode(entry: DiagnosticEntry): String = listOf(
        entry.id.toString(),
        entry.timestamp.toString(),
        entry.level.name,
        entry.repeatCount.toString(),
        entry.tag,
        entry.message,
        entry.details.orEmpty(),
    ).joinToString(FIELD_SEPARATOR) { escape(it) }

    private fun decode(line: String): DiagnosticEntry? = runCatching {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return@runCatching null

        DiagnosticEntry(
            id = fields[0].toLong(),
            timestamp = fields[1].toLong(),
            level = DiagnosticLevel.valueOf(fields[2]),
            repeatCount = fields[3].toInt(),
            tag = unescape(fields[4]),
            message = unescape(fields[5]),
            details = unescape(fields[6]).ifEmpty { null },
        )
    }.getOrNull()

    private fun escape(value: String): String = buildString(value.length) {
        for (char in value) {
            when (char) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(char)
            }
        }
    }

    private fun unescape(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    't' -> append('\t')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    '\\' -> append('\\')
                    else -> append(char).append(next)
                }
                index += 2
            } else {
                append(char)
                index++
            }
        }
    }

    companion object {
        const val MAX_ENTRIES = 150
        const val FILE_NAME = "diagnostics.log"

        private const val MAX_DETAILS_LENGTH = 6_000
        private const val COLLAPSE_WINDOW_MS = 10 * 60 * 1000L
        private const val FIELD_SEPARATOR = "\t"
        private const val FIELD_COUNT = 7
    }
}
