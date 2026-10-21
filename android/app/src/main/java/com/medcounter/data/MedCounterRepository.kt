package com.medcounter.data

import com.medcounter.data.entity.AuditLogEntity
import com.medcounter.data.entity.MedicationDoseTimeEntity
import com.medcounter.data.entity.MedicationEntity
import com.medcounter.data.entity.ProfileEntity
import com.medcounter.data.entity.RestockEventEntity
import com.medcounter.data.entity.TimeSlotEntity
import com.medcounter.data.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow

class MedCounterRepository(private val database: MedCounterDatabase) {
    fun getAllProfiles(): Flow<List<ProfileEntity>> = database.profileDao().getAllProfiles()
    suspend fun getProfileById(id: Long): ProfileEntity? = database.profileDao().getProfileById(id)
    suspend fun insertProfile(profile: ProfileEntity): Long = database.profileDao().insertProfile(profile)
    suspend fun updateProfile(profile: ProfileEntity) = database.profileDao().updateProfile(profile)
    suspend fun deleteProfile(profile: ProfileEntity) = database.profileDao().deleteProfile(profile)
    suspend fun getAllProfilesSnapshot(): List<ProfileEntity> = database.profileDao().getAllProfilesSnapshot()

    fun getMedicationsForProfile(profileId: Long): Flow<List<MedicationEntity>> =
        database.medicationDao().getMedicationsForProfile(profileId)

    suspend fun getMedicationById(id: Long): MedicationEntity? = database.medicationDao().getMedicationById(id)
    fun getMedicationByIdFlow(id: Long): Flow<MedicationEntity?> = database.medicationDao().getMedicationByIdFlow(id)
    suspend fun insertMedication(medication: MedicationEntity): Long =
        database.medicationDao().insertMedication(medication)

    suspend fun updateMedication(medication: MedicationEntity) = database.medicationDao().updateMedication(medication)
    suspend fun updateQuantity(id: Long, qty: Double, asOf: Long) =
        database.medicationDao().updateQuantity(id, qty, asOf)

    suspend fun deleteMedication(medication: MedicationEntity) = database.medicationDao().deleteMedication(medication)
    suspend fun getAllActiveMedications(): List<MedicationEntity> = database.medicationDao().getAllActiveMedications()
    suspend fun getAllMedications(): List<MedicationEntity> = database.medicationDao().getAllMedications()
    suspend fun updateRefills(id: Long, count: Int) = database.medicationDao().updateRefills(id, count)
    suspend fun updatePillboxSplit(id: Long, bottleQty: Double, pillboxQty: Double, asOf: Long) =
        database.medicationDao().updatePillboxSplit(id, bottleQty, pillboxQty, asOf)

    suspend fun updatePauseState(id: Long, paused: Boolean, reason: String, qty: Double, asOf: Long) =
        database.medicationDao().updatePauseState(id, paused, reason, qty, asOf)

    fun getAllSlots(): Flow<List<TimeSlotEntity>> = database.timeSlotDao().getAllSlots()
    suspend fun getAllSlotsSnapshot(): List<TimeSlotEntity> = database.timeSlotDao().getAllSlotsSnapshot()
    suspend fun updateSlot(slot: TimeSlotEntity) = database.timeSlotDao().updateSlot(slot)
    suspend fun insertSlots(slots: List<TimeSlotEntity>) = database.timeSlotDao().insertSlots(slots)

    fun getDoseTimesForMedication(medicationId: Long): Flow<List<MedicationDoseTimeEntity>> =
        database.medicationDoseTimeDao().getDoseTimesForMedication(medicationId)

    suspend fun getDoseTimesForMedicationSnapshot(medicationId: Long): List<MedicationDoseTimeEntity> =
        database.medicationDoseTimeDao().getDoseTimesForMedicationSnapshot(medicationId)

    suspend fun getDoseTimesForMedications(medicationIds: List<Long>): List<MedicationDoseTimeEntity> =
        database.medicationDoseTimeDao().getDoseTimesForMedications(medicationIds)

    suspend fun saveDoseTimesForMedication(medicationId: Long, doseTimes: List<MedicationDoseTimeEntity>) {
        database.medicationDoseTimeDao().deleteDoseTimesForMedication(medicationId)
        if (doseTimes.isNotEmpty()) {
            database.medicationDoseTimeDao().insertDoseTimes(doseTimes)
        }
    }

    fun getRestockEventsForMedication(medicationId: Long): Flow<List<RestockEventEntity>> =
        database.restockEventDao().getRestockEventsForMedication(medicationId)

    suspend fun insertRestockEvent(event: RestockEventEntity): Long =
        database.restockEventDao().insertRestockEvent(event)

    suspend fun getAllRestockEvents(): List<RestockEventEntity> = database.restockEventDao().getAllRestockEvents()

    fun getUsageLogsForMedication(medicationId: Long): Flow<List<UsageLogEntity>> =
        database.usageLogDao().getUsageLogsForMedication(medicationId)

    suspend fun getUsageLogsForMedicationSnapshot(medicationId: Long): List<UsageLogEntity> =
        database.usageLogDao().getUsageLogsForMedicationSnapshot(medicationId)

    suspend fun insertUsageLog(log: UsageLogEntity): Long = database.usageLogDao().insertUsageLog(log)
    suspend fun getAllUsageLogs(): List<UsageLogEntity> = database.usageLogDao().getAllUsageLogs()

    fun getAuditLogsForMedication(medicationId: Long): Flow<List<AuditLogEntity>> =
        database.auditLogDao().getAuditLogsForMedication(medicationId)

    suspend fun insertAuditLog(log: AuditLogEntity): Long = database.auditLogDao().insertAuditLog(log)
    suspend fun getAllAuditLogs(): List<AuditLogEntity> = database.auditLogDao().getAllAuditLogs()
}
