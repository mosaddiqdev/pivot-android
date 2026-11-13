@file:OptIn(ExperimentalMaterial3Api::class)

package app.pivot.orientation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pivot.orientation.data.PivotState
import app.pivot.orientation.engine.EngineSnapshot
import app.pivot.orientation.health.Health
import app.pivot.orientation.health.HealthCheck
import app.pivot.orientation.ui.components.Group
import app.pivot.orientation.ui.components.GroupDivider
import app.pivot.orientation.ui.components.Level
import app.pivot.orientation.ui.components.SectionHeader
import app.pivot.orientation.ui.components.StatusRow
import app.pivot.orientation.ui.components.SwitchRow

@Composable
fun SettingsScreen(
    state: PivotState,
    health: Health,
    engine: EngineSnapshot,
    padding: PaddingValues,
    onAllowOverlay: () -> Unit,
    onFixNotifications: () -> Unit,
    onBattery: () -> Unit,
    onAutostart: () -> Unit,
    onRestart: () -> Unit,
    onBoot: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val brand = HealthCheck.aggressiveBrand()
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                if (health.allGood) {
                    "Everything Pivot needs to keep working after you close the app is in place."
                } else {
                    "Fix the items below so rotation keeps working after you close the app."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        item { SectionHeader("Keep Pivot running") }
        item {
            Group {
                StatusRow(
                    level = if (health.overlayGranted) Level.Ok else Level.Warn,
                    title = "Display over other apps",
                    supporting = if (health.overlayGranted) {
                        "Allowed"
                    } else {
                        "Needed to change the screen direction. If the switch is greyed out, open App info, " +
                            "tap the three dots and choose Allow restricted settings."
                    },
                    actionLabel = if (health.overlayGranted) null else "Allow",
                    onAction = onAllowOverlay,
                )
                GroupDivider()
                StatusRow(
                    level = if (health.notificationsOk) Level.Ok else Level.Warn,
                    title = "Notifications",
                    supporting = when {
                        health.notificationsOk -> "The controls can show in the notification shade"
                        !health.notificationPermission -> "Allow notifications so the buttons can show."
                        health.channelBlocked -> "The Orientation controls category is turned off."
                        else -> "Notifications are turned off for Pivot."
                    },
                    actionLabel = if (health.notificationsOk) null else "Fix",
                    onAction = onFixNotifications,
                )
                GroupDivider()
                StatusRow(
                    level = if (health.batteryUnrestricted) Level.Ok else Level.Warn,
                    title = "Background use",
                    supporting = if (health.batteryUnrestricted) {
                        "Android will not put Pivot to sleep"
                    } else {
                        "Without this, many phones stop Pivot soon after you close the app."
                    },
                    actionLabel = if (health.batteryUnrestricted) null else "Allow",
                    onAction = onBattery,
                )
                if (brand != null) {
                    GroupDivider()
                    StatusRow(
                        level = Level.Info,
                        title = "$brand background settings",
                        supporting = "Turn on Autostart and set battery to No restrictions. " +
                            "If your phone can lock apps in Recents, lock Pivot.",
                        actionLabel = "Open",
                        onAction = onAutostart,
                    )
                }
                GroupDivider()
                when {
                    !health.serviceExpected -> StatusRow(
                        level = Level.Info,
                        title = "Pivot service",
                        supporting = "Idle. Nothing needs to run while Auto is on and the notification is off.",
                    )
                    health.serviceRunning -> StatusRow(
                        level = Level.Ok,
                        title = "Pivot service",
                        supporting = "Running",
                    )
                    else -> StatusRow(
                        level = Level.Warn,
                        title = "Pivot service",
                        supporting = "Not running. Android stopped it." +
                            (health.lastStartFailure?.let { " ($it)" } ?: ""),
                        actionLabel = "Start",
                        onAction = onRestart,
                    )
                }
            }
        }

        item { SectionHeader("General") }
        item {
            Group {
                SwitchRow(
                    title = "Start when the phone boots",
                    supporting = "Bring back your mode after a restart.",
                    checked = state.startOnBoot,
                    onChange = onBoot,
                )
                GroupDivider()
                SwitchRow(
                    title = "Haptic feedback",
                    supporting = "A small tick when you pick a mode.",
                    checked = state.haptics,
                    onChange = onHaptics,
                )
            }
        }

        item { SectionHeader("Diagnostics") }
        item {
            Group {
                ListItem(
                    headlineContent = { Text("Overlay window") },
                    supportingContent = { Text(if (engine.overlayAttached) "Attached" else "Not attached") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                GroupDivider()
                ListItem(
                    headlineContent = { Text("Motion sensor") },
                    supportingContent = {
                        Text(if (engine.sensorListening) "Listening, screen is on" else "Off")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        item { SectionHeader("About") }
        item {
            Group {
                ListItem(
                    headlineContent = { Text("Pivot") },
                    supportingContent = { Text(version?.let { "Version $it" } ?: "Orientation control") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                GroupDivider()
                ListItem(
                    headlineContent = { Text("Battery use") },
                    supportingContent = {
                        Text(
                            "No polling, no wake locks and no network. The motion sensor runs only for " +
                                "custom mixes Android cannot express, and only while the screen is on.",
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
