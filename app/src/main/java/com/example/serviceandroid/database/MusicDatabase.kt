package com.example.serviceandroid.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.serviceandroid.database.dao.FavouriteSongDao
import com.example.serviceandroid.model.Song

@Database(
    entities = [Song::class],
    version = MusicDatabase.VERSION,
    autoMigrations = [
        AutoMigration(
            from = MusicDatabase.VERSION - 1,
            to = MusicDatabase.VERSION,
            spec = MusicDatabase.SongsAutoMigrationSpec::class
        )
    ],
    exportSchema = true)

abstract class MusicDatabase : RoomDatabase() {
    abstract fun favouriteSongDao(): FavouriteSongDao

    @DeleteColumn.Entries(
        DeleteColumn(
            tableName = "songEntity",
            columnName = "uploaded"
        )
    )
    class SongsAutoMigrationSpec : AutoMigrationSpec

    companion object {
        const val VERSION = 5

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songEntity ADD COLUMN isDownloaded INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songEntity ADD COLUMN uploaded INTEGER NOT NULL DEFAULT 0")
            }
        }

    }
}