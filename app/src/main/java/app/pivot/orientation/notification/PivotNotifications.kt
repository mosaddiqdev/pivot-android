package app.pivot.orientation.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.pivot.orientation.MainActivity
import app.pivot.orientation.R
import app.pivot.orientation.data.PivotState
import app.pivot.orientation.model.Mode
import app.pivot.orientation.model.Side
import app.pivot.orientation.service.NotificationDismissReceiver
import app.pivot.orientation.service.PivotService

/**
 * Android allows three action buttons per notification, so the three slots are user
 * chosen. The notification body shows the live state and opens the app. The quick
 * settings tile cycles through the same three modes, which gives a fourth way in.
 */
object PivotNotifications {
    const val CHANNEL_ID = "pivot_controls"
    const val NOTIFICATION_ID = 4201

    fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.channel_name))
            .setDescription(context.getString(R.string.channel_desc))
            .setShowBadge(false)
            .setVibrationEnabled(false)
            .setLightsEnabled(false)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun build(context: Context, state: PivotState, overlayGranted: Boolean): Notification {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = "Rotation: ${state.mode.title}"
        val body = when {
            !overlayGranted -> "Permission needed. Tap to finish setup."
            state.mode == Mode.CUSTOM -> "Allowed: " + Side.fromMask(state.allowedMask).joinToString(", ") { it.label }
            else -> state.mode.hint
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pivot)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFF5F5F6.toInt())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setDeleteIntent(
                PendingIntent.getBroadcast(
                    context,
                    2,
                    Intent(context, NotificationDismissReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )

        state.shortcuts.forEach { mode ->
            val label = if (mode == state.mode) "${mode.title} ✓" else mode.title
            val intent = Intent(context, PivotService::class.java)
                .setAction(PivotService.ACTION_SET)
                .putExtra(PivotService.EXTRA_MODE, mode.id)
            // getForegroundService, not getService: the button must work even when the
            // process was stopped and Android has to start the service again.
            val pending = PendingIntent.getForegroundService(
                context,
                100 + mode.ordinal,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, label, pending)
        }
        return builder.build()
    }

    /** Updates (or puts back) the notification of the running service. */
    fun post(context: Context, state: PivotState, overlayGranted: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, build(context, state, overlayGranted))
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }
}
