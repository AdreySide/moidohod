package ru.dohod.moidohod.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.dohod.moidohod.data.dao.*
import ru.dohod.moidohod.data.entity.*

class Converters {
    @TypeConverter
    fun fromDayType(type: DayType): String = type.name
    @TypeConverter
    fun toDayType(name: String): DayType = DayType.valueOf(name)
}

@Database(
    entities = [
        Settings::class,
        WorkDay::class,
        TaskType::class,
        CompletedTask::class,
        Payment::class      // ← обязательно добавить!
    ],
    version = 5,            // ← версия 5!
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun workDayDao(): WorkDayDao
    abstract fun taskTypeDao(): TaskTypeDao
    abstract fun completedTaskDao(): CompletedTaskDao
    abstract fun paymentDao(): PaymentDao   // ← добавить!

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Миграция 1→2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `work_days` (
                        `date` TEXT PRIMARY KEY NOT NULL,
                        `type` TEXT NOT NULL,
                        `hours` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Миграция 2→3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `task_types` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `pointsPerUnit` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `completed_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `taskTypeId` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `totalPoints` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO task_types (name, pointsPerUnit, isActive)
                    VALUES 
                        ('Техничка', 1, 1),
                        ('Подключка', 2, 1),
                        ('Дозаказ', 1, 1),
                        ('ГП выполнено', 2, 1),
                        ('ГП перевел', 1, 1)
                """.trimIndent())
            }
        }

        // Миграция 3→4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `completed_tasks` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
            }
        }

        // ✅ МИГРАЦИЯ 4→5 (добавляем таблицу payments)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `periodStart` TEXT,
                        `periodEnd` TEXT,
                        `isActual` INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moidohod_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5   // ← обязательно добавить!
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}