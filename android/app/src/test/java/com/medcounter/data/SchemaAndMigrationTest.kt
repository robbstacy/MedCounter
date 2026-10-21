package com.medcounter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.DriverManager

class SchemaAndMigrationTest {
    @Test
    fun exportedSchemaMatchesV5IdentityHash() {
        val candidates = listOf(
            File("schemas/com.medcounter.data.MedCounterDatabase/5.json"),
            File("app/schemas/com.medcounter.data.MedCounterDatabase/5.json"),
        )
        val schemaFile = candidates.firstOrNull { it.exists() }
            ?: error("Room schema JSON not found. Looked in ${candidates.map { it.absolutePath }}")
        val text = schemaFile.readText()
        assertTrue(text.contains("\"version\": 5"))
        assertTrue(text.contains("\"identityHash\": \"${MedCounterDatabase.EXPECTED_IDENTITY_HASH}\""))
        listOf(
            "profiles",
            "medications",
            "restock_events",
            "usage_logs",
            "audit_logs",
            "time_slots",
            "medication_dose_times",
        ).forEach { table ->
            assertTrue(text.contains("\"tableName\": \"$table\""))
        }
        assertTrue(
            text.contains(
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, `strength` TEXT NOT NULL, `strengthUnit` TEXT NOT NULL, " +
                    "`form` TEXT NOT NULL, `scheduleType` TEXT NOT NULL, `intervalDays` INTEGER NOT NULL, " +
                    "`customDosesPerDay` REAL NOT NULL, `unitsPerDose` REAL NOT NULL, " +
                    "`quantityOnHand` REAL NOT NULL, `quantityAsOf` INTEGER NOT NULL, " +
                    "`leadTimeDays` INTEGER NOT NULL, `refillsRemaining` INTEGER, `pharmacy` TEXT NOT NULL, " +
                    "`rxNumber` TEXT NOT NULL, `notes` TEXT NOT NULL, `isActive` INTEGER NOT NULL, " +
                    "`pillboxQuantity` REAL, `isPaused` INTEGER NOT NULL, `pauseReason` TEXT NOT NULL",
            ),
        )
        assertTrue(text.contains("\"name\": \"index_medications_profileId\""))
        assertTrue(!text.contains("\"columnName\": \"isPaused\"") || !text.contains("\"defaultValue\": \"0\""))
    }

    @Test
    fun migrations2Through5MatchRecoveredSql() {
        Class.forName("org.sqlite.JDBC")
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    colorTag TEXT NOT NULL
                )
                """.trimIndent(),
            )
            st.executeUpdate(
                """
                CREATE TABLE medications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    profileId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    strength TEXT NOT NULL,
                    strengthUnit TEXT NOT NULL,
                    form TEXT NOT NULL,
                    scheduleType TEXT NOT NULL,
                    intervalDays INTEGER NOT NULL,
                    customDosesPerDay REAL NOT NULL,
                    unitsPerDose REAL NOT NULL,
                    quantityOnHand REAL NOT NULL,
                    quantityAsOf INTEGER NOT NULL,
                    leadTimeDays INTEGER NOT NULL,
                    refillsRemaining INTEGER,
                    pharmacy TEXT NOT NULL,
                    rxNumber TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    isActive INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            st.executeUpdate("INSERT INTO profiles (id, name, colorTag) VALUES (1, 'Alex', '#3D6B4F')")
            st.executeUpdate(
                """
                INSERT INTO medications (
                    profileId, name, strength, strengthUnit, form, scheduleType, intervalDays,
                    customDosesPerDay, unitsPerDose, quantityOnHand, quantityAsOf, leadTimeDays,
                    pharmacy, rxNumber, notes, isActive
                ) VALUES (1, 'Demo', '', '', 'tablet', 'BID', 1, 1.0, 1.0, 20.0, 20000, 7, '', '', '', 1)
                """.trimIndent(),
            )
        }
        connection.createStatement().use { st ->
            (MedCounterDatabase.SQL_2_3 + MedCounterDatabase.SQL_3_4 + MedCounterDatabase.SQL_4_5)
                .forEach { st.executeUpdate(it) }
        }

        connection.createStatement().use { st ->
            val asOf = st.executeQuery("SELECT quantityAsOf FROM medications WHERE name = 'Demo'")
            assertTrue(asOf.next())
            assertEquals(20000L * 86400000L, asOf.getLong(1))
            val slots = st.executeQuery("SELECT COUNT(*) FROM time_slots")
            assertTrue(slots.next())
            assertEquals(4, slots.getInt(1))
            val doses = st.executeQuery("SELECT slotId FROM medication_dose_times ORDER BY slotId")
            val slotIds = mutableListOf<Int>()
            while (doses.next()) slotIds += doses.getInt(1)
            assertEquals(listOf(1, 4), slotIds)
            val cols = st.executeQuery("PRAGMA table_info(medications)")
            val colNames = mutableListOf<String>()
            while (cols.next()) colNames += cols.getString(2)
            assertTrue(colNames.contains("pillboxQuantity"))
            assertTrue(colNames.contains("isPaused"))
            assertTrue(colNames.contains("pauseReason"))
            val paused = st.executeQuery("SELECT isPaused, pauseReason FROM medications")
            assertTrue(paused.next())
            assertEquals(0, paused.getInt(1))
            assertEquals("", paused.getString(2))
        }
        connection.close()
    }
}
