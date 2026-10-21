package com.medcounter.data.export

import com.medcounter.data.entity.AuditLogEntity
import com.medcounter.data.entity.MedicationEntity
import com.medcounter.data.entity.RestockEventEntity
import com.medcounter.data.entity.UsageLogEntity
import com.medcounter.domain.MedicationCalculation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvFormat {
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    const val MEDICATIONS_HEADER =
        "id,profile_name,name,strength,strength_unit,form,schedule_type,units_per_dose," +
            "quantity_on_hand,quantity_as_of,days_remaining,runs_out,lead_time_days,refills_remaining," +
            "pharmacy,rx_number,notes,interval_days,custom_doses_per_day,pillbox_quantity,is_paused," +
            "pause_reason,dose_times"

    const val RESTOCK_HEADER = "id,medication_name,profile_name,date,quantity_added,notes"
    const val USAGE_HEADER = "id,medication_name,profile_name,date,quantity_used"
    const val AUDIT_HEADER =
        "id,date,profile_name,medication_name,action,quantity_before,quantity_after," +
            "quantity_delta,expected_quantity,note,actor"

    fun escapeCsv(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    fun fmtQty(qty: Double): String {
        if (qty == kotlin.math.floor(qty)) return qty.toLong().toString()
        return String.format(Locale.US, "%.1f", qty)
    }

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        val amPm = if (h < 12) "AM" else "PM"
        val dh = when {
            h == 0 -> 12
            h in 13..23 -> h - 12
            else -> h
        }
        return if (m == 0) "$dh:00 $amPm" else String.format(Locale.US, "%d:%02d %s", dh, m, amPm)
    }

    fun buildSchedLabel(
        scheduleType: String,
        intervalDays: Int,
        customDosesPerDay: Double,
        doseLabels: List<String>,
    ): String {
        val base = when (scheduleType) {
            "DAILY" -> "Daily"
            "BID" -> "Twice daily"
            "TID" -> "3× daily"
            "QID" -> "4× daily"
            "WEEKLY" -> "Weekly"
            "EVERY_N_DAYS" -> "Every $intervalDays days"
            "CUSTOM" -> "${fmtQty(customDosesPerDay)}× daily"
            "PRN" -> "As needed"
            else -> scheduleType
        }
        if (doseLabels.isEmpty()) return base
        return "$base — ${doseLabels.joinToString(", ")}"
    }

    fun formatDate(date: LocalDate): String = dateFmt.format(date)

    fun formatDisplay(date: LocalDate): String = displayFmt.format(date)

    fun epochMillisToLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun medicationsCsv(
        rows: List<MedicationCsvRow>,
    ): String = buildString {
        append(MEDICATIONS_HEADER)
        append('\n')
        for (row in rows) {
            append(row.toLine())
            append('\n')
        }
    }

    fun restockCsv(rows: List<RestockCsvRow>): String = buildString {
        append(RESTOCK_HEADER)
        append('\n')
        rows.forEach { append(it.toLine()); append('\n') }
    }

    fun usageCsv(rows: List<UsageCsvRow>): String = buildString {
        append(USAGE_HEADER)
        append('\n')
        rows.forEach { append(it.toLine()); append('\n') }
    }

    fun auditCsv(rows: List<AuditCsvRow>): String = buildString {
        append(AUDIT_HEADER)
        append('\n')
        rows.forEach { append(it.toLine()); append('\n') }
    }

    fun summaryText(today: LocalDate, sections: List<SummarySection>): String = buildString {
        append("PillStock summary — ")
        append(formatDisplay(today))
        append("\n\n")
        if (sections.isEmpty()) {
            append("No medications tracked.\n")
        }
        for (section in sections) {
            append(section.profileName)
            append('\n')
            if (section.lines.isEmpty()) {
                append("  No medications.\n")
            }
            for (line in section.lines) {
                append("  ")
                append(line)
                append('\n')
            }
            append('\n')
        }
    }

    private fun csv(vararg fields: String): String = fields.joinToString(",") { escapeCsv(it) }

    data class MedicationCsvRow(
        val medication: MedicationEntity,
        val profileName: String,
        val calculation: MedicationCalculation,
        val doseLabels: List<String>,
        val zone: ZoneId = ZoneId.systemDefault(),
    ) {
        fun toLine(): String {
            val asOf = formatDate(epochMillisToLocalDate(medication.quantityAsOf, zone))
            val days = calculation.daysRemaining?.toString().orEmpty()
            val runsOut = calculation.runOutDate?.let { formatDate(it) }.orEmpty()
            val doseTimes = buildSchedLabel(
                medication.scheduleType,
                medication.intervalDays,
                medication.customDosesPerDay,
                doseLabels,
            )
            return csv(
                medication.id.toString(),
                profileName,
                medication.name,
                medication.strength,
                medication.strengthUnit,
                medication.form,
                medication.scheduleType,
                fmtQty(medication.unitsPerDose),
                fmtQty(medication.quantityOnHand),
                asOf,
                days,
                runsOut,
                medication.leadTimeDays.toString(),
                medication.refillsRemaining?.toString().orEmpty(),
                medication.pharmacy,
                medication.rxNumber,
                medication.notes,
                medication.intervalDays.toString(),
                fmtQty(medication.customDosesPerDay),
                medication.pillboxQuantity?.let { fmtQty(it) }.orEmpty(),
                if (medication.isPaused) "1" else "0",
                medication.pauseReason,
                doseTimes,
            )
        }
    }

    data class RestockCsvRow(
        val event: RestockEventEntity,
        val medicationName: String,
        val profileName: String,
        val zone: ZoneId = ZoneId.systemDefault(),
    ) {
        fun toLine(): String = csv(
            event.id.toString(),
            medicationName,
            profileName,
            formatDate(epochMillisToLocalDate(event.date, zone)),
            fmtQty(event.quantityAdded),
            event.notes,
        )
    }

    data class UsageCsvRow(
        val log: UsageLogEntity,
        val medicationName: String,
        val profileName: String,
        val zone: ZoneId = ZoneId.systemDefault(),
    ) {
        fun toLine(): String = csv(
            log.id.toString(),
            medicationName,
            profileName,
            formatDate(epochMillisToLocalDate(log.timestamp, zone)),
            fmtQty(log.quantityUsed),
        )
    }

    data class AuditCsvRow(
        val log: AuditLogEntity,
        val zone: ZoneId = ZoneId.systemDefault(),
    ) {
        fun toLine(): String = csv(
            log.id.toString(),
            formatDate(epochMillisToLocalDate(log.timestamp, zone)),
            log.profileName,
            log.medicationName,
            log.action,
            log.quantityBefore?.let { fmtQty(it) }.orEmpty(),
            log.quantityAfter?.let { fmtQty(it) }.orEmpty(),
            log.quantityDelta?.let { fmtQty(it) }.orEmpty(),
            log.expectedQuantity?.let { fmtQty(it) }.orEmpty(),
            log.note,
            log.actor,
        )
    }

    data class SummarySection(val profileName: String, val lines: List<String>)
}
