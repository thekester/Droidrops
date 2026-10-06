package com.readrops.app.util.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder
import java.time.ZoneOffset

class DiagnosticReportTest {

    private val environment = DiagnosticEnvironment(
        app = "Droidrops 2.2.7 (28) release",
        android = "15 (API 35)",
        device = "Google Pixel 9",
        network = "Wi-Fi, internet not validated",
    )

    private val timeout = DiagnosticEntry(
        id = 2,
        timestamp = 1_757_583_770_000, // 2025-09-11 09:42:50 UTC
        level = DiagnosticLevel.ERROR,
        tag = "Sync",
        message = "Synchronization failed",
        details = "java.net.SocketTimeoutException: failed to connect to rss.example.org/203.0.113.5 " +
                "(port 8443)\n\tat okhttp3.internal.connection.RealConnection.connectSocket(RealConnection.kt:297)",
        repeatCount = 3,
    )

    private val older = DiagnosticEntry(
        id = 1,
        timestamp = 1_757_583_000_000,
        level = DiagnosticLevel.WARNING,
        tag = "Feed",
        message = "Feed refresh failed",
    )

    private fun report(entries: List<DiagnosticEntry>, hideServers: Boolean = false) =
        DiagnosticReport(entries, environment, hideServers, zoneId = ZoneOffset.UTC)

    @Test
    fun reportDescribesTheEnvironmentThenEntriesNewestFirst() {
        val text = report(listOf(timeout, older)).text()

        assertTrue(text.contains("Droidrops 2.2.7 (28) release"))
        assertTrue(text.contains("Wi-Fi, internet not validated"))
        assertTrue(text.contains("2025-09-11 09:42:50 ERROR Sync: Synchronization failed (x3)"))
        assertTrue(text.indexOf("Synchronization failed") < text.indexOf("Feed refresh failed"))
    }

    @Test
    fun serverAddressesAreHiddenOnlyWhenAsked() {
        val hidden = report(listOf(timeout), hideServers = true).text()
        val shown = report(listOf(timeout), hideServers = false).text()

        assertFalse(hidden.contains("rss.example.org"))
        assertFalse(hidden.contains("203.0.113.5"))
        assertTrue(hidden.contains("(port 8443)"))
        assertTrue(shown.contains("rss.example.org"))
    }

    @Test
    fun issueLinkCarriesTheWholeLogWhenItFits() {
        val url = report(listOf(timeout, older)).gitHubIssueUrl(ISSUES_URL, "Diagnostic report")
        val body = queryParameter(url, "body")

        assertTrue(url.startsWith("$ISSUES_URL/new?"))
        assertEquals("Diagnostic report", queryParameter(url, "title"))
        assertTrue(body.contains("Synchronization failed"))
        assertTrue(body.contains("Feed refresh failed"))
        assertFalse(body.contains(DiagnosticReport.CLIPBOARD_NOTE))
    }

    @Test
    fun issueLinkKeepsTheNewestEntriesWithinTheLengthLimit() {
        val entries = (100 downTo 1).map { index ->
            older.copy(id = index.toLong(), message = "failure $index", details = "x".repeat(400))
        }

        val url = report(entries).gitHubIssueUrl(ISSUES_URL, "Diagnostic report", maxLength = 4_000)
        val body = queryParameter(url, "body")

        assertTrue(url.length <= 4_000)
        assertTrue(body.contains("failure 100"))
        assertFalse(body.contains("failure 1\n"))
        assertTrue(body.contains(DiagnosticReport.CLIPBOARD_NOTE))
    }

    @Test
    fun issueLinkEncodesSpacesAndPlusSigns() {
        val url = report(listOf(older.copy(message = "a + b"))).gitHubIssueUrl(ISSUES_URL, "a b")

        assertFalse(url.contains("+"))
        assertTrue(url.contains("title=a%20b"))
    }

    @Test
    fun textCanBeBoundedForSharing() {
        val entries = (100 downTo 1).map { index ->
            older.copy(id = index.toLong(), message = "failure $index", details = "x".repeat(400))
        }

        val text = report(entries).text(maxLength = 3_000)

        assertTrue(text.length <= 3_000)
        assertTrue(text.contains("failure 100"))
        assertTrue(text.contains(DiagnosticReport.OMITTED_NOTE))
    }

    @Test
    fun titleNamesTheNewestErrorByItsTypeOnly() {
        assertEquals(
            "Diagnostic report: SocketTimeoutException",
            report(listOf(older, timeout)).suggestedTitle()
        )
        assertEquals("Diagnostic report", report(emptyList()).suggestedTitle())
    }

    private fun queryParameter(url: String, name: String): String =
        URI(url).rawQuery.split("&")
            .map { it.substringBefore("=") to it.substringAfter("=") }
            .first { it.first == name }
            .second
            .let { URLDecoder.decode(it, "UTF-8") }

    companion object {
        private const val ISSUES_URL = "https://github.com/thekester/Droidrops/issues"
    }
}
