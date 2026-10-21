package com.medcounter.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.medcounter.data.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {
    @Query("SELECT * FROM usage_logs WHERE medicationId = :medicationId ORDER BY timestamp DESC")
    fun getUsageLogsForMedication(medicationId: Long): Flow<List<UsageLogEntity>>

    @Query("SELECT * FROM usage_logs WHERE medicationId = :medicationId ORDER BY timestamp DESC")
    suspend fun getUsageLogsForMedicationSnapshot(medicationId: Long): List<UsageLogEntity>

    @Query("SELECT COALESCE(SUM(quantityUsed), 0) FROM usage_logs WHERE medicationId = :medicationId AND timestamp >= :since")
    suspend fun getTotalUsedSince(medicationId: Long, since: Long): Double

    @Insert
    suspend fun insertUsageLog(log: UsageLogEntity): Long

    @Delete
    suspend fun deleteUsageLog(log: UsageLogEntity)

    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC")
    suspend fun getAllUsageLogs(): List<UsageLogEntity>
}
