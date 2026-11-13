package app.pivot.orientation.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.pivot.orientation.MainActivity
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.model.Mode

/** Quick settings tile. Each tap moves to the next of the user's three shortcut modes. */
class PivotTile : TileService() {

    override fun onStartListening() {
        PivotStore.init(this)
        // Opening the panel is a chance to bring back a service Android stopped.
        val s = PivotStore.state.value
        if (s.mode != Mode.AUTO && !PivotService.running) PivotController.sync(this)
        refresh()
    }

    override fun onClick() {
        PivotStore.init(this)
        if (!Settings.canDrawOverlays(this)) {
            openApp()
            return
        }
        val s = PivotStore.state.value
        val cycle = s.shortcuts
        val index = cycle.indexOf(s.mode)
        val next = cycle[(index + 1).mod(cycle.size)]
        PivotStore.update { it.copy(mode = next) }
        // If Android will not let a background tile start the service, open the app instead.
        // The app is allowed to start it and does so as soon as it is shown.
        if (!PivotController.sync(this)) openApp()
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val mode = PivotStore.state.value.mode
        val stalled = mode != Mode.AUTO && !PivotService.running
        tile.state = if (mode == Mode.AUTO) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.label = "Pivot"
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = if (stalled) "${mode.title} · tap to resume" else mode.title
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pending)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        /** Asks Android to refresh the tile, so it follows changes made from the app or notification. */
        fun requestRefresh(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, PivotTile::class.java))
            }
        }
    }
}
