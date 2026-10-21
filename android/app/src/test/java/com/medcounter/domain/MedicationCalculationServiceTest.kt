package com.medcounter.domain

import com.medcounter.data.entity.MedicationEntity
import com.medcounter.data.entity.UsageLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MedicationCalculationServiceTest {
    private val zone = ZoneId.of("America/Chicago")
    private val today = LocalDate.of(2026, 6, 16)

    private fun med(
        schedule: ScheduleType,
        quantity: Double = 30.0,
        asOf: Long,
        paused: Boolean = false,
        pillbox: Double? = null,
        units: Double = 1.0,
    ) = MedicationEntity(
        id = 1,
        profileId = 1,
        name = "Demo",
        scheduleType = schedule.name,
        unitsPerDose = units,
        quantityOnHand = quantity,
        quantityAsOf = asOf,
        leadTimeDays = 7,
        isPaused = paused,
        pillboxQuantity = pillbox,
    )

    @Test
    fun pauseFreezesAsOfAtNowWithoutTouchingSupply() {
        val asOf = today.minusDays(20).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val result = MedicationCalculationService.calculate(
            med = med(ScheduleType.DAILY, quantity = 30.0, asOf = asOf, paused = true),
            usageLogs = emptyList(),
            today = today,
            nowMillis = now,
            zone = zone,
            clockTime = LocalTime.NOON,
        )
        assertEquals(now, result.medication.quantityAsOf)
        assertEquals(30.0, result.calculation.currentQuantity, 0.0001)
    }

    @Test
    fun prnIgnoresUsageBeforeAnchorDayAndUsesThirtyDayRate() {
        val anchor = today.minusDays(10).atStartOfDay(zone).toInstant().toEpochMilli()
        val before = today.minusDays(40).atStartOfDay(zone).toInstant().toEpochMilli()
        val recent = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val logs = listOf(
            UsageLogEntity(medicationId = 1, timestamp = before, quantityUsed = 100.0),
            UsageLogEntity(medicationId = 1, timestamp = recent, quantityUsed = 6.0),
        )
        val result = MedicationCalculationService.calculate(
            med = med(ScheduleType.PRN, quantity = 20.0, asOf = anchor),
            usageLogs = logs,
            today = today,
            doseTimesMinutes = listOf(480),
            zone = zone,
            clockTime = LocalTime.NOON,
        )
        assertEquals(14.0, result.calculation.currentQuantity, 0.0001)
        assertEquals(6.0 / 30.0, result.calculation.dailyRate, 0.0001)
        assertTrue(result.calculation.isEstimate)
    }

    @Test
    fun dailyWithDoseMinutesUsesDiscretePath() {
        val anchor = today.minusDays(1).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        val result = MedicationCalculationService.calculate(
            med = med(ScheduleType.DAILY, quantity = 10.0, asOf = anchor),
            usageLogs = emptyList(),
            today = today,
            doseTimesMinutes = listOf(8 * 60),
            zone = zone,
            clockTime = LocalTime.of(9, 0),
        )
        assertEquals(8.0, result.calculation.currentQuantity, 0.0001)
        assertEquals(false, result.calculation.isEstimate)
    }

    @Test
    fun customIgnoresDoseMinutesAndUsesRate() {
        val asOf = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val result = MedicationCalculationService.calculate(
            med = med(ScheduleType.CUSTOM, quantity = 10.0, asOf = asOf).copy(customDosesPerDay = 1.0),
            usageLogs = emptyList(),
            today = today,
            doseTimesMinutes = listOf(480, 1200),
            zone = zone,
            clockTime = LocalTime.NOON,
        )
        assertEquals(8.0, result.calculation.currentQuantity, 0.0001)
    }

    @Test
    fun pillboxIsAddedToOnHand() {
        val asOf = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val result = MedicationCalculationService.calculate(
            med = med(ScheduleType.CUSTOM, quantity = 10.0, asOf = asOf, pillbox = 4.0).copy(customDosesPerDay = 0.0),
            usageLogs = emptyList(),
            today = today,
            zone = zone,
            clockTime = LocalTime.NOON,
        )
        assertEquals(14.0, result.calculation.currentQuantity, 0.0001)
    }
}
