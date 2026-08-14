package com.orion.app.cinema.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.orion.app.cinema.data.AppDatabase
import com.orion.app.cinema.data.WatchedItem
import com.orion.app.cinema.data.WatchedItemDao
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
class WatchedItemDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: WatchedItemDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.watchedItemDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun marquer_un_episode_vu_puis_le_retrouver_via_countMatching() = runBlocking {
        dao.insert(WatchedItem(tmdbId = 100, mediaType = "tv", title = "Silo", posterPath = null, seasonNumber = 3, episodeNumber = 6, watchedAt = System.currentTimeMillis()))

        val count = dao.countMatching(tmdbId = 100, mediaType = "tv", seasonNumber = 3, episodeNumber = 6)

        assertEquals(1, count)
    }

    @Test
    fun marquer_un_film_vu_avec_saison_episode_null() = runBlocking {
        dao.insert(WatchedItem(tmdbId = 200, mediaType = "movie", title = "Film", posterPath = null, seasonNumber = null, episodeNumber = null, watchedAt = System.currentTimeMillis()))

        val count = dao.countMatching(tmdbId = 200, mediaType = "movie", seasonNumber = null, episodeNumber = null)

        assertEquals(1, count)
    }

    @Test
    fun demarquer_un_episode_vu() = runBlocking {
        dao.insert(WatchedItem(tmdbId = 100, mediaType = "tv", title = "Silo", posterPath = null, seasonNumber = 3, episodeNumber = 6, watchedAt = System.currentTimeMillis()))
        dao.deleteMatching(tmdbId = 100, mediaType = "tv", seasonNumber = 3, episodeNumber = 6)

        val count = dao.countMatching(tmdbId = 100, mediaType = "tv", seasonNumber = 3, episodeNumber = 6)

        assertEquals(0, count)
    }

    @Test
    fun observeAll_trie_par_watchedAt_decroissant_le_plus_recent_en_premier() = runBlocking {
        dao.insert(WatchedItem(tmdbId = 1, mediaType = "movie", title = "Old", posterPath = null, watchedAt = 1000L))
        dao.insert(WatchedItem(tmdbId = 2, mediaType = "movie", title = "Recent", posterPath = null, watchedAt = 5000L))
        dao.insert(WatchedItem(tmdbId = 3, mediaType = "movie", title = "Middle", posterPath = null, watchedAt = 3000L))

        val all = dao.observeAll().first()

        assertEquals(listOf("Recent", "Middle", "Old"), all.map { it.title })
    }

    @Test
    fun countMatching_distingue_deux_episodes_differents_de_la_meme_serie() = runBlocking {
        dao.insert(WatchedItem(tmdbId = 100, mediaType = "tv", title = "Silo", posterPath = null, seasonNumber = 3, episodeNumber = 6, watchedAt = 1000L))

        val countEp6 = dao.countMatching(tmdbId = 100, mediaType = "tv", seasonNumber = 3, episodeNumber = 6)
        val countEp7 = dao.countMatching(tmdbId = 100, mediaType = "tv", seasonNumber = 3, episodeNumber = 7)

        assertEquals(1, countEp6)
        assertEquals(0, countEp7)
    }

    @Test
    fun clearAllWatchedItems_vide_bien_la_table() = runBlocking {
        dao.insert(WatchedItem(tmdbId = 1, mediaType = "movie", title = "A", posterPath = null, watchedAt = 1000L))
        dao.insert(WatchedItem(tmdbId = 2, mediaType = "movie", title = "B", posterPath = null, watchedAt = 2000L))

        dao.clearAllWatchedItems()

        assertTrue(dao.getAllOnce().isEmpty())
    }
}