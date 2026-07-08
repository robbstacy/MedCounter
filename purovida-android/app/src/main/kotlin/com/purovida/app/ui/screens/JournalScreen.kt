package com.purovida.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.purovida.app.LocalApp
import com.purovida.app.Routes
import com.purovida.app.ui.EmptyState
import com.purovida.app.ui.JournalEntryCard
import com.purovida.app.ui.PuroColors
import com.purovida.app.ui.SelectChip
import com.purovida.core.JournalEntry
import java.time.Instant

private val strengthFilters = listOf("All", "Mild", "Mild-Med", "Medium", "Med-Full", "Full")
private val strengthMap = mapOf(
    "Mild" to "mild",
    "Mild-Med" to "mild-medium",
    "Medium" to "medium",
    "Med-Full" to "medium-full",
    "Full" to "full",
)
private val sortOptions = listOf("Newest", "Oldest", "Highest Rated", "Lowest Rated")

private fun date(iso: String): Instant =
    runCatching { Instant.parse(iso) }.getOrDefault(Instant.EPOCH)

@Composable
fun JournalScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val entries by app.journalStore.entries.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var sortBy by rememberSaveable { mutableStateOf("Newest") }

    val filtered: List<JournalEntry> = run {
        var list = if (query.trim().isNotEmpty()) app.journalStore.search(query) else entries
        if (filter != "All") {
            val strength = strengthMap[filter]
            list = list.filter { it.strength == strength }
        }
        when (sortBy) {
            "Oldest" -> list.sortedBy { date(it.dateSmoked) }
            "Highest Rated" -> list.sortedByDescending { it.overallRating }
            "Lowest Rated" -> list.sortedBy { it.overallRating }
            else -> list.sortedByDescending { date(it.dateSmoked) }
        }
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Journal",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PuroColors.text,
                )
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PuroColors.primary)
                        .clickable { navController.navigate(Routes.JOURNAL_ADD) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Log a cigar",
                        tint = PuroColors.primaryForeground,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            SearchField(
                value = query,
                onValueChange = { query = it },
                hint = "Search cigars, notes, flavors...",
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (f in strengthFilters) {
                    SelectChip(label = f, selected = filter == f, onTap = { filter = f })
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(PuroColors.border),
                )
                for (s in sortOptions) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (sortBy == s) {
                                    PuroColors.accent.copy(alpha = 0.2f)
                                } else {
                                    PuroColors.card
                                },
                            )
                            .clickable { sortBy = s }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            s,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (sortBy == s) {
                                PuroColors.accentLight
                            } else {
                                PuroColors.mutedForeground
                            },
                        )
                    }
                }
            }
            Text(
                "${filtered.size} ${if (filtered.size == 1) "entry" else "entries"}",
                fontSize = 12.sp,
                color = PuroColors.mutedForeground,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PuroColors.border),
        )
        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.MenuBook,
                title = if (query.isNotEmpty()) "No results found" else "Your journal is empty",
                subtitle = if (query.isNotEmpty()) {
                    "Try different keywords"
                } else {
                    "Log your first cigar to get started"
                },
                actionLabel = if (query.isEmpty()) "Log a cigar" else null,
                onAction = if (query.isEmpty()) {
                    { navController.navigate(Routes.JOURNAL_ADD) }
                } else {
                    null
                },
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)) {
                items(filtered.size) { i ->
                    JournalEntryCard(
                        entry = filtered[i],
                        onTap = { navController.navigate(Routes.journalDetail(filtered[i].id)) },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(hint, fontSize = 14.sp, color = PuroColors.mutedForeground) },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = PuroColors.mutedForeground,
                modifier = Modifier.size(18.dp),
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = PuroColors.mutedForeground,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PuroColors.card,
            unfocusedContainerColor = PuroColors.card,
            focusedBorderColor = PuroColors.primary,
            unfocusedBorderColor = PuroColors.border,
            focusedTextColor = PuroColors.text,
            unfocusedTextColor = PuroColors.text,
            cursorColor = PuroColors.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
