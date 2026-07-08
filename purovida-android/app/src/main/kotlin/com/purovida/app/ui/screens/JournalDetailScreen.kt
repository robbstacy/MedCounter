package com.purovida.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.ui.FlavorTagsRow
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.RatingStars
import com.purovida.app.ui.formatShortDate
import com.purovida.core.strengthDisplay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDetailScreen(navController: NavHostController, entryId: String) {
    val app = LocalApp.current
    val entries by app.journalStore.entries.collectAsState()
    val entry = entries.firstOrNull { it.id == entryId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (entry == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Entry not found", color = PuroColors.text)
        }
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = PuroColors.card,
            titleContentColor = PuroColors.text,
            textContentColor = PuroColors.text,
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete this journal entry?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    app.journalStore.deleteEntry(entryId)
                    navController.popBackStack()
                }) {
                    Text("Delete", color = PuroColors.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = PuroColors.text)
                }
            },
        )
    }

    Scaffold(
        containerColor = PuroColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PuroColors.background,
                    titleContentColor = PuroColors.text,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PuroColors.text,
                        )
                    }
                },
                title = {
                    Text(entry.brand, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = PuroColors.destructive,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        ) {
            Text(
                entry.brand.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = PuroColors.primary,
            )
            Text(
                "${entry.productLine} ${entry.vitola}".trim(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PuroColors.text,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(rating = entry.overallRating, size = 20.dp)
                Text(
                    "${entry.overallRating}/5",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PuroColors.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            DetailCard(modifier = Modifier.padding(top = 20.dp)) {
                DetailRow("Date", formatShortDate(entry.dateSmoked))
                if (entry.location.isNotEmpty()) DetailRow("Location", entry.location)
                DetailRow("Duration", "${entry.smokingDuration} min")
                DetailRow("Strength", strengthDisplay[entry.strength] ?: entry.strength)
                DetailRow("Body", entry.body)
                if (entry.wrapper.isNotEmpty()) DetailRow("Wrapper", entry.wrapper)
                if (entry.binder.isNotEmpty()) DetailRow("Binder", entry.binder)
                if (entry.filler.isNotEmpty()) DetailRow("Filler", entry.filler)
                if (entry.country.isNotEmpty()) DetailRow("Country", entry.country)
            }
            SectionLabel("RATINGS")
            DetailCard {
                RatingDetailRow("Draw", entry.drawRating)
                RatingDetailRow("Burn", entry.burnRating)
                RatingDetailRow("Construction", entry.constructionRating)
                RatingDetailRow("Overall", entry.overallRating)
            }
            if (entry.flavorTags.isNotEmpty()) {
                SectionLabel("FLAVOR NOTES")
                FlavorTagsRow(entry.flavorTags)
            }
            if (entry.beveragePairing.isNotEmpty()) {
                SectionLabel("PAIRING")
                DetailCard {
                    DetailRow("Beverage", entry.beveragePairing)
                    if (entry.pairingNotes.isNotEmpty()) {
                        Text(
                            entry.pairingNotes,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 19.sp,
                            color = PuroColors.text,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            }
            if (entry.pricePaid > 0 || entry.purchaseSource.isNotEmpty()) {
                SectionLabel("PURCHASE")
                DetailCard {
                    if (entry.purchaseSource.isNotEmpty()) {
                        DetailRow("Source", entry.purchaseSource)
                    }
                    if (entry.pricePaid > 0) {
                        DetailRow("Price Paid", "$" + "%.2f".format(Locale.US, entry.pricePaid))
                    }
                    if (entry.msrp > 0) {
                        DetailRow("MSRP", "$" + "%.2f".format(Locale.US, entry.msrp))
                    }
                }
            }
            if (entry.notes.isNotEmpty()) {
                SectionLabel("NOTES")
                DetailCard {
                    Text(
                        entry.notes,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = PuroColors.text,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            if (entry.wouldSmokeAgain) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PuroColors.success.copy(alpha = 0.13f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = PuroColors.success,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Would smoke again",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PuroColors.success,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.7.sp,
        color = PuroColors.mutedForeground,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PuroColors.card)
            .border(1.dp, PuroColors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PuroColors.mutedForeground,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            fontSize = 13.sp,
            color = PuroColors.text,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(2f),
        )
    }
}

@Composable
private fun RatingDetailRow(label: String, rating: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PuroColors.mutedForeground,
        )
        RatingStars(rating = rating, size = 16.dp)
    }
}
