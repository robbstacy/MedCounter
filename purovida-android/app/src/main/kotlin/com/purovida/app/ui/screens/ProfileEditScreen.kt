package com.purovida.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.ui.ChipRow
import com.purovida.app.ui.FieldLabel
import com.purovida.app.ui.FlavorPicker
import com.purovida.app.ui.FormScaffold
import com.purovida.app.ui.PhotoSourceSheet
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.PuroTextField
import com.purovida.app.ui.SelectChip
import com.purovida.app.util.ImageUtils
import com.purovida.core.countries
import com.purovida.core.experienceDisplay
import com.purovida.core.strengthDisplay
import com.purovida.core.strengths
import com.purovida.core.wrapperTypes
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(navController: NavHostController) {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val profile by app.userStore.profile.collectAsState()

    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var humidorSize by remember { mutableStateOf("${profile.humidorSize}") }
    var experienceLevel by remember { mutableStateOf(profile.experienceLevel) }
    val selStrengths = remember { mutableStateListOf(*profile.preferredStrengths.toTypedArray()) }
    val selFlavors = remember { mutableStateListOf(*profile.preferredFlavors.toTypedArray()) }
    val selWrappers = remember { mutableStateListOf(*profile.preferredWrappers.toTypedArray()) }
    val selCountries = remember { mutableStateListOf(*profile.preferredCountries.toTypedArray()) }
    var avatarUri by remember { mutableStateOf(profile.avatarUri) }
    var showAvatarSheet by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun snack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun toggle(list: MutableList<String>, value: String) {
        if (list.contains(value)) list.remove(value) else list.add(value)
    }

    // Copy into app storage so the path stays valid across restarts (original behavior).
    fun saveAvatar(uri: Uri) {
        try {
            avatarUri = ImageUtils.copyToAppStorage(context, uri, "avatar")
        } catch (e: Exception) {
            snack("Could not save photo: ${e.message}")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) saveAvatar(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraUri
        if (ok && uri != null) saveAvatar(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = ImageUtils.newCameraCaptureUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            snack("Camera permission is required to take a photo.")
        }
    }

    fun save() {
        if (username.trim().isEmpty()) {
            snack("Please enter a username.")
            return
        }
        app.userStore.updateProfile(
            profile.copy(
                username = username.trim(),
                bio = bio.trim(),
                experienceLevel = experienceLevel,
                humidorSize = humidorSize.toIntOrNull() ?: 0,
                preferredStrengths = selStrengths.toList(),
                preferredFlavors = selFlavors.toList(),
                preferredWrappers = selWrappers.toList(),
                preferredCountries = selCountries.toList(),
                avatarUri = avatarUri,
            ),
        )
        navController.popBackStack()
    }

    if (showAvatarSheet) {
        PhotoSourceSheet(
            onDismiss = { showAvatarSheet = false },
            onCamera = {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }

    FormScaffold(
        title = "Edit Profile",
        onClose = { navController.popBackStack() },
        onSave = { save() },
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.clickable { showAvatarSheet = true }) {
                    AvatarCircle(avatarUri = avatarUri, size = 88.dp, iconSize = 36.dp)
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PuroColors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = "Change photo",
                            tint = PuroColors.primaryForeground,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            FieldLabel("Username")
            PuroTextField(username, { username = it })
            FieldLabel("Bio")
            PuroTextField(bio, { bio = it }, maxLines = 3)
            FieldLabel("Experience Level")
            ChipRow(
                options = experienceDisplay.keys.toList(),
                display = experienceDisplay,
                selected = experienceLevel,
                onSelect = { experienceLevel = it },
            )
            FieldLabel("Humidor Size (cigars)")
            PuroTextField(humidorSize, { humidorSize = it }, numeric = true)
            FieldLabel("Preferred Strengths")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (s in strengths) {
                    SelectChip(
                        label = strengthDisplay[s] ?: s,
                        selected = selStrengths.contains(s),
                        onTap = { toggle(selStrengths, s) },
                    )
                }
            }
            FieldLabel("Preferred Flavors")
            FlavorPicker(
                selected = selFlavors,
                onToggle = { toggle(selFlavors, it) },
            )
            FieldLabel("Preferred Wrappers")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (w in wrapperTypes) {
                    SelectChip(
                        label = w,
                        selected = selWrappers.contains(w),
                        onTap = { toggle(selWrappers, w) },
                    )
                }
            }
            FieldLabel("Preferred Countries")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (c in countries) {
                    SelectChip(
                        label = c,
                        selected = selCountries.contains(c),
                        onTap = { toggle(selCountries, c) },
                    )
                }
            }
        }
    }
}
