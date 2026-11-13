@file:OptIn(ExperimentalMaterial3Api::class)

package app.pivot.orientation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pivot.orientation.data.PivotState
import app.pivot.orientation.engine.EngineSnapshot
import app.pivot.orientation.health.Health
import app.pivot.orientation.model.Mode
import app.pivot.orientation.model.Side
import app.pivot.orientation.model.primaryFor
import app.pivot.orientation.model.sidesFor
import app.pivot.orientation.ui.components.OrientationGlyph
import app.pivot.orientation.ui.components.SectionHeader

private val sections: List<Pair<String, List<Mode>>> = listOf(
    "System" to listOf(Mode.AUTO, Mode.LOCKED),
    "Fixed direction" to listOf(Mode.PORTRAIT, Mode.LANDSCAPE, Mode.REVERSE_PORTRAIT, Mode.REVERSE_LANDSCAPE),
    "Follow the sensor" to listOf(Mode.SENSOR_PORTRAIT, Mode.SENSOR_LANDSCAPE, Mode.FULL_SENSOR),
    "Your own mix" to listOf(Mode.CUSTOM),
)

@Composable
fun HomeScreen(
    state: PivotState,
    health: Health,
    engine: EngineSnapshot,
    padding: PaddingValues,
    onMode: (Mode) -> Unit,
    onToggleSide: (Side) -> Unit,
    onAllowOverlay: () -> Unit,
    onReviewSettings: () -> Unit,
    onDismissStopNotice: () -> Unit,
) {
    val issues = listOf(!health.notificationsOk, !health.batteryUnrestricted, health.serviceStalled).count { it }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            StatusHero(state, health, engine, onOff = { onMode(Mode.AUTO) })
        }

        if (health.stoppedByUser) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NoticeCard(
                    title = "Pivot was stopped",
                    text = "It was stopped from Active apps or App info. Opening Pivot started it again.",
                    action = "Got it",
                    onAction = onDismissStopNotice,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        if (!health.overlayGranted) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NoticeCard(
                    title = "Allow display over other apps",
                    text = "Android needs this permission before Pivot can change the screen direction.",
                    action = "Allow",
                    onAction = onAllowOverlay,
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                    warn = true,
                )
            }
        } else if (issues > 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NoticeCard(
                    title = if (issues == 1) "1 thing can stop Pivot" else "$issues things can stop Pivot",
                    text = "Fix them so rotation keeps working after you close the app.",
                    action = "Review",
                    onAction = onReviewSettings,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    warn = true,
                )
            }
        }

        sections.forEach { (title, modes) ->
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(title) }
            items(
                items = modes,
                key = { it.id },
                span = { if (modes.size == 1) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            ) { mode ->
                if (mode == Mode.CUSTOM) {
                    CustomCard(state, onSelect = { onMode(mode) }, onToggleSide = onToggleSide)
                } else {
                    ModeCard(mode, state, selected = state.mode == mode, onClick = { onMode(mode) })
                }
            }
        }
    }
}

@Composable
private fun StatusHero(
    state: PivotState,
    health: Health,
    engine: EngineSnapshot,
    onOff: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val active = state.mode != Mode.AUTO && health.overlayGranted
    val line = when {
        state.mode != Mode.AUTO && !health.overlayGranted -> "Waiting for permission to draw over other apps"
        state.mode == Mode.CUSTOM -> "Allowed: " + Side.fromMask(state.allowedMask).joinToString(", ") { it.label }
        active && engine.sensorListening -> state.mode.hint + " · sensor on"
        else -> state.mode.hint
    }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) scheme.primaryContainer else scheme.surfaceContainerHigh,
            contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
        ),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            OrientationGlyph(
                sides = state.mode.sidesFor(state.allowedMask),
                primary = state.mode.primaryFor(state.allowedMask),
                frozen = state.mode.locked,
                body = LocalContentColor.current,
                accent = if (active) scheme.primary else scheme.outline,
                modifier = Modifier.size(84.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Current mode", style = MaterialTheme.typography.labelMedium)
                Text(
                    state.mode.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.mode != Mode.AUTO) {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                FilledTonalButton(onClick = onOff) { Text("Back to Auto") }
            }
        }
    }
}

@Composable
private fun NoticeCard(
    title: String,
    text: String,
    action: String,
    onAction: () -> Unit,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    warn: Boolean = false,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (warn) {
                Icon(Icons.Rounded.Warning, contentDescription = null)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun ModeCard(
    mode: Mode,
    state: PivotState,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
        label = "modeContainer",
    )
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) scheme.primary else scheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OrientationGlyph(
                    sides = mode.sidesFor(state.allowedMask),
                    primary = mode.primaryFor(state.allowedMask),
                    frozen = mode.locked,
                    body = LocalContentColor.current,
                    accent = scheme.primary,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = scheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                mode.hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CustomCard(
    state: PivotState,
    onSelect: () -> Unit,
    onToggleSide: (Side) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val selected = state.mode == Mode.CUSTOM
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
        label = "customContainer",
    )
    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) scheme.primary else scheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OrientationGlyph(
                    sides = Side.fromMask(state.allowedMask).toSet(),
                    primary = Side.fromMask(state.allowedMask).firstOrNull(),
                    body = LocalContentColor.current,
                    accent = scheme.primary,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(Mode.CUSTOM.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        Mode.CUSTOM.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    )
                }
                if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = scheme.primary)
            }
            AnimatedVisibility(visible = selected) {
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Side.entries.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { side ->
                                val on = (state.allowedMask and side.bit) != 0
                                FilterChip(
                                    selected = on,
                                    onClick = { onToggleSide(side) },
                                    label = { Text(side.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = if (on) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else {
                                        null
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
