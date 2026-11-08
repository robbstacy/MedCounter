package com.medcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Invented doctor, patient and numbers; layout mirrors a typical PH clinic slip.
class PrescriptionSlipParserTest {
    private val slip = """
        SAMPLE MEDICAL CLINIC
        Internal Medicine
        Name: Juana Halimbawa      Age: 70   Sex: F
        Address: Cebu City         Date: 11/03/2026
        Rx
        1. Amlodipine (Norvasc) 5 mg tab #30
           Sig: 1 tab OD
        2. Metformin 500mg #60  Sig: 1 tab BID p.c.
           Refill x2
        3. Losartan 50 mg
           #90
           Sig: 1/2 tab once a day
        Dr. Pedro Santos Reyes, M.D.
        Lic. No. 0123456   PTR No. 7654321   S2 No. 112233
    """.trimIndent()

    @Test
    fun readsEveryDrugOnTheSlip() {
        val scan = PrescriptionSlipParser.parse(slip)
        assertEquals(listOf("Amlodipine", "Metformin", "Losartan"), scan.items.map { it.name })

        val amlo = scan.items[0]
        assertEquals("Norvasc", amlo.brand)
        assertEquals("5", amlo.strength)
        assertEquals("mg", amlo.strengthUnit)
        assertEquals(30.0, amlo.quantity!!, 0.0)
        assertEquals(ScheduleType.DAILY, amlo.schedule)
        assertEquals(1.0, amlo.unitsPerDose!!, 0.0)
        assertEquals("tablet", amlo.form)
        assertNull(amlo.refills)

        val met = scan.items[1]
        assertEquals(60.0, met.quantity!!, 0.0)
        assertEquals(ScheduleType.BID, met.schedule)
        assertEquals(2, met.refills)

        val los = scan.items[2]
        assertEquals(90.0, los.quantity!!, 0.0)
        assertEquals(0.5, los.unitsPerDose!!, 0.0)
        assertEquals(ScheduleType.DAILY, los.schedule)
    }

    @Test
    fun readsDateAndDoctorButIgnoresLicenseNumbers() {
        val scan = PrescriptionReader.read(slip)
        assertEquals(LocalDate.of(2026, 11, 3), scan.date)
        assertEquals("Dr. Pedro Santos Reyes", scan.prescriber)
        assertNull("license/PTR numbers must not become an Rx number", scan.rxNumber)
    }

    @Test
    fun matchesByGenericOrBrand() {
        val scan = PrescriptionSlipParser.parse(slip)
        assertEquals("Amlodipine", scan.matchFor("Norvasc")?.name)
        assertEquals("Metformin", scan.matchFor("metformin")?.name)
        assertNull(scan.matchFor("Atorvastatin"))
    }

    @Test
    fun sigAbbreviations() {
        assertEquals(ScheduleType.TID, PrescriptionSlipParser.scheduleFromSig("1 cap q8h"))
        assertEquals(ScheduleType.DAILY, PrescriptionSlipParser.scheduleFromSig("1 tab HS"))
        assertEquals(ScheduleType.PRN, PrescriptionSlipParser.scheduleFromSig("1 tab every 6 hrs PRN for pain"))
        assertEquals(ScheduleType.QID, PrescriptionSlipParser.scheduleFromSig("1 tab QID"))
    }

    @Test
    fun usLabelStillWorksThroughReader() {
        val scan = PrescriptionReader.read("CVS pharmacy\nRX# 6123456\nLISINOPRIL 10 MG TAB\nQTY: 30   REFILLS: 3")
        assertEquals("6123456", scan.rxNumber)
        assertEquals("CVS", scan.pharmacy)
        val item = scan.items.single()
        assertTrue(item.name.equals("Lisinopril", ignoreCase = true))
        assertEquals(30.0, item.quantity!!, 0.0)
        assertEquals(3, item.refills)
    }

    /** Actual Tesseract output (bundled eng model) from a rendered, rotated, noisy demo slip photo. */
    @Test
    fun parsesRealOcrOutputOfDemoSlip() {
        val text = javaClass.getResource("/ocr/demo-ph-slip.txt")!!.readText()
        val scan = PrescriptionReader.read(text)
        assertEquals(listOf("Amlodipine", "Metformin", "Losartan"), scan.items.map { it.name })
        assertEquals(listOf(30.0, 60.0, 90.0), scan.items.map { it.quantity })
        assertEquals(listOf(ScheduleType.DAILY, ScheduleType.BID, ScheduleType.DAILY), scan.items.map { it.schedule })
        assertEquals(2, scan.items[1].refills)
        assertEquals(LocalDate.of(2026, 11, 3), scan.date)
        assertEquals("Dr. Pedro Santos Reyes", scan.prescriber)
        assertNull(scan.rxNumber)
    }
}
