package com.medcounter.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class MedicationSort(val label: String) {
    SOONEST_EMPTY("Soonest to empty"),
    STATUS("Status"),
    NAME("Name"),
    RX_NUMBER("Rx number"),
    REFILLS_LEFT("Refills left"),
    ;

    companion object {
        fun fromName(name: String?): MedicationSort =
            entries.firstOrNull { it.name == name } ?: SOONEST_EMPTY
    }
}

/** A medication plus the name of the person (profile) it belongs to. */
data class RefillCandidate(
    val profileName: String,
    val item: MedicationWithCalculation,
)

object RefillPlanner {
    private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun statusRank(status: MedicationStatus): Int = when (status) {
        MedicationStatus.OUT -> 0
        MedicationStatus.CRITICAL -> 1
        MedicationStatus.REORDER -> 2
        MedicationStatus.OK -> 3
    }

    /** Days of supply left; PRN meds with no usage history (null) sort last. */
    private fun daysLeft(item: MedicationWithCalculation): Int =
        item.calculation.daysRemaining ?: Int.MAX_VALUE

    fun comparator(sort: MedicationSort): Comparator<MedicationWithCalculation> {
        val byName = compareBy<MedicationWithCalculation> { it.medication.name.lowercase() }
        return when (sort) {
            MedicationSort.SOONEST_EMPTY -> compareBy<MedicationWithCalculation> { daysLeft(it) }
                .thenBy { it.calculation.currentQuantity }
                .then(byName)
            MedicationSort.STATUS -> compareBy<MedicationWithCalculation> { statusRank(it.calculation.status) }
                .thenBy { daysLeft(it) }
                .then(byName)
            MedicationSort.NAME -> byName
            MedicationSort.RX_NUMBER -> compareBy<MedicationWithCalculation> { it.medication.rxNumber.isBlank() }
                .thenBy { rxSortKey(it.medication.rxNumber) }
                .then(byName)
            MedicationSort.REFILLS_LEFT -> compareBy<MedicationWithCalculation> { it.medication.refillsRemaining ?: Int.MAX_VALUE }
                .thenBy { daysLeft(it) }
                .then(byName)
        }
    }

    /** Numeric-aware key so "Rx 912" sorts before "Rx 1203". */
    private fun rxSortKey(rx: String): String {
        val digits = rx.filter(Char::isDigit)
        return digits.padStart(20, '0') + rx
    }

    /** Default selection for the refill list: anything not paused that is due to reorder. */
    fun isDue(item: MedicationWithCalculation): Boolean =
        !item.medication.isPaused && item.calculation.status != MedicationStatus.OK

    /**
     * Plain-text message for whoever is picking up refills, grouped by pharmacy
     * and ordered soonest-to-empty within each group.
     */
    fun buildMessage(candidates: List<RefillCandidate>, today: LocalDate = LocalDate.now()): String {
        if (candidates.isEmpty()) return ""
        val soonest = comparator(MedicationSort.SOONEST_EMPTY)
        val multiplePeople = candidates.map { it.profileName }.distinct().size > 1
        val groups = candidates
            .groupBy { it.item.medication.pharmacy.trim().ifBlank { "Pharmacy not set" } }
            .toList()
            .sortedWith(compareBy({ it.first == "Pharmacy not set" }, { group -> group.second.minOf { daysLeftOf(it) } }))

        return buildString {
            append("Refills needed (${shortDate.format(today)}):")
            groups.forEach { (pharmacy, items) ->
                append("\n\n").append(pharmacy).append(':')
                items.sortedWith { a, b -> soonest.compare(a.item, b.item) }.forEach { candidate ->
                    append("\n• ").append(describe(candidate, multiplePeople, today))
                }
            }
            val renewals = candidates.filter { needsNewPrescription(it.item.medication) }
            if (renewals.isNotEmpty()) {
                append("\n\n⚠ Needs a new prescription from the doctor: ")
                append(renewals.joinToString(", ") { it.item.medication.name })
            }
        }
    }

    /**
     * True when nothing more can be bought without the doctor: either the current
     * prescription quantity is used up with no refills, or (label-style tracking)
     * refills are at zero.
     */
    fun needsNewPrescription(med: com.medcounter.data.entity.MedicationEntity): Boolean {
        val left = med.rxQuantityLeft
        return if (left != null) left <= 0.0 && (med.refillsRemaining ?: 0) == 0 else med.refillsRemaining == 0
    }

    /** Suggested purchase: [days] of supply rounded up to whole strips, capped by what the Rx allows. */
    fun suggestedPurchase(item: MedicationWithCalculation, days: Int = 30): Double? {
        val perStrip = item.medication.unitsPerPack?.takeIf { it > 0 } ?: return null
        val rate = item.calculation.dailyRate.takeIf { it > 0 } ?: return null
        var amount = kotlin.math.ceil(rate * days / perStrip) * perStrip
        item.medication.rxQuantityLeft?.let { left -> if (left > 0) amount = minOf(amount, left) }
        return amount.takeIf { it > 0 }
    }

    private fun daysLeftOf(candidate: RefillCandidate): Int = daysLeft(candidate.item)

    private fun describe(candidate: RefillCandidate, includePerson: Boolean, today: LocalDate): String {
        val med = candidate.item.medication
        val calc = candidate.item.calculation
        val strength = listOf(med.strength, med.strengthUnit).filter { it.isNotBlank() }.joinToString(" ")
        val title = buildString {
            append(med.name)
            if (strength.isNotBlank()) append(' ').append(strength)
            if (includePerson && candidate.profileName.isNotBlank()) append(" (").append(candidate.profileName).append(')')
        }
        val details = buildList {
            if (med.rxNumber.isNotBlank()) add("Rx #${med.rxNumber}")
            suggestedPurchase(candidate.item)?.let { amount ->
                val perStrip = med.unitsPerPack ?: 1.0
                val strips = amount / perStrip
                val stripPart = if (strips == kotlin.math.floor(strips)) {
                    "${formatNumber(strips)} strip${if (strips == 1.0) "" else "s"}"
                } else {
                    null
                }
                add("buy ${stripPart?.let { "$it (${formatNumber(amount)})" } ?: formatNumber(amount)}")
            }
            med.rxQuantityLeft?.let { left ->
                val prescribed = med.rxQuantityPrescribed ?: left
                add(if (left <= 0.0) "Rx used up" else "${formatNumber(left)} of ${formatNumber(prescribed)} left on Rx")
            }
            med.refillsRemaining?.let {
                add(
                    when (it) {
                        0 -> "NO refills left"
                        1 -> "1 refill left"
                        else -> "$it refills left"
                    },
                )
            }
            add(runOutText(calc, today))
        }
        return "$title — ${details.joinToString(", ")}"
    }

    private fun runOutText(calc: MedicationCalculation, today: LocalDate): String {
        if (calc.currentQuantity <= 0.0 || calc.status == MedicationStatus.OUT) return "OUT now"
        val date = calc.runOutDate ?: return "${formatNumber(calc.currentQuantity)} on hand"
        val days = ChronoUnit.DAYS.between(today, date)
        val est = if (calc.isEstimate) "~" else ""
        return when {
            days <= 0 -> "runs out today"
            days == 1L -> "runs out tomorrow"
            else -> "runs out $est${shortDate.format(date)} ($days days)"
        }
    }
}
