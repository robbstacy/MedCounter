package com.medcounter.ui.screens.detail

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medcounter.domain.MedicationStatus
import com.medcounter.domain.ScheduleType
import com.medcounter.ui.components.StatusBadge
import com.medcounter.ui.components.formatQty
import com.medcounter.ui.theme.StatusOut
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayDate = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val auditDate = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

private enum class DetailDialog { None, Log, Restock, Count, Pause, Pillbox }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    viewModel: MedicationDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long, Long) -> Unit,
) {
    val item by viewModel.medWithCalc.collectAsStateWithLifecycle()
    val audits by viewModel.auditLogs.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf(DetailDialog.None) }
    val current = item

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.medication?.name ?: "Medication") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (current != null) {
                        IconButton(onClick = { onEdit(current.medication.id, current.medication.profileId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            Text("Medication not found", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val med = current.medication
        val calc = current.calculation
        val schedule = runCatching { ScheduleType.valueOf(med.scheduleType) }.getOrDefault(ScheduleType.DAILY)
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(calc.status)
                if (med.isPaused) Text("Paused", color = MaterialTheme.colorScheme.secondary)
            }
            Text("${formatQty(calc.currentQuantity)} on hand", style = MaterialTheme.typography.headlineSmall)
            val days = calc.daysRemaining?.toString() ?: "—"
            Text("$days days left")
            val runLabel = if (calc.isEstimate) "runs out (est.)" else "runs out"
            val runDate = calc.runOutDate?.let { displayDate.format(it) } ?: "—"
            Text("$runDate $runLabel")
            if (med.pillboxQuantity != null) {
                val pill = med.pillboxQuantity
                val bottle = (calc.currentQuantity - pill).coerceAtLeast(0.0)
                Text("${formatQty(bottle)} in bottle  ·  ${formatQty(pill)} in pillbox")
            }
            if (med.pharmacy.isNotBlank()) Text("Pharmacy: ${med.pharmacy}")
            if (med.rxNumber.isNotBlank()) Text("Rx: ${med.rxNumber}")
            med.refillsRemaining?.let { Text("Refills remaining: $it") }
            calc.reorderByDate?.let { date ->
                val overdue = calc.status == MedicationStatus.REORDER || calc.status == MedicationStatus.OUT
                Text(if (overdue) "Reorder by ${displayDate.format(date)} (due)" else "Reorder by ${displayDate.format(date)}")
            }
            if (med.refillsRemaining == 0) {
                Text("⚠ No refills remaining — renew prescription", color = StatusOut)
            }
            if (med.isPaused) {
                Text("⏸ Medication Paused", style = MaterialTheme.typography.titleMedium)
                if (med.pauseReason.isNotBlank()) Text(med.pauseReason)
            }
            if (schedule == ScheduleType.PRN) {
                Button(onClick = { dialog = DetailDialog.Log }, modifier = Modifier.fillMaxWidth()) {
                    Text("Log PRN Dose")
                }
            } else {
                Button(onClick = { dialog = DetailDialog.Log }, modifier = Modifier.fillMaxWidth()) {
                    Text("Log Dose / Extra Dose")
                }
            }
            OutlinedButton(onClick = { dialog = DetailDialog.Restock }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Restock")
            }
            OutlinedButton(onClick = { dialog = DetailDialog.Count }, modifier = Modifier.fillMaxWidth()) {
                Text("Update Count")
            }
            if (med.pillboxQuantity != null) {
                OutlinedButton(onClick = { dialog = DetailDialog.Pillbox }, modifier = Modifier.fillMaxWidth()) {
                    Text("Fill Pillbox")
                }
            }
            if (med.isPaused) {
                OutlinedButton(onClick = { viewModel.resumeMedication() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Resume Medication")
                }
            } else {
                OutlinedButton(onClick = { dialog = DetailDialog.Pause }, modifier = Modifier.fillMaxWidth()) {
                    Text("Pause Medication")
                }
            }
            Text("Activity", style = MaterialTheme.typography.titleMedium)
            if (audits.isEmpty()) Text("No activity yet")
            audits.forEach { log ->
                val whenText = java.time.Instant.ofEpochMilli(log.timestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .let { auditDate.format(it) }
                Text("$whenText · ${log.action.replace('_', ' ')}", style = MaterialTheme.typography.bodyMedium)
                if (log.note.isNotBlank()) Text(log.note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    when (dialog) {
        DetailDialog.Log -> LogDialog(
            prn = current?.medication?.scheduleType == "PRN",
            onDismiss = { dialog = DetailDialog.None },
            onConfirm = { units, extra ->
                viewModel.logDose(units, extra)
                dialog = DetailDialog.None
            },
        )
        DetailDialog.Restock -> RestockDialog(
            onDismiss = { dialog = DetailDialog.None },
            onConfirm = { qty, notes, used ->
                viewModel.addRestock(qty, notes, used)
                dialog = DetailDialog.None
            },
        )
        DetailDialog.Count -> CountDialog(
            expected = current?.calculation?.currentQuantity ?: 0.0,
            onDismiss = { dialog = DetailDialog.None },
            onConfirm = { qty, notes ->
                viewModel.updateCount(qty, notes)
                dialog = DetailDialog.None
            },
        )
        DetailDialog.Pause -> PauseDialog(
            onDismiss = { dialog = DetailDialog.None },
            onConfirm = {
                viewModel.pauseMedication(it)
                dialog = DetailDialog.None
            },
        )
        DetailDialog.Pillbox -> {
            val pill = current?.medication?.pillboxQuantity ?: 0.0
            val total = current?.calculation?.currentQuantity ?: 0.0
            PillboxDialog(
                bottle = (total - pill).coerceAtLeast(0.0),
                pillbox = pill,
                onDismiss = { dialog = DetailDialog.None },
                onConfirm = {
                    viewModel.moveToPillbox(it)
                    dialog = DetailDialog.None
                },
            )
        }
        DetailDialog.None -> Unit
    }
}

@Composable
private fun LogDialog(prn: Boolean, onDismiss: () -> Unit, onConfirm: (Double, Boolean) -> Unit) {
    var units by remember { mutableStateOf("1") }
    var extra by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (prn) "Log PRN Dose" else "Log Dose") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(units, { units = it }, label = { Text("Units taken") })
                if (!prn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(extra, { extra = it })
                        Text("Extra dose")
                    }
                    Text("Deducts from on-hand count")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(units.toDoubleOrNull() ?: 0.0, extra && !prn) }) { Text("Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RestockDialog(onDismiss: () -> Unit, onConfirm: (Double, String, Boolean) -> Unit) {
    var qty by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var used by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Restock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(qty, { qty = it }, label = { Text("Quantity added") })
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(used, { used = it })
                    Text("Used a refill")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(qty.toDoubleOrNull() ?: 0.0, notes, used) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CountDialog(expected: Double, onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var qty by remember { mutableStateOf(formatQty(expected)) }
    var notes by remember { mutableStateOf("") }
    val entered = qty.toDoubleOrNull()
    val shortfall = if (entered != null) expected - entered else 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Count") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(qty, { qty = it }, label = { Text("Actual count") })
                Text("Expected on hand: ${formatQty(expected)}")
                if (shortfall > 0.5) {
                    Text("⚠ ${formatQty(shortfall)} fewer than expected — will be flagged in activity log", color = StatusOut)
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") })
            }
        },
        confirmButton = {
            TextButton(onClick = { entered?.let { onConfirm(it, notes) } }, enabled = entered != null) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PauseDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pause Medication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    reason,
                    { reason = it },
                    label = { Text("Reason (optional)") },
                    placeholder = { Text("e.g. Inpatient care, medical hold") },
                )
                Text("Doses will not be counted while paused. The run-out estimate is frozen at your current supply.")
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason) }) { Text("Pause") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PillboxDialog(bottle: Double, pillbox: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val value = amount.toDoubleOrNull()
    val error = value != null && (value < 0 || value > bottle)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to pillbox") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    amount,
                    { amount = it },
                    label = { Text("Amount") },
                    placeholder = { Text("e.g. 7") },
                    isError = error,
                )
                Text("Currently in bottle: ${formatQty(bottle)}  ·  pillbox: ${formatQty(pillbox)}")
                if (error) Text("Must be between 0 and ${formatQty(bottle)}", color = StatusOut)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let(onConfirm) },
                enabled = value != null && !error,
            ) { Text("Move") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
