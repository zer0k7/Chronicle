package io.chronicle.usagestats.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.model.AccentColorPreset
import io.chronicle.usagestats.domain.model.ThemeMode
import io.chronicle.usagestats.domain.model.UserSettings
import io.chronicle.usagestats.worker.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = userPreferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.updateThemeMode(mode) }
    }

    fun setAccentColor(accent: AccentColorPreset) {
        viewModelScope.launch { userPreferencesRepository.updateAccentColor(accent) }
    }

    fun setDailyNotificationEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            userPreferencesRepository.updateDailyNotificationEnabled(enabled)
            if (enabled) {
                val settings = userSettings.value
                NotificationHelper.scheduleDailyNotification(context, settings.dailyNotificationHour, settings.dailyNotificationMinute)
            } else {
                NotificationHelper.cancelDailyNotification(context)
            }
        }
    }

    fun setDailyNotificationTime(hour: Int, minute: Int, context: Context) {
        viewModelScope.launch {
            userPreferencesRepository.updateDailyNotificationTime(hour, minute)
            if (userSettings.value.dailyNotificationEnabled) {
                NotificationHelper.scheduleDailyNotification(context, hour, minute)
            }
        }
    }

    fun setBadgeEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateBadgeEnabled(enabled) }
    }
}
