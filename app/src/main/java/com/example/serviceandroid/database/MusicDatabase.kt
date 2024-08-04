package com.example.serviceandroid.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.serviceandroid.database.dao.FavouriteSongDao

@Database(
    entities = [SongEntity::class],
    version = MusicDatabase.VERSION,
    autoMigrations = [
//        AutoMigration(
//            from = MusicDatabase.VERSION - 1,
//            to = MusicDatabase.VERSION,
//            spec = MusicDatabase.SongsAutoMigrationSpec::class
//        )
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
        const val VERSION = 2

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songEntity ADD COLUMN idSong INTEGER NOT NULL DEFAULT 0")
            }
        }

    }
}