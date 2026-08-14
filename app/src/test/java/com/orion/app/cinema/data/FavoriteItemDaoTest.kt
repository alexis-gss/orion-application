package com.orion.app.cinema.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [])
class FavoriteItemDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: FavoriteItemDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favoriteItemDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun ajouter_un_favori_puis_le_retrouver() = runBlocking {
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "movie", title = "Film Favori", posterPath = null, addedAt = System.currentTimeMillis()))

        val result = dao.find(tmdbId = 1, mediaType = "movie")

        assertEquals("Film Favori", result?.title)
    }

    @Test
    fun un_film_et_une_serie_avec_le_meme_tmdbId_sont_bien_distincts_en_favori() = runBlocking {
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "movie", title = "Film", posterPath = null, addedAt = 1000L))
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "tv", title = "Same-name Show", posterPath = null, addedAt = 2000L))

        val all = dao.getAllOnce()

        assertEquals(2, all.size)
        assertTrue(all.any { it.mediaType == "movie" })
        assertTrue(all.any { it.mediaType == "tv" })
    }

    @Test
    fun observeAll_trie_par_addedAt_decroissant_le_plus_recent_en_premier() = runBlocking {
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "movie", title = "Old", posterPath = null, addedAt = 1000L))
        dao.upsert(FavoriteItem(tmdbId = 2, mediaType = "movie", title = "Recent", posterPath = null, addedAt = 5000L))

        val all = dao.observeAll().first()

        assertEquals(listOf("Recent", "Old"), all.map { it.title })
    }

    @Test
    fun retirer_un_favori() = runBlocking {
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "movie", title = "Film", posterPath = null, addedAt = 1000L))
        dao.remove(tmdbId = 1, mediaType = "movie")

        assertTrue(dao.getAllOnce().isEmpty())
    }

    @Test
    fun retirer_un_favori_movie_ne_supprime_pas_le_favori_tv_homonyme() = runBlocking {
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "movie", title = "Film", posterPath = null, addedAt = 1000L))
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "tv", title = "Show", posterPath = null, addedAt = 2000L))

        dao.remove(tmdbId = 1, mediaType = "movie")
        val all = dao.getAllOnce()

        assertEquals(1, all.size)
        assertEquals("tv", all.first().mediaType)
    }

    @Test
    fun upsert_remplace_un_favori_existant_sans_le_dupliquer() = runBlocking {
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "movie", title = "V1", posterPath = null, addedAt = 1000L))
        dao.upsert(FavoriteItem(tmdbId = 1, mediaType = "movie", title = "V2", posterPath = null, addedAt = 2000L))

        val all = dao.getAllOnce()

        assertEquals(1, all.size)
        assertEquals("V2", all.first().title)
    }

    @Test
    fun clearAllFavoriteItems_vide_bien_la_table() = runBlocking {
        dao.upsertAll(listOf(
            FavoriteItem(tmdbId = 1, mediaType = "movie", title = "A", posterPath = null, addedAt = 1000L),
            FavoriteItem(tmdbId = 2, mediaType = "tv", title = "B", posterPath = null, addedAt = 2000L),
        ))

        dao.clearAllFavoriteItems()

        assertTrue(dao.getAllOnce().isEmpty())
    }
}