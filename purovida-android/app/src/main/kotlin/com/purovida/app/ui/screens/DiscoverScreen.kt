package com.purovida.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.ui.ChipRow
import com.purovida.app.ui.CigarCard
import com.purovida.app.ui.EmptyState
import com.purovida.app.ui.PuroColors
import com.purovida.core.Cigar
import com.purovida.core.cigarCatalog
import com.purovida.core.getRecommendations

private val strengthFilters =
    listOf("All", "mild", "mild-medium", "medium", "medium-full", "full")
private val strengthFilterDisplay = mapOf(
    "All" to "All Strengths",
    "mild" to "Mild",
    "mild-medium" to "Mild-Med",
    "medium" to "Medium",
    "medium-full" to "Med-Full",
    "full" to "Full",
)

@Composable
fun DiscoverScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val entries by app.journalStore.entries.collectAsState()
    val profile by app.userStore.profile.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var strengthFilter by rememberSaveable { mutableStateOf("All") }
    var recommendationsTab by rememberSaveable { mutableStateOf(true) }

    val recommendations = getRecommendations(profile, entries, limit = 10)

    val filteredCatalog: List<Cigar> = run {
        var list = cigarCatalog
        if (query.trim().isNotEmpty()) {
            val q = query.lowercase()
            list = list.filter { c ->
                c.brand.lowercase().contains(q) ||
                    c.productLine.lowercase().contains(q) ||
                    c.flavorTags.any { it.lowercase().contains(q) } ||
                    c.country.lowercase().contains(q)
            }
        }
        if (strengthFilter != "All") {
            list = list.filter { it.strength == strengthFilter }
        }
        list.sortedByDescending { it.communityRating ?: 0 }
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        ) {
            Text(
                "Discover",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PuroColors.text,
            )
            SearchField(
                value = query,
                onValueChange = { query = it },
                hint = "Search brands, flavors, countries...",
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(Modifier.padding(top = 12.dp)) {
                DiscoverTab(
                    label = "For You",
                    active = recommendationsTab,
                    onTap = { recommendationsTab = true },
                    modifier = Modifier.weight(1f),
                )
                DiscoverTab(
                    label = "Catalog",
                    active = !recommendationsTab,
                    onTap = { recommendationsTab = false },
                    modifier = Modifier.weight(1f),
                )
            }
            if (!recommendationsTab) {
                ChipRow(
                    options = strengthFilters,
                    display = strengthFilterDisplay,
                    selected = strengthFilter,
                    onSelect = { strengthFilter = it },
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PuroColors.border),
        )
        if (recommendationsTab) {
            if (recommendations.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Explore,
                    title = "No recommendations yet",
                    subtitle = "Log more cigars to get personalized recommendations",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)) {
                    items(recommendations.size) { i ->
                        val rec = recommendations[i]
                        Column(
                            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                        ) {
                            CigarCard(
                                cigar = rec.cigar,
                                matchScore = rec.matchScore,
                                badge = when {
                                    i == 0 -> "TOP PICK"
                                    rec.type == "trending" -> "TRENDING"
                                    else -> null
                                },
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PuroColors.muted.copy(alpha = 0.53f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = PuroColors.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    rec.reason,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = PuroColors.mutedForeground,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredCatalog.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = "No cigars found",
                    subtitle = "Try different filters",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)) {
                    items(filteredCatalog.size) { i ->
                        CigarCard(
                            cigar = filteredCatalog[i],
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 12.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverTab(
    label: String,
    active: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clickable(onClick = onTap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = if (active) PuroColors.primary else PuroColors.mutedForeground,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (active) PuroColors.primary else Color.Transparent),
        )
    }
}
