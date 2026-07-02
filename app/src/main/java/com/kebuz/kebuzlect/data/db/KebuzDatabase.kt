package com.kebuz.kebuzlect.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlbumEntity::class,
        ConvertedHashEntity::class,
        LectureNumberEntity::class,
        ProcessedDateEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class KebuzDatabase : RoomDatabase() {

    abstract fun albumDao(): AlbumDao

    abstract fun convertedHashDao(): ConvertedHashDao

    abstract fun lectureNumberDao(): LectureNumberDao

    abstract fun processedDateDao(): ProcessedDateDao

    companion object {
        @Volatile
        private var instance: KebuzDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `lecture_numbers` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`bucketId` TEXT NOT NULL, " +
                        "`groupHash` TEXT NOT NULL, " +
                        "`number` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_lecture_numbers_bucketId_groupHash` " +
                        "ON `lecture_numbers` (`bucketId`, `groupHash`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `processed_dates` (" +
                        "`bucketId` TEXT NOT NULL, " +
                        "`date` TEXT NOT NULL, " +
                        "PRIMARY KEY(`bucketId`, `date`))",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `albums` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): KebuzDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KebuzDatabase::class.java,
                    "kebuzlect.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
