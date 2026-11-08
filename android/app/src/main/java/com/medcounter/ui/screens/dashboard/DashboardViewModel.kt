package com.medcounter.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medcounter.data.AppContainer
import com.medcounter.data.entity.ProfileEntity
import com.medcounter.domain.MedicationSort
import com.medcounter.domain.MedicationWithCalculation
import com.medcounter.domain.RefillPlanner
import com.medcounter.ui.calculatedMedications
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val selectedId = MutableStateFlow(container.preferences.selectedProfileId)
    private val _sort = MutableStateFlow(MedicationSort.fromName(container.preferences.medicationSort))
    val sort: StateFlow<MedicationSort> = _sort.asStateFlow()

    val profiles: StateFlow<List<ProfileEntity>> = repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedProfile: StateFlow<ProfileEntity?> = combine(profiles, selectedId) { list, id ->
        val match = list.firstOrNull { it.id == id }
        if (match != null) match
        else list.firstOrNull()?.also { profile ->
            if (id <= 0L || list.none { it.id == id }) selectProfile(profile.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val unsorted = selectedId.flatMapLatest { id ->
        if (id <= 0L) flowOf(emptyList())
        else repository.getMedicationsForProfile(id).flatMapLatest { meds -> calculatedMedications(repository, meds) }
    }

    val medications: StateFlow<List<MedicationWithCalculation>> = combine(unsorted, _sort) { items, sort ->
        items.sortedWith(RefillPlanner.comparator(sort))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectProfile(id: Long) {
        container.preferences.selectedProfileId = id
        selectedId.value = id
    }

    fun setSort(sort: MedicationSort) {
        container.preferences.medicationSort = sort.name
        _sort.value = sort
    }
}
