package com.medcounter.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["profileId"])],
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val strength: String = "",
    val strengthUnit: String = "",
    val form: String = "tablet",
    val scheduleType: String,
    val intervalDays: Int = 1,
    val customDosesPerDay: Double = 1.0,
    val unitsPerDose: Double = 1.0,
    val quantityOnHand: Double,
    val quantityAsOf: Long,
    val leadTimeDays: Int = 7,
    val refillsRemaining: Int? = null,
    val pharmacy: String = "",
    val rxNumber: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val pillboxQuantity: Double? = null,
    val isPaused: Boolean = false,
    val pauseReason: String = "",
)
