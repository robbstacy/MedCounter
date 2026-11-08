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
import com.medcounter.domain.SlipScan
import com.medcounter.domain.ScheduleType
import android.net.Uri
import com.medcounter.data.RxPhotoStore
import com.medcounter.ocr.RxLabelScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
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

    private val context = container.appContext
    private val _photos = MutableStateFlow(RxPhotoStore.list(context, medicationId))
    val photos = _photos.asStateFlow()

    /** Result of reading an attached photo, awaiting the user's OK. */
    private val _photoScan = MutableStateFlow<PhotoScanResult?>(null)
    val photoScan = _photoScan.asStateFlow()
    private val _reading = MutableStateFlow(false)
    val reading = _reading.asStateFlow()

    /** Returns how many photos were attached. */
    suspend fun addPhotos(uris: List<Uri>): Int {
        val saved = uris.count { RxPhotoStore.add(context, medicationId, it) != null }
        _photos.value = RxPhotoStore.list(context, medicationId)
        return saved
    }

    fun deletePhoto(file: File) {
        file.delete()
        _photos.value = RxPhotoStore.list(context, medicationId)
    }

    fun readPhoto(file: File) {
        viewModelScope.launch {
            _reading.value = true
            val scan = runCatching { RxLabelScanner.scan(context, Uri.fromFile(file)) }.getOrElse { SlipScan() }
            _reading.value = false
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val item = scan.matchFor(med.name) ?: scan.items.singleOrNull()
            _photoScan.value = PhotoScanResult(scan, item, med.name)
        }
    }

    fun dismissPhotoScan() {
        _photoScan.value = null
    }

    /**
     * Saves prescription details read from an attached photo without changing stock.
     * [isNewPrescription] resets "bought so far" to zero.
     */
    fun applyPhotoScan(isNewPrescription: Boolean) {
        val result = _photoScan.value ?: return
        _photoScan.value = null
        viewModelScope.launch {
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val item = result.item
            val scan = result.scan
            val zone = java.time.ZoneId.systemDefault()
            val updated = med.copy(
                rxQuantityPrescribed = item?.quantity ?: med.rxQuantityPrescribed,
                rxQuantityDispensed = if (isNewPrescription) 0.0 else med.rxQuantityDispensed,
                refillsRemaining = item?.refills ?: med.refillsRemaining,
                rxNumber = scan.rxNumber ?: med.rxNumber,
                rxDate = scan.date?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: med.rxDate,
                prescriber = scan.prescriber ?: med.prescriber,
                pharmacy = med.pharmacy.ifBlank { scan.pharmacy.orEmpty() },
            )
            repository.updateMedication(updated)
            val qty = medWithCalc.value?.calculation?.currentQuantity
            audit(
                med,
                AuditAction.MED_EDITED,
                delta = null,
                before = qty,
                after = qty,
                expected = null,
                note = "Prescription details from photo" + if (isNewPrescription) " (new Rx)" else "",
                now = System.currentTimeMillis(),
            )
        }
    }

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

    /**
     * Adds stock. When [scanned] is a new prescription slip for this medication, its
     * numbers (quantity prescribed, refills, Rx #, date, doctor) replace the current
     * prescription first. The amount added always counts toward "bought so far".
     */
    fun addRestock(quantity: Double, notes: String, usedRefill: Boolean, scanned: SlipScan? = null) {
        viewModelScope.launch {
            val current = medWithCalc.value ?: return@launch
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val now = System.currentTimeMillis()
            val before = current.calculation.currentQuantity
            val pill = med.pillboxQuantity
            val zone = java.time.ZoneId.systemDefault()

            val item = scanned?.let { it.matchFor(med.name) ?: it.items.singleOrNull() }
            val newRx = item?.quantity != null
            val notesOut = mutableListOf<String>()
            notes.trim().takeIf { it.isNotBlank() }?.let(notesOut::add)

            var refills = med.refillsRemaining
            var prescribed = med.rxQuantityPrescribed
            var dispensed = med.rxQuantityDispensed
            if (newRx) {
                prescribed = item!!.quantity
                dispensed = 0.0
                item.refills?.let { refills = it }
                notesOut += "New Rx: #${formatAuditQty(prescribed!!)}" + (item.refills?.let { ", refill ×$it" } ?: "")
            } else if (scanned?.rxNumber != null || item?.refills != null) {
                item?.refills?.let { refills = it }
                notesOut += listOfNotNull(scanned?.rxNumber?.let { "Rx #$it" }, item?.refills?.let { "$it refills left" })
                    .joinToString(", ", prefix = "From label: ")
            } else if (usedRefill && refills != null && refills > 0) {
                refills -= 1
                notesOut += "Used 1 refill"
            }
            if (prescribed != null) {
                dispensed += quantity
                val left = (prescribed - dispensed).coerceAtLeast(0.0)
                notesOut += "${formatAuditQty(left)} left on Rx"
            }

            val updated = med.copy(
                quantityOnHand = if (pill != null) (before - pill).coerceAtLeast(0.0) + quantity else before + quantity,
                quantityAsOf = now,
                refillsRemaining = refills,
                rxQuantityPrescribed = prescribed,
                rxQuantityDispensed = dispensed,
                rxNumber = scanned?.rxNumber ?: med.rxNumber,
                pharmacy = med.pharmacy.ifBlank { scanned?.pharmacy.orEmpty() },
                rxDate = if (newRx) scanned?.date?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: now else med.rxDate,
                prescriber = if (newRx) scanned?.prescriber ?: med.prescriber else med.prescriber,
            )
            repository.updateMedication(updated)
            val note = notesOut.joinToString(" · ")
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

    /**
     * Records a physical count (audit). For meds tracked with a pillbox, [bottleQty] is the
     * bottle count and [pillboxQty] the pillbox count; they are stored separately, and their
     * sum is what gets compared with the expected total. Previously the whole total was
     * written to the bottle while the old pillbox count was kept, which double-counted it.
     */
    fun updateCount(bottleQty: Double, pillboxQty: Double?, notes: String) {
        viewModelScope.launch {
            val current = medWithCalc.value ?: return@launch
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val now = System.currentTimeMillis()
            val expected = current.calculation.currentQuantity
            val tracksPillbox = med.pillboxQuantity != null
            val newQty = if (tracksPillbox) {
                val pill = (pillboxQty ?: med.pillboxQuantity ?: 0.0).coerceAtLeast(0.0)
                val bottle = bottleQty.coerceAtLeast(0.0)
                repository.updatePillboxSplit(med.id, bottle, pill, now)
                bottle + pill
            } else {
                repository.updateQuantity(med.id, bottleQty, now)
                bottleQty
            }
            val shortfall = expected - newQty
            val split = if (tracksPillbox) {
                "Stock ${formatAuditQty(bottleQty)} + pillbox ${formatAuditQty(pillboxQty ?: med.pillboxQuantity ?: 0.0)}"
            } else {
                ""
            }
            val flag = if (shortfall > 0.5) "⚠ Shortfall: ${formatAuditQty(shortfall)} fewer than expected" else ""
            val note = listOf(notes.trim(), split, if (notes.isBlank()) flag else "")
                .filter { it.isNotBlank() }
                .joinToString(" · ")
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

data class PhotoScanResult(
    val scan: SlipScan,
    val item: com.medcounter.domain.SlipItem?,
    val medicationName: String,
)
