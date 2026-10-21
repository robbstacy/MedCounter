package com.medcounter.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medcounter.data.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE profileId = :profileId AND isActive = 1 ORDER BY name ASC")
    fun getMedicationsForProfile(profileId: Long): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE id = :id")
    fun getMedicationByIdFlow(id: Long): Flow<MedicationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("UPDATE medications SET quantityOnHand = :qty, quantityAsOf = :asOf WHERE id = :id")
    suspend fun updateQuantity(id: Long, qty: Double, asOf: Long)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE isActive = 1")
    suspend fun getAllActiveMedications(): List<MedicationEntity>

    @Query("SELECT * FROM medications ORDER BY profileId, name ASC")
    suspend fun getAllMedications(): List<MedicationEntity>

    @Query("UPDATE medications SET refillsRemaining = :count WHERE id = :id")
    suspend fun updateRefills(id: Long, count: Int)

    @Query(
        "UPDATE medications SET quantityOnHand = :bottleQty, pillboxQuantity = :pillboxQty, quantityAsOf = :asOf WHERE id = :id",
    )
    suspend fun updatePillboxSplit(id: Long, bottleQty: Double, pillboxQty: Double, asOf: Long)

    @Query(
        "UPDATE medications SET isPaused = :paused, pauseReason = :reason, quantityOnHand = :qty, quantityAsOf = :asOf WHERE id = :id",
    )
    suspend fun updatePauseState(id: Long, paused: Boolean, reason: String, qty: Double, asOf: Long)
}
