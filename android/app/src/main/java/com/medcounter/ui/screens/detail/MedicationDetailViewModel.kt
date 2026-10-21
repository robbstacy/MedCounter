package com.medcounter.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medcounter.data.AppContainer
import com.medcounter.data.entity.AuditLogEntity
import com.medcounter.data.entity.MedicationEntity
import com.medcounter.data.entity.RestockEventEntity
import com.medcounter.data.entity.UsageLogEntity
import com.medcounter.domain.AuditAction
import com.medcounter.domain.DoseScheduleResolver
import com.medcounter.domain.MedicationCalculationService
import com.medcounter.domain.MedicationWithCalculation
import com.medcounter.domain.ScheduleType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

class MedicationDetailViewModel(
    private val container: AppContainer,
    private val medicationId: Long,
) : ViewModel() {
    private val repository = container.repository

    val medWithCalc: StateFlow<MedicationWithCalculation?> = combine(
        repository.getMedicationByIdFlow(medicationId),
        repository.getUsageLogsForMedication(medicationId),
        repository.getDoseTimesForMedication(medicationId),
        repository.getAllSlots(),
    ) { med, logs, times, slots ->
        med ?: return@combine null
        val minutes = DoseScheduleResolver.resolveMinutes(
            times.map { it.slotId to it.customMinutesOfDay },
            slots.associate { it.id to it.minutesOfDay },
        )
        MedicationCalculationService.calculate(med, logs, doseTimesMinutes = minutes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val auditLogs = repository.getAuditLogsForMedication(medicationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logDose(units: Double, isExtra: Boolean) {
        viewModelScope.launch {
            val current = medWithCalc.value ?: return@launch
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val now = System.currentTimeMillis()
            repository.insertUsageLog(
                UsageLogEntity(medicationId = med.id, timestamp = now, quantityUsed = units),
            )
            val schedule = ScheduleType.valueOf(med.scheduleType)
            val before = current.calculation.currentQuantity
            val after = if (schedule == ScheduleType.PRN) {
                before
            } else {
                val newTotal = (before - units).coerceAtLeast(0.0)
                val pill = med.pillboxQuantity
                if (pill != null) {
                    val newPill = min(pill, newTotal)
                    val newBottle = newTotal - newPill
                    repository.updatePillboxSplit(med.id, newBottle, newPill, now)
                } else {
                    repository.updateQuantity(med.id, newTotal, now)
                }
                newTotal
            }
            audit(
                med = med,
                action = AuditAction.DOSE_LOGGED,
                delta = -units,
                before = before,
                after = after,
                expected = null,
                note = if (isExtra) "Extra dose" else "",
                now = now,
            )
        }
    }

    fun addRestock(quantity: Double, notes: String, usedRefill: Boolean) {
        viewModelScope.launch {
            val current = medWithCalc.value ?: return@launch
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val now = System.currentTimeMillis()
            val before = current.calculation.currentQuantity
            val pill = med.pillboxQuantity
            if (pill != null) {
                val projectedBottle = (before - pill).coerceAtLeast(0.0)
                repository.updatePillboxSplit(med.id, projectedBottle + quantity, pill, now)
            } else {
                repository.updateQuantity(med.id, before + quantity, now)
            }
            var note = notes.trim()
            val refills = med.refillsRemaining
            if (usedRefill && refills != null && refills > 0) {
                repository.updateRefills(med.id, refills - 1)
                val refillNote = "Used 1 refill"
                note = if (note.isBlank()) refillNote else "$note · $refillNote"
            }
            repository.insertRestockEvent(
                RestockEventEntity(
                    medicationId = med.id,
                    date = now,
                    quantityAdded = quantity,
                    notes = note,
                ),
            )
            audit(
                med,
                AuditAction.RESTOCK_ADDED,
                delta = quantity,
                before = before,
                after = before + quantity,
                expected = null,
                note = note,
                now = now,
            )
        }
    }

    fun updateCount(newQty: Double, notes: String) {
        viewModelScope.launch {
            val current = medWithCalc.value ?: return@launch
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val now = System.currentTimeMillis()
            val expected = current.calculation.currentQuantity
            repository.updateQuantity(med.id, newQty, now)
            val shortfall = expected - newQty
            val note = when {
                notes.isNotBlank() -> notes.trim()
                shortfall > 0.5 -> "⚠ Shortfall: ${formatAuditQty(shortfall)} fewer than expected"
                else -> ""
            }
            audit(
                med,
                AuditAction.COUNT_ADJUSTED,
                delta = newQty - expected,
                before = expected,
                after = newQty,
                expected = expected,
                note = note,
                now = now,
            )
        }
    }

    fun pauseMedication(reason: String) {
        viewModelScope.launch {
            val current = medWithCalc.value ?: return@launch
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val now = System.currentTimeMillis()
            val frozen = current.calculation.currentQuantity
            val trimmed = reason.trim()
            repository.updatePauseState(med.id, true, trimmed, frozen, now)
            audit(
                med,
                AuditAction.MED_PAUSED,
                delta = null,
                before = frozen,
                after = frozen,
                expected = null,
                note = trimmed.ifBlank { "Medication paused" },
                now = now,
            )
        }
    }

    fun resumeMedication() {
        viewModelScope.launch {
            val current = medWithCalc.value ?: return@launch
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val now = System.currentTimeMillis()
            val qty = current.calculation.currentQuantity
            repository.updatePauseState(med.id, false, "", qty, now)
            audit(med, AuditAction.MED_RESUMED, null, qty, qty, null, "Medication resumed", now)
        }
    }

    fun moveToPillbox(amount: Double) {
        viewModelScope.launch {
            val current = medWithCalc.value ?: return@launch
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val now = System.currentTimeMillis()
            val projected = current.calculation.currentQuantity
            val pill = med.pillboxQuantity ?: 0.0
            val bottle = (projected - pill).coerceAtLeast(0.0)
            val move = amount.coerceIn(0.0, bottle)
            repository.updatePillboxSplit(med.id, bottle - move, pill + move, now)
            audit(
                med,
                AuditAction.PILLBOX_REFILL,
                delta = null,
                before = projected,
                after = projected,
                expected = null,
                note = "Moved ${formatAuditQty(move)} to pillbox",
                now = now,
            )
        }
    }

    private suspend fun audit(
        med: MedicationEntity,
        action: AuditAction,
        delta: Double?,
        before: Double?,
        after: Double?,
        expected: Double?,
        note: String,
        now: Long,
    ) {
        val profile = repository.getProfileById(med.profileId)
        repository.insertAuditLog(
            AuditLogEntity(
                timestamp = now,
                profileId = med.profileId,
                profileName = profile?.name.orEmpty(),
                medicationId = med.id,
                medicationName = med.name,
                action = action.name,
                quantityDelta = delta,
                quantityBefore = before,
                quantityAfter = after,
                expectedQuantity = expected,
                note = note,
            ),
        )
    }
}

internal fun formatAuditQty(qty: Double): String {
    if (qty == kotlin.math.floor(qty)) return qty.toLong().toString()
    return String.format(java.util.Locale.US, "%.1f", qty)
}
