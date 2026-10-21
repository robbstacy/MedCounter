package com.medcounter.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val profileId: Long,
    val profileName: String,
    val medicationId: Long,
    val medicationName: String,
    val action: String,
    val quantityDelta: Double? = null,
    val quantityBefore: Double? = null,
    val quantityAfter: Double? = null,
    val expectedQuantity: Double? = null,
    val note: String = "",
    val actor: String = "local",
)
