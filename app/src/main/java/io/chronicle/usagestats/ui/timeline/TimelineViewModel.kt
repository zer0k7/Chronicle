package io.chronicle.usagestats.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.usecase.GetTimelineUsageUseCase
import io.chronicle.usagestats.domain.usecase.SyncUsageDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val getTimelineUsageUseCase: GetTimelineUsageUseCase,
    private val syncUsageDataUseCase: SyncUsageDataUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimelinePeriod.DAY)
    val selectedPeriod: StateFlow<TimelinePeriod> = _selectedPeriod.asStateFlow()

    private val _referenceDate = MutableStateFlow(System.currentTimeMillis())
    val referenceDate: StateFlow<Long> = _referenceDate.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val dailyGoalMinutes: StateFlow<Int> = userPreferencesRepository.userSettingsFlow
        .map { it.dailyGoalMinutes }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    val timelineData: StateFlow<TimelineData?> = combine(_selectedPeriod, _referenceDate) { period, date ->
            Pair(period, date)
        }
        .flatMapLatest { (period, dateMillis) ->
            getTimelineUsageUseCase(period, dateMillis)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshData()
        viewModelScope.launch {
            try {
                syncUsageDataUseCase.syncRecentDays(7)
            } catch (_: Exception) { }
        }
    }

    private val _selectedHour = MutableStateFlow<Int?>(null)
    val selectedHour: StateFlow<Int?> = _selectedHour.asStateFlow()

    fun selectPeriod(period: TimelinePeriod) {
        _selectedPeriod.value = period
        _selectedHour.value = null
        refreshData()
    }

    fun selectDate(epochMillis: Long) {
        _referenceDate.value = epochMillis
        _selectedHour.value = null
        refreshData()
    }

    fun selectHour(hour: Int?) {
        _selectedHour.value = hour
    }

    fun navigatePrevious() {
        val current = DateTimeUtils.toZonedDateTime(_referenceDate.value)
        val newDate = when (_selectedPeriod.value) {
            TimelinePeriod.DAY -> current.minusDays(1)
            TimelinePeriod.WEEK -> current.minusWeeks(1)
            TimelinePeriod.MONTH -> current.minusMonths(1)
            TimelinePeriod.YEAR -> current.minusYears(1)
        }
        _referenceDate.value = newDate.toInstant().toEpochMilli()
        refreshData()
    }

    fun navigateNext() {
        val current = DateTimeUtils.toZonedDateTime(_referenceDate.value)
        val now = DateTimeUtils.nowInIst()
        val newDate = when (_selectedPeriod.value) {
            TimelinePeriod.DAY -> current.plusDays(1)
            TimelinePeriod.WEEK -> current.plusWeeks(1)
            TimelinePeriod.MONTH -> current.plusMonths(1)
            TimelinePeriod.YEAR -> current.plusYears(1)
        }
        if (newDate.isBefore(now) || newDate.toLocalDate() == now.toLocalDate()) {
            _referenceDate.value = newDate.toInstant().toEpochMilli()
            refreshData()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                when (_selectedPeriod.value) {
                    TimelinePeriod.DAY -> {
                        syncUsageDataUseCase.syncDate(_referenceDate.value)
                    }
                    TimelinePeriod.WEEK -> {
                        val start = DateTimeUtils.getStartOfWeek(_referenceDate.value)
                        val end = DateTimeUtils.getEndOfWeek(_referenceDate.value)
                        syncUsageDataUseCase.syncRange(start, end)
                    }
                    TimelinePeriod.MONTH -> {
                        val start = DateTimeUtils.getStartOfMonth(_referenceDate.value)
                        val end = DateTimeUtils.getEndOfMonth(_referenceDate.value)
                        syncUsageDataUseCase.syncRange(start, end)
                    }
                    TimelinePeriod.YEAR -> {
                        val start = DateTimeUtils.getStartOfYear(_referenceDate.value)
                        val end = DateTimeUtils.getEndOfYear(_referenceDate.value)
                        syncUsageDataUseCase.syncRange(start, end)
                    }
                }
            } catch (_: Exception) {
                // Sync failure is non-fatal
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
