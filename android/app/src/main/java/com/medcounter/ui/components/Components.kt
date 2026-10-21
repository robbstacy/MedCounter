package com.medcounter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medcounter.domain.MedicationStatus
import com.medcounter.domain.MedicationWithCalculation
import com.medcounter.domain.ScheduleType
import com.medcounter.ui.theme.StatusCritical
import com.medcounter.ui.theme.StatusCriticalBg
import com.medcounter.ui.theme.StatusOk
import com.medcounter.ui.theme.StatusOkBg
import com.medcounter.ui.theme.StatusOut
import com.medcounter.ui.theme.StatusOutBg
import com.medcounter.ui.theme.StatusReorder
import com.medcounter.ui.theme.StatusReorderBg
import com.medcounter.ui.theme.parseHexColor
import java.time.format.DateTimeFormatter
import java.util.Locale

fun statusColor(status: MedicationStatus): Color = when (status) {
    MedicationStatus.OUT -> StatusOut
    MedicationStatus.CRITICAL -> StatusCritical
    MedicationStatus.REORDER -> StatusReorder
    MedicationStatus.OK -> StatusOk
}

fun statusBackground(status: MedicationStatus): Color = when (status) {
    MedicationStatus.OUT -> StatusOutBg
    MedicationStatus.CRITICAL -> StatusCriticalBg
    MedicationStatus.REORDER -> StatusReorderBg
    MedicationStatus.OK -> StatusOkBg
}

fun statusLabel(status: MedicationStatus): String = when (status) {
    MedicationStatus.OUT -> "OUT"
    MedicationStatus.CRITICAL -> "LOW"
    MedicationStatus.REORDER -> "REORDER"
    MedicationStatus.OK -> "STOCKED"
}

fun scheduleChipLabel(scheduleType: String, intervalDays: Int): String = when (scheduleType) {
    "DAILY" -> "Daily"
    "BID" -> "Twice daily"
    "TID" -> "3× daily"
    "QID" -> "4× daily"
    "WEEKLY" -> "Weekly"
    "EVERY_N_DAYS" -> "Every N days"
    "CUSTOM" -> "Custom schedule"
    "PRN" -> "As needed"
    else -> scheduleType
}

@Composable
fun StatusBadge(status: MedicationStatus, modifier: Modifier = Modifier) {
    Text(
        text = statusLabel(status),
        modifier = modifier
            .background(statusBackground(status), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        color = statusColor(status),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

private val cardDateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)

@Composable
fun MedicationCard(
    item: MedicationWithCalculation,
    profileColorHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val med = item.medication
    val calc = item.calculation
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier = Modifier
                    .background(parseHexColor(profileColorHex), RoundedCornerShape(2.dp))
                    .width(4.dp)
                    .padding(vertical = 24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(med.name, style = MaterialTheme.typography.titleMedium)
                val strength = listOf(med.strength, med.strengthUnit).filter { it.isNotBlank() }.joinToString(" ")
                if (strength.isNotBlank()) {
                    Text(strength, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        scheduleChipLabel(med.scheduleType, med.intervalDays),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (med.isPaused) {
                        Spacer(Modifier.width(8.dp))
                        Text("Paused", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                    }
                    if (med.refillsRemaining == 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("Renew Rx", color = StatusOut, style = MaterialTheme.typography.labelMedium)
                    }
                }
                val days = calc.daysRemaining?.let { "$it days left" } ?: ""
                val runs = calc.runOutDate?.let {
                    val est = if (calc.isEstimate) " (est.)" else ""
                    "runs out ${cardDateFmt.format(it)}$est"
                }.orEmpty()
                val subtitle = listOf(
                    "${formatQty(calc.currentQuantity)} on hand",
                    days,
                    runs,
                ).filter { it.isNotBlank() }.joinToString(" · ")
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            StatusBadge(calc.status)
        }
    }
}

fun formatQty(qty: Double): String {
    if (qty == kotlin.math.floor(qty)) return qty.toLong().toString()
    return String.format(Locale.US, "%.1f", qty)
}

fun formChoices(): List<Pair<String, String>> = listOf(
    "tablet" to "Tablet",
    "capsule" to "Capsule",
    "liquid" to "Liquid (mL)",
    "patch" to "Patch",
    "injection" to "Injection",
    "inhaler" to "Inhaler",
    "other" to "Other",
)

fun scheduleChoices(): List<Pair<ScheduleType, String>> = listOf(
    ScheduleType.DAILY to "Daily",
    ScheduleType.BID to "Twice daily",
    ScheduleType.TID to "3× daily",
    ScheduleType.QID to "4× daily",
    ScheduleType.WEEKLY to "Weekly",
    ScheduleType.EVERY_N_DAYS to "Every N days",
    ScheduleType.CUSTOM to "Custom schedule",
    ScheduleType.PRN to "As needed",
)
