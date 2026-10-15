package com.readrops.api.localfeed

import java.io.InputStream
import java.io.PushbackInputStream

/**
 * Repairs the common HTML-style `<br>` element emitted inside Atom XHTML content.
 * The transformation is streaming and leaves every other byte unchanged.
 */
internal class LenientXmlInputStream(inputStream: InputStream) : InputStream() {

    private val input = PushbackInputStream(inputStream, 4)

    override fun read(): Int {
        val first = input.read()
        if (first != '<'.code) return first

        val lookahead = ByteArray(BREAK_TAG_TAIL.size)
        var count = 0
        while (count < lookahead.size) {
            val next = input.read()
            if (next == -1) break
            lookahead[count++] = next.toByte()
        }

        if (count == BREAK_TAG_TAIL.size && lookahead.contentEquals(BREAK_TAG_TAIL)) {
            input.unread(BREAK_TAG_REPLACEMENT_TAIL)
        } else if (count > 0) {
            input.unread(lookahead, 0, count)
        }

        return first
    }

    override fun close() = input.close()

    private companion object {
        val BREAK_TAG_TAIL = byteArrayOf('b'.code.toByte(), 'r'.code.toByte(), '>'.code.toByte())
        val BREAK_TAG_REPLACEMENT_TAIL = byteArrayOf(
            'b'.code.toByte(), 'r'.code.toByte(), '/'.code.toByte(), '>'.code.toByte()
        )
    }
}
