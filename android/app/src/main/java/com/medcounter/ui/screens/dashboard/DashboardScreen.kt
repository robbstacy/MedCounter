package com.medcounter.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medcounter.domain.MedicationStatus
import com.medcounter.ui.components.MedicationCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddMedication: (Long) -> Unit,
    onOpenMedication: (Long) -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val selected by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var profileMenu by remember { mutableStateOf(false) }
    val profileId = selected?.id ?: -1L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PillStock") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Manage profiles") },
                            onClick = { menuOpen = false; onOpenProfiles() },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = { menuOpen = false; onOpenSettings() },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (profiles.isNotEmpty()) {
                FloatingActionButton(onClick = { onAddMedication(profileId) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Medication")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (profiles.isNotEmpty()) {
                item {
                    TextButton(onClick = { profileMenu = true }) {
                        Text(selected?.name ?: "Profile")
                    }
                    DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name) },
                                onClick = {
                                    viewModel.selectProfile(profile.id)
                                    profileMenu = false
                                },
                            )
                        }
                    }
                }
            }
            if (profiles.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Let's add your first medication", style = MaterialTheme.typography.headlineSmall)
                        Text("PillStock tracks how many days of each medication you have left — so you always know when to call the pharmacy.")
                        TextButton(onClick = { onAddMedication(-1L) }) { Text("+ Add my first medication") }
                    }
                }
            } else if (medications.isEmpty()) {
                item {
                    Text("No medications yet", style = MaterialTheme.typography.titleLarge)
                    Text("Add your first medication for ${selected?.name ?: "this profile"} to start tracking.")
                }
            } else {
                val attention = medications.filter { !it.medication.isPaused && it.calculation.status != MedicationStatus.OK }
                val stocked = medications.filter { !it.medication.isPaused && it.calculation.status == MedicationStatus.OK }
                val held = medications.filter { it.medication.isPaused }
                if (attention.isNotEmpty()) {
                    item { Text("Needs Attention", style = MaterialTheme.typography.titleMedium) }
                    items(attention, key = { "a${it.medication.id}" }) { item ->
                        MedicationCard(item, selected?.colorTag ?: "#3D6B4F", onClick = { onOpenMedication(item.medication.id) })
                    }
                }
                if (stocked.isNotEmpty()) {
                    item { Text("Stocked", style = MaterialTheme.typography.titleMedium) }
                    items(stocked, key = { "s${it.medication.id}" }) { item ->
                        MedicationCard(item, selected?.colorTag ?: "#3D6B4F", onClick = { onOpenMedication(item.medication.id) })
                    }
                }
                if (held.isNotEmpty()) {
                    item { Text("On Hold", style = MaterialTheme.typography.titleMedium) }
                    items(held, key = { "h${it.medication.id}" }) { item ->
                        MedicationCard(item, selected?.colorTag ?: "#3D6B4F", onClick = { onOpenMedication(item.medication.id) })
                    }
                }
            }
        }
    }
}
