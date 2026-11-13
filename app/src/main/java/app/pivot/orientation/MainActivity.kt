package app.pivot.orientation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.pivot.orientation.service.PivotController
import app.pivot.orientation.ui.HomeScreen
import app.pivot.orientation.ui.theme.PivotTheme

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        // An app in the foreground is always allowed to start the service, so opening Pivot
        // brings back a service Android stopped (killed by the phone, or stopped by the user).
        PivotController.sync(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            PivotTheme {
                HomeScreen()
            }
        }
    }
}
