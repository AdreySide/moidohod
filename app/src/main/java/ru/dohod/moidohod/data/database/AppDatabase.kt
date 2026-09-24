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

    @TypeConverter
    fun fromWorkSchedule(schedule: WorkSchedule): String = schedule.name
    @TypeConverter
    fun toWorkSchedule(name: String): WorkSchedule = WorkSchedule.valueOf(name)
}

@Database(
    entities = [
        Settings::class,
        WorkDay::class,
        TaskType::class,
        CompletedTask::class,
        Payment::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun workDayDao(): WorkDayDao
    abstract fun taskTypeDao(): TaskTypeDao
    abstract fun completedTaskDao(): CompletedTaskDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `task_types` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `pointsPerUnit` REAL NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `completed_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `taskTypeId` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `totalPoints` REAL NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO task_types (name, pointsPerUnit, isActive)
                    VALUES 
                        ('Техничка', 1.0, 1),
                        ('Подключка', 2.0, 1),
                        ('Дозаказ', 1.0, 1),
                        ('ГП выполнено', 2.0, 1),
                        ('ГП перевел', 1.0, 1)
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `completed_tasks` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
            }
        }

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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `schedule` TEXT NOT NULL DEFAULT 'FIVE_TWO'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `carDepreciation` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `travelCompensation` REAL NOT NULL DEFAULT 0.0")
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
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}