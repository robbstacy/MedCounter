package com.medcounter.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medcounter.data.entity.TimeSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSlotDao {
    @Query("SELECT * FROM time_slots ORDER BY sortOrder ASC")
    fun getAllSlots(): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots ORDER BY sortOrder ASC")
    suspend fun getAllSlotsSnapshot(): List<TimeSlotEntity>

    @Update
    suspend fun updateSlot(slot: TimeSlotEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSlots(slots: List<TimeSlotEntity>)
}
