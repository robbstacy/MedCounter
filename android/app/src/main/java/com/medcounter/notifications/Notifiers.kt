package com.medcounter.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medcounter.MainActivity
import com.medcounter.R
import com.medcounter.data.PreferencesManager
import com.medcounter.domain.MedicationStatus
import com.medcounter.domain.MedicationWithCalculation
import java.time.format.DateTimeFormatter
import java.util.Locale

object RefillNotifier {
    private const val CHANNEL_ID = "refill_alerts"
    const val NOTIFICATION_ID = 1001
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Refill Alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders to refill medications that are running low"
        }
        manager.createNotificationChannel(channel)
    }

    fun postDigest(context: Context, meds: List<MedicationWithCalculation>) {
        val prefs = PreferencesManager(context)
        if (!prefs.notificationsEnabled) return
        val notifier = NotificationManagerCompat.from(context)
        if (meds.isEmpty()) {
            notifier.cancel(NOTIFICATION_ID)
            return
        }
        ensureChannel(context)
        val title = if (meds.size == 1) {
            "1 medication needs a refill"
        } else {
            "${meds.size} medications need a refill"
        }
        val inbox = NotificationCompat.InboxStyle()
        for (item in meds) {
            val calc = item.calculation
            var label = when (calc.status) {
                MedicationStatus.OUT -> "OUT of stock"
                MedicationStatus.CRITICAL -> "Critical — almost empty"
                MedicationStatus.REORDER -> calc.reorderByDate?.let { "Reorder by ${dateFmt.format(it)}" } ?: "Low supply"
                MedicationStatus.OK -> "Low supply"
            }
            val refills = item.medication.refillsRemaining
            if (refills == 0) label += "  ·  no refills — renew Rx"
            inbox.addLine("${item.medication.name}: $label")
        }
        val names = meds.map { it.medication.name }
        val content = if (names.size <= 2) {
            names.joinToString(", ")
        } else {
            names.take(2).joinToString(", ") + " +${names.size - 2} more"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(inbox)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifier.notify(NOTIFICATION_ID, notification)
    }
}

object DoseReminderNotifier {
    private const val CHANNEL_ID = "dose_reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Dose Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders to take scheduled medications"
        }
        manager.createNotificationChannel(channel)
    }

    fun postDoseReminder(
        context: Context,
        minuteOfDay: Int,
        slotName: String?,
        meds: List<com.medcounter.data.entity.MedicationEntity>,
    ) {
        if (meds.isEmpty()) return
        if (!PreferencesManager(context).doseRemindersEnabled) return
        ensureChannel(context)
        val timeLabel = slotName ?: CsvTime.formatMinute(minuteOfDay)
        val title = if (meds.size == 1) {
            "Time to take ${meds.first().name}"
        } else {
            "$timeLabel — ${meds.size} medications"
        }
        val body = meds.joinToString(", ") { it.name }
        val inbox = NotificationCompat.InboxStyle()
        meds.forEach { inbox.addLine(it.name) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(inbox)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(minuteOfDay + 2000, notification)
    }
}

object WeeklyReportNotifier {
    private const val CHANNEL_ID = "weekly_report"
    const val NOTIFICATION_ID = 1002
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Weekly Summary",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Weekly overview of all medication inventory"
        }
        manager.createNotificationChannel(channel)
    }

    fun postWeeklyDigest(context: Context, meds: List<MedicationWithCalculation>) {
        if (!PreferencesManager(context).weeklyReportEnabled) return
        if (meds.isEmpty()) return
        ensureChannel(context)
        val needing = meds.filter { it.calculation.status != MedicationStatus.OK }
        val ok = meds.filter { it.calculation.status == MedicationStatus.OK }
        val title = when {
            needing.isEmpty() -> "All ${meds.size} medications are well-stocked"
            needing.size == 1 -> "1 of ${meds.size} medications needs a refill"
            else -> "${needing.size} of ${meds.size} medications need a refill"
        }
        val inbox = NotificationCompat.InboxStyle()
        for (item in needing + ok) {
            val calc = item.calculation
            val label = when (calc.status) {
                MedicationStatus.OUT -> "OUT"
                MedicationStatus.CRITICAL -> "Critical"
                MedicationStatus.REORDER -> calc.reorderByDate?.let { "Reorder by ${dateFmt.format(it)}" } ?: "Low"
                MedicationStatus.OK -> calc.runOutDate?.let { "Runs out ${dateFmt.format(it)}" } ?: "OK"
            }
            inbox.addLine("${item.medication.name}: $label")
        }
        val summary = if (needing.isEmpty()) {
            "${meds.size} medications tracked"
        } else {
            "${needing.size} need attention · ${ok.size} OK"
        }
        inbox.setSummaryText(summary)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(inbox)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

private fun contentIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

private object CsvTime {
    fun formatMinute(minuteOfDay: Int): String = com.medcounter.data.export.CsvFormat.formatMinuteOfDay(minuteOfDay)
}
