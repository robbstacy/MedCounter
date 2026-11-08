package com.medcounter.domain

/**
 * Fields recognized on a printed pharmacy label. Every field is optional:
 * OCR is noisy, and the user always reviews the result before it is saved.
 */
data class RxLabelInfo(
    val rxNumber: String? = null,
    val refillsRemaining: Int? = null,
    val quantity: Double? = null,
    val pharmacy: String? = null,
    val name: String? = null,
    val strength: String? = null,
    val strengthUnit: String? = null,
    val rawText: String = "",
) {
    val isEmpty: Boolean
        get() = rxNumber == null && refillsRemaining == null && quantity == null &&
            pharmacy == null && name == null

    /** Short human summary of what was found, e.g. "Rx 6123456 · 2 refills · qty 30". */
    fun summary(): String = listOfNotNull(
        name?.let { listOfNotNull(it, strength?.let { s -> "$s ${strengthUnit.orEmpty()}".trim() }).joinToString(" ") },
        rxNumber?.let { "Rx $it" },
        refillsRemaining?.let { if (it == 1) "1 refill" else "$it refills" },
        quantity?.let { "qty ${formatNumber(it)}" },
        pharmacy,
    ).joinToString(" · ")
}

/**
 * Pulls Rx number, refills, quantity, pharmacy and drug name/strength out of
 * OCR text from a US retail pharmacy label. Pure and deterministic so it can be
 * unit tested without a device.
 */
object RxLabelParser {
    private val knownPharmacies = listOf(
        "CVS" to Regex("""\bCVS\b""", RegexOption.IGNORE_CASE),
        "Walgreens" to Regex("""\bWALGREENS?\b""", RegexOption.IGNORE_CASE),
        "Walmart" to Regex("""\bWAL-?MART\b""", RegexOption.IGNORE_CASE),
        "Rite Aid" to Regex("""\bRITE\s*AID\b""", RegexOption.IGNORE_CASE),
        "Kroger" to Regex("""\bKROGER\b""", RegexOption.IGNORE_CASE),
        "Costco" to Regex("""\bCOSTCO\b""", RegexOption.IGNORE_CASE),
        "Sam's Club" to Regex("""\bSAM'?S\s*CLUB\b""", RegexOption.IGNORE_CASE),
        "Publix" to Regex("""\bPUBLIX\b""", RegexOption.IGNORE_CASE),
        "H-E-B" to Regex("""\bH-?E-?B\b""", RegexOption.IGNORE_CASE),
        "Target" to Regex("""\bTARGET\b""", RegexOption.IGNORE_CASE),
        "Safeway" to Regex("""\bSAFEWAY\b""", RegexOption.IGNORE_CASE),
        "Albertsons" to Regex("""\bALBERTSONS\b""", RegexOption.IGNORE_CASE),
        "Meijer" to Regex("""\bMEIJER\b""", RegexOption.IGNORE_CASE),
        "Hy-Vee" to Regex("""\bHY-?VEE\b""", RegexOption.IGNORE_CASE),
        // Philippines
        "Mercury Drug" to Regex("""\bMERCURY\s*DRUG\b""", RegexOption.IGNORE_CASE),
        "Watsons" to Regex("""\bWATSONS\b""", RegexOption.IGNORE_CASE),
        "Rose Pharmacy" to Regex("""\bROSE\s*PHARMACY\b""", RegexOption.IGNORE_CASE),
        "South Star Drug" to Regex("""\bSOUTH\s*STAR\s*DRUG\b""", RegexOption.IGNORE_CASE),
        "The Generics Pharmacy" to Regex("""\bGENERICS\s*PHARMACY\b|\bTGP\b""", RegexOption.IGNORE_CASE),
        "Generika" to Regex("""\bGENERIKA\b""", RegexOption.IGNORE_CASE),
        "St. Joseph Drug" to Regex("""\bST\.?\s*JOSEPH\s*DRUG\b""", RegexOption.IGNORE_CASE),
        "Express Scripts" to Regex("""\bEXPRESS\s*SCRIPTS\b""", RegexOption.IGNORE_CASE),
        "OptumRx" to Regex("""\bOPTUM\s*RX\b""", RegexOption.IGNORE_CASE),
        "Amazon Pharmacy" to Regex("""\bAMAZON\s*PHARMACY\b""", RegexOption.IGNORE_CASE),
        "VA Pharmacy" to Regex("""\bVA\s+(CMOP|PHARMACY)\b""", RegexOption.IGNORE_CASE),
    )

    // "RX# 6123456", "Rx No: 0612345-03011", "RX: 1234567", "Rx 7654321"
    private val rxRegex = Regex(
        """\bR\s?X\s*(?:#|NO\.?|NUMBER|NUM)?\s*[:#.]?\s*([0-9][0-9\- ]{4,18}[0-9])""",
        RegexOption.IGNORE_CASE,
    )

    private val noRefillsRegex = Regex(
        """\bNO\s+(?:MORE\s+)?REFILLS?\b|\bREFILLS?\s*(?:REMAINING|LEFT)?\s*[:#]?\s*(?:NONE|ZERO)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val refillPatterns = listOf(
        // "REFILLS: 3", "Refills remaining: 2", "REFILLS LEFT 1", "RFL: 2", "RF 2"
        Regex("""\b(?:REFILLS?|RFLS?|RF)\s*(?:REMAINING|LEFT|AVAILABLE|AVAIL)?\s*[:#]?\s*(\d{1,2})\b""", RegexOption.IGNORE_CASE),
        // "3 REFILLS LEFT", "2 refills remaining", "1 REFILL"
        Regex("""\b(\d{1,2})\s+REFILLS?\b""", RegexOption.IGNORE_CASE),
        // "REFILL 2 TIMES"
        Regex("""\bREFILL\s+(\d{1,2})\s+TIMES?\b""", RegexOption.IGNORE_CASE),
    )

    // "QTY: 30", "Qty 90 TAB", "QUANTITY: 60", "#30"
    private val qtyRegex = Regex(
        """\b(?:QTY|QUANTITY|QNTY)\s*[:#]?\s*(\d{1,4}(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE,
    )

    // "LISINOPRIL 10 MG TAB", "Metformin HCl 500mg", "FLUTICASONE 50 MCG"
    private val drugRegex = Regex(
        """\b([A-Z][A-Z\-]{2,}(?:\s+[A-Z][A-Z\-/]{1,})*)\s+(\d+(?:\.\d+)?(?:\s*/\s*\d+(?:\.\d+)?)?)\s*(MG|MCG|G|ML|MEQ|UNITS?|%|IU)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val nonDrugWords = setOf(
        "TAKE", "QTY", "QUANTITY", "REFILL", "REFILLS", "RX", "DR", "DATE", "FILLED", "USE", "BY", "DISCARD",
        "EVERY", "DAILY", "TABLET", "TABLETS", "CAPSULE", "CAPSULES", "MOUTH", "ORALLY", "PHARMACY", "STORE",
        "PH", "PHONE", "NDC", "MFG", "DOSE", "EACH", "WITH", "AND", "FOR", "THE", "ONE", "TWO",
    )

    fun parse(text: String): RxLabelInfo {
        val normalized = text.replace('’', '\'').replace('|', 'I')
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val rx = rxRegex.findAll(normalized)
            .map { it.groupValues[1].replace(" ", "").trim('-') }
            .firstOrNull { it.count(Char::isDigit) >= 5 }

        val refills = when {
            noRefillsRegex.containsMatchIn(normalized) -> 0
            else -> refillPatterns.firstNotNullOfOrNull { regex ->
                regex.find(normalized)?.groupValues?.get(1)?.toIntOrNull()
            }
        }?.takeIf { it in 0..99 }

        val qty = qtyRegex.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf { it > 0 }

        val pharmacy = knownPharmacies.firstOrNull { (_, regex) -> regex.containsMatchIn(normalized) }?.first

        val drug = lines.asSequence()
            .filterNot { line -> line.contains(Regex("""\b(QTY|QUANTITY|REFILL|RX\s*#|NDC)\b""", RegexOption.IGNORE_CASE)) }
            .mapNotNull { drugRegex.find(it) }
            .firstOrNull { match ->
                val words = match.groupValues[1].uppercase().split(Regex("""\s+"""))
                words.none { it in nonDrugWords }
            }

        return RxLabelInfo(
            rxNumber = rx,
            refillsRemaining = refills,
            quantity = qty,
            pharmacy = pharmacy,
            name = drug?.groupValues?.get(1)?.let(::titleCase),
            strength = drug?.groupValues?.get(2)?.replace(" ", ""),
            strengthUnit = drug?.groupValues?.get(3)?.let(::normalizeUnit),
            rawText = text,
        )
    }

    private fun titleCase(value: String): String =
        value.lowercase().split(Regex("""\s+""")).joinToString(" ") { word ->
            if (word.length <= 3 && word.all { it.isLetter() } && word in setOf("hcl", "er", "xl", "sr", "dr", "hct")) {
                word.uppercase()
            } else {
                word.replaceFirstChar { it.uppercase() }
            }
        }

    private fun normalizeUnit(unit: String): String = when (unit.uppercase()) {
        "MG" -> "mg"
        "MCG" -> "mcg"
        "G" -> "g"
        "ML" -> "mL"
        "MEQ" -> "mEq"
        "UNIT", "UNITS" -> "units"
        "IU" -> "IU"
        else -> unit
    }
}

internal fun formatNumber(value: Double): String =
    if (value == kotlin.math.floor(value)) value.toLong().toString() else value.toString()
