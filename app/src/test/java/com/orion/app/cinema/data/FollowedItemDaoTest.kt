package com.orion.app.cinema.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.orion.app.cinema.data.AppDatabase
import com.orion.app.cinema.data.FollowedItem
import com.orion.app.cinema.data.FollowedItemDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [])
class FollowedItemDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: FollowedItemDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.followedItemDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun un_film_suivi_apparait_bien_dans_observeAll_au_meme_titre_qu_une_serie() = runBlocking {
        dao.upsert(FollowedItem(tmdbId = 1, mediaType = "movie", title = "Film Test", posterPath = null, releaseDate = "2026-08-07", nextAirDate = "2026-08-07"))
        dao.upsert(FollowedItem(tmdbId = 2, mediaType = "tv", title = "Test Show", posterPath = null, nextAirDate = "2026-08-10"))

        val all = dao.observeAll().first()

        assertEquals(2, all.size)
        assertTrue(all.any { it.mediaType == "movie" && it.nextAirDate != null })
        assertTrue(all.any { it.mediaType == "tv" && it.nextAirDate != null })
    }

    @Test
    fun observeAll_trie_par_nextAirDate_croissant_et_relegue_les_null_a_la_fin() = runBlocking {
        dao.upsertAll(listOf(
            FollowedItem(tmdbId = 1, mediaType = "tv", title = "C", posterPath = null, nextAirDate = "2026-09-01"),
            FollowedItem(tmdbId = 2, mediaType = "movie", title = "A", posterPath = null, nextAirDate = "2026-08-01"),
            FollowedItem(tmdbId = 3, mediaType = "tv", title = "SansDate", posterPath = null, nextAirDate = null),
            FollowedItem(tmdbId = 4, mediaType = "tv", title = "B", posterPath = null, nextAirDate = "2026-08-15"),
        ))

        val all = dao.observeAll().first()

        assertEquals(listOf("A", "B", "C", "SansDate"), all.map { it.title })
    }

    @Test
    fun upsert_remplace_un_item_existant_sans_le_dupliquer() = runBlocking {
        dao.upsert(FollowedItem(tmdbId = 1, mediaType = "movie", title = "V1", posterPath = null))
        dao.upsert(FollowedItem(tmdbId = 1, mediaType = "movie", title = "V2", posterPath = null))

        val all = dao.getAllOnce()

        assertEquals(1, all.size)
        assertEquals("V2", all.first().title)
    }

    @Test
    fun remove_supprime_bien_l_item_suivi() = runBlocking {
        dao.upsert(FollowedItem(tmdbId = 1, mediaType = "movie", title = "Film", posterPath = null))
        dao.remove(tmdbId = 1, mediaType = "movie")

        assertTrue(dao.getAllOnce().isEmpty())
    }

    @Test
    fun find_renvoie_null_si_l_item_n_existe_pas() = runBlocking {
        val result = dao.find(tmdbId = 999, mediaType = "movie")

        assertEquals(null, result)
    }

    @Test
    fun find_renvoie_l_item_correspondant_au_couple_tmdbId_mediaType() = runBlocking {
        dao.upsert(FollowedItem(tmdbId = 1, mediaType = "movie", title = "Film", posterPath = null))
        dao.upsert(FollowedItem(tmdbId = 1, mediaType = "tv", title = "Same-name Show", posterPath = null))

        val result = dao.find(tmdbId = 1, mediaType = "tv")

        assertEquals("Same-name Show", result?.title)
    }

    @Test
    fun clearAllFollowedItems_vide_bien_la_table() = runBlocking {
        dao.upsertAll(listOf(
            FollowedItem(tmdbId = 1, mediaType = "movie", title = "A", posterPath = null),
            FollowedItem(tmdbId = 2, mediaType = "tv", title = "B", posterPath = null),
        ))

        dao.clearAllFollowedItems()

        assertTrue(dao.getAllOnce().isEmpty())
    }
}