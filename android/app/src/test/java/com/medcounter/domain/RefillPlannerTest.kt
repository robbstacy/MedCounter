package com.medcounter.domain

import com.medcounter.data.entity.MedicationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Demo data only; no real medication records.
class RefillPlannerTest {
    private val today = LocalDate.of(2026, 11, 8)

    private fun item(
        id: Long,
        name: String,
        days: Int?,
        rx: String = "",
        refills: Int? = null,
        pharmacy: String = "",
        status: MedicationStatus = MedicationStatus.OK,
        paused: Boolean = false,
    ) = MedicationWithCalculation(
        medication = MedicationEntity(
            id = id,
            profileId = 1,
            name = name,
            strength = "10",
            strengthUnit = "mg",
            scheduleType = ScheduleType.DAILY.name,
            quantityOnHand = (days ?: 0).toDouble(),
            quantityAsOf = 0,
            rxNumber = rx,
            refillsRemaining = refills,
            pharmacy = pharmacy,
            isPaused = paused,
        ),
        calculation = MedicationCalculation(
            currentQuantity = (days ?: 5).toDouble(),
            dailyRate = 1.0,
            daysRemaining = days,
            runOutDate = days?.let { today.plusDays(it.toLong()) },
            reorderByDate = null,
            status = status,
            isEstimate = false,
        ),
    )

    @Test
    fun soonestEmptySortsByDaysLeftWithUnknownLast() {
        val list = listOf(item(1, "Alpha", 20), item(2, "Bravo", null), item(3, "Charlie", 3))
        val sorted = list.sortedWith(RefillPlanner.comparator(MedicationSort.SOONEST_EMPTY))
        assertEquals(listOf("Charlie", "Alpha", "Bravo"), sorted.map { it.medication.name })
    }

    @Test
    fun rxNumberSortIsNumericAwareAndBlanksLast() {
        val list = listOf(item(1, "A", 5, rx = "1203"), item(2, "B", 5), item(3, "C", 5, rx = "912"))
        val sorted = list.sortedWith(RefillPlanner.comparator(MedicationSort.RX_NUMBER))
        assertEquals(listOf("C", "A", "B"), sorted.map { it.medication.name })
    }

    @Test
    fun refillsLeftSortPutsFewestFirst() {
        val list = listOf(item(1, "A", 5, refills = 3), item(2, "B", 5, refills = 0), item(3, "C", 5))
        val sorted = list.sortedWith(RefillPlanner.comparator(MedicationSort.REFILLS_LEFT))
        assertEquals(listOf("B", "A", "C"), sorted.map { it.medication.name })
    }

    @Test
    fun dueExcludesPausedAndOk() {
        assertTrue(RefillPlanner.isDue(item(1, "A", 2, status = MedicationStatus.CRITICAL)))
        assertFalse(RefillPlanner.isDue(item(2, "B", 2, status = MedicationStatus.CRITICAL, paused = true)))
        assertFalse(RefillPlanner.isDue(item(3, "C", 40)))
    }

    @Test
    fun messageGroupsByPharmacyAndOrdersSoonestFirst() {
        val message = RefillPlanner.buildMessage(
            listOf(
                RefillCandidate("Pat", item(1, "Demopril", 9, rx = "6100001", refills = 2, pharmacy = "CVS")),
                RefillCandidate("Sam", item(2, "Samplestatin", 4, rx = "6100002", refills = 0, pharmacy = "CVS")),
                RefillCandidate("Pat", item(3, "Testolol", 6, pharmacy = "")),
            ),
            today,
        )
        val expected = """
            Refills needed (Nov 8):

            CVS:
            • Samplestatin 10 mg (Sam) — Rx #6100002, NO refills left, runs out Nov 12 (4 days)
            • Demopril 10 mg (Pat) — Rx #6100001, 2 refills left, runs out Nov 17 (9 days)

            Pharmacy not set:
            • Testolol 10 mg (Pat) — runs out Nov 14 (6 days)

            ⚠ Needs a new prescription from the doctor: Samplestatin
        """.trimIndent()
        assertEquals(expected, message)
    }

    @Test
    fun singlePersonOmitsName() {
        val message = RefillPlanner.buildMessage(
            listOf(RefillCandidate("Pat", item(1, "Demopril", 1, pharmacy = "CVS"))),
            today,
        )
        assertTrue(message.contains("• Demopril 10 mg — runs out tomorrow"))
    }
}
