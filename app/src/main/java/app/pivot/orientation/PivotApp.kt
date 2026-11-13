package app.pivot.orientation

import android.app.Application
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.notification.PivotNotifications

class PivotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PivotStore.init(this)
        PivotNotifications.ensureChannel(this)
    }
}
