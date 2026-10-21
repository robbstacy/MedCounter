package com.medcounter.domain

import com.medcounter.data.entity.MedicationEntity
import com.medcounter.data.entity.UsageLogEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object MedicationCalculationService {
    /**
     * PRN windows are compared in epoch millis against the start of the anchor
     * local day (and today minus 30 days). The decompiled binary compared
     * usage timestamps (millis) to [LocalDate.toEpochDay] (~20_000), which
     * counted essentially every log. That comparison is treated as a bug.
     */
    fun calculate(
        med: MedicationEntity,
        usageLogs: List<UsageLogEntity>,
        today: LocalDate = LocalDate.now(),
        doseTimesMinutes: List<Int> = emptyList(),
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        clockTime: java.time.LocalTime = LocalDateTime.now(zone).toLocalTime(),
    ): MedicationWithCalculation {
        val medForCalc = if (med.isPaused) med.copy(quantityAsOf = nowMillis) else med
        val scheduleType = ScheduleType.valueOf(medForCalc.scheduleType)
        val discreteExcluded = setOf(ScheduleType.PRN, ScheduleType.CUSTOM)
        if (doseTimesMinutes.isNotEmpty() && scheduleType !in discreteExcluded) {
            val anchor = LocalDateTime.ofInstant(Instant.ofEpochMilli(medForCalc.quantityAsOf), zone)
            val now = today.atTime(clockTime)
            val quantity = medForCalc.quantityOnHand + (medForCalc.pillboxQuantity ?: 0.0)
            val calculation = MedicationCalculator.calculateScheduledDiscrete(
                unitsPerDose = medForCalc.unitsPerDose,
                doseMinutesOfDay = doseTimesMinutes,
                quantityOnHand = quantity,
                anchor = anchor,
                now = now,
                leadTimeDays = medForCalc.leadTimeDays,
            )
            return MedicationWithCalculation(medForCalc, calculation)
        }

        val anchorDate = Instant.ofEpochMilli(medForCalc.quantityAsOf).atZone(zone).toLocalDate()
        val anchorMillis = anchorDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val prnUsed = if (scheduleType == ScheduleType.PRN) {
            usageLogs.filter { it.timestamp >= anchorMillis }.sumOf { it.quantityUsed }
        } else {
            0.0
        }
        val prnDailyRate = if (scheduleType == ScheduleType.PRN) {
            val cutoff = today.minusDays(30).atStartOfDay(zone).toInstant().toEpochMilli()
            usageLogs.filter { it.timestamp >= cutoff }.sumOf { it.quantityUsed } / 30.0
        } else {
            0.0
        }
        val quantity = medForCalc.quantityOnHand + (medForCalc.pillboxQuantity ?: 0.0)
        val calculation = MedicationCalculator.calculate(
            scheduleType = scheduleType,
            unitsPerDose = medForCalc.unitsPerDose,
            intervalDays = medForCalc.intervalDays,
            customDosesPerDay = medForCalc.customDosesPerDay,
            quantityOnHand = quantity,
            quantityAsOf = anchorDate,
            today = today,
            leadTimeDays = medForCalc.leadTimeDays,
            prnUsedSinceAsOf = prnUsed,
            prnEstimatedDailyRate = prnDailyRate,
        )
        return MedicationWithCalculation(medForCalc, calculation)
    }
}
