package com.purovida.core

import java.time.Instant

/**
 * Recommendation scoring and analytics, ported verbatim from the original
 * engine (point values and thresholds unchanged).
 */
private fun intersection(a: List<String>, b: List<String>): List<String> =
    a.filter { b.contains(it) }

private fun scoreCigar(cigar: Cigar, profile: UserProfile, topEntries: List<JournalEntry>): Int {
    var score = 0

    val flavorOverlap = intersection(cigar.flavorTags, profile.preferredFlavors)
    score += flavorOverlap.size * 3

    if (profile.preferredStrengths.contains(cigar.strength)) score += 4

    val wrapperMatch = profile.preferredWrappers.any {
        cigar.wrapper.lowercase().contains(it.lowercase())
    }
    if (wrapperMatch) score += 3

    if (profile.preferredCountries.contains(cigar.country)) score += 2

    val rating = cigar.communityRating
    if (rating != null && rating >= 95) {
        score += 3
    } else if (rating != null && rating >= 92) {
        score += 1
    }

    for (entry in topEntries) {
        score += intersection(cigar.flavorTags, entry.flavorTags).size * 2
    }

    return score
}

private fun buildReason(cigar: Cigar, profile: UserProfile, topEntries: List<JournalEntry>): String {
    val flavorOverlap = intersection(cigar.flavorTags, profile.preferredFlavors)
    if (flavorOverlap.size >= 3) {
        return "Matches your love of ${flavorOverlap.take(2).joinToString(", ")}"
    }
    if (profile.preferredStrengths.contains(cigar.strength)) {
        return "Matches your preferred ${cigar.strength} strength"
    }
    if (profile.preferredCountries.contains(cigar.country)) {
        return "From ${cigar.country}, one of your favorites"
    }
    val rating = cigar.communityRating
    if (rating != null && rating >= 95) {
        return "Rated $rating by the community"
    }
    return "Trending among enthusiasts like you"
}

fun getRecommendations(
    profile: UserProfile,
    entries: List<JournalEntry>,
    limit: Int = 10,
): List<Recommendation> {
    val top10 = entries
        .filter { it.overallRating >= 4 }
        .sortedByDescending { it.overallRating }
        .take(10)

    val smokedIds = entries.map { "${it.brand}-${it.productLine}" }.toSet()

    val scored = cigarCatalog
        .filter { !smokedIds.contains("${it.brand}-${it.productLine}") }
        .map { it to scoreCigar(it, profile, top10) }
        .sortedByDescending { it.second }

    return scored.take(limit).mapIndexed { i, (cigar, score) ->
        Recommendation(
            cigar = cigar,
            matchScore = (60 + score * 2).coerceIn(0, 99),
            type = when {
                i < 3 -> "you-may-enjoy"
                i < 6 -> "trending"
                else -> "try-next"
            },
            reason = buildReason(cigar, profile, top10),
        )
    }
}

private fun parseDateOrEpoch(iso: String): Instant =
    runCatching { Instant.parse(iso) }.getOrDefault(Instant.EPOCH)

fun getAnalytics(entries: List<JournalEntry>): Analytics {
    if (entries.isEmpty()) {
        return Analytics(
            totalEntries = 0,
            totalSpent = 0.0,
            avgRating = 0.0,
            topBrands = emptyList(),
            topFlavors = emptyList(),
            strengthBreakdown = emptyList(),
            ratingTrend = emptyList(),
            wouldSmokeAgainRate = 0.0,
        )
    }

    val totalSpent = entries.sumOf { it.pricePaid }
    val avgRating = entries.sumOf { it.overallRating.toDouble() } / entries.size

    val brandCounts = LinkedHashMap<String, Int>()
    for (e in entries) brandCounts[e.brand] = (brandCounts[e.brand] ?: 0) + 1
    val topBrands = brandCounts.entries.sortedByDescending { it.value }

    val flavorCounts = LinkedHashMap<String, Int>()
    for (e in entries) for (f in e.flavorTags) flavorCounts[f] = (flavorCounts[f] ?: 0) + 1
    val topFlavors = flavorCounts.entries.sortedByDescending { it.value }

    val strengthCounts = LinkedHashMap<String, Int>()
    for (e in entries) strengthCounts[e.strength] = (strengthCounts[e.strength] ?: 0) + 1

    val byDate = entries.sortedBy { parseDateOrEpoch(it.dateSmoked) }
    val trend = if (byDate.size > 12) byDate.subList(byDate.size - 12, byDate.size) else byDate

    val wsaRate = entries.count { it.wouldSmokeAgain }.toDouble() / entries.size

    return Analytics(
        totalEntries = entries.size,
        totalSpent = totalSpent,
        avgRating = avgRating,
        topBrands = topBrands.take(5).map { BrandCount(it.key, it.value) },
        topFlavors = topFlavors.take(8).map { FlavorCount(it.key, it.value) },
        strengthBreakdown = strengthCounts.entries.map { StrengthCount(it.key, it.value) },
        ratingTrend = trend.map { RatingPoint(it.dateSmoked, it.overallRating) },
        wouldSmokeAgainRate = wsaRate,
    )
}
