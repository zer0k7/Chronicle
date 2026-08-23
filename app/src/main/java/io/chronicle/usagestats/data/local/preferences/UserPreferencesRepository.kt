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
                isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
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
}
