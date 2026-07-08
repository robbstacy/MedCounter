package com.purovida.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.ui.ChipRow
import com.purovida.app.ui.FieldLabel
import com.purovida.app.ui.FormScaffold
import com.purovida.app.ui.PhotoSourceSheet
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.PuroTextField
import com.purovida.app.util.ImageUtils
import com.purovida.core.HumidorItem
import com.purovida.core.countries
import com.purovida.core.strengthDisplay
import com.purovida.core.strengths
import com.purovida.core.vitolas
import com.purovida.core.wrapperTypes
import kotlinx.coroutines.launch

@Composable
fun HumidorAddScreen(navController: NavHostController) {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var brand by remember { mutableStateOf("") }
    var productLine by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var purchaseSource by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var msrp by remember { mutableStateOf("") }
    var targetAging by remember { mutableStateOf("6") }
    var notes by remember { mutableStateOf("") }

    var vitola by remember { mutableStateOf("Robusto") }
    var wrapper by remember { mutableStateOf("Natural") }
    var country by remember { mutableStateOf("Nicaragua") }
    var strength by remember { mutableStateOf("medium") }
    var looking by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun snack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun applyIdentifyResult(result: com.purovida.core.CigarIdentifyResult) {
        if (result.brand.isNotEmpty()) brand = result.brand
        if (result.productLine.isNotEmpty()) productLine = result.productLine
        if (result.size.isNotEmpty()) vitola = result.size
        if (result.wrapper.isNotEmpty()) wrapper = result.wrapper
        if (result.country.isNotEmpty()) country = result.country
        if (strengths.contains(result.strength)) strength = result.strength
        if (result.description.isNotEmpty() && notes.isEmpty()) notes = result.description
        if (!result.found || result.confidence == "low") {
            snack("Low confidence — the cigar couldn't be confidently identified. Please review the fields.")
        }
    }

    fun identify(uri: Uri) {
        scanning = true
        scope.launch {
            try {
                val bytes = ImageUtils.readScaledJpeg(context, uri)
                applyIdentifyResult(app.cigarApi.identifyCigar(bytes))
            } catch (e: Exception) {
                snack("Scan failed: ${e.message}")
            } finally {
                scanning = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) identify(uri) else scanning = false }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraUri
        if (ok && uri != null) identify(uri) else scanning = false
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

    fun autoFill() {
        if (brand.trim().isEmpty()) {
            snack("Enter a brand name first to look it up.")
            return
        }
        looking = true
        scope.launch {
            try {
                val data = app.cigarApi.lookupCigar(brand, productLine)
                if (data.size.isNotEmpty()) vitola = data.size
                if (data.wrapper.isNotEmpty()) wrapper = data.wrapper
                if (data.country.isNotEmpty()) country = data.country
                if (strengths.contains(data.strength)) strength = data.strength
                if (data.description.isNotEmpty() && notes.isEmpty()) notes = data.description
                if (!data.found) {
                    snack("No exact match found — fields filled with best-guess defaults.")
                }
            } catch (e: Exception) {
                snack("Lookup failed: ${e.message}")
            } finally {
                looking = false
            }
        }
    }

    fun save() {
        if (brand.trim().isEmpty()) {
            snack("Please enter a brand name.")
            return
        }
        val qty = quantity.toIntOrNull() ?: 0
        if (qty < 1) {
            snack("Quantity must be at least 1.")
            return
        }
        app.humidorStore.addItem { id, now ->
            HumidorItem(
                id = id,
                brand = brand.trim(),
                productLine = productLine.trim(),
                vitola = vitola,
                wrapper = wrapper,
                country = country,
                quantity = qty,
                purchaseDate = now,
                purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                msrp = msrp.toDoubleOrNull() ?: 0.0,
                purchaseSource = purchaseSource.trim(),
                agingStartDate = now,
                targetAgingMonths = targetAging.toIntOrNull() ?: 6,
                strength = strength,
                notes = notes.trim(),
                createdAt = now,
                updatedAt = now,
            )
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
        title = "Add to Humidor",
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        PuroColors.primary.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp),
                    )
                    .clickable(enabled = !scanning) { showSourceSheet = true }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (scanning) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = PuroColors.primary,
                        modifier = Modifier.size(14.dp),
                    )
                } else {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = PuroColors.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    if (scanning) "Identifying..." else "Scan a Cigar with AI",
                    color = PuroColors.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            FieldLabel("Brand *")
            PuroTextField(brand, { brand = it }, hint = "e.g. Padrón")
            FieldLabel("Product Line")
            Row(verticalAlignment = Alignment.CenterVertically) {
                PuroTextField(
                    productLine,
                    { productLine = it },
                    hint = "e.g. 1964 Anniversary Series",
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { if (!looking) autoFill() },
                    enabled = !looking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PuroColors.secondary,
                        contentColor = PuroColors.primary,
                        disabledContainerColor = PuroColors.secondary,
                        disabledContentColor = PuroColors.primary,
                    ),
                    modifier = Modifier.padding(start = 10.dp),
                ) {
                    if (looking) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = PuroColors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    } else {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text("AI Fill", modifier = Modifier.padding(start = 6.dp))
                }
            }
            FieldLabel("Size")
            ChipRow(options = vitolas, selected = vitola, onSelect = { vitola = it })
            FieldLabel("Wrapper")
            ChipRow(options = wrapperTypes, selected = wrapper, onSelect = { wrapper = it })
            FieldLabel("Country")
            ChipRow(options = countries, selected = country, onSelect = { country = it })
            FieldLabel("Strength")
            ChipRow(
                options = strengths,
                display = strengthDisplay,
                selected = strength,
                onSelect = { strength = it },
            )
            FieldLabel("Quantity")
            PuroTextField(quantity, { quantity = it }, hint = "1", numeric = true)
            FieldLabel("Purchase Source")
            PuroTextField(purchaseSource, { purchaseSource = it }, hint = "e.g. Famous Smoke Shop")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("Price Paid ($)")
                    PuroTextField(purchasePrice, { purchasePrice = it }, hint = "0", numeric = true)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("MSRP ($)")
                    PuroTextField(msrp, { msrp = it }, hint = "0", numeric = true)
                }
            }
            FieldLabel("Target Aging (months)")
            PuroTextField(targetAging, { targetAging = it }, hint = "6", numeric = true)
            FieldLabel("Notes")
            PuroTextField(notes, { notes = it }, hint = "Aging plans, thoughts...", maxLines = 3)
        }
    }
}
