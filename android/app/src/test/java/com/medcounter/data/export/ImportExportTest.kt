package com.medcounter.data.export

import com.medcounter.data.entity.MedicationEntity
import com.medcounter.domain.MedicationCalculation
import com.medcounter.domain.MedicationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ImportExportTest {
    @Test
    fun escapeCsvQuotesSpecialCharacters() {
        assertEquals("plain", CsvFormat.escapeCsv("plain"))
        assertEquals("\"a,b\"", CsvFormat.escapeCsv("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvFormat.escapeCsv("say \"hi\""))
        assertEquals("\"line\nbreak\"", CsvFormat.escapeCsv("line\nbreak"))
    }

    @Test
    fun scheduleLabels() {
        assertEquals("Twice daily", CsvFormat.buildSchedLabel("BID", 1, 1.0, emptyList()))
        assertEquals("Every 3 days", CsvFormat.buildSchedLabel("EVERY_N_DAYS", 3, 1.0, emptyList()))
        assertEquals("1.5× daily — Morning", CsvFormat.buildSchedLabel("CUSTOM", 1, 1.5, listOf("Morning")))
        assertEquals("8:00 AM", CsvFormat.formatMinuteOfDay(8 * 60))
        assertEquals("12:30 PM", CsvFormat.formatMinuteOfDay(12 * 60 + 30))
        assertEquals("12:00 AM", CsvFormat.formatMinuteOfDay(0))
    }

    @Test
    fun medicationsCsvHeaderAndRoundTrip() {
        val med = MedicationEntity(
            id = 4,
            profileId = 1,
            name = "Vitamin, D",
            strength = "1000",
            strengthUnit = "IU",
            form = "tablet",
            scheduleType = "DAILY",
            quantityOnHand = 30.0,
            quantityAsOf = LocalDate.of(2026, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            leadTimeDays = 7,
            pharmacy = "Corner",
            notes = "with food",
        )
        val calc = MedicationCalculation(28.0, 1.0, 28, LocalDate.of(2026, 7, 14), LocalDate.of(2026, 7, 7), MedicationStatus.OK, false)
        val csv = CsvFormat.medicationsCsv(
            listOf(
                CsvFormat.MedicationCsvRow(
                    medication = med,
                    profileName = "Alex",
                    calculation = calc,
                    doseLabels = listOf("Morning"),
                    zone = ZoneOffset.UTC,
                ),
            ),
        )
        assertTrue(csv.startsWith(CsvFormat.MEDICATIONS_HEADER))
        val parsed = DataImporter.parseText(csv).getOrThrow()
        assertEquals(1, parsed.medications.size)
        assertEquals("Vitamin, D", parsed.medications[0].name)
        assertEquals("DAILY", parsed.medications[0].scheduleType)
        assertEquals(30.0, parsed.medications[0].quantityOnHand, 0.0001)
        assertEquals(setOf("Alex"), parsed.sourceProfileNames)
        assertEquals("with food", parsed.medications[0].notes)
    }

    @Test
    fun importerRejectsUnknownFilesAndPartialHeaders() {
        val unknown = DataImporter.parseText("foo,bar\n1,2\n")
        assertTrue(unknown.isFailure)
        assertEquals(
            "This doesn't look like a Med Counter medications export",
            unknown.exceptionOrNull()?.message,
        )
        val partial = DataImporter.parseText("name,notes\nAspirin,x\n")
        assertEquals(
            "Missing required columns: name, schedule_type, quantity_on_hand",
            partial.exceptionOrNull()?.message,
        )
        val empty = DataImporter.parseText("")
        assertEquals("File is empty", empty.exceptionOrNull()?.message)
    }

    @Test
    fun importerSkipsBlankNamesAndBadSchedules() {
        val csv = """
            name,schedule_type,quantity_on_hand,profile_name
            ,DAILY,10,Alex
            Good,daily,12,Alex
            Bad,WHENEVER,3,Alex
        """.trimIndent()
        val preview = DataImporter.parseText(csv).getOrThrow()
        assertEquals(listOf("Good"), preview.medications.map { it.name })
        assertEquals("DAILY", preview.medications[0].scheduleType)
        assertEquals("tablet", preview.medications[0].form)
        assertEquals(7, preview.medications[0].leadTimeDays)
    }

    @Test
    fun quotedCsvLineKeepsCommas() {
        assertEquals(listOf("a", "b,c", "d\"e"), DataImporter.parseCsvLine("a,\"b,c\",\"d\"\"e\""))
    }
}
