package io.chronicle.usagestats.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.chronicle.usagestats.MainActivity
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.data.local.dao.NetworkUsageDao
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DataBurnTileService : TileService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TileEntryPoint {
        fun networkUsageDao(): NetworkUsageDao
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        if (!PermissionHelper.hasUsageStatsPermission(this)) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.tile_data_burn_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.settings_permission_status_missing)
            }
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_data_burn)
            tile.updateTile()
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            TileEntryPoint::class.java
        )
        val networkUsageDao = entryPoint.networkUsageDao()
        val preferencesRepository = entryPoint.userPreferencesRepository()

        serviceScope.launch {
            try {
                val startOfDay = DateTimeUtils.getStartOfDay()
                val summary = networkUsageDao.getDailySummary(startOfDay).first()
                val settings = preferencesRepository.userSettingsFlow.first()

                val mobileBytes = summary?.totalMobileBytes ?: 0L
                val formattedMobile = DataSizeUtils.formatBytes(mobileBytes)
                val budgetBytes = settings.dailyDataBudgetMb * 1024L * 1024L
                val isAtRisk = budgetBytes > 0 && mobileBytes >= budgetBytes

                tile.state = if (isAtRisk) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                tile.label = formattedMobile

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (isAtRisk) {
                        getString(R.string.tile_data_burn_high, formattedMobile)
                    } else {
                        getString(R.string.tile_data_burn_safe, formattedMobile)
                    }
                }

                tile.icon = Icon.createWithResource(this@DataBurnTileService, R.drawable.ic_tile_data_burn)
                tile.updateTile()
            } catch (_: Exception) {
                // Silently handle background error
            }
        }
    }

    override fun onClick() {
        super.onClick()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_route", Screen.DataUsage.route)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
