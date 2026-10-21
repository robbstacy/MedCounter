package com.medcounter.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.medcounter.data.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE medicationId = :medicationId ORDER BY timestamp DESC")
    fun getAuditLogsForMedication(medicationId: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogs(): List<AuditLogEntity>
}
