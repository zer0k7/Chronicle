package io.chronicle.usagestats.data.datasource

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataUsageInfo
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

        // Data map keyed by package name or special ID
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
                        // System or unknown UID
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

        DailyDataUsageSummary(
            dateEpochMillis = startTimeMillis,
            totalWifiRxBytes = wifiTotalRx,
            totalWifiTxBytes = wifiTotalTx,
            totalMobileRxBytes = mobileTotalRx,
            totalMobileTxBytes = mobileTotalTx,
            totalHotspotBytes = hotspotRx + hotspotTx,
            appUsageList = sortedApps,
            topWifiApp = topWifi,
            topMobileApp = topMobile
        )
    }
}
