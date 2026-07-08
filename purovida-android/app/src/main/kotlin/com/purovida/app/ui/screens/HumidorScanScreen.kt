package com.purovida.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.ui.FormScaffold
import com.purovida.app.ui.PhotoSourceSheet
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.PuroTextField
import com.purovida.app.util.ImageUtils
import com.purovida.core.CigarIdentifyResult
import com.purovida.core.HumidorItem
import com.purovida.core.strengths
import kotlinx.coroutines.launch

/**
 * Batch scan: photograph multiple cigars, identify each with AI,
 * review/adjust, then save all to the humidor.
 */
private class ScannedCigar(val id: String, val imageUri: Uri) {
    var result by mutableStateOf<CigarIdentifyResult?>(null)
    var error by mutableStateOf<String?>(null)
    var identifying by mutableStateOf(true)
    var quantity by mutableStateOf(1)
    var brand by mutableStateOf("")
    var productLine by mutableStateOf("")
}

@Composable
fun HumidorScanScreen(navController: NavHostController) {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val cigars = remember { mutableStateListOf<ScannedCigar>() }
    var saving by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun snack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun identifyOne(cigar: ScannedCigar) {
        scope.launch {
            try {
                val bytes = ImageUtils.readScaledJpeg(context, cigar.imageUri)
                val result = app.cigarApi.identifyCigar(bytes)
                cigar.result = result
                cigar.brand = result.brand
                cigar.productLine = result.productLine
                cigar.identifying = false
            } catch (e: Exception) {
                cigar.error = e.message ?: "$e"
                cigar.identifying = false
            }
        }
    }

    fun addPhotos(uris: List<Uri>) {
        for (uri in uris) {
            val cigar = ScannedCigar(
                id = "${System.nanoTime()}-${uri.hashCode()}",
                imageUri = uri,
            )
            cigars.add(cigar)
            identifyOne(cigar)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris -> if (uris.isNotEmpty()) addPhotos(uris) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraUri
        if (ok && uri != null) addPhotos(listOf(uri))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = ImageUtils.newCameraCaptureUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            snack("Camera permission is required to scan cigars.")
        }
    }

    fun saveAll() {
        val identified = cigars.filter { it.brand.trim().isNotEmpty() && it.error == null }
        if (identified.isEmpty()) {
            snack("Nothing to save — no cigars were successfully identified.")
            return
        }
        saving = true
        for (c in identified) {
            val r = c.result
            app.humidorStore.addItem { id, now ->
                HumidorItem(
                    id = id,
                    brand = c.brand.trim(),
                    productLine = c.productLine.trim(),
                    vitola = r?.size ?: "",
                    wrapper = r?.wrapper ?: "",
                    country = r?.country ?: "",
                    quantity = c.quantity,
                    purchaseDate = now,
                    agingStartDate = now,
                    targetAgingMonths = 6,
                    strength = if (r != null && strengths.contains(r.strength)) {
                        r.strength
                    } else {
                        "medium"
                    },
                    notes = r?.description ?: "",
                    createdAt = now,
                    updatedAt = now,
                )
            }
        }
        navController.popBackStack()
    }

    if (showSourceSheet) {
        PhotoSourceSheet(
            onDismiss = { showSourceSheet = false },
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
        title = "Scan Cigars",
        onClose = { navController.popBackStack() },
        saveLabel = if (saving) "Saving..." else "Save All",
        onSave = if (cigars.isNotEmpty() && !saving) {
            { saveAll() }
        } else {
            null
        },
        showSave = cigars.isNotEmpty(),
        snackbarHostState = snackbarHostState,
    ) { padding ->
        if (cigars.isEmpty()) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = PuroColors.mutedForeground,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    "Scan your cigars",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PuroColors.text,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    "Photograph cigar bands or boxes and the AI will identify them and add them to your humidor.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = PuroColors.mutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = { showSourceSheet = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PuroColors.primary,
                        contentColor = PuroColors.primaryForeground,
                    ),
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Icon(
                        Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Add Photos", modifier = Modifier.padding(start = 8.dp))
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
            ) {
                items(cigars.size) { i ->
                    ScannedCigarRow(
                        cigar = cigars[i],
                        onRemove = { cigars.remove(cigars[i]) },
                    )
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                PuroColors.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp),
                            )
                            .clickable { showSourceSheet = true }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            tint = PuroColors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Add More Photos",
                            color = PuroColors.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedCigarRow(cigar: ScannedCigar, onRemove: () -> Unit) {
    val context = LocalContext.current
    val thumbnail = remember(cigar.imageUri) {
        runCatching {
            context.contentResolver.openInputStream(cigar.imageUri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PuroColors.card)
            .border(1.dp, PuroColors.border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PuroColors.muted),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Outlined.ImageNotSupported,
                    contentDescription = null,
                    tint = PuroColors.mutedForeground,
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            when {
                cigar.identifying -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = PuroColors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "Identifying...",
                            fontSize = 13.sp,
                            color = PuroColors.mutedForeground,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
                cigar.error != null -> {
                    Text(
                        "Failed: ${cigar.error}",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = PuroColors.destructive,
                    )
                }
                else -> {
                    PuroTextField(
                        value = cigar.brand,
                        onValueChange = { cigar.brand = it },
                        hint = "Brand",
                    )
                    Text(
                        cigar.productLine.ifEmpty { "Unknown line" },
                        fontSize = 12.sp,
                        color = PuroColors.mutedForeground,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    cigar.result?.let { result ->
                        Text(
                            "Confidence: ${result.confidence}",
                            fontSize = 11.sp,
                            color = if (result.confidence == "high") {
                                PuroColors.success
                            } else {
                                PuroColors.mutedForeground
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = PuroColors.mutedForeground,
                    modifier = Modifier.size(18.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (cigar.quantity > 1) cigar.quantity-- }) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Decrease",
                        tint = PuroColors.text,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    "${cigar.quantity}",
                    fontWeight = FontWeight.Bold,
                    color = PuroColors.text,
                )
                IconButton(onClick = { cigar.quantity++ }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Increase",
                        tint = PuroColors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
