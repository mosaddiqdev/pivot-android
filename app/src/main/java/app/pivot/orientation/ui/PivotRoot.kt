@file:OptIn(ExperimentalMaterial3Api::class)

package app.pivot.orientation.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pivot.orientation.data.PivotState
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.engine.EngineStatus
import app.pivot.orientation.health.HealthCheck
import app.pivot.orientation.model.Mode
import app.pivot.orientation.model.Side
import app.pivot.orientation.service.PivotController
import app.pivot.orientation.ui.screens.HomeScreen
import app.pivot.orientation.ui.screens.SettingsScreen
import app.pivot.orientation.ui.screens.ShortcutsScreen
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val title: String, val icon: ImageVector) {
    Home("Home", "Pivot", Icons.Rounded.Home),
    Shortcuts("Shortcuts", "Shortcuts", Icons.Rounded.Notifications),
    Settings("Settings", "Settings", Icons.Rounded.Settings),
}

/** App frame: large collapsing top bar, bottom navigation, and one screen per tab. */
@Composable
fun PivotRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }

    val state by PivotStore.state.collectAsStateWithLifecycle()
    val engine by EngineStatus.state.collectAsStateWithLifecycle()

    var health by remember { mutableStateOf(HealthCheck.read(context)) }
    val refreshHealth = { health = HealthCheck.read(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshHealth() }
    LaunchedEffect(state, engine) { refreshHealth() }

    var tabIndex by rememberSaveable { mutableStateOf(0) }
    BackHandler(enabled = tabIndex != 0) { tabIndex = 0 }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshHealth()
        if (granted) {
            PivotController.sync(context)
        } else {
            scope.launch {
                val result = snackbar.showSnackbar(
                    message = "Notifications are off, so the buttons cannot show",
                    actionLabel = "Settings",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) HealthCheck.openNotificationSettings(context, false)
            }
        }
    }

    fun update(block: (PivotState) -> PivotState) {
        PivotStore.update(block)
        PivotController.sync(context)
    }

    val onMode: (Mode) -> Unit = { mode ->
        if (state.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        PivotController.setMode(context, mode)
        if (mode != Mode.AUTO && !health.overlayGranted) {
            scope.launch {
                val result = snackbar.showSnackbar(
                    message = "Allow display over other apps to change the screen direction",
                    actionLabel = "Allow",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) HealthCheck.openOverlaySettings(context)
            }
        }
    }

    val onToggleSide: (Side) -> Unit = { side ->
        val next = state.allowedMask xor side.bit
        // At least one direction must stay allowed.
        if ((next and Side.ALL) != 0) update { it.copy(allowedMask = next) }
    }

    val onSetSlot: (Int, Mode) -> Unit = { slot, mode ->
        val list = state.shortcuts.toMutableList()
        val existing = list.indexOf(mode)
        if (slot < list.size) {
            if (existing >= 0 && existing != slot) list[existing] = list[slot]
            list[slot] = mode
        } else if (existing < 0) {
            list.add(mode)
        }
        update { it.copy(shortcuts = list.distinct().take(3)) }
    }

    val onFixNotifications = {
        if (Build.VERSION.SDK_INT >= 33 && !health.notificationPermission) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            HealthCheck.openNotificationSettings(context, health.channelBlocked)
        }
    }

    val scrollBehaviors = Tab.entries.map {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    }
    val scrollBehavior = scrollBehaviors[tabIndex]

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(Tab.entries[tabIndex].title) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == tabIndex,
                        onClick = { tabIndex = index },
                        icon = {
                            if (tab == Tab.Settings && !health.allGood) {
                                BadgedBox(badge = { Badge() }) { Icon(tab.icon, contentDescription = null) }
                            } else {
                                Icon(tab.icon, contentDescription = null)
                            }
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Crossfade(targetState = tabIndex, label = "tab") { index ->
            when (Tab.entries[index]) {
                Tab.Home -> HomeScreen(
                    state = state,
                    health = health,
                    engine = engine,
                    padding = padding,
                    onMode = onMode,
                    onToggleSide = onToggleSide,
                    onAllowOverlay = { HealthCheck.openOverlaySettings(context) },
                    onReviewSettings = { tabIndex = Tab.Settings.ordinal },
                    onDismissStopNotice = {
                        HealthCheck.acknowledgeUserStop(context)
                        refreshHealth()
                    },
                )
                Tab.Shortcuts -> ShortcutsScreen(
                    state = state,
                    padding = padding,
                    onSetSlot = onSetSlot,
                    onKeepNotification = { v -> update { it.copy(keepNotification = v) } },
                )
                Tab.Settings -> SettingsScreen(
                    state = state,
                    health = health,
                    engine = engine,
                    padding = padding,
                    onAllowOverlay = { HealthCheck.openOverlaySettings(context) },
                    onFixNotifications = onFixNotifications,
                    onBattery = { HealthCheck.requestBatteryExemption(context) },
                    onAutostart = { HealthCheck.openAutostart(context) },
                    onRestart = {
                        PivotController.sync(context)
                        refreshHealth()
                    },
                    onBoot = { v -> update { it.copy(startOnBoot = v) } },
                    onHaptics = { v -> update { it.copy(haptics = v) } },
                )
            }
        }
    }
}
