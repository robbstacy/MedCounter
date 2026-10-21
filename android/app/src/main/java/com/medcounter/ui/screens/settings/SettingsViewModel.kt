package com.medcounter.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medcounter.MedCounterApplication
import com.medcounter.data.entity.TimeSlotEntity
import com.medcounter.data.export.DataExporter
import com.medcounter.data.export.DataImporter
import com.medcounter.data.export.ImportPreview
import com.medcounter.work.DoseReminderScheduler
import com.medcounter.work.RefillScheduler
import com.medcounter.work.WeeklyReportScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val doseRemindersEnabled: Boolean = true,
    val weeklyReportEnabled: Boolean = true,
    val slots: List<TimeSlotEntity> = emptyList(),
    val exporting: Boolean = false,
    val importReading: Boolean = false,
    val importPreview: ImportPreview? = null,
    val importError: String? = null,
    val importTargetProfileName: String = "",
    val importTargetProfileId: Long = -1L,
    val shareUris: List<Uri>? = null,
)

class SettingsViewModel(private val app: MedCounterApplication) : ViewModel() {
    private val prefs = app.container.preferences
    private val repository = app.container.repository
    private val _state = MutableStateFlow(
        SettingsUiState(
            notificationsEnabled = prefs.notificationsEnabled,
            notificationHour = prefs.notificationHour,
            doseRemindersEnabled = prefs.doseRemindersEnabled,
            weeklyReportEnabled = prefs.weeklyReportEnabled,
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val slots = repository.getAllSlotsSnapshot()
            val profiles = repository.getAllProfilesSnapshot()
            val selected = profiles.firstOrNull { it.id == prefs.selectedProfileId } ?: profiles.firstOrNull()
            _state.value = _state.value.copy(
                slots = slots,
                importTargetProfileId = selected?.id ?: -1L,
                importTargetProfileName = selected?.name.orEmpty(),
            )
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        prefs.notificationsEnabled = enabled
        _state.value = _state.value.copy(notificationsEnabled = enabled)
        if (enabled) {
            RefillScheduler.reschedule(app, prefs.notificationHour)
            RefillScheduler.runOnceNow(app)
        } else {
            RefillScheduler.cancel(app)
        }
    }

    fun updateNotificationHour(hour: Int) {
        prefs.notificationHour = hour
        _state.value = _state.value.copy(notificationHour = hour)
        if (prefs.notificationsEnabled) {
            RefillScheduler.reschedule(app, hour)
        }
    }

    fun updateDoseRemindersEnabled(enabled: Boolean) {
        prefs.doseRemindersEnabled = enabled
        _state.value = _state.value.copy(doseRemindersEnabled = enabled)
        viewModelScope.launch {
            if (enabled) DoseReminderScheduler.rescheduleAll(app, repository)
            else DoseReminderScheduler.cancelAll(app)
        }
    }

    fun updateWeeklyReportEnabled(enabled: Boolean) {
        prefs.weeklyReportEnabled = enabled
        _state.value = _state.value.copy(weeklyReportEnabled = enabled)
        if (enabled) WeeklyReportScheduler.schedule(app) else WeeklyReportScheduler.cancel(app)
    }

    fun updateSlotTime(slot: TimeSlotEntity, minutes: Int) {
        viewModelScope.launch {
            val updated = slot.copy(minutesOfDay = minutes)
            repository.updateSlot(updated)
            val slots = repository.getAllSlotsSnapshot()
            _state.value = _state.value.copy(slots = slots)
            if (prefs.doseRemindersEnabled) {
                DoseReminderScheduler.rescheduleAll(app, repository)
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(exporting = true)
            val uris = DataExporter.exportAll(app, repository)
            _state.value = _state.value.copy(exporting = false, shareUris = uris)
        }
    }

    fun consumeShare() {
        _state.value = _state.value.copy(shareUris = null)
    }

    fun parseImport(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(importReading = true, importError = null)
            val result = DataImporter.parsePreview(app, uri)
            _state.value = result.fold(
                onSuccess = { preview -> _state.value.copy(importReading = false, importPreview = preview) },
                onFailure = { error ->
                    _state.value.copy(importReading = false, importError = error.message ?: "Import failed")
                },
            )
        }
    }

    fun dismissImport() {
        _state.value = _state.value.copy(importPreview = null, importError = null)
    }

    fun confirmImport() {
        val preview = _state.value.importPreview ?: return
        val profileId = _state.value.importTargetProfileId
        val profileName = _state.value.importTargetProfileName
        if (profileId <= 0L) {
            _state.value = _state.value.copy(importError = "Create a profile before importing")
            return
        }
        viewModelScope.launch {
            DataImporter.importMedications(repository, profileId, profileName, preview.medications)
            if (prefs.doseRemindersEnabled) DoseReminderScheduler.rescheduleAll(app, repository)
            _state.value = _state.value.copy(importPreview = null)
        }
    }
}

fun shareIntent(uris: List<Uri>): Intent {
    return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "text/plain"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
