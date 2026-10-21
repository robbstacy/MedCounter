package com.medcounter.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medcounter.data.entity.ProfileEntity
import com.medcounter.ui.theme.ProfileColorHexes
import com.medcounter.ui.theme.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    onBack: () -> Unit,
    onProfileSelected: () -> Unit,
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ProfileEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProfileEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add profile")
            }
        },
    ) { padding ->
        if (profiles.isEmpty()) {
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text("No profiles yet", style = MaterialTheme.typography.titleLarge)
                Text("Tap + to create your first profile.")
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(profiles, key = { it.id }) { profile ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(profile.colorTag)),
                            )
                            Column(Modifier.weight(1f).clickable {
                                viewModel.select(profile)
                                onProfileSelected()
                            }) {
                                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                Text("Tap to select")
                            }
                            TextButton(onClick = { editing = profile }) { Text("Edit") }
                            TextButton(onClick = { deleting = profile }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        ProfileDialog(
            existing = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, color ->
                viewModel.save(editing, name, color)
                creating = false
                editing = null
            },
        )
    }
    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text("Delete \"${profile.name}\" and all their medications? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(profile); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileDialog(existing: ProfileEntity?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var color by remember(existing) { mutableStateOf(existing?.colorTag ?: ProfileColorHexes.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add profile" else "Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
                Text("Color")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileColorHexes.forEach { hex ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (hex.equals(color, ignoreCase = true)) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { color = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, color) }, enabled = name.isNotBlank()) {
                Text(if (existing == null) "Add" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
