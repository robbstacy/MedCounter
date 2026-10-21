package com.medcounter.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.medcounter.data.MedCounterRepository
import com.medcounter.domain.DoseScheduleResolver
import com.medcounter.domain.MedicationCalculationService
import java.io.File
import java.time.LocalDate

object DataExporter {
    fun escapeCsv(value: String): String = CsvFormat.escapeCsv(value)

    suspend fun exportAll(context: Context, repository: MedCounterRepository): List<Uri> {
        val today = LocalDate.now()
        val dateStr = CsvFormat.formatDate(today)
        val exportDir = File(context.cacheDir, "exports/medcounter_$dateStr").apply { mkdirs() }

        val profiles = repository.getAllProfilesSnapshot()
        val profileMap = profiles.associateBy { it.id }
        val allMeds = repository.getAllMedications()
        val slots = repository.getAllSlotsSnapshot()
        val slotMinutes = slots.associate { it.id to it.minutesOfDay }
        val slotNames = slots.associate { it.id to it.name }
        val allDoseTimes = if (allMeds.isEmpty()) {
            emptyList()
        } else {
            repository.getDoseTimesForMedications(allMeds.map { it.id })
        }
        val doseTimesByMed = allDoseTimes.groupBy { it.medicationId }
        val usageLogs = repository.getAllUsageLogs().groupBy { it.medicationId }

        val medRows = allMeds.map { med ->
            val times = doseTimesByMed[med.id].orEmpty()
            val minutes = DoseScheduleResolver.resolveMinutes(
                times.map { it.slotId to it.customMinutesOfDay },
                slotMinutes,
            )
            val labels = times.mapNotNull { dose ->
                when {
                    dose.slotId != null -> slotNames[dose.slotId]
                    dose.customMinutesOfDay != null -> CsvFormat.formatMinuteOfDay(dose.customMinutesOfDay)
                    else -> null
                }
            }
            val withCalc = MedicationCalculationService.calculate(
                med,
                usageLogs[med.id].orEmpty(),
                today,
                minutes,
            )
            CsvFormat.MedicationCsvRow(
                medication = med,
                profileName = profileMap[med.profileId]?.name.orEmpty(),
                calculation = withCalc.calculation,
                doseLabels = labels,
            )
        }

        val medById = allMeds.associateBy { it.id }
        val restockRows = repository.getAllRestockEvents().map { event ->
            val med = medById[event.medicationId]
            CsvFormat.RestockCsvRow(
                event = event,
                medicationName = med?.name.orEmpty(),
                profileName = med?.let { profileMap[it.profileId]?.name }.orEmpty(),
            )
        }
        val usageRows = repository.getAllUsageLogs().map { log ->
            val med = medById[log.medicationId]
            CsvFormat.UsageCsvRow(
                log = log,
                medicationName = med?.name.orEmpty(),
                profileName = med?.let { profileMap[it.profileId]?.name }.orEmpty(),
            )
        }
        val auditRows = repository.getAllAuditLogs().map { CsvFormat.AuditCsvRow(it) }

        val sections = profiles.map { profile ->
            val lines = medRows.filter { it.medication.profileId == profile.id }.map { row ->
                val days = row.calculation.daysRemaining?.let { "$it days left" } ?: "days left unknown"
                val runs = row.calculation.runOutDate?.let { "runs out ${CsvFormat.formatDisplay(it)}" } ?: "no run-out date"
                val est = if (row.calculation.isEstimate) " (est.)" else ""
                "${row.medication.name}: ${CsvFormat.fmtQty(row.calculation.currentQuantity)} on hand, $days, $runs$est"
            }
            CsvFormat.SummarySection(profile.name, lines)
        }

        val files = listOf(
            "medcounter_summary_$dateStr.txt" to CsvFormat.summaryText(today, sections),
            "medications.csv" to CsvFormat.medicationsCsv(medRows),
            "restock_events.csv" to CsvFormat.restockCsv(restockRows),
            "usage_logs.csv" to CsvFormat.usageCsv(usageRows),
            "audit_log.csv" to CsvFormat.auditCsv(auditRows),
        ).map { (name, body) ->
            File(exportDir, name).apply { writeText(body) }
        }

        val authority = "${context.packageName}.fileprovider"
        return files.map { file ->
            FileProvider.getUriForFile(context, authority, file)
        }
    }
}
