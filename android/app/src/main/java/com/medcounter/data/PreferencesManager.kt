package com.medcounter.data

import android.content.Context

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("medcounter_prefs", Context.MODE_PRIVATE)

    var selectedProfileId: Long
        get() = prefs.getLong("selected_profile_id", -1L)
        set(value) {
            prefs.edit().putLong("selected_profile_id", value).apply()
        }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) {
            prefs.edit().putBoolean("notifications_enabled", value).apply()
        }

    var notificationHour: Int
        get() = prefs.getInt("notification_hour", 9)
        set(value) {
            prefs.edit().putInt("notification_hour", value).apply()
        }

    var doseRemindersEnabled: Boolean
        get() = prefs.getBoolean("dose_reminders_enabled", true)
        set(value) {
            prefs.edit().putBoolean("dose_reminders_enabled", value).apply()
        }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean("has_completed_onboarding", false)
        set(value) {
            prefs.edit().putBoolean("has_completed_onboarding", value).apply()
        }

    var weeklyReportEnabled: Boolean
        get() = prefs.getBoolean("weekly_report_enabled", true)
        set(value) {
            prefs.edit().putBoolean("weekly_report_enabled", value).apply()
        }
}
