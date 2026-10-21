package com.medcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationCalculatorTest {
    private val today = LocalDate.of(2026, 6, 16)

    @Test
    fun dailyRateMatchesSchedule() {
        assertEquals(1.0, MedicationCalculator.dailyRate(ScheduleType.DAILY, 1.0), 0.0001)
        assertEquals(2.0, MedicationCalculator.dailyRate(ScheduleType.BID, 1.0), 0.0001)
        assertEquals(3.0, MedicationCalculator.dailyRate(ScheduleType.TID, 1.0), 0.0001)
        assertEquals(4.0, MedicationCalculator.dailyRate(ScheduleType.QID, 1.0), 0.0001)
        assertEquals(1.0 / 7.0, MedicationCalculator.dailyRate(ScheduleType.WEEKLY, 1.0), 0.0000001)
        assertEquals(0.5, MedicationCalculator.dailyRate(ScheduleType.EVERY_N_DAYS, 1.0, intervalDays = 2), 0.0001)
        assertEquals(1.0, MedicationCalculator.dailyRate(ScheduleType.EVERY_N_DAYS, 1.0, intervalDays = 0), 0.0001)
        assertEquals(1.5, MedicationCalculator.dailyRate(ScheduleType.CUSTOM, 1.0, customDosesPerDay = 1.5), 0.0001)
        assertEquals(0.0, MedicationCalculator.dailyRate(ScheduleType.PRN, 1.0), 0.0001)
        assertEquals(2.0, MedicationCalculator.dailyRate(ScheduleType.DAILY, 2.0), 0.0001)
    }

    @Test
    fun scheduledCountDecaysByWholeDays() {
        val result = MedicationCalculator.calculate(
            scheduleType = ScheduleType.DAILY,
            unitsPerDose = 1.0,
            quantityOnHand = 30.0,
            quantityAsOf = today.minusDays(10),
            today = today,
            leadTimeDays = 7,
        )
        assertEquals(20.0, result.currentQuantity, 0.0001)
        assertEquals(20, result.daysRemaining)
        assertEquals(today.plusDays(20), result.runOutDate)
        assertEquals(today.plusDays(13), result.reorderByDate)
        assertEquals(MedicationStatus.OK, result.status)
        assertEquals(false, result.isEstimate)
    }

    @Test
    fun statusBoundaries() {
        val out = MedicationCalculator.calculate(
            ScheduleType.DAILY, 1.0, quantityOnHand = 2.0,
            quantityAsOf = today.minusDays(2), today = today, leadTimeDays = 7,
        )
        assertEquals(MedicationStatus.OUT, out.status)

        val critical = MedicationCalculator.calculate(
            ScheduleType.DAILY, 1.0, quantityOnHand = 12.0,
            quantityAsOf = today.minusDays(10), today = today, leadTimeDays = 1,
        )
        assertEquals(2, critical.daysRemaining)
        assertEquals(MedicationStatus.CRITICAL, critical.status)

        val reorder = MedicationCalculator.calculate(
            ScheduleType.DAILY, 1.0, quantityOnHand = 10.0,
            quantityAsOf = today, today = today, leadTimeDays = 10,
        )
        assertEquals(MedicationStatus.REORDER, reorder.status)
    }

    @Test
    fun zeroRateStaysOk() {
        val result = MedicationCalculator.calculate(
            ScheduleType.CUSTOM, 1.0, customDosesPerDay = 0.0,
            quantityOnHand = 5.0, quantityAsOf = today.minusDays(3), today = today, leadTimeDays = 7,
        )
        assertEquals(5.0, result.currentQuantity, 0.0001)
        assertNull(result.daysRemaining)
        assertEquals(MedicationStatus.OK, result.status)
    }

    @Test
    fun prnUsesSuppliedRateAndIsEstimate() {
        val unknown = MedicationCalculator.calculate(
            ScheduleType.PRN, 1.0, quantityOnHand = 10.0, quantityAsOf = today,
            today = today, leadTimeDays = 7, prnUsedSinceAsOf = 4.0, prnEstimatedDailyRate = 0.0,
        )
        assertEquals(6.0, unknown.currentQuantity, 0.0001)
        assertNull(unknown.daysRemaining)
        assertEquals(true, unknown.isEstimate)
        assertEquals(MedicationStatus.OK, unknown.status)

        val estimated = MedicationCalculator.calculate(
            ScheduleType.PRN, 1.0, quantityOnHand = 10.0, quantityAsOf = today,
            today = today, leadTimeDays = 7, prnUsedSinceAsOf = 4.0, prnEstimatedDailyRate = 2.0,
        )
        assertEquals(6.0, estimated.currentQuantity, 0.0001)
        assertEquals(3, estimated.daysRemaining)
        assertEquals(2.0, estimated.dailyRate, 0.0001)
        assertEquals(true, estimated.isEstimate)
    }

    @Test
    fun discreteCountSkipsAnchorInstantAndIncludesNow() {
        val anchor = LocalDateTime.of(2026, 6, 15, 8, 0)
        val now = LocalDateTime.of(2026, 6, 16, 9, 0)
        val minutes = listOf(8 * 60, 20 * 60)
        assertEquals(2, MedicationCalculator.countDoseOccurrences(minutes, anchor, now))
        val calc = MedicationCalculator.calculateScheduledDiscrete(
            unitsPerDose = 1.0,
            doseMinutesOfDay = minutes,
            quantityOnHand = 10.0,
            anchor = anchor,
            now = now,
            leadTimeDays = 7,
        )
        assertEquals(8.0, calc.currentQuantity, 0.0001)
        assertEquals(2.0, calc.dailyRate, 0.0001)
        assertEquals(MedicationStatus.REORDER, calc.status)
    }

    @Test
    fun emptyDoseListDoesNotEstimate() {
        val calc = MedicationCalculator.calculateScheduledDiscrete(
            1.0, emptyList(), 0.0,
            LocalDateTime.of(2026, 6, 1, 0, 0),
            LocalDateTime.of(2026, 6, 2, 0, 0),
            7,
        )
        assertEquals(MedicationStatus.OUT, calc.status)
        assertEquals(0.0, calc.dailyRate, 0.0001)
    }
}
