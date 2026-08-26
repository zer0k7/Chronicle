package io.chronicle.usagestats.ui.datausage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataFilter
import io.chronicle.usagestats.domain.model.DataPeriod
import io.chronicle.usagestats.domain.model.DataSortOrder
import io.chronicle.usagestats.domain.model.NetworkTypeFilter
import io.chronicle.usagestats.domain.model.UserSettings
import io.chronicle.usagestats.domain.usecase.GetDataUsageUseCase
import io.chronicle.usagestats.domain.usecase.SyncDataUsageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataUsageViewModel @Inject constructor(
    private val getDataUsageUseCase: GetDataUsageUseCase,
    private val syncDataUsageUseCase: SyncDataUsageUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(DataPeriod.DAY)
    val selectedPeriod: StateFlow<DataPeriod> = _selectedPeriod.asStateFlow()

    private val _referenceDate = MutableStateFlow(DateTimeUtils.getStartOfDay())
    val referenceDate: StateFlow<Long> = _referenceDate.asStateFlow()

    private val _filter = MutableStateFlow(DataFilter())
    val filter: StateFlow<DataFilter> = _filter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _hasPermission = MutableStateFlow(PermissionHelper.hasUsageStatsPermission(context))
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    val userSettings: StateFlow<UserSettings> = userPreferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val dataSummary: StateFlow<DailyDataUsageSummary?> = combine(
        _selectedPeriod,
        _referenceDate,
        _filter,
        userSettings
    ) { period, date, filter, settings ->
        FourTuple(period, date, filter, settings.billingCycleStartDay)
    }.flatMapLatest { tuple ->
        getDataUsageUseCase(
            period = tuple.period,
            referenceDate = tuple.date,
            filter = tuple.filter,
            billingCycleDay = tuple.billingCycleDay
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        checkPermission()
        refreshData()
    }

    fun checkPermission() {
        _hasPermission.value = PermissionHelper.hasUsageStatsPermission(context)
    }

    fun selectPeriod(period: DataPeriod) {
        _selectedPeriod.value = period
        refreshData()
    }

    fun selectDate(millis: Long) {
        _referenceDate.value = DateTimeUtils.getStartOfDay(millis)
        refreshData()
    }

    fun setNetworkTypeFilter(type: NetworkTypeFilter) {
        _filter.value = _filter.value.copy(networkType = type)
    }

    fun setSortOrder(order: DataSortOrder) {
        _filter.value = _filter.value.copy(sortOrder = order)
    }

    fun setSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun navigatePrevious() {
        val current = DateTimeUtils.toZonedDateTime(_referenceDate.value)
        val newDate = when (_selectedPeriod.value) {
            DataPeriod.DAY -> current.minusDays(1)
            DataPeriod.WEEK -> current.minusWeeks(1)
            DataPeriod.MONTH, DataPeriod.BILLING_CYCLE -> current.minusMonths(1)
        }
        _referenceDate.value = newDate.toInstant().toEpochMilli()
        refreshData()
    }

    fun navigateNext() {
        val current = DateTimeUtils.toZonedDateTime(_referenceDate.value)
        val now = DateTimeUtils.nowInIst()
        val newDate = when (_selectedPeriod.value) {
            DataPeriod.DAY -> current.plusDays(1)
            DataPeriod.WEEK -> current.plusWeeks(1)
            DataPeriod.MONTH, DataPeriod.BILLING_CYCLE -> current.plusMonths(1)
        }
        if (newDate.isBefore(now) || newDate.toLocalDate() == now.toLocalDate()) {
            _referenceDate.value = newDate.toInstant().toEpochMilli()
            refreshData()
        }
    }

    fun refreshData() {
        checkPermission()
        if (!_hasPermission.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                when (_selectedPeriod.value) {
                    DataPeriod.DAY -> {
                        syncDataUsageUseCase.syncDate(_referenceDate.value)
                    }
                    DataPeriod.WEEK -> {
                        val start = DateTimeUtils.getStartOfWeek(_referenceDate.value)
                        val end = DateTimeUtils.getEndOfWeek(_referenceDate.value)
                        syncDataUsageUseCase.syncRange(start, end)
                    }
                    DataPeriod.MONTH -> {
                        val start = DateTimeUtils.getStartOfMonth(_referenceDate.value)
                        val end = DateTimeUtils.getEndOfMonth(_referenceDate.value)
                        syncDataUsageUseCase.syncRange(start, end)
                    }
                    DataPeriod.BILLING_CYCLE -> {
                        val start = DateTimeUtils.getStartOfMonth(_referenceDate.value)
                        val end = DateTimeUtils.getEndOfMonth(_referenceDate.value)
                        syncDataUsageUseCase.syncRange(start, end)
                    }
                }
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private data class FourTuple<A, B, C, D>(
        val period: A,
        val date: B,
        val filter: C,
        val billingCycleDay: D
    )
}
