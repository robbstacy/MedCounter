package com.medcounter.ui.screens.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medcounter.data.AppContainer
import com.medcounter.data.entity.AuditLogEntity
import com.medcounter.data.entity.MedicationDoseTimeEntity
import com.medcounter.data.entity.MedicationEntity
import com.medcounter.data.entity.ProfileEntity
import com.medcounter.data.entity.TimeSlotEntity
import com.medcounter.domain.AuditAction
import com.medcounter.domain.DoseScheduleResolver
import com.medcounter.domain.ScheduleType
import com.medcounter.ui.theme.ProfileColorHexes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DoseTimeEntry(val slotId: Long?, val customMinutes: Int?)

data class AddEditState(
    val name: String = "",
    val strength: String = "",
    val strengthUnit: String = "",
    val form: String = "tablet",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val intervalDays: String = "2",
    val customDosesPerDay: String = "1",
    val unitsPerDose: String = "1",
    val quantityOnHand: String = "",
    val countedOn: LocalDate = LocalDate.now(),
    val leadTimeDays: String = "7",
    val pharmacy: String = "",
    val rxNumber: String = "",
    val refills: String = "",
    val notes: String = "",
    val trackPillbox: Boolean = false,
    val existingPillboxQuantity: Double? = null,
    val doseTimeEntries: List<DoseTimeEntry> = listOf(DoseTimeEntry(1L, null)),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isFirstMedication: Boolean = false,
    val firstMedicationProfileName: String = "",
    val resolvedProfileId: Long = -1L,
    val canSave: Boolean = false,
)

class AddEditMedicationViewModel(
    private val container: AppContainer,
    private val medicationId: Long,
    private val profileIdArg: Long,
) : ViewModel() {
    private val repository = container.repository
    private val _state = MutableStateFlow(AddEditState())
    val state: StateFlow<AddEditState> = _state.asStateFlow()
    private var editing: MedicationEntity? = null

    init {
        viewModelScope.launch {
            val slots = repository.getAllSlotsSnapshot().ifEmpty {
                container.ensureDefaultTimeSlots()
                repository.getAllSlotsSnapshot()
            }
            val profiles = repository.getAllProfilesSnapshot()
            val isNew = medicationId <= 0L
            if (!isNew) {
                val med = repository.getMedicationById(medicationId)
                val times = repository.getDoseTimesForMedicationSnapshot(medicationId)
                if (med != null) {
                    editing = med
                    val zone = ZoneId.systemDefault()
                    val counted = java.time.Instant.ofEpochMilli(med.quantityAsOf).atZone(zone).toLocalDate()
                    _state.value = AddEditState(
                        name = med.name,
                        strength = med.strength,
                        strengthUnit = med.strengthUnit,
                        form = med.form,
                        scheduleType = runCatching { ScheduleType.valueOf(med.scheduleType) }.getOrDefault(ScheduleType.DAILY),
                        intervalDays = med.intervalDays.toString(),
                        customDosesPerDay = trimNum(med.customDosesPerDay),
                        unitsPerDose = trimNum(med.unitsPerDose),
                        quantityOnHand = trimNum(med.quantityOnHand),
                        countedOn = counted,
                        leadTimeDays = med.leadTimeDays.toString(),
                        pharmacy = med.pharmacy,
                        rxNumber = med.rxNumber,
                        refills = med.refillsRemaining?.toString().orEmpty(),
                        notes = med.notes,
                        trackPillbox = med.pillboxQuantity != null,
                        existingPillboxQuantity = med.pillboxQuantity,
                        doseTimeEntries = times.map { DoseTimeEntry(it.slotId, it.customMinutesOfDay) }
                            .ifEmpty { defaultEntries(ScheduleType.valueOf(med.scheduleType)) },
                        timeSlots = slots,
                        isEditing = true,
                        isLoading = false,
                        resolvedProfileId = med.profileId,
                        canSave = med.name.isNotBlank(),
                    )
                    return@launch
                }
            }
            val resolved = when {
                profileIdArg > 0 -> profileIdArg
                container.preferences.selectedProfileId > 0 -> container.preferences.selectedProfileId
                else -> -1L
            }
            _state.value = AddEditState(
                timeSlots = slots,
                isEditing = false,
                isLoading = false,
                isFirstMedication = profiles.isEmpty(),
                resolvedProfileId = resolved,
                doseTimeEntries = defaultEntries(ScheduleType.DAILY),
            )
        }
    }

    fun update(block: (AddEditState) -> AddEditState) {
        _state.update { current ->
            val next = block(current)
            next.copy(canSave = next.name.isNotBlank() && (!next.isFirstMedication || next.firstMedicationProfileName.isNotBlank()))
        }
    }

    fun updateScheduleType(type: ScheduleType) {
        update { it.copy(scheduleType = type, doseTimeEntries = defaultEntries(type)) }
    }

    fun updateDoseTimeEntry(index: Int, entry: DoseTimeEntry) {
        update { state ->
            val list = state.doseTimeEntries.toMutableList()
            if (index in list.indices) list[index] = entry
            state.copy(doseTimeEntries = list)
        }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            if (!s.canSave) return@launch
            var profileId = s.resolvedProfileId
            var profileName = repository.getProfileById(profileId)?.name.orEmpty()
            if (s.isFirstMedication || profileId <= 0L) {
                val name = s.firstMedicationProfileName.ifBlank { "Me" }
                profileId = repository.insertProfile(
                    ProfileEntity(name = name, colorTag = ProfileColorHexes.first()),
                )
                profileName = name
                container.preferences.selectedProfileId = profileId
            }
            val zone = ZoneId.systemDefault()
            val asOf = s.countedOn.atStartOfDay(zone).toInstant().toEpochMilli()
            val pillbox = when {
                !s.trackPillbox -> null
                s.existingPillboxQuantity != null -> s.existingPillboxQuantity
                else -> 0.0
            }
            val entity = MedicationEntity(
                id = editing?.id ?: 0L,
                profileId = profileId,
                name = s.name.trim(),
                strength = s.strength.trim(),
                strengthUnit = s.strengthUnit.trim(),
                form = s.form,
                scheduleType = s.scheduleType.name,
                intervalDays = s.intervalDays.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                customDosesPerDay = s.customDosesPerDay.toDoubleOrNull() ?: 1.0,
                unitsPerDose = s.unitsPerDose.toDoubleOrNull() ?: 1.0,
                quantityOnHand = s.quantityOnHand.toDoubleOrNull() ?: 0.0,
                quantityAsOf = asOf,
                leadTimeDays = s.leadTimeDays.toIntOrNull() ?: 7,
                refillsRemaining = s.refills.toIntOrNull(),
                pharmacy = s.pharmacy.trim(),
                rxNumber = s.rxNumber.trim(),
                notes = s.notes.trim(),
                isActive = editing?.isActive ?: true,
                pillboxQuantity = pillbox,
                isPaused = editing?.isPaused ?: false,
                pauseReason = editing?.pauseReason ?: "",
            )
            val now = System.currentTimeMillis()
            val id = if (s.isEditing) {
                repository.updateMedication(entity)
                entity.id
            } else {
                repository.insertMedication(entity)
            }
            val doseTimes = if (s.scheduleType == ScheduleType.PRN) {
                emptyList()
            } else {
                s.doseTimeEntries.map {
                    MedicationDoseTimeEntity(medicationId = id, slotId = it.slotId, customMinutesOfDay = it.customMinutes)
                }
            }
            repository.saveDoseTimesForMedication(id, doseTimes)
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = now,
                    profileId = profileId,
                    profileName = profileName,
                    medicationId = id,
                    medicationName = entity.name,
                    action = if (s.isEditing) AuditAction.MED_EDITED.name else AuditAction.MED_CREATED.name,
                ),
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val med = editing ?: return@launch
            val profile = repository.getProfileById(med.profileId)
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    profileId = med.profileId,
                    profileName = profile?.name.orEmpty(),
                    medicationId = med.id,
                    medicationName = med.name,
                    action = AuditAction.MED_DELETED.name,
                ),
            )
            repository.deleteMedication(med)
            onDone()
        }
    }

    private fun defaultEntries(type: ScheduleType): List<DoseTimeEntry> =
        DoseScheduleResolver.defaultSlotIdsFor(type).map { DoseTimeEntry(it, null) }

    private fun trimNum(value: Double): String =
        if (value == kotlin.math.floor(value)) value.toLong().toString() else value.toString()
}
