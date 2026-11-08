package com.medcounter.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** One drug line from a doctor's prescription slip. */
data class SlipItem(
    val name: String,
    val brand: String? = null,
    val strength: String? = null,
    val strengthUnit: String? = null,
    val form: String? = null,
    /** Total quantity prescribed, e.g. "#90". */
    val quantity: Double? = null,
    /** Doctor-authorized refills ("Refill x2"). */
    val refills: Int? = null,
    val unitsPerDose: Double? = null,
    val schedule: ScheduleType? = null,
    val sig: String? = null,
) {
    fun summary(): String = listOfNotNull(
        listOfNotNull(name, brand?.let { "($it)" }, strength?.let { "$it${strengthUnit.orEmpty()}" }).joinToString(" "),
        quantity?.let { "#${formatNumber(it)}" },
        sig,
        refills?.let { "refill ×$it" },
    ).joinToString(" · ")
}

/** Everything read from one prescription slip. */
data class SlipScan(
    val items: List<SlipItem> = emptyList(),
    val date: LocalDate? = null,
    val prescriber: String? = null,
    /** Pharmacy-label fields (US-style labels); usually null on a doctor's slip. */
    val rxNumber: String? = null,
    val pharmacy: String? = null,
    val rawText: String = "",
) {
    val isEmpty: Boolean get() = items.isEmpty() && rxNumber == null

    fun summary(): String = buildList {
        if (items.size == 1) add(items.first().summary()) else if (items.isNotEmpty()) add("${items.size} medications")
        rxNumber?.let { add("Rx #$it") }
        prescriber?.let { add(it) }
        date?.let { add(it.toString()) }
    }.joinToString(" · ")

    /** Best item for a medication already named [medName], matched loosely on generic or brand name. */
    fun matchFor(medName: String): SlipItem? {
        val key = normalizeName(medName)
        if (key.isBlank()) return null
        return items.firstOrNull { normalizeName(it.name) == key || it.brand?.let(::normalizeName) == key }
            ?: items.firstOrNull { item ->
                val n = normalizeName(item.name)
                n.isNotBlank() && (n.startsWith(key) || key.startsWith(n))
            }
    }
}

internal fun normalizeName(value: String): String =
    value.lowercase(Locale.ROOT).replace(Regex("""[^a-z]"""), "")

/**
 * Parses OCR text of a doctor's prescription slip as written in the Philippines:
 *
 * ```
 * Rx
 * 1. Amlodipine (Norvasc) 5 mg tab #30
 *    Sig: 1 tab OD
 * 2. Metformin 500mg #60  Sig: 1 tab BID p.c.
 *    Refill x2
 * Dr. Juan Dela Cruz, M.D.   Lic. No. 0123456   PTR No. 7654321
 * ```
 *
 * License/PTR/S2 numbers are ignored. Handwriting reads poorly with on-device
 * OCR; printed or typed slips work best, and the user reviews every field.
 */
object PrescriptionSlipParser {
    private const val UNIT = """(MG|MCG|µG|UG|G|ML|MEQ|IU|UNITS?|%)"""
    private val drugRegex = Regex(
        """^(?:\d{1,2}\s*[.)]\s*)?([A-Za-z][A-Za-z\-]{2,}(?:\s+[A-Za-z][A-Za-z\-]{1,}){0,3})\s*""" +
            """(?:\(([^)]{2,40})\)\s*)?""" +
            """(\d+(?:\.\d+)?(?:\s*/\s*\d+(?:\.\d+)?)?)\s*$UNIT\b(.*)$""",
        RegexOption.IGNORE_CASE,
    )
    private val quantityRegexes = listOf(
        Regex("""#\s*(\d{1,4})\b"""),
        Regex("""\b(?:DISP(?:ENSE)?|QTY|QUANTITY|NO\.)\s*[:#]?\s*(\d{1,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(\d{1,4})\s*(?:TABS?|TABLETS?|CAPS?|CAPSULES?)\b(?!\s*(?:OD|BID|TID|QID|HS|ONCE|TWICE|DAILY|A DAY|\d))""", RegexOption.IGNORE_CASE),
    )
    private val refillRegex = Regex(
        """\bREFILLS?\s*[:x×]?\s*(\d{1,2})\s*(?:X|×|TIMES?)?\b|\bRF\s*[x×:]\s*(\d{1,2})\b""",
        RegexOption.IGNORE_CASE,
    )
    private val sigRegex = Regex("""\bSIG(?:NA)?\s*[:.]?\s*(.+)$""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    private val formRegex = Regex("""\b(TAB(?:LET)?S?|CAP(?:SULE)?S?|SYR(?:UP)?|SUSP(?:ENSION)?|AMP(?:ULE)?S?|NEB(?:ULE)?S?|PATCH(?:ES)?|INHALER)\b""", RegexOption.IGNORE_CASE)
    private val dosePrefix = Regex("""^\s*(1/2|½|HALF|ONE|TWO|\d+(?:\.\d+)?)\s*(?:TABS?|TABLETS?|CAPS?|CAPSULES?)?""", RegexOption.IGNORE_CASE)
    private val credentialRegex = Regex(
        """\b(LIC(?:ENSE)?\.?\s*(?:NO)?|PRC|PTR|S2|S-2|TIN|PHIC|PHILHEALTH|CONTACT|TEL|MOBILE|CEL+|CLINIC\s+HOURS)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val prescriberRegex = Regex(
        """\b(?:DR\.?\s+)?([A-Z][A-Za-z.\-]+(?:\s+[A-Z][A-Za-z.\-]+){1,4}),?\s*M\.?\s?D\.?\b""",
    )
    private val drPrefixRegex = Regex("""\bDR\.?\s+([A-Z][A-Za-z.\-]+(?:\s+[A-Z][A-Za-z.\-]+){1,4})""")
    private val notDrugs = setOf(
        "take", "sig", "signa", "rx", "date", "name", "age", "sex", "address", "refill", "disp", "dispense",
        "qty", "lic", "ptr", "prc", "every", "once", "twice", "for", "after", "before", "with", "tab",
        "tablet", "tablets", "cap", "capsule", "capsules", "mg", "dr", "md", "patient", "clinic",
    )

    fun parse(text: String): SlipScan {
        val lines = text.replace('|', 'l').replace('’', '\'').lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Group lines into blocks that start with a drug line.
        val items = mutableListOf<SlipItem>()
        var current: SlipItem? = null
        var currentDetail = StringBuilder()

        fun flush() {
            current?.let { items += finish(it, currentDetail.toString()) }
            current = null
            currentDetail = StringBuilder()
        }

        for (line in lines) {
            if (credentialRegex.containsMatchIn(line)) continue
            val match = drugRegex.find(line)
            val candidate = match?.groupValues?.get(1)?.trim()
            val firstWord = candidate?.split(Regex("""\s+"""))?.firstOrNull()?.lowercase(Locale.ROOT)
            if (match != null && candidate != null && firstWord !in notDrugs) {
                flush()
                current = SlipItem(
                    name = titleCase(candidate),
                    brand = match.groupValues[2].trim().takeIf { it.isNotBlank() }?.let(::titleCase),
                    strength = match.groupValues[3].replace(" ", ""),
                    strengthUnit = normalizeUnit(match.groupValues[4]),
                )
                currentDetail.append(match.groupValues[5]).append('\n')
            } else if (current != null) {
                currentDetail.append(line).append('\n')
            }
        }
        flush()

        return SlipScan(
            items = items,
            date = findDate(text),
            prescriber = findPrescriber(lines),
            rawText = text,
        )
    }

    private fun finish(item: SlipItem, detail: String): SlipItem {
        val sigText = sigRegex.find(detail)?.groupValues?.get(1)?.lineSequence()?.firstOrNull()?.trim()
        val beforeSig = sigRegex.find(detail)?.let { detail.substring(0, it.range.first) } ?: detail
        val quantity = quantityRegexes.firstNotNullOfOrNull { regex ->
            regex.find(beforeSig)?.groupValues?.get(1)?.toDoubleOrNull()
        } ?: quantityRegexes.first().find(detail)?.groupValues?.get(1)?.toDoubleOrNull()
        val refills = refillRegex.find(detail)?.let { m ->
            (m.groupValues[1].ifBlank { m.groupValues[2] }).toIntOrNull()
        }
        val sig = sigText?.let(::cleanSig)
        return item.copy(
            form = formRegex.find(detail)?.value?.let(::normalizeForm),
            quantity = quantity?.takeIf { it > 0 },
            refills = refills,
            sig = sig,
            schedule = sig?.let(::scheduleFromSig),
            unitsPerDose = sig?.let(::unitsFromSig),
        )
    }

    private fun cleanSig(sig: String): String =
        sig.replace(Regex("""\s+"""), " ").trim().trimEnd('.', ',')

    /** Latin dosing abbreviations common on PH (and US) prescriptions. */
    fun scheduleFromSig(sig: String): ScheduleType? {
        val s = " " + sig.uppercase(Locale.ROOT).replace(".", "") + " "
        return when {
            Regex("""\bPRN\b|AS NEEDED|IF NEEDED|WHEN NEEDED""").containsMatchIn(s) -> ScheduleType.PRN
            Regex("""\bQID\b|\bQ6H\b|\bQ6\s*HRS?\b|EVERY 6 H|4X|4 TIMES""").containsMatchIn(s) -> ScheduleType.QID
            Regex("""\bTID\b|\bQ8H\b|\bQ8\s*HRS?\b|EVERY 8 H|3X|3 TIMES|THRICE""").containsMatchIn(s) -> ScheduleType.TID
            Regex("""\bBID\b|\bQ12H\b|\bQ12\s*HRS?\b|EVERY 12 H|2X|TWICE""").containsMatchIn(s) -> ScheduleType.BID
            Regex("""\bOD\b|\bQD\b|\bHS\b|\bOM\b|\bQAM\b|\bQPM\b|ONCE A DAY|ONCE DAILY|\bDAILY\b|EVERY MORNING|EVERY NIGHT|AT BEDTIME|\bQ24H\b""").containsMatchIn(s) -> ScheduleType.DAILY
            Regex("""ONCE A WEEK|WEEKLY|\bQWK\b""").containsMatchIn(s) -> ScheduleType.WEEKLY
            else -> null
        }
    }

    private fun unitsFromSig(sig: String): Double? {
        val m = dosePrefix.find(sig) ?: return null
        return when (val v = m.groupValues[1].uppercase(Locale.ROOT)) {
            "½", "1/2", "HALF" -> 0.5
            "ONE" -> 1.0
            "TWO" -> 2.0
            else -> v.toDoubleOrNull()
        }?.takeIf { it in 0.25..10.0 }
    }

    private val datePatterns = listOf(
        Regex("""\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})\b""") to "numeric",
        Regex("""\b([A-Za-z]{3,9})\.?\s+(\d{1,2}),?\s+(\d{4})\b""") to "MMM d yyyy",
        Regex("""\b(\d{1,2})\s+([A-Za-z]{3,9})\.?\s+(\d{4})\b""") to "d MMM yyyy",
    )

    private fun findDate(text: String): LocalDate? {
        for ((regex, kind) in datePatterns) {
            for (m in regex.findAll(text)) {
                val parsed = when (kind) {
                    "numeric" -> {
                        // PH convention is MM/DD/YYYY; fall back to DD/MM when the month is impossible.
                        val a = m.groupValues[1].toInt()
                        val b = m.groupValues[2].toInt()
                        var y = m.groupValues[3].toInt()
                        if (y < 100) y += 2000
                        runCatching { LocalDate.of(y, a, b) }.getOrNull()
                            ?: runCatching { LocalDate.of(y, b, a) }.getOrNull()
                    }
                    "MMM d yyyy" -> parseMonthName(m.groupValues[1], m.groupValues[2], m.groupValues[3])
                    else -> parseMonthName(m.groupValues[2], m.groupValues[1], m.groupValues[3])
                }
                if (parsed != null && parsed.year in 2000..2100) return parsed
            }
        }
        return null
    }

    private fun parseMonthName(month: String, day: String, year: String): LocalDate? {
        val m = month.take(3).lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
        return try {
            LocalDate.parse("$m ${day.toInt()} $year", DateTimeFormatter.ofPattern("MMM d yyyy", Locale.US))
        } catch (_: DateTimeParseException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun findPrescriber(lines: List<String>): String? {
        for (line in lines.asReversed()) {
            prescriberRegex.find(line)?.let { return "Dr. " + it.groupValues[1].trim().removePrefix("Dr.").removePrefix("DR.").trim() }
        }
        for (line in lines) {
            drPrefixRegex.find(line)?.let { return "Dr. " + it.groupValues[1].trim() }
        }
        return null
    }

    private fun titleCase(value: String): String =
        value.trim().lowercase(Locale.ROOT).split(Regex("""\s+""")).joinToString(" ") { word ->
            if (word in setOf("hcl", "er", "xr", "sr", "mr", "od", "hct")) word.uppercase(Locale.ROOT)
            else word.replaceFirstChar { it.uppercase() }
        }

    private fun normalizeUnit(unit: String): String = when (unit.uppercase(Locale.ROOT)) {
        "MG" -> "mg"
        "MCG", "UG", "µG" -> "mcg"
        "G" -> "g"
        "ML" -> "mL"
        "MEQ" -> "mEq"
        "UNIT", "UNITS" -> "units"
        else -> unit
    }

    private fun normalizeForm(value: String): String = when {
        value.startsWith("tab", ignoreCase = true) -> "tablet"
        value.startsWith("cap", ignoreCase = true) -> "capsule"
        value.startsWith("syr", ignoreCase = true) || value.startsWith("susp", ignoreCase = true) -> "liquid"
        value.startsWith("amp", ignoreCase = true) -> "injection"
        value.startsWith("neb", ignoreCase = true) || value.startsWith("inhal", ignoreCase = true) -> "inhaler"
        value.startsWith("patch", ignoreCase = true) -> "patch"
        else -> "other"
    }
}

/** Reads OCR text as a doctor's slip first, falling back to a pharmacy-label layout. */
object PrescriptionReader {
    fun read(text: String): SlipScan {
        val slip = PrescriptionSlipParser.parse(text)
        val label = RxLabelParser.parse(text)
        val items = slip.items.ifEmpty {
            label.name?.let { name ->
                listOf(
                    SlipItem(
                        name = name,
                        strength = label.strength,
                        strengthUnit = label.strengthUnit,
                        quantity = label.quantity,
                        refills = label.refillsRemaining,
                    ),
                )
            } ?: emptyList()
        }
        return slip.copy(items = items, rxNumber = label.rxNumber, pharmacy = label.pharmacy)
    }
}
