package com.orion.app.books.data

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

/** Mirrors com.orion.app.data.FavoriteItemDaoTest (cinema domain), adapted to FavoriteBook's single-key (volumeId, a String) schema. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [])
class FavoriteBookDaoTest {
    private lateinit var db: BooksDatabase
    private lateinit var dao: FavoriteBookDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favoriteBookDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun ajouter_un_favori_puis_le_retrouver() = runBlocking {
        dao.upsert(FavoriteBook(volumeId = "abc123", title = "Berserk Tome 36", coverUrl = null, addedAt = System.currentTimeMillis()))

        val result = dao.find(volumeId = "abc123")

        assertEquals("Berserk Tome 36", result?.title)
    }

    @Test
    fun observeAll_trie_par_addedAt_decroissant_le_plus_recent_en_premier() = runBlocking {
        dao.upsert(FavoriteBook(volumeId = "a", title = "Old", coverUrl = null, addedAt = 1000L))
        dao.upsert(FavoriteBook(volumeId = "b", title = "Recent", coverUrl = null, addedAt = 5000L))

        val all = dao.observeAll().first()

        assertEquals(listOf("Recent", "Old"), all.map { it.title })
    }

    @Test
    fun retirer_un_favori() = runBlocking {
        dao.upsert(FavoriteBook(volumeId = "a", title = "Livre", coverUrl = null, addedAt = 1000L))
        dao.remove(volumeId = "a")

        assertTrue(dao.getAllOnce().isEmpty())
    }

    @Test
    fun upsert_remplace_un_favori_existant_sans_le_dupliquer() = runBlocking {
        dao.upsert(FavoriteBook(volumeId = "a", title = "V1", coverUrl = null, addedAt = 1000L))
        dao.upsert(FavoriteBook(volumeId = "a", title = "V2", coverUrl = null, addedAt = 2000L))

        val all = dao.getAllOnce()

        assertEquals(1, all.size)
        assertEquals("V2", all.first().title)
    }

    @Test
    fun find_renvoie_null_pour_un_volumeId_absent() = runBlocking {
        assertEquals(null, dao.find(volumeId = "inexistant"))
    }
}
