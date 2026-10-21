package com.medcounter.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_slots")
data class TimeSlotEntity(
    @PrimaryKey val id: Long = 0,
    val name: String,
    val minutesOfDay: Int,
    val sortOrder: Int,
)
