package com.purovida.app.data

import android.content.SharedPreferences
import com.purovida.core.HumidorItem
import com.purovida.core.JournalEntry
import com.purovida.core.UserProfile
import com.purovida.core.buildDefaultProfile
import com.purovida.core.buildSeedHumidorItems
import com.purovida.core.buildSeedJournalEntries
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Local persistence, ported from the Flutter stores: JSON strings in
 * SharedPreferences under the same keys, seeded on first launch, newest
 * entries first. IDs are epoch millis + a random base-36 suffix.
 */
private val json = Json { ignoreUnknownKeys = true }

private fun newId(): String =
    "${System.currentTimeMillis()}${Random.nextInt(0xFFFFFF).toString(36)}"

private fun nowIso(): String = Instant.now().toString()

class JournalStore(private val prefs: SharedPreferences) {
    private companion object {
        const val STORAGE_KEY = "puro_journal_entries"
        const val SEEDED_KEY = "puro_journal_seeded"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serializer = ListSerializer(JournalEntry.serializer())

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<JournalEntry>> = _entries.asStateFlow()

    private fun load(): List<JournalEntry> = runCatching {
        if (!prefs.getBoolean(SEEDED_KEY, false)) {
            val seed = buildSeedJournalEntries()
            prefs.edit()
                .putString(STORAGE_KEY, json.encodeToString(serializer, seed))
                .putBoolean(SEEDED_KEY, true)
                .apply()
            seed
        } else {
            prefs.getString(STORAGE_KEY, null)
                ?.let { json.decodeFromString(serializer, it) }
                ?: emptyList()
        }
    }.getOrDefault(emptyList())

    private fun save(list: List<JournalEntry>) {
        scope.launch {
            prefs.edit()
                .putString(STORAGE_KEY, json.encodeToString(serializer, list))
                .apply()
        }
    }

    fun addEntry(build: (id: String, now: String) -> JournalEntry) {
        val updated = listOf(build(newId(), nowIso())) + _entries.value
        _entries.value = updated
        save(updated)
    }

    fun deleteEntry(id: String) {
        val updated = _entries.value.filter { it.id != id }
        _entries.value = updated
        save(updated)
    }

    fun getEntry(id: String): JournalEntry? = _entries.value.firstOrNull { it.id == id }

    fun search(query: String): List<JournalEntry> {
        val q = query.lowercase()
        return _entries.value.filter { e ->
            e.brand.lowercase().contains(q) ||
                e.productLine.lowercase().contains(q) ||
                e.vitola.lowercase().contains(q) ||
                e.notes.lowercase().contains(q) ||
                e.flavorTags.any { it.lowercase().contains(q) }
        }
    }
}

/** Humidor inventory — quantity of zero deletes the item (original behavior). */
class HumidorStore(private val prefs: SharedPreferences) {
    private companion object {
        const val STORAGE_KEY = "puro_humidor_items"
        const val SEEDED_KEY = "puro_humidor_seeded"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serializer = ListSerializer(HumidorItem.serializer())

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<HumidorItem>> = _items.asStateFlow()

    private fun load(): List<HumidorItem> = runCatching {
        if (!prefs.getBoolean(SEEDED_KEY, false)) {
            val seed = buildSeedHumidorItems()
            prefs.edit()
                .putString(STORAGE_KEY, json.encodeToString(serializer, seed))
                .putBoolean(SEEDED_KEY, true)
                .apply()
            seed
        } else {
            prefs.getString(STORAGE_KEY, null)
                ?.let { json.decodeFromString(serializer, it) }
                ?: emptyList()
        }
    }.getOrDefault(emptyList())

    private fun save(list: List<HumidorItem>) {
        scope.launch {
            prefs.edit()
                .putString(STORAGE_KEY, json.encodeToString(serializer, list))
                .apply()
        }
    }

    fun addItem(build: (id: String, now: String) -> HumidorItem) {
        val updated = listOf(build(newId(), nowIso())) + _items.value
        _items.value = updated
        save(updated)
    }

    fun deleteItem(id: String) {
        val updated = _items.value.filter { it.id != id }
        _items.value = updated
        save(updated)
    }

    fun updateQuantity(id: String, quantity: Int) {
        if (quantity <= 0) {
            deleteItem(id)
            return
        }
        val updated = _items.value.map {
            if (it.id == id) it.copy(quantity = quantity, updatedAt = nowIso()) else it
        }
        _items.value = updated
        save(updated)
    }
}

/** Local user profile — no accounts, id is always "local-user". */
class UserStore(private val prefs: SharedPreferences) {
    private companion object {
        const val STORAGE_KEY = "puro_user_profile"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private fun load(): UserProfile = runCatching {
        prefs.getString(STORAGE_KEY, null)
            ?.let { json.decodeFromString(UserProfile.serializer(), it) }
    }.getOrNull() ?: buildDefaultProfile()

    fun updateProfile(updated: UserProfile) {
        _profile.value = updated
        scope.launch {
            prefs.edit()
                .putString(STORAGE_KEY, json.encodeToString(UserProfile.serializer(), updated))
                .apply()
        }
    }
}
