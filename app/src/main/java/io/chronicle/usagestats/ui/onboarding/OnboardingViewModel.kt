package io.chronicle.usagestats.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.model.AccentColorPreset
import io.chronicle.usagestats.domain.model.ThemeMode
import io.chronicle.usagestats.domain.model.UserSettings
import io.chronicle.usagestats.domain.usecase.SyncUsageDataUseCase
import io.chronicle.usagestats.worker.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME,
    USAGE_ACCESS,
    NOTIFICATIONS,
    STORAGE,
    NOTIFICATION_TIME,
    APPEARANCE
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val syncUsageDataUseCase: SyncUsageDataUseCase
) : ViewModel() {

    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    val userSettings: StateFlow<UserSettings> = userPreferencesRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun nextStep() {
        val next = when (_currentStep.value) {
            OnboardingStep.WELCOME -> OnboardingStep.USAGE_ACCESS
            OnboardingStep.USAGE_ACCESS -> OnboardingStep.NOTIFICATIONS
            OnboardingStep.NOTIFICATIONS -> OnboardingStep.STORAGE
            OnboardingStep.STORAGE -> OnboardingStep.NOTIFICATION_TIME
            OnboardingStep.NOTIFICATION_TIME -> OnboardingStep.APPEARANCE
            OnboardingStep.APPEARANCE -> OnboardingStep.APPEARANCE
        }
        _currentStep.value = next
    }

    fun previousStep() {
        val prev = when (_currentStep.value) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.USAGE_ACCESS -> OnboardingStep.WELCOME
            OnboardingStep.NOTIFICATIONS -> OnboardingStep.USAGE_ACCESS
            OnboardingStep.STORAGE -> OnboardingStep.NOTIFICATIONS
            OnboardingStep.NOTIFICATION_TIME -> OnboardingStep.STORAGE
            OnboardingStep.APPEARANCE -> OnboardingStep.NOTIFICATION_TIME
        }
        _currentStep.value = prev
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.updateThemeMode(mode)
        }
    }

    fun setAccentColor(accent: AccentColorPreset) {
        viewModelScope.launch {
            userPreferencesRepository.updateAccentColor(accent)
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateDailyNotificationTime(hour, minute)
        }
    }

    fun completeOnboarding(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)

            val settings = userSettings.value
            if (settings.dailyNotificationEnabled) {
                NotificationHelper.scheduleDailyNotification(
                    context = context,
                    hour = settings.dailyNotificationHour,
                    minute = settings.dailyNotificationMinute
                )
            }

            if (PermissionHelper.hasUsageStatsPermission(context)) {
                syncUsageDataUseCase.syncRecentDays(7)
            }

            onComplete()
        }
    }
}
