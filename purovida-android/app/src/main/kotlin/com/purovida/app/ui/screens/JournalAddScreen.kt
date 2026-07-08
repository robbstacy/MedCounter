package com.purovida.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.ui.ChipRow
import com.purovida.app.ui.FieldLabel
import com.purovida.app.ui.FlavorPicker
import com.purovida.app.ui.FormScaffold
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.PuroTextField
import com.purovida.app.ui.RatingRow
import com.purovida.core.JournalEntry
import com.purovida.core.beveragePairings
import com.purovida.core.countries
import com.purovida.core.strengthDisplay
import com.purovida.core.strengths
import com.purovida.core.vitolas
import com.purovida.core.wrapperTypes
import kotlinx.coroutines.launch

@Composable
fun JournalAddScreen(navController: NavHostController) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var brand by remember { mutableStateOf("") }
    var productLine by remember { mutableStateOf("") }
    var vitola by remember { mutableStateOf("") }
    var binder by remember { mutableStateOf("") }
    var filler by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var pairingNotes by remember { mutableStateOf("") }
    var purchaseSource by remember { mutableStateOf("") }
    var pricePaid by remember { mutableStateOf("") }
    var msrp by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var wrapper by remember { mutableStateOf("Natural") }
    var country by remember { mutableStateOf("Nicaragua") }
    var strength by remember { mutableStateOf("medium") }
    var body by remember { mutableStateOf("medium") }
    val flavorTags = remember { mutableStateListOf<String>() }
    var drawRating by remember { mutableStateOf(3) }
    var burnRating by remember { mutableStateOf(3) }
    var constructionRating by remember { mutableStateOf(3) }
    var overallRating by remember { mutableStateOf(3) }
    var beveragePairing by remember { mutableStateOf("") }
    var wouldSmokeAgain by remember { mutableStateOf(true) }
    var looking by remember { mutableStateOf(false) }

    fun snack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
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
        app.journalStore.addEntry { id, now ->
            JournalEntry(
                id = id,
                brand = brand.trim(),
                productLine = productLine.trim(),
                vitola = vitola.trim(),
                wrapper = wrapper,
                binder = binder.trim(),
                filler = filler.trim(),
                country = country,
                strength = strength,
                body = body,
                flavorTags = flavorTags.toList(),
                drawRating = drawRating,
                burnRating = burnRating,
                constructionRating = constructionRating,
                overallRating = overallRating,
                dateSmoked = now,
                location = location.trim(),
                smokingDuration = duration.toIntOrNull() ?: 60,
                beveragePairing = beveragePairing,
                pairingNotes = pairingNotes.trim(),
                purchaseSource = purchaseSource.trim(),
                pricePaid = pricePaid.toDoubleOrNull() ?: 0.0,
                msrp = msrp.toDoubleOrNull() ?: 0.0,
                notes = notes.trim(),
                wouldSmokeAgain = wouldSmokeAgain,
                createdAt = now,
                updatedAt = now,
            )
        }
        navController.popBackStack()
    }

    FormScaffold(
        title = "Log a Cigar",
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
            FieldLabel("Vitola / Size")
            ChipRow(options = vitolas, selected = vitola, onSelect = { vitola = it })
            PuroTextField(
                vitola,
                { vitola = it },
                hint = "Or type a custom size",
                modifier = Modifier.padding(top = 8.dp),
            )
            FieldLabel("Wrapper")
            ChipRow(options = wrapperTypes, selected = wrapper, onSelect = { wrapper = it })
            FieldLabel("Binder")
            PuroTextField(binder, { binder = it }, hint = "e.g. Nicaraguan")
            FieldLabel("Filler")
            PuroTextField(filler, { filler = it }, hint = "e.g. Nicaraguan")
            FieldLabel("Country")
            ChipRow(options = countries, selected = country, onSelect = { country = it })
            FieldLabel("Strength")
            ChipRow(
                options = strengths,
                display = strengthDisplay,
                selected = strength,
                onSelect = { strength = it },
            )
            FieldLabel("Body")
            ChipRow(
                options = listOf("light", "medium", "full"),
                display = mapOf("light" to "Light", "medium" to "Medium", "full" to "Full"),
                selected = body,
                onSelect = { body = it },
            )
            FieldLabel("Flavor Notes")
            FlavorPicker(
                selected = flavorTags,
                onToggle = { tag ->
                    if (flavorTags.contains(tag)) flavorTags.remove(tag) else flavorTags.add(tag)
                },
            )
            FieldLabel("Ratings")
            RatingRow("Draw", drawRating) { drawRating = it }
            RatingRow("Burn", burnRating) { burnRating = it }
            RatingRow("Construction", constructionRating) { constructionRating = it }
            RatingRow("Overall", overallRating) { overallRating = it }
            FieldLabel("Location")
            PuroTextField(location, { location = it }, hint = "e.g. Back Porch")
            FieldLabel("Smoking Duration (minutes)")
            PuroTextField(duration, { duration = it }, hint = "60", numeric = true)
            FieldLabel("Beverage Pairing")
            ChipRow(
                options = beveragePairings,
                selected = beveragePairing,
                onSelect = { beveragePairing = if (beveragePairing == it) "" else it },
            )
            FieldLabel("Pairing Notes")
            PuroTextField(
                pairingNotes,
                { pairingNotes = it },
                hint = "How did the pairing work?",
                maxLines = 3,
            )
            FieldLabel("Purchase Source")
            PuroTextField(purchaseSource, { purchaseSource = it }, hint = "e.g. Local Tobacconist")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("Price Paid ($)")
                    PuroTextField(pricePaid, { pricePaid = it }, hint = "0", numeric = true)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("MSRP ($)")
                    PuroTextField(msrp, { msrp = it }, hint = "0", numeric = true)
                }
            }
            FieldLabel("Notes")
            PuroTextField(
                notes,
                { notes = it },
                hint = "Your thoughts on this cigar...",
                maxLines = 4,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PuroColors.card)
                    .border(1.dp, PuroColors.border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Would smoke again",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PuroColors.text,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = wouldSmokeAgain,
                    onCheckedChange = { wouldSmokeAgain = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PuroColors.cream,
                        checkedTrackColor = PuroColors.primary,
                        uncheckedThumbColor = PuroColors.cream,
                        uncheckedTrackColor = PuroColors.muted,
                    ),
                )
            }
        }
    }
}
