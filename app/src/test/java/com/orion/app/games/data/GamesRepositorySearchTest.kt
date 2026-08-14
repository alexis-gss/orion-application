package com.orion.app.games.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fake [IgdbApi] returning a fixed list of games regardless of the request body: since
 * the apicalypse query is a plain text blob (not structured params), asserting on filtering
 * behaviour is more useful done client-side (in GamesRepository.search) than by trying to
 * parse the query string here.
 */
private class FakeIgdbApi(private val results: List<IgdbGame>) : IgdbApi {
    override suspend fun searchGames(query: RequestBody): List<IgdbGame> = results
    override suspend fun getGamesByIds(query: RequestBody): List<IgdbGame> = results
    override suspend fun getPopularGames(query: RequestBody): List<IgdbGame> = results
    override suspend fun getUpcomingGames(query: RequestBody): List<IgdbGame> = results
    override suspend fun getGameDetail(query: RequestBody): List<IgdbGame> = results
    override suspend fun getGamesByFranchise(query: RequestBody): List<IgdbGame> = results
    override suspend fun getPopularityPrimitives(query: RequestBody): List<IgdbPopularityPrimitive> = emptyList()
    override suspend fun getTimeToBeat(query: RequestBody): List<IgdbTimeToBeat> = emptyList()
}

/**
 * Covers the "Cyberpunk 2077: Phantom Liberty / 2.0 Update / Ultimate Edition still show up
 * in search" fix: GamesRepository.search must keep only entries with no parent_game and no
 * version_parent, regardless of what IGDB's (often unreliable) `category` field says.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GamesRepositorySearchTest {

    private lateinit var db: GamesDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GamesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun baseGame(id: Int, name: String, parentGame: Int? = null, versionParent: Int? = null, category: Int? = 0) =
        IgdbGame(id = id, name = name, category = category, parentGame = parentGame, versionParent = versionParent)

    @Test
    fun `search exclut les entrees avec un parent_game meme si category vaut 0`() = runBlocking {
        val results = listOf(
            baseGame(id = 1, name = "Cyberpunk 2077"),
            // Category = 0 (main_game) is misreported by IGDB, but parent_game reveals
            // this is really an expansion: must be excluded despite category.
            baseGame(id = 2, name = "Cyberpunk 2077: Phantom Liberty", parentGame = 1, category = 0),
        )
        val repository = GamesRepository(FakeIgdbApi(results), db)

        val filtered = repository.search("Cyberpunk 2077")

        assertEquals(1, filtered.size)
        assertEquals("Cyberpunk 2077", filtered.first().name)
    }

    @Test
    fun `search exclut aussi les entrees avec un version_parent`() = runBlocking {
        val results = listOf(
            baseGame(id = 1, name = "Cyberpunk 2077"),
            baseGame(id = 3, name = "Cyberpunk 2077: Ultimate Edition", versionParent = 1),
        )
        val repository = GamesRepository(FakeIgdbApi(results), db)

        val filtered = repository.search("Cyberpunk 2077")

        assertTrue(filtered.none { it.name.contains("Ultimate Edition") })
    }

    @Test
    fun `search garde un jeu de base sans parent_game ni version_parent`() = runBlocking {
        val results = listOf(baseGame(id = 1, name = "Cyberpunk 2077"))
        val repository = GamesRepository(FakeIgdbApi(results), db)

        val filtered = repository.search("Cyberpunk 2077")

        assertEquals(1, filtered.size)
    }

    @Test
    fun `search renvoie une liste vide si tous les resultats sont des editions liees`() = runBlocking {
        val results = listOf(
            baseGame(id = 2, name = "DLC A", parentGame = 1),
            baseGame(id = 3, name = "Edition B", versionParent = 1),
        )
        val repository = GamesRepository(FakeIgdbApi(results), db)

        val filtered = repository.search("Cyberpunk 2077")

        assertTrue(filtered.isEmpty())
    }
}
