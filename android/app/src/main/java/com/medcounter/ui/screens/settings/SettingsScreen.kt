package com.medcounter.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medcounter.data.entity.TimeSlotEntity
import com.medcounter.data.export.CsvFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickingHour by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimeSlotEntity?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.parseImport(uri)
    }
    LaunchedEffect(state.shareUris) {
        val uris = state.shareUris ?: return@LaunchedEffect
        val share = Intent.createChooser(shareIntent(uris), "Share export")
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(share)
        viewModel.consumeShare()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Text("Notifications", modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp))
            ListItem(
                headlineContent = { Text("Refill alerts") },
                trailingContent = {
                    Switch(state.notificationsEnabled, viewModel::updateNotificationsEnabled)
                },
            )
            ListItem(
                headlineContent = { Text("Alert time") },
                supportingContent = { Text(CsvFormat.formatMinuteOfDay(state.notificationHour * 60)) },
                modifier = Modifier.clickable { pickingHour = true },
            )
            Text("Dose Reminders", modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp))
            ListItem(
                headlineContent = { Text("Dose reminders") },
                trailingContent = {
                    Switch(state.doseRemindersEnabled, viewModel::updateDoseRemindersEnabled)
                },
            )
            Text("Dose times", modifier = Modifier.padding(horizontal = 16.dp))
            state.slots.forEach { slot ->
                ListItem(
                    headlineContent = { Text(slot.name) },
                    supportingContent = { Text(CsvFormat.formatMinuteOfDay(slot.minutesOfDay)) },
                    modifier = Modifier.clickable { editingSlot = slot },
                )
            }
            Text("Weekly Summary", modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp))
            ListItem(
                headlineContent = { Text("Weekly summary") },
                supportingContent = { Text("Sunday at 9:00 AM") },
                trailingContent = {
                    Switch(state.weeklyReportEnabled, viewModel::updateWeeklyReportEnabled)
                },
            )
            Text("Data", modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp))
            ListItem(
                headlineContent = { Text("Export all data") },
                supportingContent = {
                    Text(if (state.exporting) "Preparing files…" else "Summary + CSV files • share or save")
                },
                modifier = Modifier.clickable(enabled = !state.exporting) { viewModel.exportData() },
            )
            ListItem(
                headlineContent = { Text("Import medications") },
                supportingContent = {
                    Text(if (state.importReading) "Reading file…" else "Load from a medications.csv export file")
                },
                modifier = Modifier.clickable(enabled = !state.importReading) {
                    importLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "application/csv"))
                },
            )
            Text("About", modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp))
            ListItem(
                headlineContent = { Text("About PillStock") },
                supportingContent = { Text("Version, privacy policy, disclaimer") },
                modifier = Modifier.clickable { onOpenAbout() },
            )
        }
    }

    if (pickingHour) {
        val timeState = rememberTimePickerState(initialHour = state.notificationHour, initialMinute = 0)
        AlertDialog(
            onDismissRequest = { pickingHour = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateNotificationHour(timeState.hour)
                    pickingHour = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingHour = false }) { Text("Cancel") } },
            text = { TimePicker(timeState) },
        )
    }
    editingSlot?.let { slot ->
        val timeState = rememberTimePickerState(initialHour = slot.minutesOfDay / 60, initialMinute = slot.minutesOfDay % 60)
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text(slot.name) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSlotTime(slot, timeState.hour * 60 + timeState.minute)
                    editingSlot = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingSlot = null }) { Text("Cancel") } },
            text = { TimePicker(timeState) },
        )
    }
    state.importPreview?.let { preview ->
        val shown = preview.medications.take(5)
        val extra = preview.medications.size - shown.size
        AlertDialog(
            onDismissRequest = viewModel::dismissImport,
            title = { Text("Import medications") },
            text = {
                Column {
                    shown.forEach { Text("• ${it.name}") }
                    if (extra > 0) Text("  … and $extra more")
                    Text("Will be added to: ${state.importTargetProfileName.ifBlank { "selected profile" }}")
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmImport) { Text("Import") } },
            dismissButton = { TextButton(onClick = viewModel::dismissImport) { Text("Cancel") } },
        )
    }
    state.importError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImport,
            title = { Text("Import failed") },
            text = { Text("Import failed: $message") },
            confirmButton = { TextButton(onClick = viewModel::dismissImport) { Text("OK") } },
        )
    }
}
