package com.medcounter.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.max

object MedicationCalculator {
    fun calculate(
        scheduleType: ScheduleType,
        unitsPerDose: Double,
        intervalDays: Int = 1,
        customDosesPerDay: Double = 1.0,
        quantityOnHand: Double,
        quantityAsOf: LocalDate,
        today: LocalDate,
        leadTimeDays: Int,
        prnUsedSinceAsOf: Double = 0.0,
        prnEstimatedDailyRate: Double = 0.0,
    ): MedicationCalculation {
        if (scheduleType == ScheduleType.PRN) {
            return calculatePrn(
                quantityOnHand,
                today,
                leadTimeDays,
                prnUsedSinceAsOf,
                prnEstimatedDailyRate,
            )
        }
        return calculateScheduled(
            scheduleType,
            unitsPerDose,
            intervalDays,
            customDosesPerDay,
            quantityOnHand,
            quantityAsOf,
            today,
            leadTimeDays,
        )
    }

    private fun calculateScheduled(
        scheduleType: ScheduleType,
        unitsPerDose: Double,
        intervalDays: Int,
        customDosesPerDay: Double,
        quantityOnHand: Double,
        quantityAsOf: LocalDate,
        today: LocalDate,
        leadTimeDays: Int,
    ): MedicationCalculation {
        val rate = dailyRate(scheduleType, unitsPerDose, intervalDays, customDosesPerDay)
        val daysElapsed = ChronoUnit.DAYS.between(quantityAsOf, today).toDouble()
        val currentQty = max(0.0, quantityOnHand - (rate * daysElapsed))
        if (rate <= 0.0) {
            return MedicationCalculation(currentQty, rate, null, null, null, MedicationStatus.OK, false)
        }
        val daysRemaining = floor(currentQty / rate).toInt()
        val runOutDate = today.plusDays(daysRemaining.toLong())
        val reorderByDate = runOutDate.minusDays(leadTimeDays.toLong())
        val status = statusFor(currentQty, daysRemaining, today, reorderByDate)
        return MedicationCalculation(
            currentQty,
            rate,
            daysRemaining,
            runOutDate,
            reorderByDate,
            status,
            false,
        )
    }

    private fun calculatePrn(
        quantityOnHand: Double,
        today: LocalDate,
        leadTimeDays: Int,
        prnUsedSinceAsOf: Double,
        prnEstimatedDailyRate: Double,
    ): MedicationCalculation {
        val currentQty = max(0.0, quantityOnHand - prnUsedSinceAsOf)
        if (prnEstimatedDailyRate <= 0.0) {
            val status = if (currentQty <= 0.0) MedicationStatus.OUT else MedicationStatus.OK
            return MedicationCalculation(currentQty, 0.0, null, null, null, status, true)
        }
        val daysRemaining = floor(currentQty / prnEstimatedDailyRate).toInt()
        val runOutDate = today.plusDays(daysRemaining.toLong())
        val reorderByDate = runOutDate.minusDays(leadTimeDays.toLong())
        val status = statusFor(currentQty, daysRemaining, today, reorderByDate)
        return MedicationCalculation(
            currentQty,
            prnEstimatedDailyRate,
            daysRemaining,
            runOutDate,
            reorderByDate,
            status,
            true,
        )
    }

    fun dailyRate(
        scheduleType: ScheduleType,
        unitsPerDose: Double,
        intervalDays: Int = 1,
        customDosesPerDay: Double = 1.0,
    ): Double {
        val dosesPerDay = when (scheduleType) {
            ScheduleType.DAILY -> 1.0
            ScheduleType.BID -> 2.0
            ScheduleType.TID -> 3.0
            ScheduleType.QID -> 4.0
            ScheduleType.WEEKLY -> 1.0 / 7.0
            ScheduleType.EVERY_N_DAYS -> if (intervalDays > 0) 1.0 / intervalDays else 1.0
            ScheduleType.CUSTOM -> customDosesPerDay
            ScheduleType.PRN -> 0.0
        }
        return unitsPerDose * dosesPerDay
    }

    fun calculateScheduledDiscrete(
        unitsPerDose: Double,
        doseMinutesOfDay: List<Int>,
        quantityOnHand: Double,
        anchor: LocalDateTime,
        now: LocalDateTime,
        leadTimeDays: Int,
    ): MedicationCalculation {
        val sorted = doseMinutesOfDay.sorted()
        val rate = unitsPerDose * sorted.size
        if (sorted.isEmpty()) {
            val current = max(0.0, quantityOnHand)
            val status = if (quantityOnHand <= 0.0) MedicationStatus.OUT else MedicationStatus.OK
            return MedicationCalculation(current, 0.0, null, null, null, status, false)
        }
        val dosesSinceAnchor = countDoseOccurrences(sorted, anchor, now)
        val currentQty = max(0.0, quantityOnHand - (dosesSinceAnchor * unitsPerDose))
        if (currentQty <= 0.0) {
            val today = now.toLocalDate()
            return MedicationCalculation(
                0.0,
                rate,
                0,
                today,
                today.minusDays(leadTimeDays.toLong()),
                MedicationStatus.OUT,
                false,
            )
        }
        val dosesRemaining = floor(currentQty / unitsPerDose).toInt()
        val runOutDateTime = findRunOutDateTime(sorted, now, dosesRemaining)
            ?: return MedicationCalculation(currentQty, rate, null, null, null, MedicationStatus.OK, false)
        val today = now.toLocalDate()
        val runOutDate = runOutDateTime.toLocalDate()
        val daysRem = max(0, ChronoUnit.DAYS.between(today, runOutDate).toInt())
        val reorderByDate = runOutDate.minusDays(leadTimeDays.toLong())
        val status = statusFor(currentQty, daysRem, today, reorderByDate)
        return MedicationCalculation(currentQty, rate, daysRem, runOutDate, reorderByDate, status, false)
    }

    fun countDoseOccurrences(sortedDoseMinutes: List<Int>, from: LocalDateTime, to: LocalDateTime): Int {
        if (sortedDoseMinutes.isEmpty() || !to.isAfter(from)) return 0
        var count = 0
        val lastDay = to.toLocalDate()
        var day = from.toLocalDate()
        while (!day.isAfter(lastDay)) {
            for (minute in sortedDoseMinutes) {
                val doseTime = day.atTime(minute / 60, minute % 60)
                if (doseTime.isAfter(from) && !doseTime.isAfter(to)) {
                    count++
                }
            }
            day = day.plusDays(1)
        }
        return count
    }

    private fun findRunOutDateTime(
        sortedDoseMinutes: List<Int>,
        from: LocalDateTime,
        dosesRemaining: Int,
    ): LocalDateTime? {
        if (dosesRemaining <= 0) return from
        var counted = 0
        var day = from.toLocalDate()
        val maxDay = day.plusDays(3650)
        while (!day.isAfter(maxDay)) {
            for (minute in sortedDoseMinutes) {
                val doseTime = day.atTime(minute / 60, minute % 60)
                if (doseTime.isAfter(from)) {
                    counted += 1
                    if (counted >= dosesRemaining) return doseTime
                }
            }
            day = day.plusDays(1)
        }
        return null
    }

    private fun statusFor(
        currentQty: Double,
        daysRemaining: Int,
        today: LocalDate,
        reorderByDate: LocalDate,
    ): MedicationStatus {
        return when {
            currentQty <= 0.0 -> MedicationStatus.OUT
            daysRemaining <= 2 -> MedicationStatus.CRITICAL
            !today.isBefore(reorderByDate) -> MedicationStatus.REORDER
            else -> MedicationStatus.OK
        }
    }
}
