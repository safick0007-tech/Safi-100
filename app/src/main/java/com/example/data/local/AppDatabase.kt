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
        StrategyLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun dropVideoTables(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `video_clips`")
            db.execSQL("DROP TABLE IF EXISTS `video_generations`")
        }

        private val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) = dropVideoTables(db)
        }
        private val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) = dropVideoTables(db)
        }
        private val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) = dropVideoTables(db)
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) = dropVideoTables(db)
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "godmode_studio.db"
                )
                    .addMigrations(MIGRATION_1_5, MIGRATION_2_5, MIGRATION_3_5, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
