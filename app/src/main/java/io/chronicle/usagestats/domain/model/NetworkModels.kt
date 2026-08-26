package io.chronicle.usagestats.domain.model

enum class NetworkTypeFilter {
    ALL,
    MOBILE,
    WIFI,
    HOTSPOT
}

enum class DataSortOrder {
    TOTAL_DESC,
    MOBILE_DESC,
    WIFI_DESC,
    NAME_ASC
}

enum class DataPeriod {
    DAY,
    WEEK,
    MONTH,
    BILLING_CYCLE
}

data class DataUsageInfo(
    val packageName: String,
    val appLabel: String,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val wifiTotalBytes: Long = wifiRxBytes + wifiTxBytes,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val mobileTotalBytes: Long = mobileRxBytes + mobileTxBytes,
    val totalBytes: Long = wifiTotalBytes + mobileTotalBytes,
    val isRemoved: Boolean = false,
    val isHotspot: Boolean = false,
    val isWifiPreferred: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)

data class HourlyDataPoint(
    val hour: Int, // 0..23
    val wifiBytes: Long = 0L,
    val mobileBytes: Long = 0L,
    val totalBytes: Long = wifiBytes + mobileBytes
)

data class DailyDataPoint(
    val dateEpochMillis: Long,
    val dayLabel: String, // e.g. "Mon 24" or "24 Aug"
    val wifiBytes: Long = 0L,
    val mobileBytes: Long = 0L,
    val totalBytes: Long = wifiBytes + mobileBytes
)

data class CategoryDataShare(
    val category: AppCategory,
    val totalBytes: Long,
    val percentage: Float
)

data class SleepDataLeakInfo(
    val app: DataUsageInfo,
    val sleepBytes: Long,
    val percentageOfNight: Float
)

data class DataDepletionForecast(
    val burnRateBytesPerHour: Long,
    val projectedDepletionEpochMillis: Long?,
    val isAtRiskOfDepletion: Boolean,
    val percentOfQuotaBurned: Int,
    val statusMessage: String
)

data class DailyDataUsageSummary(
    val dateEpochMillis: Long,
    val totalWifiRxBytes: Long = 0L,
    val totalWifiTxBytes: Long = 0L,
    val totalWifiBytes: Long = totalWifiRxBytes + totalWifiTxBytes,
    val totalMobileRxBytes: Long = 0L,
    val totalMobileTxBytes: Long = 0L,
    val totalMobileBytes: Long = totalMobileRxBytes + totalMobileTxBytes,
    val totalHotspotBytes: Long = 0L,
    val grandTotalBytes: Long = totalWifiBytes + totalMobileBytes,
    val appUsageList: List<DataUsageInfo> = emptyList(),
    val hourlyDataPoints: List<HourlyDataPoint> = emptyList(),
    val multiDayDataPoints: List<DailyDataPoint> = emptyList(),
    val categoryShares: List<CategoryDataShare> = emptyList(),
    val sleepDataLeaks: List<SleepDataLeakInfo> = emptyList(),
    val depletionForecast: DataDepletionForecast? = null,
    val totalSleepBytes: Long = 0L,
    val topWifiApp: DataUsageInfo? = null,
    val topMobileApp: DataUsageInfo? = null
)

data class DataFilter(
    val searchQuery: String = "",
    val networkType: NetworkTypeFilter = NetworkTypeFilter.ALL,
    val sortOrder: DataSortOrder = DataSortOrder.TOTAL_DESC,
    val selectedHour: Int? = null
)
