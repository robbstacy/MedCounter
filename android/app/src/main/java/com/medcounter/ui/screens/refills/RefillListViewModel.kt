package com.medcounter.ui.screens.refills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medcounter.data.AppContainer
import com.medcounter.data.entity.ProfileEntity
import com.medcounter.domain.MedicationSort
import com.medcounter.domain.RefillCandidate
import com.medcounter.domain.RefillPlanner
import com.medcounter.ui.calculatedMedications
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RefillListState(
    val profiles: List<ProfileEntity> = emptyList(),
    /** null = everyone */
    val profileFilter: Long? = null,
    val candidates: List<RefillCandidate> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val message: String = "",
    val loaded: Boolean = false,
)

/** Builds the copy-and-send refill list across every profile, soonest to empty first. */
@OptIn(ExperimentalCoroutinesApi::class)
class RefillListViewModel(container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val filter = MutableStateFlow<Long?>(null)
    /** null until the first load, so "due" meds can be preselected exactly once. */
    private val selection = MutableStateFlow<Set<Long>?>(null)

    private val allCandidates = repository.getAllProfiles().flatMapLatest { profiles ->
        if (profiles.isEmpty()) {
            flowOf(profiles to emptyList())
        } else {
            val perProfile = profiles.map { profile ->
                repository.getMedicationsForProfile(profile.id)
                    .flatMapLatest { meds -> calculatedMedications(repository, meds) }
                    .map { items -> items.map { RefillCandidate(profile.name, it) } }
            }
            combine(perProfile) { lists -> profiles to lists.flatMap { it } }
        }
    }

    val state: StateFlow<RefillListState> = combine(allCandidates, filter, selection) { (profiles, all), profileId, selected ->
        val comparator = RefillPlanner.comparator(MedicationSort.SOONEST_EMPTY)
        val visible = all
            .filter { profileId == null || it.item.medication.profileId == profileId }
            .sortedWith { a, b -> comparator.compare(a.item, b.item) }
        val effective = selected ?: all.filter { RefillPlanner.isDue(it.item) }.map { it.item.medication.id }.toSet()
            .also { selection.value = it }
        val chosen = visible.filter { it.item.medication.id in effective }
        RefillListState(
            profiles = profiles,
            profileFilter = profileId,
            candidates = visible,
            selectedIds = effective,
            message = RefillPlanner.buildMessage(chosen),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RefillListState())

    fun setFilter(profileId: Long?) {
        filter.value = profileId
    }

    fun toggle(medicationId: Long) {
        val current = selection.value ?: emptySet()
        selection.value = if (medicationId in current) current - medicationId else current + medicationId
    }

    fun selectDue() {
        selection.value = state.value.candidates.filter { RefillPlanner.isDue(it.item) }.map { it.item.medication.id }.toSet()
    }

    fun selectAll() {
        selection.value = state.value.candidates.map { it.item.medication.id }.toSet()
    }

    fun clear() {
        selection.value = emptySet()
    }
}
