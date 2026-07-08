package com.purovida.app.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.Routes
import com.purovida.app.ui.FlavorTagsRow
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.SectionHeader
import com.purovida.app.ui.StatCard
import com.purovida.app.ui.money0
import com.purovida.core.experienceDisplay
import com.purovida.core.getAnalytics
import java.io.File
import java.util.Locale

@Composable
fun ProfileScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val entries by app.journalStore.entries.collectAsState()
    val items by app.humidorStore.items.collectAsState()
    val profile by app.userStore.profile.collectAsState()

    val analytics = getAnalytics(entries)
    val humidorCount = items.sumOf { it.quantity }
    val countryCount = entries.map { it.country }.toSet().size

    val strengthData = analytics.strengthBreakdown.map { s ->
        s.strength.split("-").joinToString("-") { w ->
            w.replaceFirstChar { it.titlecase(Locale.US) }
        } to s.count
    }
    val brandData = analytics.topBrands.take(5).map { it.brand to it.count }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PuroColors.card)
                        .border(1.dp, PuroColors.border, RoundedCornerShape(16.dp))
                        .clickable { navController.navigate(Routes.PROFILE_EDIT) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = PuroColors.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "Edit Profile",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PuroColors.primary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AvatarCircle(avatarUri = profile.avatarUri, size = 80.dp, iconSize = 32.dp)
                Text(
                    profile.username,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PuroColors.text,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PuroColors.primary.copy(alpha = 0.13f))
                        .border(
                            1.dp,
                            PuroColors.primary.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        (experienceDisplay[profile.experienceLevel] ?: profile.experienceLevel)
                            .uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = PuroColors.primary,
                    )
                }
                if (profile.bio.isNotEmpty()) {
                    Text(
                        profile.bio,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        color = PuroColors.mutedForeground,
                        modifier = Modifier.padding(start = 40.dp, end = 40.dp, top = 10.dp),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp)
                    .height(1.dp)
                    .background(PuroColors.primary.copy(alpha = 0.4f)),
            )
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        Icons.Outlined.MenuBook,
                        "Cigars Logged",
                        "${analytics.totalEntries}",
                        accent = true,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        Icons.Outlined.StarOutline,
                        "Avg Rating",
                        if (analytics.avgRating > 0) "%.1f".format(analytics.avgRating) else "–",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        Icons.Outlined.Inventory2,
                        "In Humidor",
                        "$humidorCount",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        Icons.Outlined.AttachMoney,
                        "Total Spent",
                        "$" + money0(analytics.totalSpent),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        Icons.Outlined.CheckCircle,
                        "Would Smoke Again",
                        "${Math.round(analytics.wouldSmokeAgainRate * 100)}%",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        Icons.Outlined.Public,
                        "Countries",
                        "$countryCount",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (analytics.topFlavors.isNotEmpty()) {
            item {
                Box(Modifier.padding(top = 24.dp)) {
                    SectionHeader(
                        title = "Flavor Profile",
                        subtitle = "Your top detected flavors",
                    )
                }
                SectionCard {
                    FlavorTagsRow(analytics.topFlavors.map { it.tag })
                }
            }
        }
        if (strengthData.isNotEmpty()) {
            item {
                Box(Modifier.padding(top = 24.dp)) {
                    SectionHeader(title = "Strength Breakdown")
                }
                SectionCard {
                    BarChart(data = strengthData, color = PuroColors.primary)
                }
            }
        }
        if (brandData.isNotEmpty()) {
            item {
                Box(Modifier.padding(top = 24.dp)) {
                    SectionHeader(title = "Favorite Brands")
                }
                SectionCard {
                    BarChart(data = brandData, color = PuroColors.accent)
                }
            }
        }
        item {
            Box(Modifier.padding(top = 24.dp)) {
                SectionHeader(title = "My Preferences")
            }
            SectionCard {
                Column {
                    if (profile.preferredStrengths.isNotEmpty()) {
                        PrefRow("Strengths", profile.preferredStrengths.joinToString(", "))
                    }
                    if (profile.preferredWrappers.isNotEmpty()) {
                        PrefRow("Wrappers", profile.preferredWrappers.joinToString(", "))
                    }
                    if (profile.preferredCountries.isNotEmpty()) {
                        PrefRow("Countries", profile.preferredCountries.joinToString(", "))
                    }
                    if (profile.humidorSize > 0) {
                        PrefRow("Humidor Size", "${profile.humidorSize} cigars")
                    }
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PuroColors.primary.copy(alpha = 0.07f))
                    .border(
                        1.dp,
                        PuroColors.primary.copy(alpha = 0.27f),
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { navController.navigate(Routes.ASSISTANT) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Memory,
                    contentDescription = null,
                    tint = PuroColors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(
                        "AI Cigar Assistant",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PuroColors.primary,
                    )
                    Text(
                        "Get personalized recommendations and advice",
                        fontSize = 12.sp,
                        color = PuroColors.mutedForeground,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = PuroColors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun AvatarCircle(avatarUri: String?, size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(avatarUri) {
        avatarUri
            ?.takeIf { File(it).exists() }
            ?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(PuroColors.primary.copy(alpha = 0.13f))
            .border(2.dp, PuroColors.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Outlined.Person,
                contentDescription = "Avatar",
                tint = PuroColors.primary,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PuroColors.card)
            .border(1.dp, PuroColors.border, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun BarChart(data: List<Pair<String, Int>>, color: Color) {
    val max = data.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column {
        for ((label, value) in data) {
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, fontSize = 12.sp, color = PuroColors.mutedForeground)
                    Text(
                        "$value",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PuroColors.text,
                    )
                }
                LinearProgressIndicator(
                    progress = { value.toFloat() / max },
                    color = color,
                    trackColor = PuroColors.muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}

@Composable
private fun PrefRow(label: String, value: String) {
    Column {
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
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PuroColors.border),
        )
    }
}
