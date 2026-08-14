package com.orion.app.books.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fake [HardcoverApi] simulating the two-step search flow used by
 * [BooksRepository.search] (a `search(...) { ids }` call resolved via Typesense, followed
 * by a `books(where: {id: {_in: [...]}}) { ... }` call) — see the note on `search()` in
 * BooksRepository. Dispatches on the query text since Hardcover exposes a single GraphQL
 * endpoint rather than distinct REST paths.
 */
private class FakeHardcoverApi(private val books: List<HardcoverBookRaw>) : HardcoverApi {
    override suspend fun query(body: GraphQlRequest): GraphQlResponse {
        return when {
            body.query.contains("search(") -> GraphQlResponse(
                data = HardcoverData(search = HardcoverSearchResult(ids = books.map { it.id }))
            )
            body.query.contains("books(") -> GraphQlResponse(data = HardcoverData(books = books))
            else -> GraphQlResponse(data = HardcoverData())
        }
    }
}

/**
 * Covers the "search fails 1 time in 3 / ten different Berserk entries with a lost
 * title" fix: BooksRepository.search must filter out ghost entries (no exploitable
 * title) and deduplicate by id.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BooksRepositorySearchTest {

    private lateinit var db: BooksDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun rawBook(id: Int, title: String = "Berserk Tome 36") =
        HardcoverBookRaw(id = id, title = title, slug = "berserk-$id")

    @Test
    fun `search exclut les fiches sans titre exploitable`() = runBlocking {
        val results = listOf(rawBook(id = 1, title = "Berserk Tome 36"), rawBook(id = 2, title = ""))
        val repository = BooksRepository(FakeHardcoverApi(results), db)

        val filtered = repository.search("Berserk")

        assertEquals(1, filtered.size)
        assertEquals("1", filtered.first().id)
    }

    @Test
    fun `search deduplique les entrees ayant le meme id`() = runBlocking {
        val results = listOf(rawBook(id = 1), rawBook(id = 1), rawBook(id = 2))
        val repository = BooksRepository(FakeHardcoverApi(results), db)

        val filtered = repository.search("Berserk")

        assertEquals(2, filtered.size)
        assertEquals(setOf("1", "2"), filtered.map { it.id }.toSet())
    }

    @Test
    fun `search garde tous les volumes valides et distincts`() = runBlocking {
        val results = listOf(rawBook(id = 1), rawBook(id = 2), rawBook(id = 3))
        val repository = BooksRepository(FakeHardcoverApi(results), db)

        val filtered = repository.search("Berserk")

        assertTrue(filtered.size == 3)
    }

    @Test
    fun `search renvoie une liste vide pour une requete blanche`() = runBlocking {
        val repository = BooksRepository(FakeHardcoverApi(emptyList()), db)

        val filtered = repository.search("   ")

        assertTrue(filtered.isEmpty())
    }
}
