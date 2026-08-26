package io.chronicle.usagestats.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.chronicle.usagestats.core.updater.AppUpdateInfo
import io.chronicle.usagestats.core.updater.AppUpdateManager
import io.chronicle.usagestats.core.updater.UpdateState
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.data.local.ChronicleDatabase
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.model.AccentColorPreset
import io.chronicle.usagestats.domain.model.ThemeMode
import io.chronicle.usagestats.domain.model.UserSettings
import io.chronicle.usagestats.domain.usecase.ExportCsvUseCase
import io.chronicle.usagestats.worker.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appUpdateManager: AppUpdateManager,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val database: ChronicleDatabase
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = userPreferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val updateState: StateFlow<UpdateState> = appUpdateManager.updateState
    val isUpdateDialogVisible: StateFlow<Boolean> = appUpdateManager.isDialogVisible

    private val _dataActionMessage = MutableStateFlow<String?>(null)
    val dataActionMessage: StateFlow<String?> = _dataActionMessage.asStateFlow()

    fun clearDataActionMessage() {
        _dataActionMessage.value = null
    }

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

    // Screen Time Budgets
    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { userPreferencesRepository.updateDailyGoalMinutes(minutes) }
    }

    fun setWeekendGoalEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateWeekendGoalEnabled(enabled) }
    }

    fun setWeekendGoalMinutes(minutes: Int) {
        viewModelScope.launch { userPreferencesRepository.updateWeekendGoalMinutes(minutes) }
    }

    // Focus Mode
    fun setFocusModeEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateFocusModeEnabled(enabled) }
    }

    fun setFocusSchedule(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch { userPreferencesRepository.updateFocusSchedule(startHour, startMinute, endHour, endMinute) }
    }

    // Enhanced Notifications
    fun setRealityCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateRealityCheckEnabled(enabled) }
    }

    fun setMilestoneNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateMilestoneNotificationsEnabled(enabled) }
    }

    fun setWeekendNotificationsMuted(muted: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateWeekendNotificationsMuted(muted) }
    }

    // General
    fun setFirstDayOfWeek(day: String) {
        viewModelScope.launch { userPreferencesRepository.updateFirstDayOfWeek(day) }
    }

    fun setDailyResetHour(hour: Int) {
        viewModelScope.launch { userPreferencesRepository.updateDailyResetHour(hour) }
    }

    fun setShowRemovedApps(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateShowRemovedApps(show) }
    }

    // Accessibility
    fun setCompactView(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateCompactView(enabled) }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateHighContrast(enabled) }
    }

    // Data Management
    fun setDataRetentionDays(days: Int) {
        viewModelScope.launch { userPreferencesRepository.updateDataRetentionDays(days) }
    }

    // Network & Data Budgets
    fun setDailyDataBudgetMb(budgetMb: Int) {
        viewModelScope.launch { userPreferencesRepository.updateDailyDataBudgetMb(budgetMb) }
    }

    fun setMonthlyDataBudgetGb(budgetGb: Int) {
        viewModelScope.launch { userPreferencesRepository.updateMonthlyDataBudgetGb(budgetGb) }
    }

    fun setBillingCycleStartDay(day: Int) {
        viewModelScope.launch { userPreferencesRepository.updateBillingCycleStartDay(day) }
    }

    fun setDataAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateDataAlertsEnabled(enabled) }
    }

    fun setLiveNetworkSpeedMeterEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            userPreferencesRepository.updateLiveNetworkSpeedMeterEnabled(enabled)
            if (enabled) {
                io.chronicle.usagestats.service.LiveNetworkSpeedService.start(context)
            } else {
                io.chronicle.usagestats.service.LiveNetworkSpeedService.stop(context)
            }
        }
    }

    fun clearUsageData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.clearAllTables()
                _dataActionMessage.value = "Data cleared successfully"
            } catch (e: Exception) {
                _dataActionMessage.value = "Failed to clear data"
            }
        }
    }

    fun exportCsvData(context: Context) {
        viewModelScope.launch {
            try {
                val start = DateTimeUtils.toZonedDateTime(System.currentTimeMillis()).minusYears(2).toInstant().toEpochMilli()
                val end = System.currentTimeMillis()
                exportCsvUseCase(context, start, end)
                _dataActionMessage.value = "CSV exported successfully"
            } catch (e: Exception) {
                _dataActionMessage.value = "Failed to export CSV: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun checkForUpdates() {
        appUpdateManager.checkForUpdates(silent = false)
    }

    fun startDownload(info: AppUpdateInfo) {
        appUpdateManager.startDownload(info)
    }

    fun installApk(apkFile: File) {
        appUpdateManager.installApk(apkFile)
    }

    fun dismissUpdateDialog() {
        appUpdateManager.dismissDialog()
    }
}
