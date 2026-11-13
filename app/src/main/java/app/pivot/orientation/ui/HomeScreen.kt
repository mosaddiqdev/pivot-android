package app.pivot.orientation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pivot.orientation.data.PivotState
import app.pivot.orientation.data.PivotStore
import app.pivot.orientation.engine.EngineSnapshot
import app.pivot.orientation.engine.EngineStatus
import app.pivot.orientation.model.Mode
import app.pivot.orientation.model.Side
import app.pivot.orientation.model.primaryFor
import app.pivot.orientation.model.sidesFor
import app.pivot.orientation.service.PivotController
import app.pivot.orientation.ui.components.BadgeTone
import app.pivot.orientation.ui.components.ButtonVariant
import app.pivot.orientation.ui.components.LocalHaptics
import app.pivot.orientation.ui.components.ModeTile
import app.pivot.orientation.ui.components.PBadge
import app.pivot.orientation.ui.components.PButton
import app.pivot.orientation.ui.components.PCard
import app.pivot.orientation.ui.components.PChip
import app.pivot.orientation.ui.components.PSeparator
import app.pivot.orientation.ui.components.PText
import app.pivot.orientation.ui.components.PhoneGlyph
import app.pivot.orientation.ui.components.SectionLabel
import app.pivot.orientation.ui.components.SideTile
import app.pivot.orientation.ui.components.ToggleRow
import app.pivot.orientation.ui.theme.Pivot

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val c = Pivot.colors
    val state by PivotStore.state.collectAsStateWithLifecycle()
    val engine by EngineStatus.state.collectAsStateWithLifecycle()

    var overlayOk by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    var notifOk by remember { mutableStateOf(hasNotificationPermission(ctx)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayOk = Settings.canDrawOverlays(ctx)
        notifOk = hasNotificationPermission(ctx)
        PivotController.sync(ctx)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notifOk = granted
        PivotController.sync(ctx)
    }

    fun update(block: (PivotState) -> PivotState) {
        PivotStore.update(block)
        PivotController.sync(ctx)
    }

    CompositionLocalProvider(LocalHaptics provides state.haptics) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Header(state, overlayOk)

            Hero(state) { PivotController.setMode(ctx, Mode.AUTO) }

            if (!overlayOk || !notifOk) {
                SetupCard(
                    overlayOk = overlayOk,
                    notifOk = notifOk,
                    onOverlay = { openOverlaySettings(ctx) },
                    onAppInfo = { openAppInfo(ctx) },
                    onNotifications = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }

            Section("Orientation", "Applies instantly to every app on your phone.") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Mode.entries.chunked(2).forEach { pair ->
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { mode ->
                                ModeTile(
                                    mode = mode,
                                    customMask = state.allowedMask,
                                    selected = state.mode == mode,
                                    onClick = { PivotController.setMode(ctx, mode) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.mode == Mode.CUSTOM,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Section(
                    "Allowed directions",
                    "Choose where the screen may turn. At least one stays on. " +
                        "The motion sensor is used only when your choice needs it, and only while the screen is on.",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Side.entries.forEach { side ->
                            SideTile(
                                side = side,
                                selected = (state.allowedMask and side.bit) != 0,
                                onClick = {
                                    val next = if ((state.allowedMask and side.bit) != 0) {
                                        state.allowedMask and side.bit.inv()
                                    } else {
                                        state.allowedMask or side.bit
                                    }
                                    if (next != 0) update { it.copy(allowedMask = next) }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Section(
                "Notification buttons",
                "Android shows three buttons. Tap a mode to add it. A fourth tap replaces the oldest. " +
                    "The quick settings tile cycles through the same three.",
            ) {
                NotificationPreview(state)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mode.entries.chunked(2).forEach { pair ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { mode ->
                                PChip(
                                    text = mode.title,
                                    selected = mode in state.shortcuts,
                                    onClick = { update { it.copy(shortcuts = toggleShortcut(it.shortcuts, mode)) } },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            Section("Behavior") {
                PCard(padding = 16.dp) {
                    ToggleRow(
                        title = "Keep notification in Auto",
                        description = "Stay one tap away even when nothing is locked. Idle cost is zero.",
                        checked = state.keepNotification,
                        onChange = { v -> update { it.copy(keepNotification = v) } },
                    )
                    PSeparator()
                    ToggleRow(
                        title = "Restore after restart",
                        description = "Bring your last mode back when the phone boots.",
                        checked = state.startOnBoot,
                        onChange = { v -> update { it.copy(startOnBoot = v) } },
                    )
                    PSeparator()
                    ToggleRow(
                        title = "Haptic feedback",
                        description = "A soft tick on every selection.",
                        checked = state.haptics,
                        onChange = { v -> update { it.copy(haptics = v) } },
                    )
                }
            }

            Section("Under the hood") {
                EngineCard(state, engine, overlayOk)
            }

            Footer(ctx)
        }
    }
}

// Header and hero -------------------------------------------------------------------------

@Composable
private fun Header(state: PivotState, overlayOk: Boolean) {
    val (label, tone) = when {
        state.mode == Mode.AUTO -> "Auto" to BadgeTone.Neutral
        !overlayOk -> "Setup needed" to BadgeTone.Warning
        else -> "Active" to BadgeTone.Success
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PText("Pivot", Pivot.type.display)
            PText("Orientation control", Pivot.type.body, color = Pivot.colors.mutedForeground)
        }
        PBadge(label, tone)
    }
}

@Composable
private fun Hero(state: PivotState, onRelease: () -> Unit) {
    val c = Pivot.colors
    val stage = RoundedCornerShape(24.dp)
    PCard(padding = 20.dp) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(204.dp)
                    .clip(stage)
                    .background(c.muted)
                    .border(1.dp, c.border, stage),
                contentAlignment = Alignment.Center,
            ) {
                PhoneGlyph(
                    modifier = Modifier.size(172.dp),
                    primary = state.mode.primaryFor(state.allowedMask),
                    sides = state.mode.sidesFor(state.allowedMask),
                    locked = state.mode.locked,
                    dashedGhosts = state.mode == Mode.AUTO,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        PText(
            state.mode.title,
            Pivot.type.title,
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        PText(
            if (state.mode == Mode.CUSTOM) {
                Side.fromMask(state.allowedMask).joinToString(", ") { it.label }
            } else {
                state.mode.hint
            },
            Pivot.type.body,
            Modifier.fillMaxWidth(),
            color = c.mutedForeground,
            textAlign = TextAlign.Center,
        )
        if (state.mode != Mode.AUTO) {
            Spacer(Modifier.height(18.dp))
            PButton("Release to Auto", onRelease, Modifier.fillMaxWidth(), ButtonVariant.Outline)
        }
    }
}

// Setup -----------------------------------------------------------------------------------

@Composable
private fun SetupCard(
    overlayOk: Boolean,
    notifOk: Boolean,
    onOverlay: () -> Unit,
    onAppInfo: () -> Unit,
    onNotifications: () -> Unit,
) {
    val c = Pivot.colors
    PCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PText("Finish setup", Pivot.type.heading, Modifier.weight(1f))
            PBadge("Required", BadgeTone.Warning)
        }
        if (!overlayOk) {
            Spacer(Modifier.height(14.dp))
            PText("Display over other apps", Pivot.type.heading)
            Spacer(Modifier.height(2.dp))
            PText(
                "Pivot sets orientation with a hidden one pixel window. Nothing is ever drawn on your screen.",
                Pivot.type.small,
                color = c.mutedForeground,
            )
            Spacer(Modifier.height(12.dp))
            PButton("Allow", onOverlay, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            PText(
                "Switch greyed out? This APK was installed outside the Play Store, so Android restricts it. " +
                    "Open App info, tap the three dot menu, choose Allow restricted settings, then come back.",
                Pivot.type.small,
                color = c.mutedForeground,
            )
            Spacer(Modifier.height(10.dp))
            PButton("Open App info", onAppInfo, Modifier.fillMaxWidth(), ButtonVariant.Secondary, compact = true)
        }
        if (!notifOk) {
            if (!overlayOk) {
                Spacer(Modifier.height(16.dp))
                PSeparator()
            }
            Spacer(Modifier.height(14.dp))
            PText("Notifications", Pivot.type.heading)
            Spacer(Modifier.height(2.dp))
            PText(
                "Needed for the control notification with its quick buttons.",
                Pivot.type.small,
                color = c.mutedForeground,
            )
            Spacer(Modifier.height(12.dp))
            PButton("Allow notifications", onNotifications, Modifier.fillMaxWidth())
        }
    }
}

// Notification preview --------------------------------------------------------------------

@Composable
private fun NotificationPreview(state: PivotState) {
    val c = Pivot.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.popover)
            .border(1.dp, c.border, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhoneGlyph(
                modifier = Modifier.size(20.dp),
                primary = Side.PORTRAIT,
                sides = setOf(Side.LANDSCAPE),
            )
            PText("Pivot", Pivot.type.small, color = c.mutedForeground)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PText("Rotation: ${state.mode.title}", Pivot.type.heading)
            PText(
                if (state.mode == Mode.CUSTOM) {
                    "Allowed: " + Side.fromMask(state.allowedMask).joinToString(", ") { it.label }
                } else {
                    state.mode.hint
                },
                Pivot.type.small,
                color = c.mutedForeground,
                maxLines = 2,
            )
        }
        PSeparator()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.shortcuts.forEach { mode ->
                PText(
                    text = if (mode == state.mode) "${mode.title} ✓" else mode.title,
                    style = Pivot.type.small,
                    modifier = Modifier.weight(1f),
                    color = if (mode == state.mode) c.foreground else c.mutedForeground,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// Engine card -----------------------------------------------------------------------------

@Composable
private fun EngineCard(state: PivotState, engine: EngineSnapshot, overlayOk: Boolean) {
    val c = Pivot.colors
    PCard {
        StatusRow(
            title = "Overlay window",
            badge = when {
                engine.overlayAttached -> "Attached" to BadgeTone.Success
                state.mode != Mode.AUTO && !overlayOk -> "Not allowed" to BadgeTone.Warning
                else -> "Released" to BadgeTone.Neutral
            },
        )
        Spacer(Modifier.height(12.dp))
        PSeparator()
        Spacer(Modifier.height(12.dp))
        StatusRow(
            title = "Motion sensor",
            badge = if (engine.sensorListening) "Listening" to BadgeTone.Info else "Off" to BadgeTone.Neutral,
        )
        Spacer(Modifier.height(14.dp))
        PText(
            "Built for battery. No polling, no wake locks, no network. Fixed modes are a single window " +
                "attribute and use no CPU afterwards. Only custom combinations the system cannot express " +
                "read the sensor, and only while the screen is on.",
            Pivot.type.small,
            color = c.mutedForeground,
        )
    }
}

@Composable
private fun StatusRow(title: String, badge: Pair<String, BadgeTone>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PText(title, Pivot.type.heading, Modifier.weight(1f))
        PBadge(badge.first, badge.second)
    }
}

@Composable
private fun Footer(ctx: Context) {
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    PText(
        "Pivot $version",
        Pivot.type.small,
        Modifier.fillMaxWidth(),
        color = Pivot.colors.mutedForeground,
        textAlign = TextAlign.Center,
    )
}

// Layout helpers --------------------------------------------------------------------------

@Composable
private fun Section(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel(title)
            if (subtitle != null) {
                PText(subtitle, Pivot.type.small, color = Pivot.colors.mutedForeground)
            }
        }
        content()
    }
}

// Logic helpers ---------------------------------------------------------------------------

private fun toggleShortcut(current: List<Mode>, mode: Mode): List<Mode> = when {
    mode in current -> if (current.size > 1) current - mode else current
    current.size >= 3 -> current.drop(1) + mode
    else -> current + mode
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun openOverlaySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
    )
}

private fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
    )
}
