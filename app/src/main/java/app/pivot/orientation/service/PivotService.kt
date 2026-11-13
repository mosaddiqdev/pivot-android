package app.pivot.orientation.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.ServiceCompat
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.engine.EngineStatus
import app.pivot.orientation.engine.OrientationEngine
import app.pivot.orientation.model.Mode
import app.pivot.orientation.notification.PivotNotifications

/**
 * Owns the overlay window and the control notification. It does nothing between
 * commands: no loops, no timers, no wake locks. It reacts only when the stored state
 * changes, so the app, the quick settings tile and the notification buttons all end up
 * at the same place no matter which one made the change.
 */
class PivotService : Service() {

    private lateinit var engine: OrientationEngine
    private val main = Handler(Looper.getMainLooper())
    private var stopObserving: (() -> Unit)? = null
    private var destroyed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        PivotStore.init(this)
        PivotNotifications.ensureChannel(this)
        engine = OrientationEngine(this)
        running = true
        stopObserving = PivotStore.observe { main.post { if (!destroyed) applyState() } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every startForegroundService call must be answered with startForeground quickly,
        // even if the service is about to stop. A null intent means Android restarted us.
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_SET) {
            Mode.fromId(intent.getStringExtra(EXTRA_MODE))?.let { mode ->
                PivotStore.update { it.copy(mode = mode) }
            }
        }
        return if (applyState()) START_STICKY else START_NOT_STICKY
    }

    /** Some phones remove or hide the notification when the app is swiped from recents. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!destroyed) PivotNotifications.post(this, PivotStore.state.value, Settings.canDrawOverlays(this))
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        destroyed = true
        stopObserving?.invoke()
        main.removeCallbacksAndMessages(null)
        engine.release()
        EngineStatus.set(overlayAttached = false, sensorListening = false)
        running = false
        PivotTile.requestRefresh(this)
        super.onDestroy()
    }

    /** Applies the stored state. Returns false when the service has no reason to run. */
    private fun applyState(): Boolean {
        val state = PivotStore.state.value
        engine.apply(state)

        if (state.mode == Mode.AUTO && !state.keepNotification) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return false
        }

        PivotNotifications.post(this, state, Settings.canDrawOverlays(this))
        PivotTile.requestRefresh(this)
        return true
    }

    private fun enterForeground(): Boolean {
        val state = PivotStore.state.value
        val notification = PivotNotifications.build(this, state, Settings.canDrawOverlays(this))
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        return try {
            ServiceCompat.startForeground(this, PivotNotifications.NOTIFICATION_ID, notification, type)
            true
        } catch (t: Exception) {
            // Android 12+ throws when a foreground service is started from the background
            // without an exemption. Stop cleanly instead of crashing; opening the app starts it.
            Log.w(TAG, "startForeground refused", t)
            lastStartFailure = t.javaClass.simpleName
            false
        }
    }

    companion object {
        private const val TAG = "PivotService"
        const val ACTION_SET = "app.pivot.orientation.SET_MODE"
        const val ACTION_SYNC = "app.pivot.orientation.SYNC"
        const val EXTRA_MODE = "mode"

        @Volatile
        var running = false
            private set

        /** Name of the last exception that kept the service from starting, shown in the app. */
        @Volatile
        var lastStartFailure: String? = null
            internal set
    }
}
