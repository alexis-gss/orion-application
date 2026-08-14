package com.orion.app.cinema.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// ---------- Entities ----------

/** A followed work (movie, or an ongoing/upcoming TV show/anime) for the Planning tab. */
@Serializable
@Entity(tableName = "followed_items")
data class FollowedItem(
    @PrimaryKey val tmdbId: Int,
    val mediaType: String,               // "movie" | "tv"
    val title: String,
    val posterPath: String?,
    val releaseDate: String? = null,
    val nextAirDate: String? = null,     // ISO format yyyy-MM-dd, provided by TMDB
    val nextEpisodeName: String? = null,
    val nextSeasonNumber: Int? = null,
    val nextEpisodeNumber: Int? = null,
    val status: String? = null,
    val lastAiredSeasonNumber: Int? = null,  // season of the last episode that actually aired
    val lastAiredEpisodeNumber: Int? = null, // number of the last episode that actually aired
    val airedEpisodesCount: Int? = null, // total number of episodes aired so far (across every released season), for Bookmark's "remaining to watch" badge
    val lastCheckedAt: Long = 0L,        // timestamp of the last refresh (throttling)
    val genres: String? = null,          // comma-separated TMDB genres, for SeeAllScreen's genre filter
    val voteAverage: Double? = null,     // TMDB rating out of 10 at the time of following, for SeeAllScreen's rating filter
    val addedAt: Long = 0L               // epoch-millis timestamp of when it was followed, for SeeAllScreen's added-date filter
)

/** An item marked as watched: a movie, documentary, TV episode, or anime episode. */
@Serializable
@Entity(tableName = "watched_items")
data class WatchedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tmdbId: Int,
    val mediaType: String,           // "movie" | "tv"
    val title: String,
    val posterPath: String?,
    val seasonNumber: Int? = null,   // null for a movie
    val episodeNumber: Int? = null,  // null for a movie
    val episodeName: String? = null,
    val watchedAt: Long,             // epoch-millis timestamp
    val genres: String? = null,      // comma-separated TMDB genres (e.g. "Action,Drama"), for the Stats screen and SeeAllScreen's genre filter
    val durationMinutes: Int? = null, // TMDB runtime in minutes (movie or episode), for the estimated watch time
    val voteAverage: Double? = null, // TMDB rating out of 10 at the time it was watched, for SeeAllScreen's rating filter
    val releaseDate: String? = null  // TMDB release date (yyyy-MM-dd), for SeeAllScreen's release-date filter
)

/** A movie or TV show added to favorites (independent from following and watch history). */
@Serializable
@Entity(tableName = "favorite_items", primaryKeys = ["tmdbId", "mediaType"])
data class FavoriteItem(
    val tmdbId: Int,
    val mediaType: String,           // "movie" | "tv"
    val title: String,
    val posterPath: String?,
    val addedAt: Long,               // epoch-millis timestamp
    val genres: String? = null,      // comma-separated TMDB genres, for SeeAllScreen's genre filter
    val voteAverage: Double? = null, // TMDB rating out of 10 at the time it was added, for SeeAllScreen's rating filter
    val releaseDate: String? = null  // TMDB release date (yyyy-MM-dd), for SeeAllScreen's release-date filter
)

// ---------- DAO ----------

@Dao
interface FollowedItemDao {
    @Query("SELECT * FROM followed_items ORDER BY nextAirDate IS NULL, nextAirDate ASC")
    fun observeAll(): Flow<List<FollowedItem>>

    @Query("SELECT * FROM followed_items")
    suspend fun getAllOnce(): List<FollowedItem>

    @Query("SELECT * FROM followed_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType LIMIT 1")
    suspend fun find(tmdbId: Int, mediaType: String): FollowedItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FollowedItem)

    @Query("DELETE FROM followed_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun remove(tmdbId: Int, mediaType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FollowedItem>)

    @Query("DELETE FROM followed_items")
    suspend fun clearAllFollowedItems()
}

@Dao
interface WatchedItemDao {
    @Query("SELECT * FROM watched_items ORDER BY watchedAt DESC")
    fun observeAll(): Flow<List<WatchedItem>>

    @Query("SELECT * FROM watched_items")
    suspend fun getAllOnce(): List<WatchedItem>

    @Query("SELECT COUNT(*) FROM watched_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType AND (seasonNumber IS :seasonNumber) AND (episodeNumber IS :episodeNumber)")
    suspend fun countMatching(tmdbId: Int, mediaType: String, seasonNumber: Int?, episodeNumber: Int?): Int

    @Insert
    suspend fun insert(item: WatchedItem)

    @Query("DELETE FROM watched_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType AND (seasonNumber IS :seasonNumber) AND (episodeNumber IS :episodeNumber)")
    suspend fun deleteMatching(tmdbId: Int, mediaType: String, seasonNumber: Int?, episodeNumber: Int?)

    @Query("DELETE FROM watched_items")
    suspend fun clearAllWatchedItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WatchedItem>)
}

@Dao
interface FavoriteItemDao {
    @Query("SELECT * FROM favorite_items ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteItem>>

    @Query("SELECT * FROM favorite_items")
    suspend fun getAllOnce(): List<FavoriteItem>

    @Query("SELECT * FROM favorite_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType LIMIT 1")
    suspend fun find(tmdbId: Int, mediaType: String): FavoriteItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteItem)

    @Query("DELETE FROM favorite_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun remove(tmdbId: Int, mediaType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FavoriteItem>)

    @Query("DELETE FROM favorite_items")
    suspend fun clearAllFavoriteItems()
}

// ---------- Database ----------

/**
 * v1 -> v2: adds the genres/voteAverage/releaseDate/addedAt columns needed by
 * SeeAllScreen's filters (genre, rating, release date, added date). A real migration
 * (rather than fallbackToDestructiveMigration) so existing users' followed items/favorites
 * aren't wiped; the new columns stay null for rows already in the database (they'll simply
 * be absent from the relevant filters until the next refresh, without crashing or losing
 * the item).
 */
private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE followed_items ADD COLUMN genres TEXT")
        db.execSQL("ALTER TABLE followed_items ADD COLUMN voteAverage REAL")
        db.execSQL("ALTER TABLE followed_items ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE watched_items ADD COLUMN voteAverage REAL")
        db.execSQL("ALTER TABLE watched_items ADD COLUMN releaseDate TEXT")
        db.execSQL("ALTER TABLE favorite_items ADD COLUMN genres TEXT")
        db.execSQL("ALTER TABLE favorite_items ADD COLUMN voteAverage REAL")
        db.execSQL("ALTER TABLE favorite_items ADD COLUMN releaseDate TEXT")
        // Rows already followed before the migration have no known added date: fall back
        // to lastCheckedAt (already present) rather than 0, for at least an approximate
        // sort by added date instead of sending them all to the end of the list.
        db.execSQL("UPDATE followed_items SET addedAt = lastCheckedAt WHERE addedAt = 0")
    }
}

@Database(
    entities = [FollowedItem::class, WatchedItem::class, FavoriteItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followedItemDao(): FollowedItemDao
    abstract fun watchedItemDao(): WatchedItemDao
    abstract fun favoriteItemDao(): FavoriteItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** Returns the process-wide singleton, creating it on first access (double-checked locking). */
        fun getInstance(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orion.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
