package com.orion.app.books.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Result of a French release-date lookup for one ISBN.
 *
 * [epochSeconds] is an exact day (UTC midnight, same convention as
 * HardcoverBook.frenchReleaseTimestamp). [year] is only used when a source knows the
 * publication year but not the day (e.g. BnF): it can prove a book is already out
 * (year < current year) but never a precise date.
 */
data class FrenchRelease(
    val epochSeconds: Long? = null,
    val year: Int? = null,
    val source: String,
)

/** One source of French release dates, keyed by ISBN-13. Must never throw: return null on any failure. */
interface FrenchReleaseSource {
    val name: String
    suspend fun lookup(isbn13: String): FrenchRelease?
}

/**
 * Resolves the French release date of a book from its (French edition) ISBN, trying each
 * source in order and keeping the first hit.
 *
 * Uses its OWN OkHttp client: the Hardcover client carries an Authorization interceptor
 * with the user's personal token, which must never be sent to a third-party site.
 *
 * Only exact-day hits are cached long-term; a miss is cached briefly so the daily worker
 * and repeated detail-page opens don't hammer the sources.
 */
class FrenchReleaseResolver(
    private val sources: List<FrenchReleaseSource> = defaultSources(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private data class CacheEntry(val value: FrenchRelease?, val at: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    suspend fun resolve(isbn13: String): FrenchRelease? {
        val isbn = isbn13.filter { it.isDigit() }
        if (isbn.length != 13) return null

        val now = nowMillis()
        cache[isbn]?.let { entry ->
            val ttl = if (entry.value?.epochSeconds != null) HIT_TTL_MS else MISS_TTL_MS
            if (now - entry.at < ttl) return entry.value
        }

        var best: FrenchRelease? = null
        for (source in sources) {
            val r = try { source.lookup(isbn) } catch (e: Exception) { null } ?: continue
            if (r.epochSeconds != null) { best = r; break }        // exact day: stop here
            if (best == null) best = r                              // keep a year-only hint, keep looking
        }
        cache[isbn] = CacheEntry(best, now)
        return best
    }

    companion object {
        private const val HIT_TTL_MS = 12 * 60 * 60 * 1000L   // dates can still move: re-check twice a day at most
        private const val MISS_TTL_MS = 60 * 60 * 1000L

        internal val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }

        fun defaultSources(): List<FrenchReleaseSource> = listOf(DecitreSource(client), BnfSruSource(client))
    }
}

// ---------------------------------------------------------------------------------------
// Source 1: Decitre — exact day, including upcoming books ("À paraître").
// No public API: the product page is fetched by EAN and the "Date de parution" row is
// read. Decitre resolves /livres/<any-slug>-<ean>.html by the trailing EAN and redirects
// to the canonical URL (observed with a stale slug), which is what makes an ISBN-only
// lookup possible. HTML scraping is inherently fragile — if the markup changes this
// source just returns null and the chain falls through to the next one.
// ---------------------------------------------------------------------------------------
class DecitreSource(private val client: OkHttpClient) : FrenchReleaseSource {
    override val name = "decitre"

    override suspend fun lookup(isbn13: String): FrenchRelease? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://www.decitre.fr/livres/isbn-$isbn13.html")
                .header("User-Agent", "Orion-App (Android, personal use)")
                .header("Accept-Language", "fr-FR,fr;q=0.9")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val html = response.body?.string() ?: return@use null
                // The page must actually be THIS book, not a "not found"/search fallback.
                if (!html.contains(isbn13)) return@use null
                parseParutionDate(html)?.let { FrenchRelease(epochSeconds = it, source = name) }
            }
        }

    companion object {
        // "Date de parution" followed (through tags/whitespace/colon) by dd/MM/yyyy
        private val DATE_REGEX = Regex("""Date de parution(?:\s|:|&nbsp;|<[^>]*>)*(\d{2})/(\d{2})/(\d{4})""")

        internal fun parseParutionDate(html: String): Long? {
            val m = DATE_REGEX.find(html) ?: return null
            return utcMidnight(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
        }
    }
}

// ---------------------------------------------------------------------------------------
// Source 2: BnF (catalogue général, SRU, open data). Official and stable, but it only knows
// books after legal deposit and generally exposes the publication YEAR, not the day. Its
// value here is proving that older books ARE released (so they stop lingering in Planning).
// ---------------------------------------------------------------------------------------
class BnfSruSource(private val client: OkHttpClient) : FrenchReleaseSource {
    override val name = "bnf"

    override suspend fun lookup(isbn13: String): FrenchRelease? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = okhttp3.HttpUrl.Builder()
                .scheme("https").host("catalogue.bnf.fr").addPathSegments("api/SRU")
                .addQueryParameter("version", "1.2")
                .addQueryParameter("operation", "searchRetrieve")
                .addQueryParameter("query", "bib.isbn all \"$isbn13\"")
                .addQueryParameter("recordSchema", "dublincore")
                .addQueryParameter("maximumRecords", "1")
                .build()
            val request = Request.Builder().url(url)
                .header("User-Agent", "Orion-App (Android, personal use)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val xml = response.body?.string() ?: return@use null
                parseYear(xml)?.let { FrenchRelease(year = it, source = name) }
            }
        }

    companion object {
        private val DATE_TAG = Regex("""<dc:date[^>]*>\s*([^<]+?)\s*</dc:date>""")
        private val YEAR = Regex("""(1[5-9]\d{2}|20\d{2})""")

        internal fun parseYear(xml: String): Int? {
            val raw = DATE_TAG.find(xml)?.groupValues?.get(1) ?: return null
            return YEAR.find(raw)?.value?.toIntOrNull()
        }
    }
}

/** yyyy-MM-dd → epoch seconds at UTC midnight (Calendar, not java.time: see HardcoverApi.kt note on API 26). */
internal fun utcMidnight(year: Int, month: Int, day: Int): Long? = try {
    if (month !in 1..12 || day !in 1..31) null else {
        val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        c.clear()
        c.set(year, month - 1, day, 0, 0, 0)
        c.timeInMillis / 1000L
    }
} catch (e: Exception) { null }
