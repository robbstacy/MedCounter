package com.medcounter.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.medcounter.MedCounterApplication
import com.medcounter.domain.DoseScheduleResolver
import com.medcounter.domain.MedicationCalculationService
import com.medcounter.domain.MedicationStatus
import com.medcounter.notifications.DoseReminderNotifier
import com.medcounter.notifications.RefillNotifier
import com.medcounter.notifications.WeeklyReportNotifier
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Dose reminders stay on inexact WorkManager periodic work, matching the
 * recovered app. Exact alarms are a roadmap idea and are not used here.
 */
object RefillScheduler {
    const val WORK_NAME = "refill_check_daily"

    fun schedule(context: Context, hour: Int, now: Calendar = Calendar.getInstance()) {
        val request = PeriodicWorkRequestBuilder<RefillCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext(hour, now), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun reschedule(context: Context, hour: Int) {
        cancel(context)
        schedule(context, hour)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun runOnceNow(context: Context) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RefillCheckWorker>().build())
    }

    fun delayUntilNext(hour: Int, now: Calendar = Calendar.getInstance()): Long {
        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, hour)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}

object WeeklyReportScheduler {
    const val WORK_NAME = "weekly_report"

    fun schedule(context: Context, now: Calendar = Calendar.getInstance()) {
        val request = PeriodicWorkRequestBuilder<WeeklyReportWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNextSunday(9, now), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun delayUntilNextSunday(hour: Int, now: Calendar = Calendar.getInstance()): Long {
        val target = now.clone() as Calendar
        val daysUntilSunday = (Calendar.SUNDAY - target.get(Calendar.DAY_OF_WEEK) + 7) % 7
        target.add(Calendar.DAY_OF_YEAR, daysUntilSunday)
        target.set(Calendar.HOUR_OF_DAY, hour)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 7)
        return target.timeInMillis - now.timeInMillis
    }
}

object DoseReminderScheduler {
    const val TAG = "dose_reminder"

    suspend fun rescheduleAll(context: Context, repository: com.medcounter.data.MedCounterRepository) {
        val meds = repository.getAllActiveMedications()
        val slots = repository.getAllSlotsSnapshot()
        val slotMinutes = slots.associate { it.id to it.minutesOfDay }
        val times = if (meds.isEmpty()) {
            emptyList()
        } else {
            repository.getDoseTimesForMedications(meds.map { it.id })
        }
        val byMed = times.groupBy { it.medicationId }
        val minutes = meds.filter { !it.isPaused }.flatMap { med ->
            DoseScheduleResolver.resolveMinutes(
                byMed[med.id].orEmpty().map { it.slotId to it.customMinutesOfDay },
                slotMinutes,
            )
        }.toSortedSet()
        scheduleMinutes(context, minutes)
    }

    fun scheduleMinutes(context: Context, minutes: Collection<Int>, now: Calendar = Calendar.getInstance()) {
        cancelAll(context)
        val workManager = WorkManager.getInstance(context)
        for (minute in minutes) {
            val request = PeriodicWorkRequestBuilder<DoseReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextMinute(minute, now), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("minute_of_day" to minute))
                .addTag(TAG)
                .build()
            workManager.enqueueUniquePeriodicWork(
                "dose_reminder_$minute",
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                request,
            )
        }
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    fun delayUntilNextMinute(minuteOfDay: Int, now: Calendar = Calendar.getInstance()): Long {
        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        target.set(Calendar.MINUTE, minuteOfDay % 60)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}

class RefillCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MedCounterApplication
        val repository = app.container.repository
        val today = LocalDate.now()
        val slots = repository.getAllSlotsSnapshot().associate { it.id to it.minutesOfDay }
        val meds = repository.getAllActiveMedications().filter { !it.isPaused }
        val calculated = meds.map { med ->
            val times = repository.getDoseTimesForMedicationSnapshot(med.id)
            val minutes = DoseScheduleResolver.resolveMinutes(
                times.map { it.slotId to it.customMinutesOfDay },
                slots,
            )
            val logs = repository.getUsageLogsForMedicationSnapshot(med.id)
            MedicationCalculationService.calculate(med, logs, today, minutes)
        }.filter { it.calculation.status != MedicationStatus.OK }
        RefillNotifier.postDigest(applicationContext, calculated)
        return Result.success()
    }
}

class WeeklyReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MedCounterApplication
        val repository = app.container.repository
        val today = LocalDate.now()
        val slots = repository.getAllSlotsSnapshot().associate { it.id to it.minutesOfDay }
        val calculated = repository.getAllActiveMedications().map { med ->
            val times = repository.getDoseTimesForMedicationSnapshot(med.id)
            val minutes = DoseScheduleResolver.resolveMinutes(
                times.map { it.slotId to it.customMinutesOfDay },
                slots,
            )
            val logs = repository.getUsageLogsForMedicationSnapshot(med.id)
            MedicationCalculationService.calculate(med, logs, today, minutes)
        }
        WeeklyReportNotifier.postWeeklyDigest(applicationContext, calculated)
        return Result.success()
    }
}

class DoseReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val minute = inputData.getInt("minute_of_day", -1)
        if (minute < 0) return Result.success()
        val app = applicationContext as MedCounterApplication
        val repository = app.container.repository
        val slots = repository.getAllSlotsSnapshot()
        val slotMinutes = slots.associate { it.id to it.minutesOfDay }
        val slotName = slots.firstOrNull { it.minutesOfDay == minute }?.name
        val meds = repository.getAllActiveMedications().filter { med ->
            if (med.isPaused) return@filter false
            val times = repository.getDoseTimesForMedicationSnapshot(med.id)
            val minutes = DoseScheduleResolver.resolveMinutes(
                times.map { it.slotId to it.customMinutesOfDay },
                slotMinutes,
            )
            minute in minutes
        }
        if (meds.isNotEmpty()) {
            DoseReminderNotifier.postDoseReminder(applicationContext, minute, slotName, meds)
        }
        return Result.success()
    }
}
