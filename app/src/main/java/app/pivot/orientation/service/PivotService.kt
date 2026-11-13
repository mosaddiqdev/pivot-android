package app.pivot.orientation.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.ServiceCompat
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.engine.EngineStatus
import app.pivot.orientation.engine.OrientationEngine
import app.pivot.orientation.model.Mode
import app.pivot.orientation.notification.PivotNotifications

/**
 * Owns the overlay window and the control notification. It does nothing between
 * commands: no loops, no timers, no wake locks.
 */
class PivotService : Service() {

    private lateinit var engine: OrientationEngine

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        PivotStore.init(this)
        PivotNotifications.ensureChannel(this)
        engine = OrientationEngine(this)
        running = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A foreground service must call startForeground quickly, even if it is about to stop.
        enterForeground()

        when (intent?.action) {
            ACTION_SET -> Mode.fromId(intent?.getStringExtra(EXTRA_MODE))?.let { mode ->
                PivotStore.update { it.copy(mode = mode) }
            }
        }

        val state = PivotStore.state.value
        engine.apply(state)

        if (state.mode == Mode.AUTO && !state.keepNotification) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        enterForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        engine.release()
        EngineStatus.set(overlayAttached = false, sensorListening = false)
        running = false
        super.onDestroy()
    }

    private fun enterForeground() {
        val state = PivotStore.state.value
        val notification = PivotNotifications.build(this, state, Settings.canDrawOverlays(this))
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, PivotNotifications.NOTIFICATION_ID, notification, type)
    }

    companion object {
        const val ACTION_SET = "app.pivot.orientation.SET_MODE"
        const val ACTION_SYNC = "app.pivot.orientation.SYNC"
        const val EXTRA_MODE = "mode"

        @Volatile
        var running = false
            private set
    }
}
