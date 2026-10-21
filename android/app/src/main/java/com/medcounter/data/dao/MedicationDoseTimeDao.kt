package com.medcounter.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medcounter.data.entity.MedicationDoseTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDoseTimeDao {
    @Query("SELECT * FROM medication_dose_times WHERE medicationId = :medicationId ORDER BY id ASC")
    fun getDoseTimesForMedication(medicationId: Long): Flow<List<MedicationDoseTimeEntity>>

    @Query("SELECT * FROM medication_dose_times WHERE medicationId = :medicationId ORDER BY id ASC")
    suspend fun getDoseTimesForMedicationSnapshot(medicationId: Long): List<MedicationDoseTimeEntity>

    @Query("SELECT * FROM medication_dose_times WHERE medicationId IN (:medicationIds)")
    suspend fun getDoseTimesForMedications(medicationIds: List<Long>): List<MedicationDoseTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseTimes(doseTimes: List<MedicationDoseTimeEntity>)

    @Query("DELETE FROM medication_dose_times WHERE medicationId = :medicationId")
    suspend fun deleteDoseTimesForMedication(medicationId: Long)
}
