package io.chronicle.usagestats.data.repository

import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.data.datasource.NetworkStatsDataSource
import io.chronicle.usagestats.data.local.dao.AppOverrideDao
import io.chronicle.usagestats.data.local.dao.NetworkUsageDao
import io.chronicle.usagestats.data.local.entity.AppDataUsageEntity
import io.chronicle.usagestats.data.local.entity.DailyDataUsageEntity
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataFilter
import io.chronicle.usagestats.domain.model.DataSortOrder
import io.chronicle.usagestats.domain.model.DataUsageInfo
import io.chronicle.usagestats.domain.model.NetworkTypeFilter
import io.chronicle.usagestats.domain.repository.NetworkUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUsageRepositoryImpl @Inject constructor(
    private val dataSource: NetworkStatsDataSource,
    private val networkDao: NetworkUsageDao,
    private val appOverrideDao: AppOverrideDao
) : NetworkUsageRepository {

    override fun getDailyDataUsage(dateMillis: Long, filter: DataFilter): Flow<DailyDataUsageSummary> = flow {
        val startOfDay = DateTimeUtils.getStartOfDay(dateMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateMillis)

        // Query fresh telemetry
        val rawSummary = dataSource.getNetworkUsage(startOfDay, endOfDay)
        
        // Cache to Room
        saveSummaryToDb(rawSummary, startOfDay)

        // Apply overrides and filters
        val enriched = enrichAndFilterSummary(rawSummary, filter)
        emit(enriched)
    }.flowOn(Dispatchers.IO)

    override fun getDataUsageRange(
        startMillis: Long,
        endMillis: Long,
        filter: DataFilter
    ): Flow<DailyDataUsageSummary> = flow {
        val rawSummary = dataSource.getNetworkUsage(startMillis, endMillis)
        val enriched = enrichAndFilterSummary(rawSummary, filter)
        emit(enriched)
    }.flowOn(Dispatchers.IO)

    override suspend fun syncDataUsage(dateMillis: Long) = withContext(Dispatchers.IO) {
        val startOfDay = DateTimeUtils.getStartOfDay(dateMillis)
        val endOfDay = DateTimeUtils.getEndOfDay(dateMillis)
        val summary = dataSource.getNetworkUsage(startOfDay, endOfDay)
        saveSummaryToDb(summary, startOfDay)
    }

    override suspend fun syncDateRange(startMillis: Long, endMillis: Long) = withContext(Dispatchers.IO) {
        val summary = dataSource.getNetworkUsage(startMillis, endMillis)
        saveSummaryToDb(summary, startMillis)
    }

    private suspend fun saveSummaryToDb(summary: DailyDataUsageSummary, startOfDay: Long) {
        val dailyEntity = DailyDataUsageEntity(
            dateStartEpochMillis = startOfDay,
            totalWifiRxBytes = summary.totalWifiRxBytes,
            totalWifiTxBytes = summary.totalWifiTxBytes,
            totalWifiBytes = summary.totalWifiBytes,
            totalMobileRxBytes = summary.totalMobileRxBytes,
            totalMobileTxBytes = summary.totalMobileTxBytes,
            totalMobileBytes = summary.totalMobileBytes,
            totalHotspotBytes = summary.totalHotspotBytes,
            grandTotalBytes = summary.grandTotalBytes
        )

        val appEntities = summary.appUsageList.map { app ->
            AppDataUsageEntity(
                dateStartEpochMillis = startOfDay,
                packageName = app.packageName,
                appLabel = app.appLabel,
                wifiRxBytes = app.wifiRxBytes,
                wifiTxBytes = app.wifiTxBytes,
                mobileRxBytes = app.mobileRxBytes,
                mobileTxBytes = app.mobileTxBytes,
                isRemoved = app.isRemoved,
                isHotspot = app.isHotspot
            )
        }

        networkDao.saveFullDailyDataUsage(dailyEntity, appEntities)
    }

    private suspend fun enrichAndFilterSummary(
        summary: DailyDataUsageSummary,
        filter: DataFilter
    ): DailyDataUsageSummary {
        val overrides = appOverrideDao.getAllOverridesDirect().associateBy { it.packageName }

        // Filter network type
        var filteredList = summary.appUsageList.map { app ->
            val override = overrides[app.packageName]
            val category = override?.customCategory?.let {
                runCatching { io.chronicle.usagestats.domain.model.AppCategory.valueOf(it) }.getOrNull()
            } ?: app.category
            app.copy(
                category = category,
                isWifiPreferred = override?.isWifiPreferred ?: false
            )
        }.filter { app ->
            when (filter.networkType) {
                NetworkTypeFilter.ALL -> app.totalBytes > 0
                NetworkTypeFilter.MOBILE -> app.mobileTotalBytes > 0
                NetworkTypeFilter.WIFI -> app.wifiTotalBytes > 0
                NetworkTypeFilter.HOTSPOT -> app.isHotspot
            }
        }

        // Filter search query
        if (filter.searchQuery.isNotBlank()) {
            val query = filter.searchQuery.trim().lowercase()
            filteredList = filteredList.filter {
                it.appLabel.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }

        // Apply sorting
        filteredList = when (filter.sortOrder) {
            DataSortOrder.TOTAL_DESC -> filteredList.sortedByDescending { it.totalBytes }
            DataSortOrder.MOBILE_DESC -> filteredList.sortedByDescending { it.mobileTotalBytes }
            DataSortOrder.WIFI_DESC -> filteredList.sortedByDescending { it.wifiTotalBytes }
            DataSortOrder.NAME_ASC -> filteredList.sortedBy { it.appLabel.lowercase() }
        }

        val topWifi = summary.appUsageList.filter { it.wifiTotalBytes > 0 }.maxByOrNull { it.wifiTotalBytes }
        val topMobile = summary.appUsageList.filter { it.mobileTotalBytes > 0 }.maxByOrNull { it.mobileTotalBytes }

        return summary.copy(
            appUsageList = filteredList,
            topWifiApp = topWifi,
            topMobileApp = topMobile
        )
    }
}
