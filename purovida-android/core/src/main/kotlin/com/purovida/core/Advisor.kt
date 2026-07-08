package com.purovida.core

import kotlin.math.roundToInt
import kotlin.random.Random

private fun money0(v: Double): String = v.roundToInt().toString()

/**
 * Rule-based cigar advisor, ported from the original app. Pattern-matching
 * rules and copy are unchanged; runs fully on-device.
 */
fun generateAdvisorResponse(
    input: String,
    entries: List<JournalEntry>,
    profile: UserProfile,
    random: Random = Random.Default,
): String {
    val q = input.lowercase()

    if (Regex("^(hi|hello|hey|what'?s up|yo)").containsMatchIn(q)) {
        return "Hello, ${profile.username}! I'm your personal cigar advisor. Ask me for recommendations, pairing suggestions, or anything about your collection. What can I help you with tonight?"
    }

    if (q.contains("tonight") ||
        (q.contains("recommend") && !q.contains("similar") && !q.contains("like"))
    ) {
        val recs = getRecommendations(profile, entries, limit = 3)
        if (recs.isEmpty()) {
            return "Log a few cigars first and I'll learn your palate. Based on your profile, I'd suggest starting with a Padrón 1964 or an Oliva Serie V Melanio — both are exceptional."
        }
        val top = recs.first()
        return "Based on your taste profile, I'd recommend the **${top.cigar.brand} ${top.cigar.productLine}** tonight.\n\n${top.reason}. It's a ${top.cigar.strength} cigar from ${top.cigar.country} featuring ${top.cigar.flavorTags.take(3).joinToString(", ")} notes.\n\nMSRP: $${money0(top.cigar.msrp)}"
    }

    val similarMatch = Regex("similar to (.+)|like (.+)").find(q)
    if (similarMatch != null) {
        val targetName =
            (similarMatch.groupValues[1].ifEmpty { similarMatch.groupValues[2] }).trim()
        val found = cigarCatalog.firstOrNull {
            it.brand.lowercase().contains(targetName) ||
                it.productLine.lowercase().contains(targetName)
        }
        if (found != null) {
            val similars = cigarCatalog
                .filter { it.id != found.id && it.strength == found.strength }
                .take(3)
            if (similars.isNotEmpty()) {
                val list = similars.joinToString("\n") {
                    "• **${it.brand} ${it.productLine}** — ${it.flavorTags.take(2).joinToString(", ")}"
                }
                return "If you enjoy the **${found.brand} ${found.productLine}**, you'll likely love:\n\n$list\n\nAll share a similar ${found.strength} strength profile with comparable flavor characteristics."
            }
        }
        return "I couldn't find that cigar in my database, but I can recommend great ${if (targetName.contains("mild")) "mild" else "full"}-bodied options. Could you be more specific about the brand or product line?"
    }

    if ((q.contains("mild") || q.contains("light")) &&
        (q.contains("cream") || q.contains("smooth") || q.contains("easy"))
    ) {
        val mild = cigarCatalog.filter {
            (it.strength == "mild" || it.strength == "mild-medium") &&
                it.flavorTags.contains("Cream")
        }
        if (mild.isNotEmpty()) {
            val list = mild.take(3).joinToString("\n") {
                "• **${it.brand} ${it.productLine}** — ${it.flavorTags.take(3).joinToString(", ")} ($${money0(it.msrp)})"
            }
            return "For mild and creamy cigars, these are excellent choices:\n\n$list\n\nThe Davidoff Anniversary and Arturo Fuente Hemingway lines are classic examples of silky, refined smokes."
        }
    }

    if (q.contains("full") || q.contains("bold") || q.contains("strong") || q.contains("powerful")) {
        val full = cigarCatalog.filter { it.strength == "full" }.take(3)
        val list = full.joinToString("\n") {
            "• **${it.brand} ${it.productLine}** — ${it.flavorTags.take(2).joinToString(", ")}"
        }
        return "For a bold, full-bodied experience:\n\n$list\n\nThe Padrón 1964 Maduro and Drew Estate Liga Privada No. 9 are considered benchmarks of full-bodied complexity."
    }

    if (q.contains("bourbon") || q.contains("whiskey") || q.contains("whisky")) {
        return "**Best cigars with bourbon:**\n\n• **Padrón 1964 Maduro** — The dark chocolate notes harmonize beautifully with bourbon's vanilla and caramel\n• **Drew Estate Liga Privada No. 9** — Sweet spice and leather complement whiskey's oakiness\n• **My Father Le Bijou 1922** — Molasses and raisin notes mirror bourbon's sweetness\n\nPro tip: Medium-full to full cigars with maduro wrappers tend to be the best bourbon companions."
    }

    if (q.contains("coffee") || q.contains("espresso")) {
        return "**Best cigars with coffee:**\n\n• **Arturo Fuente Hemingway** — The creamy, almond notes are a perfect morning espresso companion\n• **Davidoff Anniversary 2000** — Delicate enough for a morning coffee ritual\n• **Oliva Serie G** — Cedar and coffee notes create a natural harmony\n\nFor espresso specifically, look for cigars with natural (claro) or colorado wrappers and medium strength."
    }

    if (q.contains("rum")) {
        return "**Best cigars with rum:**\n\n• **Padrón 1964 Natural** — Cocoa and coffee notes pair perfectly with aged rum\n• **Rocky Patel Vintage 1990** — The caramel and cedar notes complement rum's sweetness\n• **My Father Le Bijou 1922** — Molasses and sweetness echo rum's character\n\nRum and cigars are a classic Caribbean pairing — look for Nicaraguan and Cuban-heritage cigars."
    }

    if (q.contains("age") || q.contains("aging") || q.contains("cellar")) {
        return "**Cigars that benefit most from aging:**\n\n• **Full-bodied Nicaraguans** (Padrón, My Father, Oliva V) — 1–3 years smooths the pepper\n• **Cuban cigars** (Cohiba, Montecristo) — Peak at 5–10 years\n• **Maduro wrappers** — Aging deepens the sweetness\n• **Connecticut wrappers** — Generally better fresh; don't over-age\n\nIn your humidor, I'd prioritize aging your Padrón and Opus X before anything else."
    }

    if (q.contains("beginner") || q.contains("new") || q.contains("first") || q.contains("start")) {
        return "**Best cigars for beginners:**\n\n• **Arturo Fuente Hemingway Short Story** — Mild-medium, creamy, and approachable\n• **Davidoff Anniversary 2000** — Smooth, elegant, and forgiving\n• **Oliva Serie G** — Medium-bodied with a gentle learning curve\n• **Rocky Patel Vintage 1990** — Accessible with interesting complexity\n\nStart with mild to medium-bodied cigars and work your way up. Don't start with a full-bodied Nicaraguan — it can be overwhelming."
    }

    if (q.contains("palate") || q.contains("taste") ||
        q.contains("what do i like") || q.contains("my profile")
    ) {
        val topFlavors = profile.preferredFlavors.take(4).joinToString(", ")
        val topStrength = profile.preferredStrengths.joinToString(" and ")
        val wrapper = profile.preferredWrappers.firstOrNull() ?: "maduro"
        return "**Your Flavor Profile:**\n\nBased on ${entries.size} logged cigars, you gravitate toward $topFlavors notes.\n\nYou prefer $topStrength strength cigars, and your highest-rated smokes tend to come from ${profile.preferredCountries.joinToString(" and ")}.\n\n**My recommendation:** Explore more $wrapper wrapper cigars from Nicaragua — they consistently match your palate."
    }

    if (q.contains("budget") || q.contains("cheap") || q.contains("affordable") || q.contains("value")) {
        val affordable = cigarCatalog.filter { it.msrp <= 12 }.take(3)
        val list = affordable.joinToString("\n") {
            "• **${it.brand} ${it.productLine}** — $${money0(it.msrp)} MSRP"
        }
        return "**Best value cigars under \$12:**\n\n$list\n\nThese are exceptional smokes at an accessible price point. The Drew Estate Undercrown and Oliva Serie G regularly punch well above their price."
    }

    val suggestions = listOf(
        "\"Recommend a cigar for tonight\"",
        "\"Find mild and creamy cigars\"",
        "\"Best cigars for bourbon\"",
        "\"What should I age longer?\"",
        "\"Something similar to Padron 1964\"",
        "\"Best beginner cigars\"",
    )
    val randomSuggestion = suggestions[random.nextInt(suggestions.size)]
    return "I'm your personal cigar advisor. I can help with:\n\n• **Recommendations** based on your taste profile\n• **Pairing** suggestions (bourbon, coffee, rum, wine)\n• **Aging** advice for your humidor\n• **Similar cigars** to ones you've loved\n• **Beginner guides** and education\n\nTry asking: $randomSuggestion"
}
