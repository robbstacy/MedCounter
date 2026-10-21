package com.medcounter

import android.app.Application
import com.medcounter.data.AppContainer
import com.medcounter.notifications.DoseReminderNotifier
import com.medcounter.notifications.RefillNotifier
import com.medcounter.notifications.WeeklyReportNotifier
import com.medcounter.work.DoseReminderScheduler
import com.medcounter.work.RefillScheduler
import com.medcounter.work.WeeklyReportScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedCounterApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RefillNotifier.ensureChannel(this)
        DoseReminderNotifier.ensureChannel(this)
        WeeklyReportNotifier.ensureChannel(this)
        val prefs = container.preferences
        if (prefs.notificationsEnabled) {
            RefillScheduler.schedule(this, prefs.notificationHour)
        }
        if (prefs.doseRemindersEnabled) {
            appScope.launch {
                container.ensureDefaultTimeSlots()
                DoseReminderScheduler.rescheduleAll(this@MedCounterApplication, container.repository)
            }
        } else {
            appScope.launch { container.ensureDefaultTimeSlots() }
        }
        if (prefs.weeklyReportEnabled) {
            WeeklyReportScheduler.schedule(this)
        }
    }
}
