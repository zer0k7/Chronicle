package io.chronicle.usagestats.ui.limits

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.chronicle.usagestats.domain.model.AppLimitConfig
import io.chronicle.usagestats.domain.model.AppLimitStatus
import io.chronicle.usagestats.domain.model.LimitBypassMode
import io.chronicle.usagestats.domain.repository.AppLimitRepository
import io.chronicle.usagestats.service.AppLimitMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AppLimitUiItem(
    val packageName: String,
    val appLabel: String,
    val usedTodayMinutes: Int,
    val limitMinutes: Int?,
    val isBlocked: Boolean,
    val bypassMode: LimitBypassMode,
    val hasLimit: Boolean
)

@HiltViewModel
class AppLimitsViewModel @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rawInstalledApps = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    val limitsFlow: StateFlow<List<AppLimitConfig>> = appLimitRepository.getAllLimits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val uiItems: StateFlow<List<AppLimitUiItem>> = combine(
        limitsFlow,
        _searchQuery,
        _rawInstalledApps
    ) { limits, query, installedApps ->
        val limitMap = limits.associateBy { it.packageName }
        val now = System.currentTimeMillis()
        val todayStart = LocalDate.now(ZoneId.of("Asia/Kolkata"))
            .atStartOfDay(ZoneId.of("Asia/Kolkata"))
            .toInstant()
            .toEpochMilli()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val usageMap = mutableMapOf<String, Long>()
        if (usageStatsManager != null) {
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, todayStart, now)
            stats?.forEach { stat ->
                usageMap[stat.packageName] = (usageMap[stat.packageName] ?: 0L) + stat.totalTimeInForeground
            }
        }

        installedApps
            .filter { (pkg, label) ->
                query.isBlank() || label.contains(query, ignoreCase = true) || pkg.contains(query, ignoreCase = true)
            }
            .map { (pkg, label) ->
                val limitConfig = limitMap[pkg]
                val usedMillis = usageMap[pkg] ?: 0L
                val usedMinutes = (usedMillis / 60000).toInt()
                val limitMinutes = limitConfig?.dailyLimitMinutes
                val isBlocked = limitConfig != null && limitConfig.isEnabled && (usedMinutes >= limitConfig.dailyLimitMinutes)
                val bypassMode = limitConfig?.bypassMode ?: LimitBypassMode.NO_BYPASS

                AppLimitUiItem(
                    packageName = pkg,
                    appLabel = label,
                    usedTodayMinutes = usedMinutes,
                    limitMinutes = limitMinutes,
                    isBlocked = isBlocked,
                    bypassMode = bypassMode,
                    hasLimit = limitConfig != null
                )
            }
            .sortedWith(
                compareByDescending<AppLimitUiItem> { it.hasLimit }
                    .thenByDescending { it.usedTodayMinutes }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val launchableIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launchablePackages = pm.queryIntentActivities(launchableIntent, 0)
                .map { it.activityInfo.packageName }
                .toSet()

            val apps = packages
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM == 0) || launchablePackages.contains(it.packageName) }
                .filter { it.packageName != context.packageName }
                .map { appInfo ->
                    val label = pm.getApplicationLabel(appInfo).toString()
                    Pair(appInfo.packageName, label)
                }
                .sortedBy { it.second.lowercase() }

            _rawInstalledApps.value = apps
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setLimit(packageName: String, appLabel: String, dailyMinutes: Int, bypassMode: LimitBypassMode) {
        viewModelScope.launch {
            appLimitRepository.setLimit(packageName, appLabel, dailyMinutes, bypassMode)
            ensureServiceRunning()
        }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch {
            appLimitRepository.removeLimit(packageName)
            checkServiceStateAfterRemoval()
        }
    }

    private suspend fun ensureServiceRunning() {
        withContext(Dispatchers.Main) {
            AppLimitMonitorService.start(context)
        }
    }

    private suspend fun checkServiceStateAfterRemoval() {
        // If no limits remain, we can stop the service
        val remainingLimits = limitsFlow.value.filter { it.isEnabled }
        if (remainingLimits.isEmpty()) {
            withContext(Dispatchers.Main) {
                AppLimitMonitorService.stop(context)
            }
        }
    }

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
