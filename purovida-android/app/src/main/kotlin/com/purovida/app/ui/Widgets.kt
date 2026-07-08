package com.purovida.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purovida.core.Cigar
import com.purovida.core.HumidorItem
import com.purovida.core.JournalEntry
import com.purovida.core.flavorColor
import com.purovida.core.flavorTaxonomy
import com.purovida.core.strengthDisplay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US).withZone(ZoneId.systemDefault())

fun formatShortDate(iso: String): String =
    runCatching { shortDateFormatter.format(Instant.parse(iso)) }.getOrDefault(iso)

fun money0(v: Double): String = "%.0f".format(Locale.US, v)

fun flavorTint(tag: String): Color = Color(flavorColor(tag))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlavorTagsRow(
    tags: List<String>,
    small: Boolean = false,
    maxVisible: Int? = null,
    modifier: Modifier = Modifier,
) {
    val visible = if (maxVisible != null) tags.take(maxVisible) else tags
    val overflow = tags.size - visible.size
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (tag in visible) {
            val tint = flavorTint(tag)
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.27f))
                    .border(1.dp, tint.copy(alpha = 0.53f), RoundedCornerShape(10.dp))
                    .padding(
                        horizontal = if (small) 8.dp else 10.dp,
                        vertical = if (small) 3.dp else 5.dp,
                    ),
            ) {
                Text(tag, fontSize = if (small) 11.sp else 12.sp, color = PuroColors.cream)
            }
        }
        if (overflow > 0) {
            Text(
                "+$overflow",
                fontSize = 11.sp,
                color = PuroColors.mutedForeground,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun RatingStars(
    rating: Int,
    size: Dp = 14.dp,
    onChange: ((Int) -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            val filled = i < rating
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "${i + 1} star",
                tint = if (filled) PuroColors.primary else PuroColors.mutedForeground,
                modifier = Modifier
                    .size(size)
                    .let { m ->
                        if (onChange != null) {
                            m.clickable { onChange(i + 1) }
                        } else {
                            m
                        }
                    },
            )
            if (onChange != null) Box(Modifier.width(2.dp))
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PuroColors.card)
            .border(
                1.dp,
                if (accent) PuroColors.primary.copy(alpha = 0.27f) else PuroColors.border,
                RoundedCornerShape(14.dp),
            )
            .padding(14.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (accent) PuroColors.primary else PuroColors.mutedForeground,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.padding(top = 8.dp)) {
            Text(
                value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (accent) PuroColors.primary else PuroColors.text,
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PuroColors.mutedForeground,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PuroColors.text)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = PuroColors.mutedForeground)
            }
        }
        if (action != null && onAction != null) {
            Row(
                Modifier.clickable(onClick = onAction),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, fontSize = 13.sp, color = PuroColors.primary)
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = PuroColors.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PuroColors.mutedForeground,
            modifier = Modifier.size(44.dp),
        )
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = PuroColors.text,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            subtitle,
            fontSize = 13.sp,
            color = PuroColors.mutedForeground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PuroColors.primary,
                    contentColor = PuroColors.primaryForeground,
                ),
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun StrengthBadge(strength: String, fontSize: Int = 11) {
    val sColor = strengthColors[strength] ?: PuroColors.mutedForeground
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(sColor.copy(alpha = 0.2f))
            .border(1.dp, sColor.copy(alpha = 0.53f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(strength, fontSize = fontSize.sp, color = sColor)
    }
}

@Composable
fun CigarCard(
    cigar: Cigar,
    onTap: (() -> Unit)? = null,
    badge: String? = null,
    matchScore: Int? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PuroColors.card)
            .border(1.dp, PuroColors.border, RoundedCornerShape(16.dp))
            .let { if (onTap != null) it.clickable(onClick = onTap) else it }
            .padding(16.dp),
    ) {
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PuroColors.primary)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = PuroColors.primaryForeground,
                )
            }
        }
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        cigar.brand.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = PuroColors.primary,
                    )
                    Text(
                        cigar.productLine,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PuroColors.text,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        cigar.vitola,
                        fontSize = 12.sp,
                        color = PuroColors.mutedForeground,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (matchScore != null || cigar.communityRating != null) {
                    Column(
                        Modifier
                            .padding(top = if (badge != null) 20.dp else 0.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                PuroColors.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (matchScore != null) "$matchScore%" else "${cigar.communityRating}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PuroColors.primary,
                        )
                        Text(
                            if (matchScore != null) "MATCH" else "PTS",
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                            color = PuroColors.mutedForeground,
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Public,
                    contentDescription = null,
                    tint = PuroColors.mutedForeground,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    cigar.country,
                    fontSize = 12.sp,
                    color = PuroColors.mutedForeground,
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                )
                StrengthBadge(cigar.strength)
                Box(Modifier.weight(1f))
                Text(
                    "$" + money0(cigar.msrp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PuroColors.mutedForeground,
                )
            }
            FlavorTagsRow(
                cigar.flavorTags,
                small = true,
                maxVisible = 4,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                cigar.description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 18.sp,
                color = PuroColors.mutedForeground,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalEntryCard(entry: JournalEntry, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PuroColors.card)
            .border(1.dp, PuroColors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onTap)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.brand.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = PuroColors.primary,
                )
                Text(
                    "${entry.productLine} · ${entry.vitola}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PuroColors.text,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PuroColors.primary.copy(alpha = 0.13f))
                    .border(1.dp, PuroColors.primary.copy(alpha = 0.27f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${entry.overallRating}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PuroColors.primary,
                )
            }
        }
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = PuroColors.mutedForeground,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    formatShortDate(entry.dateSmoked),
                    fontSize = 12.sp,
                    color = PuroColors.mutedForeground,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (entry.location.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Place,
                        contentDescription = null,
                        tint = PuroColors.mutedForeground,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        entry.location,
                        fontSize = 12.sp,
                        color = PuroColors.mutedForeground,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(PuroColors.surface)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    strengthDisplay[entry.strength] ?: entry.strength,
                    fontSize = 11.sp,
                    color = PuroColors.mutedForeground,
                )
            }
        }
        Box(Modifier.padding(top = 8.dp)) {
            RatingStars(rating = entry.overallRating)
        }
        if (entry.flavorTags.isNotEmpty()) {
            FlavorTagsRow(
                entry.flavorTags,
                small = true,
                maxVisible = 4,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (entry.notes.isNotEmpty()) {
            Text(
                entry.notes,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 19.sp,
                color = PuroColors.mutedForeground,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (entry.wouldSmokeAgain || entry.beveragePairing.isNotEmpty()) {
            FlowRow(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (entry.wouldSmokeAgain) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PuroColors.success.copy(alpha = 0.13f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = PuroColors.success,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            "Would smoke again",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PuroColors.success,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                if (entry.beveragePairing.isNotEmpty()) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PuroColors.accent.copy(alpha = 0.13f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            entry.beveragePairing,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PuroColors.accentLight,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HumidorItemCard(
    item: HumidorItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = runCatching { Instant.parse(item.agingStartDate) }.getOrDefault(Instant.now())
    val targetMillis = item.targetAgingMonths * 30L * 24 * 60 * 60 * 1000
    val elapsed = System.currentTimeMillis() - start.toEpochMilli()
    val progress = if (targetMillis <= 0) 1f else (elapsed.toFloat() / targetMillis).coerceIn(0f, 1f)
    val monthsAged = (elapsed / (30L * 24 * 60 * 60 * 1000)).toInt()
    val isReady = progress >= 1f
    val progressColor = if (isReady) PuroColors.success else PuroColors.primary

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PuroColors.card)
            .border(
                1.dp,
                if (isReady) PuroColors.success.copy(alpha = 0.27f) else PuroColors.border,
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                if (isReady) {
                    Row(
                        Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PuroColors.success)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            "READY TO SMOKE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                Text(
                    item.brand.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = PuroColors.primary,
                )
                Text(
                    item.productLine,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PuroColors.text,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    "${item.vitola} · ${item.wrapper}",
                    fontSize = 12.sp,
                    color = PuroColors.mutedForeground,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                QtyButton(Icons.Filled.Remove, onDecrement)
                Box(Modifier.widthIn(min = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "${item.quantity}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PuroColors.text,
                    )
                }
                QtyButton(Icons.Filled.Add, onIncrement, color = PuroColors.primary)
            }
        }
        Row(
            Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = PuroColors.mutedForeground,
                modifier = Modifier.size(12.dp),
            )
            Text(
                "${monthsAged}mo aged · ${item.targetAgingMonths}mo target",
                fontSize = 12.sp,
                color = PuroColors.mutedForeground,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            color = progressColor,
            trackColor = PuroColors.muted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(2.dp)),
        )
        FlowRow(
            Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Public,
                    contentDescription = null,
                    tint = PuroColors.mutedForeground,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    item.country,
                    fontSize = 12.sp,
                    color = PuroColors.mutedForeground,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AttachMoney,
                    contentDescription = null,
                    tint = PuroColors.mutedForeground,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    "${money0(item.purchasePrice)} paid",
                    fontSize = 12.sp,
                    color = PuroColors.mutedForeground,
                )
            }
            if (item.purchaseSource.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        tint = PuroColors.mutedForeground,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        item.purchaseSource,
                        fontSize = 12.sp,
                        color = PuroColors.mutedForeground,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QtyButton(
    icon: ImageVector,
    onTap: () -> Unit,
    color: Color = PuroColors.text,
) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .border(1.dp, PuroColors.border, CircleShape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
    }
}

/** Horizontal single-select chip row used across forms and filter bars. */
@Composable
fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    display: Map<String, String>? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (opt in options) {
            SelectChip(
                label = display?.get(opt) ?: opt,
                selected = selected == opt,
                onTap = { onSelect(opt) },
            )
        }
    }
}

@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onTap: () -> Unit,
    selectedColor: Color? = null,
) {
    val color = selectedColor ?: PuroColors.primary
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) color else PuroColors.card)
            .border(1.dp, if (selected) color else PuroColors.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) PuroColors.primaryForeground else PuroColors.mutedForeground,
        )
    }
}

@Composable
fun FieldLabel(label: String) {
    Text(
        label.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.7.sp,
        color = PuroColors.mutedForeground,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

/** Flavor multi-select grouped by taxonomy category. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlavorPicker(selected: List<String>, onToggle: (String) -> Unit) {
    val categories = flavorTaxonomy.map { it.category }.distinct()
    Column {
        for (cat in categories) {
            Text(
                cat,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PuroColors.mutedForeground,
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (f in flavorTaxonomy.filter { it.category == cat }) {
                    val tint = Color(f.color)
                    val isSelected = selected.contains(f.tag)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) tint.copy(alpha = 0.27f) else PuroColors.card)
                            .border(
                                1.dp,
                                if (isSelected) tint else PuroColors.border,
                                RoundedCornerShape(20.dp),
                            )
                            .clickable { onToggle(f.tag) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            f.tag,
                            fontSize = 12.sp,
                            color = if (isSelected) PuroColors.cream else PuroColors.mutedForeground,
                        )
                    }
                }
            }
        }
    }
}
