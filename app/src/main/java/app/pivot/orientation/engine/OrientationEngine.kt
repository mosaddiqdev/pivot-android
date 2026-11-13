package app.pivot.orientation.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import app.pivot.orientation.data.PivotState
import app.pivot.orientation.model.OrientationPlanner
import app.pivot.orientation.model.Plan
import app.pivot.orientation.model.Side
import app.pivot.orientation.model.angularDistance
import kotlin.math.abs

/**
 * Forces the screen orientation system wide with a single invisible 1px overlay window
 * whose screenOrientation attribute the window manager obeys.
 *
 * Battery rules this class follows:
 * 1. Fixed modes are one window attribute. No sensor, no timers, no wakeups.
 * 2. Only combinations the system cannot express (for example portrait plus landscape
 *    but not reverse) use the motion sensor.
 * 3. That sensor is registered only while the screen is on.
 */
class OrientationEngine(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())

    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var listener: OrientationEventListener? = null
    private var sensorOn = false
    private var screenReceiver: BroadcastReceiver? = null

    private var allowedMask = 0
    private var current: Side? = null
    private var pending: Side? = null

    private val commit = Runnable {
        val next = pending ?: return@Runnable
        pending = null
        if (next != current) {
            current = next
            show(next.constant)
        }
    }

    fun apply(state: PivotState) {
        if (!Settings.canDrawOverlays(context)) {
            teardown()
            return
        }
        when (val plan = OrientationPlanner.plan(state.mode, state.allowedMask)) {
            Plan.Release -> teardown()
            is Plan.Fixed -> {
                stopSensing()
                current = null
                show(plan.constant)
            }
            is Plan.Adaptive -> startSensing(plan.allowed)
        }
        publish()
    }

    fun release() {
        teardown()
    }

    // Overlay -----------------------------------------------------------------------------

    private fun show(constant: Int) {
        val existing = view
        val p = params
        if (existing != null && p != null) {
            if (p.screenOrientation != constant) {
                p.screenOrientation = constant
                runCatching { windowManager.updateViewLayout(existing, p) }
                    .onFailure { Log.w(TAG, "updateViewLayout failed", it) }
            }
            publish()
            return
        }
        val v = View(context)
        val lp = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            screenOrientation = constant
        }
        runCatching { windowManager.addView(v, lp) }
            .onSuccess {
                view = v
                params = lp
            }
            .onFailure { Log.w(TAG, "addView failed", it) }
        publish()
    }

    private fun removeOverlay() {
        view?.let { v -> runCatching { windowManager.removeViewImmediate(v) } }
        view = null
        params = null
    }

    // Adaptive sensing --------------------------------------------------------------------

    private fun startSensing(mask: Int) {
        allowedMask = mask
        val start = nearestAllowed(displaySide(), mask)
        if (current != start || view == null) {
            current = start
            show(start.constant)
        }
        registerScreenReceiver()
        if (isInteractive()) enableSensor()
    }

    private fun stopSensing() {
        disableSensor()
        unregisterScreenReceiver()
    }

    private fun enableSensor() {
        if (sensorOn) return
        val l = listener ?: object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) = onDegrees(orientation)
        }.also { listener = it }
        if (l.canDetectOrientation()) {
            l.enable()
            sensorOn = true
        }
        publish()
    }

    private fun disableSensor() {
        listener?.disable()
        sensorOn = false
        pending = null
        main.removeCallbacks(commit)
        publish()
    }

    private fun onDegrees(degrees: Int) {
        if (degrees == OrientationEventListener.ORIENTATION_UNKNOWN) return
        val axis = ((degrees + 45) / 90) % 4
        val offset = abs(((degrees - axis * 90 + 540) % 360) - 180)
        // Dead zone around the diagonals stops flicker while the phone is held at an angle.
        if (offset > 35) return
        val candidate = when (axis) {
            0 -> Side.PORTRAIT
            1 -> Side.REVERSE_LANDSCAPE
            2 -> Side.REVERSE_PORTRAIT
            else -> Side.LANDSCAPE
        }
        if ((allowedMask and candidate.bit) == 0 || candidate == current) {
            main.removeCallbacks(commit)
            pending = null
            return
        }
        if (pending != candidate) {
            pending = candidate
            main.removeCallbacks(commit)
            main.postDelayed(commit, SETTLE_MS)
        }
    }

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> enableSensor()
                    Intent.ACTION_SCREEN_OFF -> disableSensor()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(context, r, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        screenReceiver = r
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        screenReceiver = null
    }

    // Helpers -----------------------------------------------------------------------------

    private fun teardown() {
        stopSensing()
        removeOverlay()
        current = null
        publish()
    }

    private fun publish() {
        EngineStatus.set(overlayAttached = view != null, sensorListening = sensorOn)
    }

    private fun isInteractive(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive

    private fun displaySide(): Side {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return when (dm.getDisplay(Display.DEFAULT_DISPLAY)?.rotation) {
            Surface.ROTATION_90 -> Side.LANDSCAPE
            Surface.ROTATION_180 -> Side.REVERSE_PORTRAIT
            Surface.ROTATION_270 -> Side.REVERSE_LANDSCAPE
            else -> Side.PORTRAIT
        }
    }

    private fun nearestAllowed(from: Side, mask: Int): Side {
        if ((mask and from.bit) != 0) return from
        return Side.fromMask(mask).minByOrNull { angularDistance(it.angle, from.angle) } ?: Side.PORTRAIT
    }

    private companion object {
        const val TAG = "PivotEngine"
        const val SETTLE_MS = 300L
    }
}
