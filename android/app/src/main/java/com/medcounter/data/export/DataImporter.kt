package com.medcounter.data.export

import android.content.Context
import android.net.Uri
import com.medcounter.data.MedCounterRepository
import com.medcounter.data.entity.AuditLogEntity
import com.medcounter.data.entity.MedicationDoseTimeEntity
import com.medcounter.data.entity.MedicationEntity
import com.medcounter.domain.AuditAction
import com.medcounter.domain.DoseScheduleResolver
import com.medcounter.domain.ScheduleType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class ImportedMedication(
    val name: String,
    val strength: String,
    val strengthUnit: String,
    val form: String,
    val scheduleType: String,
    val unitsPerDose: Double,
    val quantityOnHand: Double,
    val leadTimeDays: Int,
    val refillsRemaining: Int?,
    val pharmacy: String,
    val rxNumber: String,
    val notes: String,
)

data class ImportPreview(
    val medications: List<ImportedMedication>,
    val sourceProfileNames: Set<String>,
)

object DataImporter {
    private val requiredColumns = listOf("name", "schedule_type", "quantity_on_hand")
    private val validScheduleTypes = setOf(
        "DAILY", "BID", "TID", "QID", "WEEKLY", "EVERY_N_DAYS", "CUSTOM", "PRN",
    )

    fun parsePreview(context: Context, uri: Uri): Result<ImportPreview> {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Could not open file"))
            val text = stream.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }
            parseText(text)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun parseText(text: String): Result<ImportPreview> {
        return try {
            val lines = text.split(Regex("\\r?\\n"))
            if (lines.isEmpty() || (lines.size == 1 && lines[0].isEmpty())) {
                return Result.failure(IllegalStateException("File is empty"))
            }
            if (lines.all { it.isBlank() }) {
                return Result.failure(IllegalStateException("File is empty"))
            }
            val header = parseCsvLine(lines.first())
            val recognized = requiredColumns.any { col ->
                header.any { it.trim() == col }
            }
            if (!recognized) {
                return Result.failure(IllegalStateException("This doesn't look like a Med Counter medications export"))
            }
            val idxName = colIdx(header, "name")
            val idxProfileName = colIdx(header, "profile_name")
            val idxStrength = colIdx(header, "strength")
            val idxStrengthUnit = colIdx(header, "strength_unit")
            val idxForm = colIdx(header, "form")
            val idxScheduleType = colIdx(header, "schedule_type")
            val idxUnitsPerDose = colIdx(header, "units_per_dose")
            val idxQuantityOnHand = colIdx(header, "quantity_on_hand")
            val idxLeadTimeDays = colIdx(header, "lead_time_days")
            val idxRefills = colIdx(header, "refills_remaining")
            val idxPharmacy = colIdx(header, "pharmacy")
            val idxRxNumber = colIdx(header, "rx_number")
            val idxNotes = colIdx(header, "notes")
            if (idxName < 0 || idxScheduleType < 0 || idxQuantityOnHand < 0) {
                return Result.failure(
                    IllegalStateException("Missing required columns: name, schedule_type, quantity_on_hand"),
                )
            }
            val medications = mutableListOf<ImportedMedication>()
            val profileNames = linkedSetOf<String>()
            lines.drop(1).filter { it.isNotBlank() }.forEach { line ->
                val cols = parseCsvLine(line)
                val rawName = col(cols, idxName)
                if (rawName.isBlank()) return@forEach
                val rawSchedule = col(cols, idxScheduleType).uppercase(Locale.ROOT)
                if (rawSchedule !in validScheduleTypes) return@forEach
                if (idxProfileName >= 0) {
                    val pn = col(cols, idxProfileName)
                    if (pn.isNotBlank()) profileNames.add(pn)
                }
                val form = col(cols, idxForm).ifBlank { "tablet" }
                medications.add(
                    ImportedMedication(
                        name = rawName,
                        strength = col(cols, idxStrength),
                        strengthUnit = col(cols, idxStrengthUnit),
                        form = form,
                        scheduleType = rawSchedule,
                        unitsPerDose = col(cols, idxUnitsPerDose).toDoubleOrNull() ?: 1.0,
                        quantityOnHand = col(cols, idxQuantityOnHand).toDoubleOrNull() ?: 0.0,
                        leadTimeDays = col(cols, idxLeadTimeDays).toIntOrNull() ?: 7,
                        refillsRemaining = col(cols, idxRefills).toIntOrNull(),
                        pharmacy = col(cols, idxPharmacy),
                        rxNumber = col(cols, idxRxNumber),
                        notes = col(cols, idxNotes),
                    ),
                )
            }
            if (medications.isEmpty()) {
                return Result.failure(IllegalStateException("No medications found in file"))
            }
            Result.success(ImportPreview(medications, profileNames))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Inserts medications under [profileId]. interval_days and custom_doses_per_day
     * are not columns the original importer read, so they stay at entity defaults
     * (1 and 1.0). Dose times are the schedule defaults. History is not imported.
     */
    suspend fun importMedications(
        repository: MedCounterRepository,
        profileId: Long,
        profileName: String,
        medications: List<ImportedMedication>,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        for (med in medications) {
            val id = repository.insertMedication(
                MedicationEntity(
                    profileId = profileId,
                    name = med.name,
                    strength = med.strength,
                    strengthUnit = med.strengthUnit,
                    form = med.form,
                    scheduleType = med.scheduleType,
                    quantityOnHand = med.quantityOnHand,
                    quantityAsOf = nowMillis,
                    leadTimeDays = med.leadTimeDays,
                    refillsRemaining = med.refillsRemaining,
                    pharmacy = med.pharmacy,
                    rxNumber = med.rxNumber,
                    notes = med.notes,
                    isActive = true,
                ),
            )
            val schedule = ScheduleType.valueOf(med.scheduleType)
            val doseTimes = DoseScheduleResolver.defaultSlotIdsFor(schedule).map { slotId ->
                MedicationDoseTimeEntity(medicationId = id, slotId = slotId)
            }
            repository.saveDoseTimesForMedication(id, doseTimes)
            repository.insertAuditLog(
                AuditLogEntity(
                    timestamp = nowMillis,
                    profileId = profileId,
                    profileName = profileName,
                    medicationId = id,
                    medicationName = med.name,
                    action = AuditAction.MED_CREATED.name,
                    note = "Imported",
                ),
            )
        }
    }

    internal fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"' && !inQuotes) {
                inQuotes = true
            } else if (c == '"' && inQuotes) {
                if (i + 1 < line.length && line[i + 1] == '"') {
                    field.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            } else if (c != ',' || inQuotes) {
                field.append(c)
            } else {
                result.add(field.toString())
                field.clear()
            }
            i++
        }
        result.add(field.toString())
        return result
    }

    private fun colIdx(header: List<String>, name: String): Int =
        header.indexOfFirst { it.trim() == name }

    private fun col(cols: List<String>, idx: Int): String =
        if (idx in cols.indices) cols[idx].trim() else ""
}
