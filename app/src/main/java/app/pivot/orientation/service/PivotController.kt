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

    /** Re-applies the stored state. Starts the service only when something needs it. */
    fun sync(context: Context) {
        PivotStore.init(context)
        val s = PivotStore.state.value
        val needed = s.mode != Mode.AUTO || s.keepNotification || PivotService.running
        if (!needed) return
        val intent = Intent(context, PivotService::class.java).setAction(PivotService.ACTION_SYNC)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (t: Throwable) {
            // Background start can be refused by the system. The stored state is kept
            // and applied the next time the app or the notification is used.
            Log.w("PivotController", "Could not start service", t)
        }
    }
}
