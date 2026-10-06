package com.readrops.app.util.diagnostics

import com.readrops.app.util.diagnostics.DiagnosticRedaction.maskServerAddresses
import com.readrops.app.util.diagnostics.DiagnosticRedaction.redactSecrets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticRedactionTest {

    @Test
    fun credentialsInUrlAreRemoved() {
        assertEquals(
            "https://<redacted>@rss.example.org/api",
            redactSecrets("https://john:hunter2@rss.example.org/api")
        )
    }

    @Test
    fun sensitiveQueryParametersAreRemoved() {
        val redacted = redactSecrets(
            "POST https://rss.example.org/accounts/ClientLogin?Email=john&Passwd=hunter2&output=json"
        )

        assertFalse(redacted.contains("hunter2"))
        assertFalse(redacted.contains("john"))
        assertTrue(redacted.contains("output=json"))
    }

    @Test
    fun authorizationValuesAreRemoved() {
        assertEquals(
            "Authorization: <redacted>",
            redactSecrets("Authorization: GoogleLogin auth=abc123")
        )
        assertEquals("Bearer <redacted> sent", redactSecrets("Bearer abc.def-123 sent"))
    }

    @Test
    fun clientLoginTokensAreRemoved() {
        val redacted = redactSecrets("SID=abc\nLSID=def\nAuth=ghi")

        assertFalse(redacted.contains("abc"))
        assertFalse(redacted.contains("def"))
        assertFalse(redacted.contains("ghi"))
    }

    @Test
    fun emailAddressesAreRemoved() {
        assertEquals("login <email> failed", redactSecrets("login john.doe@example.org failed"))
    }

    @Test
    fun ordinaryDiagnosticTextIsUntouched() {
        val text = "java.net.SocketTimeoutException: timeout\n" +
                "\tat okhttp3.internal.connection.RealConnection.connectSocket(RealConnection.kt:297)"

        assertEquals(text, redactSecrets(text))
    }

    @Test
    fun serverAddressesAreMaskedButPortsKept() {
        val masked = maskServerAddresses(
            "failed to connect to rss.example.org/203.0.113.5 (port 8443) " +
                    "from /10.0.2.16 (port 51234) after 10000ms"
        )

        assertEquals(
            "failed to connect to <server>/<ip> (port 8443) from /<ip> (port 51234) after 10000ms",
            masked
        )
    }

    @Test
    fun urlHostIsMaskedButPortAndPathKept() {
        assertEquals(
            "GET https://<server>:8443/api/greader.php/reader/api/0/stream",
            maskServerAddresses("GET https://rss.example.org:8443/api/greader.php/reader/api/0/stream")
        )
    }

    @Test
    fun unresolvedHostIsMasked() {
        assertEquals(
            "Unable to resolve host \"<server>\": No address associated with hostname",
            maskServerAddresses("Unable to resolve host \"rss.example.org\": No address associated with hostname")
        )
    }

    @Test
    fun knownHostsAreMaskedWhereverTheyAppear() {
        assertEquals(
            "certificate for <server> rejected",
            maskServerAddresses("certificate for rss.example.org rejected", setOf("rss.example.org"))
        )
    }

    @Test
    fun stackFramesSurviveMasking() {
        val frame = "\tat okhttp3.internal.connection.RealConnection.connectSocket(RealConnection.kt:297)"

        assertEquals(frame, maskServerAddresses(frame))
    }
}
