package com.orion.app.games.data

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

/** Mirrors com.orion.app.data.FavoriteItemDaoTest (cinema domain), adapted to FavoriteGame's single-key (igdbId) schema — games have no movie/tv media-type ambiguity to test for. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [])
class FavoriteGameDaoTest {
    private lateinit var db: GamesDatabase
    private lateinit var dao: FavoriteGameDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GamesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favoriteGameDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun ajouter_un_favori_puis_le_retrouver() = runBlocking {
        dao.upsert(FavoriteGame(igdbId = 1091, title = "Cyberpunk 2077", coverUrl = null, addedAt = System.currentTimeMillis()))

        val result = dao.find(igdbId = 1091)

        assertEquals("Cyberpunk 2077", result?.title)
    }

    @Test
    fun observeAll_trie_par_addedAt_decroissant_le_plus_recent_en_premier() = runBlocking {
        dao.upsert(FavoriteGame(igdbId = 1, title = "Old", coverUrl = null, addedAt = 1000L))
        dao.upsert(FavoriteGame(igdbId = 2, title = "Recent", coverUrl = null, addedAt = 5000L))

        val all = dao.observeAll().first()

        assertEquals(listOf("Recent", "Old"), all.map { it.title })
    }

    @Test
    fun retirer_un_favori() = runBlocking {
        dao.upsert(FavoriteGame(igdbId = 1, title = "Jeu", coverUrl = null, addedAt = 1000L))
        dao.remove(igdbId = 1)

        assertTrue(dao.getAllOnce().isEmpty())
    }

    @Test
    fun upsert_remplace_un_favori_existant_sans_le_dupliquer() = runBlocking {
        dao.upsert(FavoriteGame(igdbId = 1, title = "V1", coverUrl = null, addedAt = 1000L))
        dao.upsert(FavoriteGame(igdbId = 1, title = "V2", coverUrl = null, addedAt = 2000L))

        val all = dao.getAllOnce()

        assertEquals(1, all.size)
        assertEquals("V2", all.first().title)
    }

    @Test
    fun find_renvoie_null_pour_un_igdbId_absent() = runBlocking {
        assertEquals(null, dao.find(igdbId = 999))
    }
}
