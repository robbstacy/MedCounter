package com.purovida.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoCamera
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
import com.purovida.app.ui.EmptyState
import com.purovida.app.ui.HumidorItemCard
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.StatCard
import com.purovida.app.ui.money0
import java.time.Instant

@Composable
fun HumidorScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val items by app.humidorStore.items.collectAsState()

    val totalCount = items.sumOf { it.quantity }
    val totalValue = items.sumOf { it.purchasePrice * it.quantity }
    val now = System.currentTimeMillis()
    val readyCount = items.count { item ->
        val start = runCatching { Instant.parse(item.agingStartDate) }
            .getOrDefault(Instant.now()).toEpochMilli()
        val target = start + item.targetAgingMonths * 30L * 24 * 60 * 60 * 1000
        now >= target
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Humidor",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PuroColors.text,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PuroColors.card)
                            .border(1.dp, PuroColors.border, CircleShape)
                            .clickable { navController.navigate(Routes.HUMIDOR_SCAN) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = "Scan cigars",
                            tint = PuroColors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PuroColors.primary)
                            .clickable { navController.navigate(Routes.HUMIDOR_ADD) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add to humidor",
                            tint = PuroColors.primaryForeground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    Icons.Outlined.Inventory2,
                    "Total Cigars",
                    "$totalCount",
                    accent = true,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    Icons.Outlined.AttachMoney,
                    "Est. Value",
                    "$" + money0(totalValue),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    Icons.Outlined.CheckCircle,
                    "Ready",
                    "$readyCount",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PuroColors.border),
        )
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Inventory2,
                title = "Your humidor is empty",
                subtitle = "Add cigars you've purchased to track aging and inventory",
                actionLabel = "Add cigars",
                onAction = { navController.navigate(Routes.HUMIDOR_ADD) },
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)) {
                items(items.size) { i ->
                    val item = items[i]
                    HumidorItemCard(
                        item = item,
                        onIncrement = {
                            app.humidorStore.updateQuantity(item.id, item.quantity + 1)
                        },
                        onDecrement = {
                            app.humidorStore.updateQuantity(item.id, item.quantity - 1)
                        },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    )
                }
            }
        }
    }
}
