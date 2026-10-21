package com.medcounter.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medcounter.data.AppContainer
import com.medcounter.data.entity.ProfileEntity
import com.medcounter.domain.DoseScheduleResolver
import com.medcounter.domain.MedicationCalculationService
import com.medcounter.domain.MedicationStatus
import com.medcounter.domain.MedicationWithCalculation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val selectedId = MutableStateFlow(container.preferences.selectedProfileId)

    val profiles: StateFlow<List<ProfileEntity>> = repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedProfile: StateFlow<ProfileEntity?> = combine(profiles, selectedId) { list, id ->
        val match = list.firstOrNull { it.id == id }
        if (match != null) match
        else list.firstOrNull()?.also { profile ->
            if (id <= 0L || list.none { it.id == id }) selectProfile(profile.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val medications: StateFlow<List<MedicationWithCalculation>> = selectedId.flatMapLatest { id ->
        if (id <= 0L) flowOf(emptyList())
        else repository.getMedicationsForProfile(id).flatMapLatest { meds ->
            if (meds.isEmpty()) flowOf(emptyList())
            else {
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
                combine(perMed) { items ->
                    items.sortedWith(
                        compareBy<MedicationWithCalculation> { statusRank(it.calculation.status) }
                            .thenBy { it.medication.name },
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectProfile(id: Long) {
        container.preferences.selectedProfileId = id
        selectedId.value = id
    }

    private fun statusRank(status: MedicationStatus): Int = when (status) {
        MedicationStatus.OUT -> 0
        MedicationStatus.CRITICAL -> 1
        MedicationStatus.REORDER -> 2
        MedicationStatus.OK -> 3
    }
}
