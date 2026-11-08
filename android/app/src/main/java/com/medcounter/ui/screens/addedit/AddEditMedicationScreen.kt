package com.medcounter.ui.screens.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medcounter.data.export.CsvFormat
import com.medcounter.domain.ScheduleType
import com.medcounter.ui.components.RxScanButtons
import com.medcounter.ui.components.formChoices
import com.medcounter.ui.components.scheduleChoices
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    viewModel: AddEditMedicationViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showRxDate by remember { mutableStateOf(false) }
    var timeEditIndex by remember { mutableStateOf<Int?>(null) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Medication" else "Add Medication") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Text("Loading…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isFirstMedication) {
                Text("Who is this for?", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    state.firstMedicationProfileName,
                    { value -> viewModel.update { it.copy(firstMedicationProfileName = value) } },
                    label = { Text("Profile name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text("Medication Info", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            RxScanButtons(onResult = viewModel::onScanned, keepPhoto = viewModel::keepPhoto)
            if (state.attachedPhotoCount > 0) {
                Text(
                    "${state.attachedPhotoCount} prescription photo${if (state.attachedPhotoCount == 1) "" else "s"} attached. See them on the medication's page.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                state.name,
                { value -> viewModel.update { it.copy(name = value) } },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    state.strength,
                    { value -> viewModel.update { it.copy(strength = value) } },
                    label = { Text("Strength") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    state.strengthUnit,
                    { value -> viewModel.update { it.copy(strengthUnit = value) } },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                )
            }
            ChoiceField(
                label = "Form",
                value = formChoices().firstOrNull { it.first == state.form }?.second ?: state.form,
                options = formChoices().map { it.second },
                onSelect = { label ->
                    val key = formChoices().first { it.second == label }.first
                    viewModel.update { it.copy(form = key) }
                },
            )
            Text("Schedule", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            ChoiceField(
                label = "Dosing Schedule",
                value = scheduleChoices().first { it.first == state.scheduleType }.second,
                options = scheduleChoices().map { it.second },
                onSelect = { label ->
                    viewModel.updateScheduleType(scheduleChoices().first { it.second == label }.first)
                },
            )
            if (state.scheduleType == ScheduleType.EVERY_N_DAYS) {
                OutlinedTextField(
                    state.intervalDays,
                    { value -> viewModel.update { it.copy(intervalDays = value) } },
                    label = { Text("Every N days") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.scheduleType == ScheduleType.CUSTOM) {
                OutlinedTextField(
                    state.customDosesPerDay,
                    { value -> viewModel.update { it.copy(customDosesPerDay = value) } },
                    label = { Text("Doses per day") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                state.unitsPerDose,
                { value -> viewModel.update { it.copy(unitsPerDose = value) } },
                label = { Text("Units per dose") },
                supportingText = { Text("e.g. 0.5 for a half ${state.form}") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.scheduleType != ScheduleType.PRN) {
                Text("Dose times")
                state.doseTimeEntries.forEachIndexed { index, entry ->
                    val label = when {
                        entry.slotId != null -> state.timeSlots.firstOrNull { it.id == entry.slotId }?.name ?: "Slot"
                        entry.customMinutes != null -> CsvFormat.formatMinuteOfDay(entry.customMinutes)
                        else -> "Custom"
                    }
                    val options = state.timeSlots.map { it.name } + "Custom"
                    ChoiceField(label = "Dose ${index + 1}", value = label, options = options) { picked ->
                        if (picked == "Custom") {
                            viewModel.updateDoseTimeEntry(index, DoseTimeEntry(null, entry.customMinutes ?: 480))
                            timeEditIndex = index
                        } else {
                            val slot = state.timeSlots.first { it.name == picked }
                            viewModel.updateDoseTimeEntry(index, DoseTimeEntry(slot.id, null))
                        }
                    }
                }
            }
            Text("Inventory", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                state.unitsPerPack,
                { value -> viewModel.update { it.copy(unitsPerPack = value) } },
                label = { Text("${formUnitLabel(state.form)} per blister strip (optional)") },
                supportingText = { Text("Lets you count by strips, e.g. 10") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                state.quantityOnHand,
                { value -> viewModel.update { it.copy(quantityOnHand = value) } },
                label = { Text("Count on hand (${formUnitLabel(state.form).lowercase()})") },
                supportingText = stripHint(state.quantityOnHand, state.unitsPerPack)?.let { hint -> { Text(hint) } },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { showDate = true }) { Text("Counted on ${dateFmt.format(state.countedOn)}") }
            OutlinedTextField(
                state.leadTimeDays,
                { value -> viewModel.update { it.copy(leadTimeDays = value) } },
                label = { Text("Lead time (days)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Track pillbox separately")
                    Text("Shows stock vs. pillbox split; adds Fill Pillbox action")
                }
                Switch(state.trackPillbox, { checked -> viewModel.update { it.copy(trackPillbox = checked) } })
            }
            Text("Prescription (Optional)", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    state.rxQuantityPrescribed,
                    { v -> viewModel.update { it.copy(rxQuantityPrescribed = v) } },
                    label = { Text("Qty on Rx (#)") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    state.rxQuantityDispensed,
                    { v -> viewModel.update { it.copy(rxQuantityDispensed = v) } },
                    label = { Text("Bought so far") },
                    modifier = Modifier.weight(1f),
                )
            }
            rxLeftHint(state.rxQuantityPrescribed, state.rxQuantityDispensed)?.let {
                Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(state.prescriber, { v -> viewModel.update { it.copy(prescriber = v) } }, label = { Text("Doctor") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { showRxDate = true }) {
                Text(state.rxDate?.let { "Prescription dated ${dateFmt.format(it)}" } ?: "Set prescription date")
            }
            Text("Pharmacy Info (Optional)", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(state.pharmacy, { v -> viewModel.update { it.copy(pharmacy = v) } }, label = { Text("Pharmacy") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.rxNumber, { v -> viewModel.update { it.copy(rxNumber = v) } }, label = { Text("Rx number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.refills, { v -> viewModel.update { it.copy(refills = v) } }, label = { Text("Refills remaining (doctor's \"refill ×N\")") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.notes, { v -> viewModel.update { it.copy(notes = v) } }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(onClick = { viewModel.save(onSaved) }, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
            if (state.isEditing) {
                TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete medication") }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete medication") },
            text = { Text("Remove ${state.name} from your list? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(onSaved) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
    if (showDate) {
        val initial = state.countedOn.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.update { it.copy(countedOn = date) }
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(picker) }
    }
    if (showRxDate) {
        val initial = (state.rxDate ?: LocalDate.now()).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showRxDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.update { it.copy(rxDate = date) }
                    }
                    showRxDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showRxDate = false }) { Text("Cancel") } },
        ) { DatePicker(picker) }
    }
    state.pendingScan?.let { scan ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissScanChoice() },
            title = { Text("Which medication is this?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("The prescription lists ${scan.items.size} medications. Pick the one for this entry.")
                    scan.items.forEach { item ->
                        TextButton(onClick = { viewModel.applyScan(scan, item) }, modifier = Modifier.fillMaxWidth()) {
                            Text(item.summary(), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { viewModel.dismissScanChoice() }) { Text("Cancel") } },
        )
    }
    timeEditIndex?.let { index ->
        val existing = state.doseTimeEntries.getOrNull(index)?.customMinutes ?: 480
        val timeState = rememberTimePickerState(initialHour = existing / 60, initialMinute = existing % 60)
        AlertDialog(
            onDismissRequest = { timeEditIndex = null },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = timeState.hour * 60 + timeState.minute
                    viewModel.updateDoseTimeEntry(index, DoseTimeEntry(null, minutes))
                    timeEditIndex = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { timeEditIndex = null }) { Text("Cancel") } },
            text = { TimePicker(timeState) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

internal fun formUnitLabel(form: String): String = when (form) {
    "capsule" -> "Capsules"
    "liquid" -> "mL"
    "patch" -> "Patches"
    "injection" -> "Doses"
    "inhaler" -> "Inhalers"
    "other" -> "Units"
    else -> "Tablets"
}

private fun stripHint(count: String, perStrip: String): String? {
    val c = count.toDoubleOrNull() ?: return null
    val p = perStrip.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
    val strips = kotlin.math.floor(c / p).toLong()
    val loose = c - strips * p
    val looseText = if (loose > 0.0) " + ${com.medcounter.ui.components.formatQty(loose)} loose" else ""
    return "= $strips full strip${if (strips == 1L) "" else "s"}$looseText"
}

private fun rxLeftHint(prescribed: String, bought: String): String? {
    val p = prescribed.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
    val b = bought.toDoubleOrNull() ?: 0.0
    val left = (p - b).coerceAtLeast(0.0)
    return "${com.medcounter.ui.components.formatQty(left)} of ${com.medcounter.ui.components.formatQty(p)} still available on this prescription"
}
