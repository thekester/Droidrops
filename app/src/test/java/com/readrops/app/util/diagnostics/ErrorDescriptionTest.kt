package com.readrops.app.util.diagnostics

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
}
