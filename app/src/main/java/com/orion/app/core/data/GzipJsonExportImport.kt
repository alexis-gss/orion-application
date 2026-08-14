package com.orion.app.core.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Shared gzip+JSON snapshot writer/reader, factored out of the cinema/games/books export
 * managers which previously duplicated this exact read/write logic — only the bundle's
 * [KSerializer] and a couple of user-facing strings ever differed between them.
 *
 * Format choice (documented here once instead of three times): JSON for forward/backward
 * compatibility across app versions (readable, versionable schema via each bundle's own
 * `version` field), compressed with GZIP for compactness (typically 5x-10x smaller on this
 * kind of repetitive JSON).
 */
object GzipJsonExportImport {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Serializes [bundle] to JSON, gzips it, and writes it to [uri] (a SAF destination the caller already resolved via a document-creation intent). */
    fun <T> writeToUri(context: Context, uri: Uri, serializer: KSerializer<T>, bundle: T) {
        val text = json.encodeToString(serializer, bundle)
        context.contentResolver.openOutputStream(uri)?.use { rawOut ->
            GZIPOutputStream(rawOut).use { gzOut ->
                gzOut.write(text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    /** Reads a gzip+JSON snapshot from [uri] and decodes it with [serializer]. Throws [IllegalStateException] with [readErrorMessage] if the stream can't be opened at all. */
    fun <T> readFromUri(context: Context, uri: Uri, serializer: KSerializer<T>, readErrorMessage: String): T {
        context.contentResolver.openInputStream(uri)?.use { rawIn ->
            GZIPInputStream(rawIn).use { gzIn ->
                val text = BufferedReader(InputStreamReader(gzIn, Charsets.UTF_8)).readText()
                return json.decodeFromString(serializer, text)
            }
        }
        throw IllegalStateException(readErrorMessage)
    }

    /** Builds a timestamped default file name: `<prefix>-yyyyMMdd-HHmmss.<extension>`. */
    fun defaultFileName(prefix: String, extension: String): String {
        val ts = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date())
        return "$prefix-$ts.$extension"
    }
}
