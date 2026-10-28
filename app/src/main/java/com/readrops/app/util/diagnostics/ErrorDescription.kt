package com.readrops.app.util.diagnostics

import com.readrops.api.utils.exceptions.HttpException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URI
import javax.net.ssl.SSLException

/**
 * A message fit for display, never "null". Kotlin's `!!` and many network failures throw
 * exceptions without a message, and interpolating `exception.message` printed the word null.
 * Falls back to the first cause that has a message, then to the exception type.
 */
fun Throwable.readableMessage(): String {
    var current: Throwable? = this
    while (current != null) {
        val message = current.message
        if (!message.isNullOrBlank()) return message
        current = current.cause?.takeIf { it !== current }
    }

    return javaClass.simpleName
}

/** The exception type, followed by its [readableMessage] when that says more than the type. */
fun Throwable.readableDescription(): String {
    val type = javaClass.simpleName
    val message = readableMessage()

    return if (message == type) type else "$type: $message"
}

/**
 * The port of [url] when it is not the default for its scheme. Public, hospital and company
 * networks commonly let only 80 and 443 through, so an instance served on 8443 syncs at home
 * and times out there. Knowing the port lets the error say so instead of a generic failure.
 */
fun nonStandardPort(url: String?): Int? {
    val uri = url?.let { runCatching { URI(it.trim()) }.getOrNull() } ?: return null
    val port = uri.port.takeIf { it != -1 } ?: return null

    val defaultPort = when (uri.scheme?.lowercase()) {
        "http" -> 80
        "https" -> 443
        else -> return null
    }

    return port.takeIf { it != defaultPort }
}

/**
 * Whether the failure looks like a network that drops or refuses the connection, as opposed
 * to a DNS, TLS or protocol error. A firewall that silently drops packets produces a timeout,
 * one that rejects them produces a refused connection or no route.
 */
fun Throwable.suggestsBlockedConnection(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        val blocked = when (current) {
            is ConnectException, is NoRouteToHostException, is PortUnreachableException -> true
            // SocketTimeoutException, and OkHttp's call timeout which only says "timeout"
            is InterruptedIOException -> true
            else -> false
        }
        if (blocked) return true
        current = current.cause?.takeIf { it !== current }
    }

    return false
}

/**
 * A plain-language interpretation for a diagnostic report. It describes what the failed
 * request proves, without claiming that the server or the app is at fault when it cannot tell.
 */
fun Throwable.networkFailureDiagnosis(network: String): String {
    val causes = generateSequence(this) { current ->
        current.cause?.takeIf { cause -> cause !== current }
    }.toList()
    val httpException = causes.filterIsInstance<HttpException>().firstOrNull()

    return when {
        httpException != null && httpException.code in 500..599 ->
            "The server returned HTTP ${httpException.code}. " +
                    "The request reached the server; it reported a server-side error."

        httpException != null ->
            "The server returned HTTP ${httpException.code}. " +
                    "The request reached the server; check the feed URL, credentials, or API support."

        causes.any { it is UnknownHostException } ->
            "The server hostname could not be resolved (DNS). Check the address and the network's DNS service."

        causes.any { it is ConnectException } ->
            "The TCP connection was refused before an HTTP response. The server or a firewall rejected the port. " +
                    "Check that the server is running, listening for remote connections, and that the port is open. " +
                    "This result alone does not indicate a Droidrops bug."

        causes.any { it is NoRouteToHostException || it is PortUnreachableException } ->
            "The device could not route a connection to the server. Check server reachability, VPN, and firewall rules."

        causes.any { it is SocketTimeoutException || it is InterruptedIOException } ->
            "No response arrived before the timeout. The server may be slow or unreachable, or a network/firewall " +
                    "may be dropping traffic; this log cannot distinguish those causes."

        causes.any { it is SSLException } ->
            "The connection reached TLS negotiation but secure setup failed. Check the server certificate and TLS configuration."

        network == "offline" ->
            "Android reported no active network when the request failed."

        else -> "The request failed before a usable response. See the exception details below."
    }
}
