package com.readrops.app.util.diagnostics

import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.URI

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
