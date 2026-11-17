package com.example.biblia.data.importer

import android.content.Context
import com.example.biblia.data.BibleVersion
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.io.PushbackInputStream

/** Removes a UTF-8 BOM (EF BB BF) from the start of a stream, if present. */
object BomStreams {

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

/** Reports read progress as a 0..1 fraction of [totalBytes]. */
class ProgressInputStream(
    input: InputStream,
    private val totalBytes: Long,
    private val onProgress: (Float) -> Unit,
) : FilterInputStream(input) {

    private var bytesRead = 0L

    override fun read(): Int {
        val r = super.read()
        if (r >= 0) {
            bytesRead++
            report()
        }
        return r
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val r = super.read(b, off, len)
        if (r > 0) {
            bytesRead += r
            report()
        }
        return r
    }

    private fun report() {
        onProgress((bytesRead.toDouble() / totalBytes).toFloat().coerceIn(0f, 1f))
    }
}

/** Copies an asset XML into the cache dir so byte-accurate progress is possible. */
fun Context.copyBibleAssetToCache(version: BibleVersion): File {
    val dir = File(cacheDir, "bible_imports").apply { mkdirs() }
    val target = File(dir, version.id + ".xml")
    assets.open(version.assetPath).use { input ->
        target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
    }
    return target
}
