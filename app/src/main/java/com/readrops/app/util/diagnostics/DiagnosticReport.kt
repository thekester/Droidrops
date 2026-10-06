package com.readrops.app.util.diagnostics

import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Context a developer needs to read a log: the same error means different things per network. */
data class DiagnosticEnvironment(
    val app: String,
    val android: String,
    val device: String,
    val network: String,
)

/**
 * Turns log entries into the text a user reviews and sends. The report stays in English
 * whatever the app language, since it is written for whoever reads the GitHub issue.
 */
class DiagnosticReport(
    /** Newest first, as [DiagnosticLog.entries] provides them. */
    private val entries: List<DiagnosticEntry>,
    private val environment: DiagnosticEnvironment,
    private val hideServers: Boolean,
    private val knownHosts: Set<String> = emptySet(),
    zoneId: ZoneId = ZoneId.systemDefault(),
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId)

    /**
     * The report, keeping the newest entries within [maxLength]. Sharing and the clipboard both
     * go through a Binder transaction, which fails past about a megabyte.
     */
    fun text(maxLength: Int = Int.MAX_VALUE): String = largestFitting(maxLength, OMITTED_NOTE) { it }

    /**
     * A link opening a new issue with the report as its body. Browsers and GitHub reject very
     * long URLs, so when the log does not fit the oldest entries are left out, and the body says
     * so; the caller copies [text] to the clipboard for the user to paste in full.
     */
    fun gitHubIssueUrl(issuesUrl: String, title: String, maxLength: Int = MAX_URL_LENGTH): String {
        val prefix = "${issuesUrl.trimEnd('/')}/new?title=${encode(title)}&body="

        return largestFitting(maxLength, CLIPBOARD_NOTE) { prefix + encode(it) }
    }

    /** Names the newest error by its exception type only: an issue title is public. */
    fun suggestedTitle(): String {
        val failure = entries.firstOrNull { it.level == DiagnosticLevel.ERROR && it.summary != null }
            ?: entries.firstOrNull { it.summary != null }
        val type = failure?.summary?.substringBefore(':')?.substringAfterLast('.')

        return if (type.isNullOrBlank()) "Diagnostic report" else "Diagnostic report: $type"
    }

    private fun largestFitting(maxLength: Int, note: String, build: (String) -> String): String {
        fun attempt(count: Int) =
            build(render(entries.take(count), note.takeIf { count < entries.size }))

        val complete = attempt(entries.size)
        if (complete.length <= maxLength) return complete

        // fewer entries always make a shorter result, so the largest count that fits is searchable
        var fits = 0
        var tooLong = entries.size
        while (tooLong - fits > 1) {
            val middle = (fits + tooLong) / 2
            if (attempt(middle).length <= maxLength) fits = middle else tooLong = middle
        }

        return attempt(fits)
    }

    private fun render(shown: List<DiagnosticEntry>, note: String?): String = buildString {
        appendLine("**Droidrops diagnostic log**")
        appendLine()
        appendLine("- App: ${environment.app}")
        appendLine("- Android: ${environment.android}")
        appendLine("- Device: ${environment.device}")
        appendLine("- Network: ${environment.network}")
        appendLine("- Server addresses hidden: ${if (hideServers) "yes" else "no"}")
        appendLine()

        if (note != null) {
            appendLine(note)
            appendLine()
        }

        appendLine("```text")
        if (shown.isEmpty()) appendLine("No entries.")

        for (entry in shown) {
            append(timeFormatter.format(Instant.ofEpochMilli(entry.timestamp)))
            append(' ').append(entry.level.name)
            append(' ').append(entry.tag).append(": ").append(clean(entry.message))
            if (entry.repeatCount > 1) append(" (x").append(entry.repeatCount).append(')')
            appendLine()

            entry.details?.let { appendLine(clean(it)) }
            appendLine()
        }

        appendLine("```")
    }

    private fun clean(text: String): String {
        val visible = if (hideServers) DiagnosticRedaction.maskServerAddresses(text, knownHosts) else text
        // a stray fence would end the code block early and garble the issue
        return visible.replace("```", "'''")
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    companion object {
        const val CLIPBOARD_NOTE = "Only the most recent entries fit in this link. " +
                "The full log was copied to the clipboard: paste it below."
        const val OMITTED_NOTE = "Older entries were left out to keep the report short."

        private const val MAX_URL_LENGTH = 7_500
    }
}
