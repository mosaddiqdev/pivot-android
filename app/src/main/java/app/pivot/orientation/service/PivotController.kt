package app.pivot.orientation.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.model.Mode

/** Single entry point used by the UI, the tile and the boot receiver. */
object PivotController {

    fun setMode(context: Context, mode: Mode) {
        PivotStore.init(context)
        PivotStore.update { it.copy(mode = mode) }
        sync(context)
    }

    /**
     * Re-applies the stored state. Starts the service only when something needs it.
     *
     * Returns false when Android refused to start the service (for example because the app
     * is in the background and has no start exemption). The stored state is kept, and the
     * caller can fall back to opening the app, which is always allowed to start it.
     */
    fun sync(context: Context): Boolean {
        PivotStore.init(context)
        val s = PivotStore.state.value
        val needed = s.mode != Mode.AUTO || s.keepNotification || PivotService.running
        if (!needed) return true
        val intent = Intent(context, PivotService::class.java).setAction(PivotService.ACTION_SYNC)
        return try {
            ContextCompat.startForegroundService(context, intent)
            PivotService.lastStartFailure = null
            true
        } catch (t: Exception) {
            Log.w("PivotController", "Could not start service", t)
            PivotService.lastStartFailure = t.javaClass.simpleName
            false
        }
    }
}
