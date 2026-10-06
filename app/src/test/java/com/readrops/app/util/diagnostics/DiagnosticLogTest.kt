package com.readrops.app.util.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.Executor

class DiagnosticLogTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000_000L

    private val file: File
        get() = File(folder.root, "diagnostics.log")

    private fun openLog(maxEntries: Int = 50) = DiagnosticLog(
        file = file,
        clock = { now },
        persistExecutor = Executor { it.run() },
        maxEntries = maxEntries,
    )

    @Test
    fun recordedFailureSurvivesARestart() {
        val failure = SocketTimeoutException("failed to connect (port 8443)\tafter 10000ms")
        openLog().error("Sync", "Synchronization failed", failure)

        with(openLog().entries.value.single()) {
            assertEquals(DiagnosticLevel.ERROR, level)
            assertEquals("Sync", tag)
            assertEquals("Synchronization failed", message)
            assertEquals(1_000_000L, timestamp)
            assertEquals(
                "java.net.SocketTimeoutException: failed to connect (port 8443)\tafter 10000ms",
                summary
            )
            assertTrue(details!!.contains("\n\tat "))
        }
    }

    @Test
    fun secretsNeverReachTheDisk() {
        openLog().error(
            "Login",
            "POST https://rss.example.org/accounts/ClientLogin?Email=john&Passwd=hunter2",
            IOException("redirected to https://john:hunter2@rss.example.org/")
        )

        assertFalse(file.readText().contains("hunter2"))
        assertFalse(openLog().entries.value.single().message.contains("hunter2"))
    }

    @Test
    fun repeatedFailureIsCollapsedIntoOneEntry() {
        val log = openLog()
        log.error("Sync", "Synchronization failed", SocketTimeoutException("from port 51234"))
        now += 60_000
        log.error("Sync", "Synchronization failed", SocketTimeoutException("from port 51299"))

        with(log.entries.value.single()) {
            assertEquals(2, repeatCount)
            assertEquals(now, timestamp)
            assertTrue(summary!!.contains("51299"))
        }
    }

    @Test
    fun differentFailuresAreKeptApartNewestFirst() {
        val log = openLog()
        log.error("Sync", "Synchronization failed", SocketTimeoutException("timeout"))
        now += 1
        log.error("Sync", "Synchronization failed", ConnectException("refused"))

        assertEquals(
            listOf("java.net.ConnectException: refused", "java.net.SocketTimeoutException: timeout"),
            log.entries.value.map { it.summary }
        )
    }

    @Test
    fun oldestEntriesAreDroppedPastCapacity() {
        val log = openLog(maxEntries = 3)
        repeat(5) {
            log.warning("Feed", "failure $it")
            now += 1
        }

        assertEquals(
            listOf("failure 4", "failure 3", "failure 2"),
            openLog(maxEntries = 3).entries.value.map { it.message }
        )
    }

    @Test
    fun hugeStackTracesAreTruncated() {
        openLog().error("Parser", "Parsing failed", IllegalStateException("x".repeat(50_000)))

        assertTrue(openLog().entries.value.single().details!!.length < 10_000)
    }

    @Test
    fun clearEmptiesMemoryAndDisk() {
        val log = openLog()
        log.error("Sync", "Synchronization failed", SocketTimeoutException())
        log.clear()

        assertTrue(log.entries.value.isEmpty())
        assertTrue(openLog().entries.value.isEmpty())
    }

    @Test
    fun unreadableLinesAreSkipped() {
        openLog().info("Sync", "kept")
        file.appendText("half written line\n")

        assertEquals(listOf("kept"), openLog().entries.value.map { it.message })
    }
}
