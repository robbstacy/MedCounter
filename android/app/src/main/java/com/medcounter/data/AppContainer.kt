package com.medcounter.data

import android.content.Context
import com.medcounter.data.entity.TimeSlotEntity

class AppContainer(context: Context) {
    val database: MedCounterDatabase = MedCounterDatabase.getInstance(context)
    val repository: MedCounterRepository = MedCounterRepository(database)
    val preferences: PreferencesManager = PreferencesManager(context)

    suspend fun ensureDefaultTimeSlots() {
        if (repository.getAllSlotsSnapshot().isEmpty()) {
            repository.insertSlots(DEFAULT_TIME_SLOTS)
        }
    }

    companion object {
        val DEFAULT_TIME_SLOTS = listOf(
            TimeSlotEntity(id = 1, name = "Morning", minutesOfDay = 480, sortOrder = 0),
            TimeSlotEntity(id = 2, name = "Midday", minutesOfDay = 840, sortOrder = 1),
            TimeSlotEntity(id = 3, name = "Evening", minutesOfDay = 1080, sortOrder = 2),
            TimeSlotEntity(id = 4, name = "Bedtime", minutesOfDay = 1320, sortOrder = 3),
        )
    }
}
