package com.orion.app.books.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrenchReleaseResolverTest {

    private class Fake(override val name: String, val result: FrenchRelease?) : FrenchReleaseSource {
        var calls = 0
        override suspend fun lookup(isbn13: String): FrenchRelease? { calls++; return result }
    }

    @Test fun decitreDateIsParsed() {
        val html = "<li>EAN9782344071120</li><li>Date de parution23/09/2026</li>"
        assertEquals(utcMidnight(2026, 9, 23), DecitreSource.parseParutionDate(html))
        assertEquals(utcMidnight(2026, 9, 23), DecitreSource.parseParutionDate("Date de parution : <b>23/09/2026</b>"))
        assertNull(DecitreSource.parseParutionDate("<p>rien</p>"))
    }

    @Test fun bnfYearIsParsed() {
        assertEquals(2019, BnfSruSource.parseYear("<dc:date>2019</dc:date>"))
        assertEquals(2019, BnfSruSource.parseYear("<dc:date>impr. 2019</dc:date>"))
        assertNull(BnfSruSource.parseYear("<x/>"))
    }

    @Test fun exactDateBeatsYearAndStopsChain() = runBlocking {
        val year = Fake("bnf", FrenchRelease(year = 2020, source = "bnf"))
        val exact = Fake("decitre", FrenchRelease(epochSeconds = 1L, source = "decitre"))
        val never = Fake("x", FrenchRelease(epochSeconds = 2L, source = "x"))
        val r = FrenchReleaseResolver(listOf(year, exact, never)).resolve("9782344071120")
        assertEquals("decitre", r?.source)
        assertEquals(0, never.calls)
    }

    @Test fun yearOnlyHintIsKeptWhenNoExactDate() = runBlocking {
        val r = FrenchReleaseResolver(listOf(Fake("d", null), Fake("bnf", FrenchRelease(year = 2018, source = "bnf"))))
            .resolve("9782344071120")
        assertEquals(2018, r?.year)
        assertNull(r?.epochSeconds)
    }

    @Test fun resultIsCachedAndBadIsbnRejected() = runBlocking {
        val f = Fake("d", FrenchRelease(epochSeconds = 5L, source = "d"))
        val resolver = FrenchReleaseResolver(listOf(f))
        resolver.resolve("9782344071120"); resolver.resolve("978-2-344-07112-0")
        assertEquals(1, f.calls)
        assertNull(resolver.resolve("123"))
    }

    @Test fun yearOnlyReleasedLogic() {
        fun book(y: Int?) = HardcoverBook("1","t",null,emptyList(),null,null,emptyList(),null,
            publishedTimestamp = null, frenchReleaseYear = y, year = null, seriesId = null, seriesName = null,
            seriesOrderNumber = null, primaryAuthorId = null, slug = null, ratingOn5 = null)
        assertTrue(book(2000).isReleasedInFrance)
        assertEquals(false, book(null).isReleasedInFrance)
        assertEquals(false, book(2999).isReleasedInFrance)
    }
}
