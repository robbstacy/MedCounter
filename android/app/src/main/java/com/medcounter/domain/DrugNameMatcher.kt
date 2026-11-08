package com.medcounter.domain

import java.util.Locale

/**
 * Tolerant drug-name comparison for typos ("Mematine") and OCR misreads
 * ("Amlod1pine", "Metforrnin").
 *
 * Tolerance is deliberately tight: real look-alike drugs differ by only a couple
 * of letters (Prednisone / Prednisolone, Hydroxyzine / Hydralazine), so names
 * under 12 letters allow one edit and longer names two. Ties between candidates
 * return no match rather than a guess; the user picks instead.
 */
object DrugNameMatcher {
    /** Lowercase letters only, after folding common OCR confusions. */
    fun fold(value: String): String {
        val lower = value.lowercase(Locale.ROOT)
            .replace('0', 'o')
            .replace('1', 'l')
            .replace('|', 'l')
            .replace('5', 's')
        return lower.replace(Regex("""[^a-z]"""), "")
            .replace("rn", "m")
            .replace("vv", "w")
    }

    private fun allowedEdits(length: Int): Int = when {
        length < 5 -> 0
        length < 12 -> 1
        else -> 2
    }

    /**
     * Edit distance between [a] and [b] (insert, delete, substitute, swap adjacent),
     * or null if it exceeds the tolerance for their length.
     */
    fun distance(a: String, b: String): Int? {
        val x = fold(a)
        val y = fold(b)
        if (x.isEmpty() || y.isEmpty()) return null
        if (x == y) return 0
        val limit = allowedEdits(minOf(x.length, y.length))
        if (limit == 0 || kotlin.math.abs(x.length - y.length) > limit) return null
        val d = osa(x, y)
        return d.takeIf { it <= limit }
    }

    fun matches(a: String, b: String): Boolean = distance(a, b) != null

    /**
     * Closest of [candidates] to [query] by name, comparing against each candidate's
     * names (e.g. generic, brand, first word). Null if nothing is close or two tie.
     */
    fun <T> best(query: String, candidates: List<T>, namesOf: (T) -> List<String>): T? {
        val scored = candidates.mapNotNull { c ->
            namesOf(c).mapNotNull { distance(query, it) }.minOrNull()?.let { c to it }
        }
        val min = scored.minOfOrNull { it.second } ?: return null
        return scored.filter { it.second == min }.singleOrNull()?.first
    }

    /** Optimal string alignment distance (Damerau-Levenshtein without repeated edits). */
    private fun osa(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, d[i - 2][j - 2] + 1)
                d[i][j] = v
            }
        }
        return d[a.length][b.length]
    }
}
