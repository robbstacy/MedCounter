package com.purovida.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.Routes
import com.purovida.app.ui.CigarCard
import com.purovida.app.ui.EmptyState
import com.purovida.app.ui.JournalEntryCard
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.SectionHeader
import com.purovida.app.ui.StatCard
import com.purovida.app.ui.money0
import com.purovida.core.getAnalytics
import com.purovida.core.getRecommendations

@Composable
fun HomeScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val entries by app.journalStore.entries.collectAsState()
    val items by app.humidorStore.items.collectAsState()
    val profile by app.userStore.profile.collectAsState()

    val analytics = getAnalytics(entries)
    val recommendations = getRecommendations(profile, entries, limit = 3)
    val recentEntries = entries.take(3)

    val topFlavor = analytics.topFlavors.firstOrNull()?.tag ?: "–"
    val topBrand = analytics.topBrands.firstOrNull()?.brand ?: "–"
    val humidorCount = items.sumOf { it.quantity }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "WELCOME BACK",
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = PuroColors.mutedForeground,
                    )
                    Text(
                        profile.username,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PuroColors.text,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PuroColors.primary)
                        .clickable { navController.navigate(Routes.ASSISTANT) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Memory,
                        contentDescription = "AI Assistant",
                        tint = PuroColors.primaryForeground,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
                    .height(1.dp)
                    .background(PuroColors.primary.copy(alpha = 0.5f)),
            )
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    Icons.Outlined.MenuBook,
                    "Smoked",
                    "${analytics.totalEntries}",
                    accent = true,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    Icons.Outlined.Inventory2,
                    "Humidor",
                    "$humidorCount",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    Icons.Outlined.StarOutline,
                    "Avg Rating",
                    if (analytics.avgRating > 0) "%.1f".format(analytics.avgRating) else "–",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    Icons.Outlined.AttachMoney,
                    "Spent",
                    "$" + money0(analytics.totalSpent),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    Icons.Outlined.LocalOffer,
                    "Top Flavor",
                    topFlavor,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    Icons.Outlined.EmojiEvents,
                    "Fav Brand",
                    topBrand.split(" ").first(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PuroColors.primary)
                        .clickable { navController.navigate(Routes.JOURNAL_ADD) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = PuroColors.primaryForeground,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Log a Cigar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PuroColors.primaryForeground,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PuroColors.card)
                        .clickable { navController.navigate(Routes.HUMIDOR_ADD) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                        tint = PuroColors.text,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Add to Humidor",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PuroColors.text,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
        if (recommendations.isNotEmpty()) {
            item {
                Box(Modifier.padding(top = 28.dp)) {
                    SectionHeader(
                        title = "Recommended For You",
                        subtitle = "Based on your taste profile",
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(recommendations.size) { i ->
                        val rec = recommendations[i]
                        CigarCard(
                            cigar = rec.cigar,
                            matchScore = rec.matchScore,
                            badge = when (rec.type) {
                                "you-may-enjoy" -> "FOR YOU"
                                "trending" -> "TRENDING"
                                else -> null
                            },
                            modifier = Modifier.width(290.dp),
                        )
                    }
                }
            }
        }
        item {
            Box(Modifier.padding(top = 24.dp)) {
                SectionHeader(title = "Recent Entries")
            }
        }
        if (recentEntries.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.MenuBook,
                    title = "No entries yet",
                    subtitle = "Start logging your cigars",
                    actionLabel = "Log your first cigar",
                    onAction = { navController.navigate(Routes.JOURNAL_ADD) },
                )
            }
        } else {
            items(recentEntries.size) { i ->
                val entry = recentEntries[i]
                JournalEntryCard(
                    entry = entry,
                    onTap = { navController.navigate(Routes.journalDetail(entry.id)) },
                )
            }
        }
    }
}
