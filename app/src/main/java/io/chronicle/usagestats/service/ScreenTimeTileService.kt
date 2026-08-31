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
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.domain.repository.UsageRepository
import io.chronicle.usagestats.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScreenTimeTileService : TileService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TileEntryPoint {
        fun usageRepository(): UsageRepository
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
            tile.label = getString(R.string.tile_screen_time_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.settings_permission_status_missing)
            }
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_screen_time)
            tile.updateTile()
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            TileEntryPoint::class.java
        )
        val usageRepository = entryPoint.usageRepository()
        val preferencesRepository = entryPoint.userPreferencesRepository()

        serviceScope.launch {
            try {
                val totalMillis = usageRepository.getTodayTotalScreenTimeMillis()
                val settings = preferencesRepository.userSettingsFlow.first()
                val goalMillis = settings.dailyGoalMinutes * 60 * 1000L
                val isOverBudget = goalMillis > 0 && totalMillis >= goalMillis

                val durationStr = DateTimeUtils.formatDuration(totalMillis)

                tile.state = if (isOverBudget) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                tile.label = durationStr

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (goalMillis > 0) {
                        val pct = ((totalMillis.toDouble() / goalMillis.toDouble()) * 100.0).toInt()
                        if (isOverBudget) {
                            getString(R.string.tile_screen_time_exceeded, durationStr)
                        } else {
                            getString(R.string.tile_screen_time_budget_pct, durationStr, pct)
                        }
                    } else {
                        getString(R.string.tile_screen_time_label)
                    }
                }

                tile.icon = Icon.createWithResource(this@ScreenTimeTileService, R.drawable.ic_tile_screen_time)
                tile.updateTile()
            } catch (_: Exception) {
                // Silently handle background read failure
            }
        }
    }

    override fun onClick() {
        super.onClick()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_route", Screen.Timeline.route)
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
