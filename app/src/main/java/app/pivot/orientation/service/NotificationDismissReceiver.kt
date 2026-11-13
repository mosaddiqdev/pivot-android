package app.pivot.orientation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.notification.PivotNotifications

/**
 * Android 14 and newer let people swipe away a foreground service notification even when
 * it is marked ongoing. The service keeps running, but the buttons vanish and it looks
 * like Pivot stopped. When the user asked to keep the controls, put the notification back.
 */
class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PivotStore.init(context)
        val state = PivotStore.state.value
        if (PivotService.running && state.keepNotification) {
            PivotNotifications.post(context, state, Settings.canDrawOverlays(context))
        }
    }
}
