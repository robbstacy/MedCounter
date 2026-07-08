package com.purovida.core

import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Client for the AI backend (`server/` in the PuroVida repo).
 *
 * The base URL is supplied by the app (BuildConfig / local.properties);
 * it defaults to the Android emulator loopback for local development.
 * Implements POST /api/cigars/lookup and POST /api/cigars/identify.
 */
@Serializable
data class CigarLookupResult(
    val description: String = "",
    val country: String = "",
    val wrapper: String = "",
    val size: String = "",
    val strength: String = "",
    val found: Boolean = false,
)

@Serializable
data class CigarIdentifyResult(
    val brand: String = "",
    val productLine: String = "",
    val description: String = "",
    val country: String = "",
    val wrapper: String = "",
    val size: String = "",
    val strength: String = "",
    val confidence: String = "low", // high | medium | low
    val found: Boolean = false,
)

@Serializable
private data class LookupRequest(val brand: String, val productLine: String)

@Serializable
private data class IdentifyRequest(val image: String)

class CigarApi(private val baseUrl: String = "http://10.0.2.2:3001") {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun post(path: String, body: String): String {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().use { res ->
            val text = res.body?.string() ?: ""
            if (res.code != 200) {
                throw RuntimeException("Request failed (${res.code}): $text")
            }
            return text
        }
    }

    suspend fun lookupCigar(brand: String, productLine: String): CigarLookupResult =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(
                LookupRequest.serializer(),
                LookupRequest(brand.trim(), productLine.trim()),
            )
            json.decodeFromString(
                CigarLookupResult.serializer(),
                post("/api/cigars/lookup", body),
            )
        }

    suspend fun identifyCigar(jpegBytes: ByteArray): CigarIdentifyResult =
        withContext(Dispatchers.IO) {
            val dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpegBytes)
            val body = json.encodeToString(IdentifyRequest.serializer(), IdentifyRequest(dataUrl))
            json.decodeFromString(
                CigarIdentifyResult.serializer(),
                post("/api/cigars/identify", body),
            )
        }
}
