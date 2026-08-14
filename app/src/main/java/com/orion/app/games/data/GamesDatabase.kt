package com.orion.app.games.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// ---------- Entities ----------
// Separate database from cinema's (games.db file): the video games domain has no
// season/episode equivalent, but has its own concepts (release status, platform,
// "completed"/"playing" progress), hence different columns rather than a forced generic
// model.

/** A followed game (released or upcoming) for the video games Planning tab. */
@Serializable
@Entity(tableName = "followed_games")
data class FollowedGame(
    @PrimaryKey val igdbId: Int,
    val title: String,
    val coverUrl: String?,
    val releaseTimestamp: Long? = null,  // epoch seconds, known release date (game or DLC)
    val releaseLabel: String? = null,    // IGDB human-readable label ("Q4 2026", "Dec 12, 2026"...)
    val isReleased: Boolean = false,
    val lastCheckedAt: Long = 0L,        // throttles how often this item gets refreshed
    val genres: String? = null,          // comma-separated IGDB genres, for Bookmark/SeeAllScreen's genre filter
    val voteAverage: Double? = null,     // IGDB rating out of 100 at the time of following, for Bookmark/SeeAllScreen's rating filter
    val studio: String? = null           // developer studio (IgdbGame.developerNames) at the time of following, shown in place of the date on the Planning card
)

/** A game marked as played/completed, along with its progress. */
@Serializable
@Entity(tableName = "played_games")
data class PlayedGame(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val igdbId: Int,
    val title: String,
    val coverUrl: String?,
    val status: String,              // "playing" | "completed" | "dropped" | "backlog"
    val playedAt: Long,              // timestamp of the last status update
    val genres: String? = null,      // comma-separated IGDB genres, for Stats and Bookmark/SeeAllScreen's genre filter
    val hoursPlayed: Int? = null,    // estimated/user-entered play time, optional
    val voteAverage: Double? = null, // IGDB rating out of 100 at the time it was added, for Bookmark/SeeAllScreen's rating filter
    val releaseDate: Long? = null    // release date in epoch millis, for Bookmark/SeeAllScreen's release-date filter
)

/** A game added to favorites (independent from following and progress). */
@Serializable
@Entity(tableName = "favorite_games")
data class FavoriteGame(
    @PrimaryKey val igdbId: Int,
    val title: String,
    val coverUrl: String?,
    val addedAt: Long,
    val genres: String? = null,      // comma-separated IGDB genres, for Bookmark/SeeAllScreen's genre filter
    val voteAverage: Double? = null, // IGDB rating out of 100 at the time it was added, for Bookmark/SeeAllScreen's rating filter
    val releaseDate: Long? = null    // release date in epoch millis, for Bookmark/SeeAllScreen's release-date filter
)

// ---------- DAO ----------

@Dao
interface FollowedGameDao {
    @Query("SELECT * FROM followed_games ORDER BY releaseTimestamp IS NULL, releaseTimestamp ASC")
    fun observeAll(): Flow<List<FollowedGame>>

    @Query("SELECT * FROM followed_games")
    suspend fun getAllOnce(): List<FollowedGame>

    @Query("SELECT * FROM followed_games WHERE igdbId = :igdbId LIMIT 1")
    suspend fun find(igdbId: Int): FollowedGame?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FollowedGame)

    @Query("DELETE FROM followed_games WHERE igdbId = :igdbId")
    suspend fun remove(igdbId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FollowedGame>)

    @Query("DELETE FROM followed_games")
    suspend fun clearAll()
}

@Dao
interface PlayedGameDao {
    @Query("SELECT * FROM played_games ORDER BY playedAt DESC")
    fun observeAll(): Flow<List<PlayedGame>>

    @Query("SELECT * FROM played_games")
    suspend fun getAllOnce(): List<PlayedGame>

    @Query("SELECT * FROM played_games WHERE igdbId = :igdbId LIMIT 1")
    suspend fun find(igdbId: Int): PlayedGame?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlayedGame)

    @Query("DELETE FROM played_games WHERE igdbId = :igdbId")
    suspend fun remove(igdbId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlayedGame>)

    @Query("DELETE FROM played_games")
    suspend fun clearAll()
}

@Dao
interface FavoriteGameDao {
    @Query("SELECT * FROM favorite_games ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteGame>>

    @Query("SELECT * FROM favorite_games")
    suspend fun getAllOnce(): List<FavoriteGame>

    @Query("SELECT * FROM favorite_games WHERE igdbId = :igdbId LIMIT 1")
    suspend fun find(igdbId: Int): FavoriteGame?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteGame)

    @Query("DELETE FROM favorite_games WHERE igdbId = :igdbId")
    suspend fun remove(igdbId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FavoriteGame>)

    @Query("DELETE FROM favorite_games")
    suspend fun clearAll()
}

// ---------- Database ----------

/**
 * v1 -> v2: adds the genres/voteAverage/releaseDate columns needed by Bookmark/SeeAllScreen's
 * filters (genre, rating, release date, added date). A real migration (rather than
 * fallbackToDestructiveMigration) so existing users' library isn't wiped; the new columns
 * stay null for rows already in the database (simply absent from the relevant filters until
 * the next refresh).
 */
private val GAMES_MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE followed_games ADD COLUMN genres TEXT")
        db.execSQL("ALTER TABLE followed_games ADD COLUMN voteAverage REAL")
        db.execSQL("ALTER TABLE played_games ADD COLUMN voteAverage REAL")
        db.execSQL("ALTER TABLE played_games ADD COLUMN releaseDate INTEGER")
        db.execSQL("ALTER TABLE favorite_games ADD COLUMN genres TEXT")
        db.execSQL("ALTER TABLE favorite_games ADD COLUMN voteAverage REAL")
        db.execSQL("ALTER TABLE favorite_games ADD COLUMN releaseDate INTEGER")
    }
}

/**
 * v2 -> v3: adds the studio (developer) column, shown in place of the release date on
 * Planning's followed cards. Null for games already followed before this update; gets
 * populated on the next refresh (refreshFollowedIfStale), no explicit backfill needed.
 */
private val GAMES_MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE followed_games ADD COLUMN studio TEXT")
    }
}

@Database(
    entities = [FollowedGame::class, PlayedGame::class, FavoriteGame::class],
    version = 3,
    exportSchema = false
)
abstract class GamesDatabase : RoomDatabase() {
    abstract fun followedGameDao(): FollowedGameDao
    abstract fun playedGameDao(): PlayedGameDao
    abstract fun favoriteGameDao(): FavoriteGameDao

    companion object {
        @Volatile private var INSTANCE: GamesDatabase? = null

        /** Returns the process-wide singleton, creating it on first access (double-checked locking). */
        fun getInstance(context: android.content.Context): GamesDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GamesDatabase::class.java,
                    "orion_games.db"
                )
                    .addMigrations(GAMES_MIGRATION_1_2, GAMES_MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
