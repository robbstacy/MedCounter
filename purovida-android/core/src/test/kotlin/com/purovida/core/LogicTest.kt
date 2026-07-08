package com.purovida.core

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ported from the Flutter app's test/logic_test.dart. */
class LogicTest {

    private fun profile(): UserProfile = buildDefaultProfile()

    // recommendations

    @Test
    fun `excludes already-smoked cigars and caps score at 99`() {
        val entries = buildSeedJournalEntries()
        val recs = getRecommendations(profile(), entries, limit = 10)

        assertTrue(recs.isNotEmpty())
        for (rec in recs) {
            assertTrue(rec.matchScore in 0..99)
            for (e in entries) {
                assertFalse(
                    "${rec.cigar.brand}-${rec.cigar.productLine}" == "${e.brand}-${e.productLine}",
                )
            }
        }
    }

    @Test
    fun `assigns types by rank - 3 you-may-enjoy then trending`() {
        val recs = getRecommendations(profile(), emptyList(), limit = 10)
        assertTrue(recs.take(3).all { it.type == "you-may-enjoy" })
        assertTrue(recs.drop(3).take(3).all { it.type == "trending" })
        assertTrue(recs.drop(6).all { it.type == "try-next" })
    }

    // analytics

    @Test
    fun `empty entries produce zeroed analytics`() {
        val a = getAnalytics(emptyList())
        assertEquals(0, a.totalEntries)
        assertEquals(0.0, a.totalSpent, 0.0)
        assertEquals(0.0, a.wouldSmokeAgainRate, 0.0)
    }

    @Test
    fun `aggregates seed entries correctly`() {
        val a = getAnalytics(buildSeedJournalEntries())
        assertEquals(3, a.totalEntries)
        assertEquals(25.0 + 14 + 17, a.totalSpent, 0.0)
        assertTrue(abs(a.avgRating - (5 + 4 + 5) / 3.0) < 0.001)
        assertEquals(1.0, a.wouldSmokeAgainRate, 0.0)
        assertEquals(1, a.topBrands.first().count)
    }

    // advisor

    @Test
    fun `greets by username`() {
        val r = generateAdvisorResponse("hello", emptyList(), profile())
        assertTrue(r.contains("Cigar Enthusiast"))
    }

    @Test
    fun `answers bourbon pairing`() {
        val r = generateAdvisorResponse("best cigars for bourbon", emptyList(), profile())
        assertTrue(r.contains("bourbon"))
        assertTrue(r.contains("Padrón 1964 Maduro"))
    }

    @Test
    fun `recommends for tonight from catalog`() {
        val r = generateAdvisorResponse(
            "recommend something for tonight",
            buildSeedJournalEntries(),
            profile(),
        )
        assertTrue(r.contains("MSRP"))
    }

    // humidor model

    @Test
    fun `quantity round-trips through JSON`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val item = buildSeedHumidorItems().first()
        val decoded = json.decodeFromString(
            HumidorItem.serializer(),
            json.encodeToString(HumidorItem.serializer(), item),
        )
        assertEquals(item.quantity, decoded.quantity)
        assertEquals(item.brand, decoded.brand)
        assertEquals(item.targetAgingMonths, decoded.targetAgingMonths)
    }
}
