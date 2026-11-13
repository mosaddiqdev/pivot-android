package app.pivot.orientation.health

import android.Manifest
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.model.Mode
import app.pivot.orientation.notification.PivotNotifications
import app.pivot.orientation.service.PivotService

/** Everything that decides whether Pivot keeps working once the app is closed. */
data class Health(
    val overlayGranted: Boolean,
    val notificationPermission: Boolean,
    val notificationsEnabled: Boolean,
    val channelBlocked: Boolean,
    val batteryUnrestricted: Boolean,
    val serviceRunning: Boolean,
    val serviceExpected: Boolean,
    val stoppedByUser: Boolean,
    val lastStartFailure: String?,
) {
    val notificationsOk: Boolean get() = notificationPermission && notificationsEnabled && !channelBlocked
    val serviceStalled: Boolean get() = serviceExpected && !serviceRunning
    val allGood: Boolean get() = overlayGranted && notificationsOk && batteryUnrestricted && !serviceStalled
}

object HealthCheck {
    private const val PREFS = "pivot_health"
    private const val KEY_ACK_EXIT = "ack_exit_time"

    fun read(context: Context): Health {
        PivotStore.init(context)
        val s = PivotStore.state.value
        val manager = NotificationManagerCompat.from(context)
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val permission = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val channel = manager.getNotificationChannelCompat(PivotNotifications.CHANNEL_ID)
        return Health(
            overlayGranted = Settings.canDrawOverlays(context),
            notificationPermission = permission,
            notificationsEnabled = manager.areNotificationsEnabled(),
            channelBlocked = channel?.importance == NotificationManagerCompat.IMPORTANCE_NONE,
            batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName),
            serviceRunning = PivotService.running,
            serviceExpected = s.mode != Mode.AUTO || s.keepNotification,
            stoppedByUser = wasStoppedByUser(context),
            lastStartFailure = PivotService.lastStartFailure,
        )
    }

    /**
     * Android 13+ lets people stop an app from the Active apps list in the notification
     * shade, which kills the whole process without any callback. The only trace is the
     * process exit history, read here.
     */
    private fun wasStoppedByUser(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 30) return false
        return runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val last = am.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()
            val seen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_ACK_EXIT, 0L)
            last != null && last.reason == ApplicationExitInfo.REASON_USER_REQUESTED && last.timestamp > seen
        }.getOrDefault(false)
    }

    fun acknowledgeUserStop(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_ACK_EXIT, System.currentTimeMillis()).apply()
    }

    // Fixes ---------------------------------------------------------------------------------

    /**
     * Android lists "the user turned off battery optimization for the app" as an exemption
     * that lets a foreground service start from the background, and it is what keeps most
     * phones from stopping Pivot after the app is closed.
     */
    fun requestBatteryExemption(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        if (!launch(context, direct)) {
            if (!launch(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) openAppInfo(context)
        }
    }

    fun openNotificationSettings(context: Context, channelBlocked: Boolean) {
        val intent = if (channelBlocked) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, PivotNotifications.CHANNEL_ID)
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        if (!launch(context, intent)) openAppInfo(context)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        if (!launch(context, intent)) openAppInfo(context)
    }

    fun openAppInfo(context: Context) {
        launch(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }

    /** Brand specific "autostart" or "background activity" screen, or app info when none opens. */
    fun openAutostart(context: Context) {
        val opened = autostartScreens().any { launch(context, Intent().setComponent(it)) }
        if (!opened) openAppInfo(context)
    }

    /** Brand name when that brand is known to stop background apps aggressively. */
    fun aggressiveBrand(): String? {
        val maker = Build.MANUFACTURER.lowercase()
        return when {
            "xiaomi" in maker || "redmi" in maker || "poco" in maker -> "Xiaomi"
            "oppo" in maker -> "Oppo"
            "realme" in maker -> "Realme"
            "vivo" in maker || "iqoo" in maker -> "Vivo"
            "oneplus" in maker -> "OnePlus"
            "huawei" in maker || "honor" in maker -> "Huawei"
            "samsung" in maker -> "Samsung"
            else -> null
        }
    }

    // These screens are not part of Android. They belong to each phone maker, so they can
    // move between versions; every one is tried and app info is the fallback.
    private fun autostartScreens(): List<ComponentName> = when (aggressiveBrand()) {
        "Xiaomi" -> listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        "Oppo", "Realme" -> listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        )
        "Vivo" -> listOf(
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        )
        "OnePlus" -> listOf(
            ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
        )
        "Huawei" -> listOf(
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        )
        "Samsung" -> listOf(
            ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
        )
        else -> emptyList()
    }

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
