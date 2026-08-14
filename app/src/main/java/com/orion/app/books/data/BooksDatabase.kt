package com.orion.app.books.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// ---------- Entities ----------
// Separate database (books.db file), following the same principle as games.db: no state
// shared with cinema or games, only the common infrastructure (theme, networking).

/** A followed book (already published or upcoming) for the Planning tab. */
@Serializable
@Entity(tableName = "followed_books")
data class FollowedBook(
    @PrimaryKey val volumeId: String,
    val title: String,
    val coverUrl: String?,
    val releaseTimestamp: Long? = null,  // epoch seconds, known publication date
    val releaseLabel: String? = null,    // human-readable label ("2026", "June 18, 2026"...)
    val isReleased: Boolean = false,
    val lastCheckedAt: Long = 0L,        // throttles how often this item gets refreshed
    val categories: String? = null       // comma-separated Hardcover categories, for BooksLibraryScreen's genre filter
)

/** A book marked with a reading status. */
@Serializable
@Entity(tableName = "read_books")
data class ReadBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val volumeId: String,
    val title: String,
    val coverUrl: String?,
    val status: String,              // "to_read" | "reading" | "read" | "dropped"
    val updatedAt: Long,             // timestamp of the last status update
    val categories: String? = null,  // comma-separated Hardcover categories, for Stats
    val pageCount: Int? = null
)

/** A book added to favorites (independent from following and reading progress). */
@Serializable
@Entity(tableName = "favorite_books")
data class FavoriteBook(
    @PrimaryKey val volumeId: String,
    val title: String,
    val coverUrl: String?,
    val addedAt: Long,
    val categories: String? = null      // comma-separated Hardcover categories, for BooksSeeAllScreen's genre filter
)

// ---------- DAO ----------

@Dao
interface FollowedBookDao {
    @Query("SELECT * FROM followed_books ORDER BY releaseTimestamp IS NULL, releaseTimestamp ASC")
    fun observeAll(): Flow<List<FollowedBook>>

    @Query("SELECT * FROM followed_books")
    suspend fun getAllOnce(): List<FollowedBook>

    @Query("SELECT * FROM followed_books WHERE volumeId = :volumeId LIMIT 1")
    suspend fun find(volumeId: String): FollowedBook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FollowedBook)

    @Query("DELETE FROM followed_books WHERE volumeId = :volumeId")
    suspend fun remove(volumeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FollowedBook>)

    @Query("DELETE FROM followed_books")
    suspend fun clearAll()
}

@Dao
interface ReadBookDao {
    @Query("SELECT * FROM read_books ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ReadBook>>

    @Query("SELECT * FROM read_books")
    suspend fun getAllOnce(): List<ReadBook>

    @Query("SELECT * FROM read_books WHERE volumeId = :volumeId LIMIT 1")
    suspend fun find(volumeId: String): ReadBook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReadBook)

    @Query("DELETE FROM read_books WHERE volumeId = :volumeId")
    suspend fun remove(volumeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReadBook>)

    @Query("DELETE FROM read_books")
    suspend fun clearAll()
}

@Dao
interface FavoriteBookDao {
    @Query("SELECT * FROM favorite_books ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteBook>>

    @Query("SELECT * FROM favorite_books")
    suspend fun getAllOnce(): List<FavoriteBook>

    @Query("SELECT * FROM favorite_books WHERE volumeId = :volumeId LIMIT 1")
    suspend fun find(volumeId: String): FavoriteBook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteBook)

    @Query("DELETE FROM favorite_books WHERE volumeId = :volumeId")
    suspend fun remove(volumeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FavoriteBook>)

    @Query("DELETE FROM favorite_books")
    suspend fun clearAll()
}

// ---------- Database ----------

@Database(
    entities = [FollowedBook::class, ReadBook::class, FavoriteBook::class],
    // v1 -> v2: adds FollowedBook.categories and FavoriteBook.categories (genre filter).
    // Without this bump, Room keeps the schema fingerprint expected by the database
    // already installed on the device, detects the mismatch against the new columns on
    // first access, and throws a fatal IllegalStateException — hence the immediate crash
    // when opening the books planning screen (the first query against followed_books).
    // The bump triggers the destructive migration already configured below
    // (fallbackToDestructiveMigration).
    version = 2,
    exportSchema = false
)
abstract class BooksDatabase : RoomDatabase() {
    abstract fun followedBookDao(): FollowedBookDao
    abstract fun readBookDao(): ReadBookDao
    abstract fun favoriteBookDao(): FavoriteBookDao

    companion object {
        @Volatile private var INSTANCE: BooksDatabase? = null

        /** Returns the process-wide singleton, creating it on first access (double-checked locking). */
        fun getInstance(context: android.content.Context): BooksDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BooksDatabase::class.java,
                    "orion_books.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
