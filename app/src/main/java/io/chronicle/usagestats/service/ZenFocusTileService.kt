package io.chronicle.usagestats.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.chronicle.usagestats.R

class ZenFocusTileService : TileService() {

    companion object {
        private const val PREFS_NAME = "chronicle_zen_tile_prefs"
        private const val KEY_FOCUS_END_TIME = "key_zen_focus_end_time"
        const val FOCUS_BLOCK_DURATION_MINUTES = 25

        fun isFocusActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val endTime = prefs.getLong(KEY_FOCUS_END_TIME, 0L)
            return endTime > System.currentTimeMillis()
        }

        fun getRemainingMinutes(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val endTime = prefs.getLong(KEY_FOCUS_END_TIME, 0L)
            val remainingMillis = endTime - System.currentTimeMillis()
            return if (remainingMillis > 0) ((remainingMillis / 60000L) + 1).toInt() else 0
        }

        fun toggleFocus(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isActive = isFocusActive(context)
            if (isActive) {
                prefs.edit().putLong(KEY_FOCUS_END_TIME, 0L).apply()
                return false
            } else {
                val endTime = System.currentTimeMillis() + (FOCUS_BLOCK_DURATION_MINUTES * 60 * 1000L)
                prefs.edit().putLong(KEY_FOCUS_END_TIME, endTime).apply()
                return true
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val active = isFocusActive(this)

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_zen_focus)
        tile.label = getString(R.string.tile_zen_focus_label)

        if (active) {
            tile.state = Tile.STATE_ACTIVE
            val remaining = getRemainingMinutes(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_zen_focus_active, remaining)
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_zen_focus_idle)
            }
        }

        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        toggleFocus(this)
        updateTileState()
    }
}
