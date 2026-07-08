package com.purovida.core

import kotlinx.serialization.Serializable

/**
 * Data model for Puro Vida, ported from the Flutter implementation (which
 * ported the original TypeScript types). JSON field names and defaults are
 * kept identical so stored data stays compatible in shape.
 *
 * Strength values: mild | mild-medium | medium | medium-full | full
 * Body values: light | medium | full
 * Experience: beginner | intermediate | advanced | aficionado
 */
data class Cigar(
    val id: String,
    val brand: String,
    val productLine: String,
    val vitola: String,
    val wrapper: String,
    val binder: String = "",
    val filler: String = "",
    val country: String,
    val factory: String? = null,
    val strength: String,
    val flavorTags: List<String>,
    val msrp: Double,
    val lengthInches: Double = 0.0,
    val ringGauge: Int = 0,
    val description: String,
    val communityRating: Int? = null,
    val reviewCount: Int? = null,
)

@Serializable
data class JournalEntry(
    val id: String,
    val cigarId: String? = null,
    val brand: String = "",
    val productLine: String = "",
    val vitola: String = "",
    val wrapper: String = "",
    val binder: String = "",
    val filler: String = "",
    val country: String = "",
    val strength: String = "medium",
    val body: String = "medium",
    val flavorTags: List<String> = emptyList(),
    val drawRating: Int = 3,
    val burnRating: Int = 3,
    val constructionRating: Int = 3,
    val overallRating: Int = 3,
    val dateSmoked: String = "",
    val location: String = "",
    val smokingDuration: Int = 60,
    val beveragePairing: String = "",
    val pairingNotes: String = "",
    val purchaseSource: String = "",
    val pricePaid: Double = 0.0,
    val msrp: Double = 0.0,
    val notes: String = "",
    val wouldSmokeAgain: Boolean = true,
    val photos: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class HumidorItem(
    val id: String,
    val cigarId: String? = null,
    val brand: String = "",
    val productLine: String = "",
    val vitola: String = "",
    val wrapper: String = "",
    val country: String = "",
    val quantity: Int = 1,
    val purchaseDate: String = "",
    val purchasePrice: Double = 0.0,
    val msrp: Double = 0.0,
    val purchaseSource: String = "",
    val agingStartDate: String = "",
    val targetAgingMonths: Int = 6,
    val flavorTags: List<String> = emptyList(),
    val strength: String = "medium",
    val notes: String = "",
    val photos: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class UserProfile(
    val id: String = "local-user",
    val username: String = "Cigar Enthusiast",
    val experienceLevel: String = "intermediate",
    val preferredStrengths: List<String> = emptyList(),
    val preferredFlavors: List<String> = emptyList(),
    val preferredWrappers: List<String> = emptyList(),
    val preferredCountries: List<String> = emptyList(),
    val humidorSize: Int = 50,
    val bio: String = "",
    val joinedDate: String = "",
    val avatarUri: String? = null,
)

data class Recommendation(
    val cigar: Cigar,
    val reason: String,
    val matchScore: Int,
    val type: String, // you-may-enjoy | similar | trending | try-next | pairing
)

data class ChatMessage(
    val id: String,
    val role: String, // user | assistant
    val content: String,
    val timestamp: String,
)

data class BrandCount(val brand: String, val count: Int)
data class FlavorCount(val tag: String, val count: Int)
data class StrengthCount(val strength: String, val count: Int)
data class RatingPoint(val date: String, val rating: Int)

data class Analytics(
    val totalEntries: Int,
    val totalSpent: Double,
    val avgRating: Double,
    val topBrands: List<BrandCount>,
    val topFlavors: List<FlavorCount>,
    val strengthBreakdown: List<StrengthCount>,
    val ratingTrend: List<RatingPoint>,
    val wouldSmokeAgainRate: Double,
)
