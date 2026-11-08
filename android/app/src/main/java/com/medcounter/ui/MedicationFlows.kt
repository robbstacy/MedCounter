package com.medcounter.ui

import com.medcounter.data.MedCounterRepository
import com.medcounter.data.entity.MedicationEntity
import com.medcounter.domain.DoseScheduleResolver
import com.medcounter.domain.MedicationCalculationService
import com.medcounter.domain.MedicationWithCalculation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/** Live supply calculations for a list of medications (unsorted, same order as [meds]). */
fun calculatedMedications(
    repository: MedCounterRepository,
    meds: List<MedicationEntity>,
): Flow<List<MedicationWithCalculation>> {
    if (meds.isEmpty()) return flowOf(emptyList())
    val perMed = meds.map { med ->
        combine(
            repository.getUsageLogsForMedication(med.id),
            repository.getDoseTimesForMedication(med.id),
            repository.getAllSlots(),
        ) { logs, times, slots ->
            val minutes = DoseScheduleResolver.resolveMinutes(
                times.map { it.slotId to it.customMinutesOfDay },
                slots.associate { it.id to it.minutesOfDay },
            )
            MedicationCalculationService.calculate(med, logs, LocalDate.now(), minutes)
        }
    }
    return combine(perMed) { it.toList() }
}
