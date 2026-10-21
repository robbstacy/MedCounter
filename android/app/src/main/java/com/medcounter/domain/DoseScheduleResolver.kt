package com.medcounter.domain

object DoseScheduleResolver {
    fun resolveMinutes(
        doseTimes: List<Pair<Long?, Int?>>,
        slotMinutes: Map<Long, Int>,
    ): List<Int> {
        return doseTimes.mapNotNull { (slotId, customMinutes) ->
            if (slotId != null) slotMinutes[slotId] else customMinutes
        }.sorted()
    }

    fun defaultSlotIdsFor(scheduleType: ScheduleType): List<Long> = when (scheduleType) {
        ScheduleType.DAILY -> listOf(1L)
        ScheduleType.BID -> listOf(1L, 4L)
        ScheduleType.TID -> listOf(1L, 2L, 4L)
        ScheduleType.QID -> listOf(1L, 2L, 3L, 4L)
        ScheduleType.WEEKLY, ScheduleType.EVERY_N_DAYS, ScheduleType.CUSTOM -> listOf(1L)
        ScheduleType.PRN -> emptyList()
    }
}

object RefillAlert {
    fun medsNeedingRefill(meds: List<MedicationWithCalculation>): List<MedicationWithCalculation> =
        meds.filter { it.calculation.status != MedicationStatus.OK }
}
