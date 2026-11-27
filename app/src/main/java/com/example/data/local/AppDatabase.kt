package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        StrategyLogEntity::class,
        VideoClipEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao
    abstract fun videoClipDao(): VideoClipDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun createVideoClipsTable(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `video_generations`")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `video_clips` (
                    `id` TEXT NOT NULL,
                    `prompt` TEXT NOT NULL,
                    `mode` TEXT NOT NULL,
                    `model` TEXT NOT NULL,
                    `ratio` TEXT NOT NULL,
                    `segmentDurationSec` INTEGER NOT NULL,
                    `totalDurationSec` INTEGER NOT NULL,
                    `videoPath` TEXT NOT NULL,
                    `thumbPath` TEXT NOT NULL,
                    `lastFramePath` TEXT NOT NULL,
                    `parentClipId` TEXT,
                    `extendStep` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_clips_createdAt` ON `video_clips` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_clips_parentClipId` ON `video_clips` (`parentClipId`)")
        }

        private val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = createVideoClipsTable(db)
        }
        private val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = createVideoClipsTable(db)
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = createVideoClipsTable(db)
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "godmode_studio.db"
                )
                    .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
