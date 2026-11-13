package app.pivot.orientation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.model.Mode

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != ACTION_QUICKBOOT
        ) return
        PivotStore.init(context)
        val s = PivotStore.state.value
        if (s.startOnBoot && (s.mode != Mode.AUTO || s.keepNotification)) {
            PivotController.sync(context)
        }
    }

    private companion object {
        /** Sent by some phones after a fast boot instead of BOOT_COMPLETED. */
        const val ACTION_QUICKBOOT = "android.intent.action.QUICKBOOT_POWERON"
    }
}
