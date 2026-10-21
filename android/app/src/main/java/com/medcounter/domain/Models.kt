package com.medcounter.domain

enum class ScheduleType {
    DAILY,
    BID,
    TID,
    QID,
    WEEKLY,
    EVERY_N_DAYS,
    CUSTOM,
    PRN,
}

enum class MedicationStatus {
    OUT,
    CRITICAL,
    REORDER,
    OK,
}

enum class AuditAction {
    DOSE_LOGGED,
    RESTOCK_ADDED,
    COUNT_ADJUSTED,
    MED_CREATED,
    MED_EDITED,
    MED_DELETED,
    PILLBOX_REFILL,
    MED_PAUSED,
    MED_RESUMED,
}

data class MedicationCalculation(
    val currentQuantity: Double,
    val dailyRate: Double,
    val daysRemaining: Int?,
    val runOutDate: java.time.LocalDate?,
    val reorderByDate: java.time.LocalDate?,
    val status: MedicationStatus,
    val isEstimate: Boolean,
)

data class MedicationWithCalculation(
    val medication: com.medcounter.data.entity.MedicationEntity,
    val calculation: MedicationCalculation,
)
