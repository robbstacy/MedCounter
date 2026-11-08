package com.medcounter.ui.screens.refills

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medcounter.data.RxPhotoStore
import com.medcounter.domain.RefillCandidate
import com.medcounter.ui.components.sharePhotos
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.medcounter.domain.RefillPlanner
import com.medcounter.ui.components.StatusBadge
import com.medcounter.ui.components.formatQty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillListScreen(viewModel: RefillListViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val multiplePeople = state.profiles.size > 1
    var includePhotos by remember { mutableStateOf(true) }
    // Newest attached prescription photo for each selected medication.
    val selectedPhotos = remember(state.selectedIds, state.candidates) {
        state.candidates
            .filter { it.item.medication.id in state.selectedIds }
            .mapNotNull { RxPhotoStore.list(context, it.item.medication.id).firstOrNull() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refill list") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val count = state.selectedIds.count { id -> state.candidates.any { it.item.medication.id == id } }
                    Text(
                        if (count == 0) "Tick the medications to refill" else "$count medication${if (count == 1) "" else "s"} in the message",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Button(
                            onClick = { copyToClipboard(context, state.message) },
                            enabled = state.message.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Text("  Copy")
                        }
                        OutlinedButton(
                            onClick = {
                                if (includePhotos && selectedPhotos.isNotEmpty()) {
                                    sharePhotos(context, selectedPhotos, state.message)
                                } else {
                                    shareText(context, state.message)
                                }
                            },
                            enabled = state.message.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text("  Send…")
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Pick what to refill, then copy or share the message with whoever is going to the pharmacy. " +
                        "Sorted by soonest to run out.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (multiplePeople) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(state.profileFilter == null, { viewModel.setFilter(null) }, label = { Text("Everyone") })
                        state.profiles.forEach { p ->
                            FilterChip(state.profileFilter == p.id, { viewModel.setFilter(p.id) }, label = { Text(p.name) })
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = viewModel::selectDue) { Text("Select due") }
                    TextButton(onClick = viewModel::selectAll) { Text("All") }
                    TextButton(onClick = viewModel::clear) { Text("None") }
                }
            }
            if (state.loaded && state.candidates.isEmpty()) {
                item { Text("No medications yet.") }
            }
            items(state.candidates, key = { it.item.medication.id }) { candidate ->
                RefillRow(
                    candidate = candidate,
                    showPerson = multiplePeople && state.profileFilter == null,
                    checked = candidate.item.medication.id in state.selectedIds,
                    onToggle = { viewModel.toggle(candidate.item.medication.id) },
                )
            }
            item {
                Text("Message", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Text(
                            state.message.ifBlank { "Select at least one medication." },
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (selectedPhotos.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Include prescription photos")
                            Text(
                                "${selectedPhotos.size} photo${if (selectedPhotos.size == 1) "" else "s"} go with Send…",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(includePhotos, { includePhotos = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun RefillRow(candidate: RefillCandidate, showPerson: Boolean, checked: Boolean, onToggle: () -> Unit) {
    val med = candidate.item.medication
    val calc = candidate.item.calculation
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, { onToggle() })
            Column(Modifier.weight(1f)) {
                val strength = listOf(med.strength, med.strengthUnit).filter { it.isNotBlank() }.joinToString(" ")
                Text(
                    listOf(med.name, strength).filter { it.isNotBlank() }.joinToString(" ") +
                        if (showPerson) " · ${candidate.profileName}" else "",
                    style = MaterialTheme.typography.titleSmall,
                )
                val details = buildList {
                    add(calc.daysRemaining?.let { "$it days left" } ?: "${formatQty(calc.currentQuantity)} on hand")
                    med.rxQuantityLeft?.let { add("${formatQty(it)} left on Rx") }
                    med.refillsRemaining?.let { add("$it refills") }
                    if (med.rxNumber.isNotBlank()) add("Rx #${med.rxNumber}")
                    if (med.isPaused) add("paused")
                }
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                if (RefillPlanner.needsNewPrescription(med)) {
                    Text("Needs new prescription", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            StatusBadge(calc.status)
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Refill list", text))
    // Android 13+ shows its own clipboard confirmation; a toast would cover it.
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Refill list copied. Long-press in a chat and tap Paste.", Toast.LENGTH_LONG).show()
    }
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Send refill list"))
}
