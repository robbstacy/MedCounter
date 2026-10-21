package com.medcounter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.medcounter.data.dao.AuditLogDao
import com.medcounter.data.dao.MedicationDao
import com.medcounter.data.dao.MedicationDoseTimeDao
import com.medcounter.data.dao.ProfileDao
import com.medcounter.data.dao.RestockEventDao
import com.medcounter.data.dao.TimeSlotDao
import com.medcounter.data.dao.UsageLogDao
import com.medcounter.data.entity.AuditLogEntity
import com.medcounter.data.entity.MedicationDoseTimeEntity
import com.medcounter.data.entity.MedicationEntity
import com.medcounter.data.entity.ProfileEntity
import com.medcounter.data.entity.RestockEventEntity
import com.medcounter.data.entity.TimeSlotEntity
import com.medcounter.data.entity.UsageLogEntity

@Database(
    entities = [
        ProfileEntity::class,
        MedicationEntity::class,
        RestockEventEntity::class,
        UsageLogEntity::class,
        AuditLogEntity::class,
        TimeSlotEntity::class,
        MedicationDoseTimeEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class MedCounterDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun medicationDao(): MedicationDao
    abstract fun restockEventDao(): RestockEventDao
    abstract fun usageLogDao(): UsageLogDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun timeSlotDao(): TimeSlotDao
    abstract fun medicationDoseTimeDao(): MedicationDoseTimeDao

    companion object {
        const val EXPECTED_IDENTITY_HASH = "b34374850426b5710cd0139087c85a1f"

        @Volatile
        private var instance: MedCounterDatabase? = null

        fun getInstance(context: Context): MedCounterDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedCounterDatabase::class.java,
                    "medcounter.db",
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }

        val SQL_2_3 = listOf(
            """
            CREATE TABLE IF NOT EXISTS time_slots (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                minutesOfDay INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent(),
            "INSERT INTO time_slots VALUES (1, 'Morning', 480, 0)",
            "INSERT INTO time_slots VALUES (2, 'Midday', 840, 1)",
            "INSERT INTO time_slots VALUES (3, 'Evening', 1080, 2)",
            "INSERT INTO time_slots VALUES (4, 'Bedtime', 1320, 3)",
            """
            CREATE TABLE IF NOT EXISTS medication_dose_times (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                medicationId INTEGER NOT NULL,
                slotId INTEGER,
                customMinutesOfDay INTEGER,
                FOREIGN KEY(medicationId) REFERENCES medications(id) ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_medication_dose_times_medicationId ON medication_dose_times(medicationId)",
            "UPDATE medications SET quantityAsOf = quantityAsOf * 86400000",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 1 FROM medications WHERE scheduleType = 'DAILY'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 1 FROM medications WHERE scheduleType = 'BID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 4 FROM medications WHERE scheduleType = 'BID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 1 FROM medications WHERE scheduleType = 'TID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 2 FROM medications WHERE scheduleType = 'TID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 4 FROM medications WHERE scheduleType = 'TID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 1 FROM medications WHERE scheduleType = 'QID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 2 FROM medications WHERE scheduleType = 'QID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 3 FROM medications WHERE scheduleType = 'QID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 4 FROM medications WHERE scheduleType = 'QID'",
            "INSERT INTO medication_dose_times (medicationId, slotId) SELECT id, 1 FROM medications WHERE scheduleType IN ('WEEKLY', 'EVERY_N_DAYS', 'CUSTOM')",
        )

        val SQL_3_4 = listOf("ALTER TABLE medications ADD COLUMN pillboxQuantity REAL")

        val SQL_4_5 = listOf(
            "ALTER TABLE medications ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE medications ADD COLUMN pauseReason TEXT NOT NULL DEFAULT ''",
        )

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SQL_2_3.forEach { db.execSQL(it) }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SQL_3_4.forEach { db.execSQL(it) }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SQL_4_5.forEach { db.execSQL(it) }
            }
        }
    }
}
