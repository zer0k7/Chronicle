package io.chronicle.usagestats.service

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService

object ChronicleTileUpdater {

    fun updateAll(context: Context) {
        updateScreenTimeTile(context)
        updateZenFocusTile(context)
        updateDataBurnTile(context)
    }

    fun updateScreenTimeTile(context: Context) {
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, ScreenTimeTileService::class.java)
            )
        } catch (_: Exception) { }
    }

    fun updateZenFocusTile(context: Context) {
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, ZenFocusTileService::class.java)
            )
        } catch (_: Exception) { }
    }

    fun updateDataBurnTile(context: Context) {
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, DataBurnTileService::class.java)
            )
        } catch (_: Exception) { }
    }
}
