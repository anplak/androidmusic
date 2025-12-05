package com.anplak.androidmusic.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        TrackStatsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun trackStatsDao(): TrackStatsDao

    companion object {
        private const val DATABASE_NAME = "music_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to 2: adds track_stats table and firstSeenAt column.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add track_stats table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS track_stats (
                        trackId INTEGER PRIMARY KEY NOT NULL,
                        playCount INTEGER NOT NULL DEFAULT 0,
                        lastPlayedAt INTEGER,
                        completionCount INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(trackId) REFERENCES tracks(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_stats_trackId ON track_stats(trackId)")
                
                // Add firstSeenAt column to tracks table with current timestamp as default
                db.execSQL("ALTER TABLE tracks ADD COLUMN firstSeenAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}

