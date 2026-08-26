package io.chronicle.usagestats.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.chronicle.usagestats.core.util.Constants
import io.chronicle.usagestats.domain.model.AccentColorPreset
import io.chronicle.usagestats.domain.model.ThemeMode
import io.chronicle.usagestats.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val DAILY_NOTIFICATION_ENABLED = booleanPreferencesKey("daily_notification_enabled")
        val DAILY_NOTIFICATION_HOUR = intPreferencesKey("daily_notification_hour")
        val DAILY_NOTIFICATION_MINUTE = intPreferencesKey("daily_notification_minute")
        val BADGE_ENABLED = booleanPreferencesKey("badge_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        // Screen Time Budgets
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_screen_time_goal_minutes")
        val WEEKEND_GOAL_ENABLED = booleanPreferencesKey("weekend_goal_enabled")
        val WEEKEND_GOAL_MINUTES = intPreferencesKey("weekend_goal_minutes")
        // Focus Mode
        val FOCUS_MODE_ENABLED = booleanPreferencesKey("focus_mode_enabled")
        val FOCUS_START_HOUR = intPreferencesKey("focus_start_hour")
        val FOCUS_START_MINUTE = intPreferencesKey("focus_start_minute")
        val FOCUS_END_HOUR = intPreferencesKey("focus_end_hour")
        val FOCUS_END_MINUTE = intPreferencesKey("focus_end_minute")
        // Enhanced Notifications
        val REALITY_CHECK_ENABLED = booleanPreferencesKey("reality_check_enabled")
        val MILESTONE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("milestone_notifications_enabled")
        val WEEKEND_NOTIFICATIONS_MUTED = booleanPreferencesKey("weekend_notifications_muted")
        // General
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val DAILY_RESET_HOUR = intPreferencesKey("daily_reset_hour")
        val SHOW_REMOVED_APPS = booleanPreferencesKey("show_removed_apps")
        // Accessibility
        val COMPACT_VIEW = booleanPreferencesKey("compact_view")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        // Data Management
        val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
        // Network & Data Budgets
        val DAILY_DATA_BUDGET_MB = intPreferencesKey("daily_data_budget_mb")
        val MONTHLY_DATA_BUDGET_GB = intPreferencesKey("monthly_data_budget_gb")
        val BILLING_CYCLE_START_DAY = intPreferencesKey("billing_cycle_start_day")
        val DATA_ALERTS_ENABLED = booleanPreferencesKey("data_alerts_enabled")
        val LIVE_NETWORK_SPEED_METER_ENABLED = booleanPreferencesKey("live_network_speed_meter_enabled")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.DARK.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeString)
            } catch (_: Exception) {
                ThemeMode.DARK
            }

            val accentColorString = preferences[PreferencesKeys.ACCENT_COLOR] ?: AccentColorPreset.SAPPHIRE.name
            val accentColor = try {
                AccentColorPreset.valueOf(accentColorString)
            } catch (_: Exception) {
                AccentColorPreset.SAPPHIRE
            }

            UserSettings(
                themeMode = themeMode,
                accentColor = accentColor,
                dailyNotificationEnabled = preferences[PreferencesKeys.DAILY_NOTIFICATION_ENABLED] ?: true,
                dailyNotificationHour = preferences[PreferencesKeys.DAILY_NOTIFICATION_HOUR] ?: Constants.DEFAULT_NOTIFICATION_HOUR,
                dailyNotificationMinute = preferences[PreferencesKeys.DAILY_NOTIFICATION_MINUTE] ?: Constants.DEFAULT_NOTIFICATION_MINUTE,
                badgeEnabled = preferences[PreferencesKeys.BADGE_ENABLED] ?: true,
                isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                // Screen Time Budgets
                dailyGoalMinutes = preferences[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 150,
                weekendGoalEnabled = preferences[PreferencesKeys.WEEKEND_GOAL_ENABLED] ?: false,
                weekendGoalMinutes = preferences[PreferencesKeys.WEEKEND_GOAL_MINUTES] ?: 240,
                // Focus Mode
                focusModeEnabled = preferences[PreferencesKeys.FOCUS_MODE_ENABLED] ?: false,
                focusStartHour = preferences[PreferencesKeys.FOCUS_START_HOUR] ?: 9,
                focusStartMinute = preferences[PreferencesKeys.FOCUS_START_MINUTE] ?: 0,
                focusEndHour = preferences[PreferencesKeys.FOCUS_END_HOUR] ?: 17,
                focusEndMinute = preferences[PreferencesKeys.FOCUS_END_MINUTE] ?: 0,
                // Enhanced Notifications
                realityCheckEnabled = preferences[PreferencesKeys.REALITY_CHECK_ENABLED] ?: true,
                milestoneNotificationsEnabled = preferences[PreferencesKeys.MILESTONE_NOTIFICATIONS_ENABLED] ?: true,
                weekendNotificationsMuted = preferences[PreferencesKeys.WEEKEND_NOTIFICATIONS_MUTED] ?: false,
                // General
                firstDayOfWeek = preferences[PreferencesKeys.FIRST_DAY_OF_WEEK] ?: "MONDAY",
                dailyResetHour = preferences[PreferencesKeys.DAILY_RESET_HOUR] ?: 0,
                showRemovedApps = preferences[PreferencesKeys.SHOW_REMOVED_APPS] ?: true,
                // Accessibility
                compactView = preferences[PreferencesKeys.COMPACT_VIEW] ?: false,
                highContrast = preferences[PreferencesKeys.HIGH_CONTRAST] ?: false,
                // Data Management
                dataRetentionDays = preferences[PreferencesKeys.DATA_RETENTION_DAYS] ?: -1,
                // Network & Data Budgets
                dailyDataBudgetMb = preferences[PreferencesKeys.DAILY_DATA_BUDGET_MB] ?: 2048,
                monthlyDataBudgetGb = preferences[PreferencesKeys.MONTHLY_DATA_BUDGET_GB] ?: 50,
                billingCycleStartDay = preferences[PreferencesKeys.BILLING_CYCLE_START_DAY] ?: 1,
                dataAlertsEnabled = preferences[PreferencesKeys.DATA_ALERTS_ENABLED] ?: true,
                liveNetworkSpeedMeterEnabled = preferences[PreferencesKeys.LIVE_NETWORK_SPEED_METER_ENABLED] ?: false
            )
        }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateAccentColor(accentColor: AccentColorPreset) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = accentColor.name
        }
    }

    suspend fun updateDailyNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun updateDailyNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_NOTIFICATION_HOUR] = hour
            preferences[PreferencesKeys.DAILY_NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun updateBadgeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BADGE_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    // Screen Time Budgets
    suspend fun updateDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_GOAL_MINUTES] = minutes
        }
    }

    suspend fun updateWeekendGoalEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEEKEND_GOAL_ENABLED] = enabled
        }
    }

    suspend fun updateWeekendGoalMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEEKEND_GOAL_MINUTES] = minutes
        }
    }

    // Focus Mode
    suspend fun updateFocusModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOCUS_MODE_ENABLED] = enabled
        }
    }

    suspend fun updateFocusSchedule(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOCUS_START_HOUR] = startHour
            preferences[PreferencesKeys.FOCUS_START_MINUTE] = startMinute
            preferences[PreferencesKeys.FOCUS_END_HOUR] = endHour
            preferences[PreferencesKeys.FOCUS_END_MINUTE] = endMinute
        }
    }

    // Enhanced Notifications
    suspend fun updateRealityCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REALITY_CHECK_ENABLED] = enabled
        }
    }

    suspend fun updateMilestoneNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MILESTONE_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateWeekendNotificationsMuted(muted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEEKEND_NOTIFICATIONS_MUTED] = muted
        }
    }

    // General
    suspend fun updateFirstDayOfWeek(day: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_DAY_OF_WEEK] = day
        }
    }

    suspend fun updateDailyResetHour(hour: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_RESET_HOUR] = hour
        }
    }

    suspend fun updateShowRemovedApps(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_REMOVED_APPS] = show
        }
    }

    // Accessibility
    suspend fun updateCompactView(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COMPACT_VIEW] = enabled
        }
    }

    suspend fun updateHighContrast(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_CONTRAST] = enabled
        }
    }

    // Data Management
    suspend fun updateDataRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_RETENTION_DAYS] = days
        }
    }

    // Network & Data Budgets
    suspend fun updateDailyDataBudgetMb(budgetMb: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_DATA_BUDGET_MB] = budgetMb
        }
    }

    suspend fun updateMonthlyDataBudgetGb(budgetGb: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_DATA_BUDGET_GB] = budgetGb
        }
    }

    suspend fun updateBillingCycleStartDay(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BILLING_CYCLE_START_DAY] = day
        }
    }

    suspend fun updateDataAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun updateLiveNetworkSpeedMeterEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIVE_NETWORK_SPEED_METER_ENABLED] = enabled
        }
    }
}
