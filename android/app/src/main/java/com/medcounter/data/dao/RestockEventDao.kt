package com.medcounter.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.medcounter.data.entity.RestockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockEventDao {
    @Query("SELECT * FROM restock_events WHERE medicationId = :medicationId ORDER BY date DESC")
    fun getRestockEventsForMedication(medicationId: Long): Flow<List<RestockEventEntity>>

    @Insert
    suspend fun insertRestockEvent(event: RestockEventEntity): Long

    @Delete
    suspend fun deleteRestockEvent(event: RestockEventEntity)

    @Query("SELECT * FROM restock_events ORDER BY date DESC")
    suspend fun getAllRestockEvents(): List<RestockEventEntity>
}
