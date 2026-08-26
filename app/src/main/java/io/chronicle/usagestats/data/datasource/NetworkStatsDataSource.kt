package io.chronicle.usagestats.data.datasource

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.CategoryDataShare
import io.chronicle.usagestats.domain.model.DailyDataPoint
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataUsageInfo
import io.chronicle.usagestats.domain.model.HourlyDataPoint
import io.chronicle.usagestats.domain.model.SleepDataLeakInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val networkStatsManager by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }

    private val packageManager: PackageManager by lazy {
        context.packageManager
    }

    suspend fun getNetworkUsage(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): DailyDataUsageSummary = withContext(Dispatchers.IO) {
        val manager = networkStatsManager
        if (manager == null || !PermissionHelper.hasUsageStatsPermission(context)) {
            return@withContext DailyDataUsageSummary(dateEpochMillis = startTimeMillis)
        }

        var wifiTotalRx = 0L
        var wifiTotalTx = 0L
        var mobileTotalRx = 0L
        var mobileTotalTx = 0L
        var hotspotRx = 0L
        var hotspotTx = 0L

        // 1. Device summaries
        try {
            val wifiSummary = manager.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTimeMillis,
                endTimeMillis
            )
            wifiTotalRx = wifiSummary.rxBytes
            wifiTotalTx = wifiSummary.txBytes
        } catch (_: Exception) { }

        try {
            val mobileSummary = manager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTimeMillis,
                endTimeMillis
            )
            mobileTotalRx = mobileSummary.rxBytes
            mobileTotalTx = mobileSummary.txBytes
        } catch (_: Exception) { }

        // Data map keyed by UID
        data class UidData(
            var wifiRx: Long = 0L,
            var wifiTx: Long = 0L,
            var mobileRx: Long = 0L,
            var mobileTx: Long = 0L
        )

        val uidMap = mutableMapOf<Int, UidData>()

        // 2. Query per-UID Wi-Fi usage
        try {
            val wifiStats = manager.querySummary(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTimeMillis,
                endTimeMillis
            )
            val bucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                val uid = bucket.uid
                val entry = uidMap.getOrPut(uid) { UidData() }
                entry.wifiRx += bucket.rxBytes
                entry.wifiTx += bucket.txBytes
            }
            wifiStats.close()
        } catch (_: Exception) { }

        // 3. Query per-UID Mobile usage
        try {
            val mobileStats = manager.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTimeMillis,
                endTimeMillis
            )
            val bucket = NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                val uid = bucket.uid
                val entry = uidMap.getOrPut(uid) { UidData() }
                entry.mobileRx += bucket.rxBytes
                entry.mobileTx += bucket.txBytes
            }
            mobileStats.close()
        } catch (_: Exception) { }

        // 4. Convert UIDs to App records
        val appUsageList = mutableListOf<DataUsageInfo>()

        for ((uid, data) in uidMap) {
            val totalBytes = data.wifiRx + data.wifiTx + data.mobileRx + data.mobileTx
            if (totalBytes <= 0) continue

            when (uid) {
                NetworkStats.Bucket.UID_TETHERING -> {
                    hotspotRx += data.mobileRx + data.wifiRx
                    hotspotTx += data.mobileTx + data.wifiTx
                    appUsageList.add(
                        DataUsageInfo(
                            packageName = "system.tethering.hotspot",
                            appLabel = "Mobile Hotspot & Tethering",
                            wifiRxBytes = data.wifiRx,
                            wifiTxBytes = data.wifiTx,
                            mobileRxBytes = data.mobileRx,
                            mobileTxBytes = data.mobileTx,
                            isRemoved = false,
                            isHotspot = true,
                            category = AppCategory.SYSTEM
                        )
                    )
                }
                NetworkStats.Bucket.UID_REMOVED -> {
                    appUsageList.add(
                        DataUsageInfo(
                            packageName = "system.uid.removed",
                            appLabel = "Removed Apps",
                            wifiRxBytes = data.wifiRx,
                            wifiTxBytes = data.wifiTx,
                            mobileRxBytes = data.mobileRx,
                            mobileTxBytes = data.mobileTx,
                            isRemoved = true,
                            isHotspot = false,
                            category = AppCategory.REMOVED
                        )
                    )
                }
                else -> {
                    val packages = try {
                        packageManager.getPackagesForUid(uid)
                    } catch (_: Exception) {
                        null
                    }

                    if (!packages.isNullOrEmpty()) {
                        val pkgName = packages[0]
                        val appLabel = try {
                            val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            pkgName
                        }

                        appUsageList.add(
                            DataUsageInfo(
                                packageName = pkgName,
                                appLabel = appLabel,
                                wifiRxBytes = data.wifiRx,
                                wifiTxBytes = data.wifiTx,
                                mobileRxBytes = data.mobileRx,
                                mobileTxBytes = data.mobileTx,
                                isRemoved = false,
                                isHotspot = false,
                                category = AppCategory.OTHER
                            )
                        )
                    } else {
                        appUsageList.add(
                            DataUsageInfo(
                                packageName = "system.uid.$uid",
                                appLabel = "Android System ($uid)",
                                wifiRxBytes = data.wifiRx,
                                wifiTxBytes = data.wifiTx,
                                mobileRxBytes = data.mobileRx,
                                mobileTxBytes = data.mobileTx,
                                isRemoved = false,
                                isHotspot = false,
                                category = AppCategory.SYSTEM
                            )
                        )
                    }
                }
            }
        }

        // Sort descending by total bytes
        val sortedApps = appUsageList.sortedByDescending { it.totalBytes }
        val topWifi = sortedApps.filter { it.wifiTotalBytes > 0 }.maxByOrNull { it.wifiTotalBytes }
        val topMobile = sortedApps.filter { it.mobileTotalBytes > 0 }.maxByOrNull { it.mobileTotalBytes }

        // 5. 24 Hourly data buckets (for Day view)
        val hourlyDataPoints = mutableListOf<HourlyDataPoint>()
        for (h in 0..23) {
            val hStart = startTimeMillis + (h * 3600000L)
            val hEnd = hStart + 3600000L
            var hWifi = 0L
            var hMobile = 0L
            try {
                val wSummary = manager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, hStart, hEnd)
                hWifi = wSummary.rxBytes + wSummary.txBytes
            } catch (_: Exception) { }
            try {
                val mSummary = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, hStart, hEnd)
                hMobile = mSummary.rxBytes + mSummary.txBytes
            } catch (_: Exception) { }
            hourlyDataPoints.add(HourlyDataPoint(hour = h, wifiBytes = hWifi, mobileBytes = hMobile))
        }

        // 6. Multi-day data points (for Week & Month views)
        val multiDayPoints = mutableListOf<DailyDataPoint>()
        val totalDays = ((endTimeMillis - startTimeMillis) / 86400000L).toInt().coerceIn(1, 31)
        if (totalDays > 1) {
            for (d in 0 until totalDays) {
                val dStart = startTimeMillis + (d * 86400000L)
                val dEnd = dStart + 86400000L
                var dWifi = 0L
                var dMobile = 0L
                try {
                    val wSummary = manager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, dStart, dEnd)
                    dWifi = wSummary.rxBytes + wSummary.txBytes
                } catch (_: Exception) { }
                try {
                    val mSummary = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, dStart, dEnd)
                    dMobile = mSummary.rxBytes + mSummary.txBytes
                } catch (_: Exception) { }
                val label = DateTimeUtils.formatDate(dStart)
                multiDayPoints.add(DailyDataPoint(dateEpochMillis = dStart, dayLabel = label, wifiBytes = dWifi, mobileBytes = dMobile))
            }
        }

        // 7. Category Distribution
        val grandTotal = (wifiTotalRx + wifiTotalTx) + (mobileTotalRx + mobileTotalTx)
        val categoryShares = sortedApps.groupBy { it.category }.map { (cat, list) ->
            val catTotal = list.sumOf { it.totalBytes }
            val pct = if (grandTotal > 0) (catTotal.toFloat() / grandTotal.toFloat()) * 100f else 0f
            CategoryDataShare(category = cat, totalBytes = catTotal, percentage = pct)
        }.sortedByDescending { it.totalBytes }

        // 8. Sleep Window Data Leak Detector (00:00 to 07:00 IST)
        val sleepDataLeaks = mutableListOf<SleepDataLeakInfo>()
        var totalSleepBytes = 0L
        try {
            val sleepStart = startTimeMillis
            val sleepEnd = startTimeMillis + (7 * 3600000L)
            val sleepWifi = manager.querySummary(ConnectivityManager.TYPE_WIFI, null, sleepStart, sleepEnd)
            val sleepMobile = manager.querySummary(ConnectivityManager.TYPE_MOBILE, null, sleepStart, sleepEnd)
            val sleepMap = mutableMapOf<Int, Long>()
            val b = NetworkStats.Bucket()

            while (sleepWifi.hasNextBucket()) {
                sleepWifi.getNextBucket(b)
                sleepMap[b.uid] = (sleepMap[b.uid] ?: 0L) + b.rxBytes + b.txBytes
            }
            sleepWifi.close()

            while (sleepMobile.hasNextBucket()) {
                sleepMobile.getNextBucket(b)
                sleepMap[b.uid] = (sleepMap[b.uid] ?: 0L) + b.rxBytes + b.txBytes
            }
            sleepMobile.close()

            totalSleepBytes = sleepMap.values.sum()

            if (totalSleepBytes > 0) {
                for (app in sortedApps) {
                    if (app.isHotspot) continue
                    val appSleepBytes = (app.totalBytes * (totalSleepBytes.toFloat() / grandTotal.coerceAtLeast(1L).toFloat())).toLong()
                    if (appSleepBytes >= (512 * 1024L)) { // at least 512 KB
                        val nightPct = (appSleepBytes.toFloat() / totalSleepBytes.toFloat()) * 100f
                        sleepDataLeaks.add(
                            SleepDataLeakInfo(
                                app = app,
                                sleepBytes = appSleepBytes,
                                percentageOfNight = nightPct
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) { }

        // 9. Data Depletion Forecast
        val now = System.currentTimeMillis()
        val elapsedHours = maxOf(1f, (now - startTimeMillis).toFloat() / 3600000f)
        val mobileUsed = mobileTotalRx + mobileTotalTx
        val burnRatePerHour = (mobileUsed.toFloat() / elapsedHours).toLong()
        val budgetBytes = 2048 * 1024L * 1024L // 2.0 GB default daily reference
        val remainingBytes = budgetBytes - mobileUsed
        val projectedDepletionMillis = if (burnRatePerHour > 0 && remainingBytes > 0) {
            now + ((remainingBytes.toFloat() / burnRatePerHour.toFloat()) * 3600000L).toLong()
        } else null
        val isAtRisk = burnRatePerHour > (budgetBytes / 14f)
        val pctBurned = if (budgetBytes > 0) ((mobileUsed.toFloat() / budgetBytes.toFloat()) * 100).toInt() else 0
        val statusMsg = if (isAtRisk) {
            "High cellular burn rate. Daily carrier quota projected to exhaust before evening."
        } else {
            "Balanced mobile telemetry pacing. Safe for continuous daily usage."
        }
        val depletionForecast = io.chronicle.usagestats.domain.model.DataDepletionForecast(
            burnRateBytesPerHour = burnRatePerHour,
            projectedDepletionEpochMillis = projectedDepletionMillis,
            isAtRiskOfDepletion = isAtRisk,
            percentOfQuotaBurned = pctBurned,
            statusMessage = statusMsg
        )

        DailyDataUsageSummary(
            dateEpochMillis = startTimeMillis,
            totalWifiRxBytes = wifiTotalRx,
            totalWifiTxBytes = wifiTotalTx,
            totalWifiBytes = totalWifiRxBytes + totalWifiTxBytes,
            totalMobileRxBytes = mobileTotalRx,
            totalMobileTxBytes = mobileTotalTx,
            totalMobileBytes = totalMobileRxBytes + totalMobileTxBytes,
            totalHotspotBytes = hotspotRx + hotspotTx,
            grandTotalBytes = (wifiTotalRx + wifiTotalTx) + (mobileTotalRx + mobileTotalTx),
            appUsageList = sortedApps,
            hourlyDataPoints = hourlyDataPoints,
            multiDayDataPoints = multiDayPoints,
            categoryShares = categoryShares,
            sleepDataLeaks = sleepDataLeaks.sortedByDescending { it.sleepBytes },
            depletionForecast = depletionForecast,
            totalSleepBytes = totalSleepBytes,
            topWifiApp = topWifi,
            topMobileApp = topMobile
        )
    }
}
