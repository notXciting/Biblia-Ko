package com.example.biblia.data.parser

import java.io.InputStream
import java.io.PushbackInputStream

/** Removes a UTF-8 BOM (EF BB BF) from the start of a stream, if present. */
object BomSupport {

    fun stripBom(input: InputStream): InputStream {
        val pushback = PushbackInputStream(input, 3)
        val head = ByteArray(3)
        var read = 0
        while (read < 3) {
            val n = pushback.read(head, read, 3 - read)
            if (n < 0) break
            read += n
        }
        val hasBom = read == 3 &&
            head[0] == 0xEF.toByte() &&
            head[1] == 0xBB.toByte() &&
            head[2] == 0xBF.toByte()
        if (!hasBom && read > 0) {
            pushback.unread(head, 0, read)
        }
        return pushback
    }
}