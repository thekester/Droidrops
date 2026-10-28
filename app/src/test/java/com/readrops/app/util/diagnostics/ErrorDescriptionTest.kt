package com.readrops.app.util.diagnostics

import com.readrops.api.utils.exceptions.HttpException
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ErrorDescriptionTest {

    @Test
    fun kotlinNullAssertionIsDescribedByItsType() {
        // `!!` throws a NullPointerException that carries no message at all
        val exception = runCatching { (null as String?)!! }.exceptionOrNull()!!

        assertEquals("NullPointerException", exception.readableMessage())
    }

    @Test
    fun messageLessExceptionFallsBackToItsCause() {
        val exception = IOException(null as String?, ConnectException("Connection refused"))

        assertEquals("Connection refused", exception.readableMessage())
    }

    @Test
    fun presentMessageIsKept() {
        assertEquals("timeout", SocketTimeoutException("timeout").readableMessage())
    }

    @Test
    fun blankMessageCountsAsMissing() {
        assertEquals("IllegalStateException", IllegalStateException("  ").readableMessage())
    }

    @Test
    fun descriptionNamesTheTypeAndAddsTheMessageWhenThereIsOne() {
        assertEquals("IllegalStateException: boom", IllegalStateException("boom").readableDescription())
        assertEquals("NullPointerException", NullPointerException().readableDescription())
    }

    @Test
    fun explicitNonStandardPortIsReported() {
        assertEquals(8443, nonStandardPort("https://rss.example.org:8443/"))
        assertEquals(8080, nonStandardPort("http://nas:8080/api/greader.php/"))
    }

    @Test
    fun defaultPortsAreNotReported() {
        assertNull(nonStandardPort("https://rss.example.org/"))
        assertNull(nonStandardPort("https://rss.example.org:443/"))
        assertNull(nonStandardPort("http://rss.example.org:80/feed"))
    }

    @Test
    fun missingOrMalformedUrlIsIgnored() {
        assertNull(nonStandardPort(null))
        assertNull(nonStandardPort("not a url"))
    }

    @Test
    fun connectionFailuresSuggestABlockedConnection() {
        assertTrue(SocketTimeoutException().suggestsBlockedConnection())
        assertTrue(ConnectException("Connection refused").suggestsBlockedConnection())
        assertTrue(NoRouteToHostException().suggestsBlockedConnection())
        // OkHttp's call timeout
        assertTrue(InterruptedIOException("timeout").suggestsBlockedConnection())
        assertTrue(IOException("wrapped", SocketTimeoutException()).suggestsBlockedConnection())
    }

    @Test
    fun otherFailuresDoNotSuggestABlockedConnection() {
        assertFalse(UnknownHostException("rss.example.org").suggestsBlockedConnection())
        assertFalse(SSLHandshakeException("handshake").suggestsBlockedConnection())
        assertFalse(IOException("unexpected end of stream").suggestsBlockedConnection())
        assertFalse(NullPointerException().suggestsBlockedConnection())
    }

    @Test
    fun networkDiagnosisDistinguishesRefusedConnectionFromMissingInternet() {
        val diagnosis = ConnectException("Connection refused").networkFailureDiagnosis(
            "mobile data, internet validated, metered"
        )

        assertTrue(diagnosis.contains("TCP connection was refused"))
        assertTrue(diagnosis.contains("firewall"))
        assertTrue(diagnosis.contains("does not indicate a Droidrops bug"))
    }

    @Test
    fun networkDiagnosisExplainsDnsFailures() {
        val diagnosis = UnknownHostException("rss.example.org").networkFailureDiagnosis("Wi-Fi, internet validated")

        assertTrue(diagnosis.contains("could not be resolved (DNS)"))
    }

    @Test
    fun networkDiagnosisDoesNotOverstateTheCauseOfTimeouts() {
        val diagnosis = SocketTimeoutException("timeout").networkFailureDiagnosis("Wi-Fi, internet validated")

        assertTrue(diagnosis.contains("cannot distinguish those causes"))
    }

    @Test
    fun networkDiagnosisIdentifiesAnHttpResponseAsReachingTheServer() {
        val response = Response.Builder()
            .request(Request.Builder().url("https://rss.example.org/feed").build())
            .protocol(Protocol.HTTP_1_1)
            .code(503)
            .message("Service Unavailable")
            .build()

        val diagnosis = HttpException(response).networkFailureDiagnosis("Wi-Fi, internet validated")

        assertTrue(diagnosis.contains("HTTP 503"))
        assertTrue(diagnosis.contains("request reached the server"))
    }
}
