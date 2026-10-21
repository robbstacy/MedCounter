package com.medcounter.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medcounter.data.AppContainer
import com.medcounter.data.entity.ProfileEntity
import com.medcounter.ui.theme.ProfileColorHexes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesViewModel(private val container: AppContainer) : ViewModel() {
    val profiles: StateFlow<List<ProfileEntity>> = container.repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(profile: ProfileEntity) {
        container.preferences.selectedProfileId = profile.id
    }

    fun save(existing: ProfileEntity?, name: String, color: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (existing == null) {
                val id = container.repository.insertProfile(ProfileEntity(name = trimmed, colorTag = color))
                if (container.preferences.selectedProfileId <= 0) {
                    container.preferences.selectedProfileId = id
                }
            } else {
                container.repository.updateProfile(existing.copy(name = trimmed, colorTag = color))
            }
        }
    }

    fun delete(profile: ProfileEntity) {
        viewModelScope.launch {
            container.repository.deleteProfile(profile)
            if (container.preferences.selectedProfileId == profile.id) {
                val remaining = container.repository.getAllProfilesSnapshot()
                container.preferences.selectedProfileId = remaining.firstOrNull()?.id ?: -1L
            }
        }
    }
}
