@file:OptIn(ExperimentalMaterial3Api::class)

package app.pivot.orientation.ui.screens

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon as TileIcon
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pivot.orientation.R
import app.pivot.orientation.data.PivotState
import app.pivot.orientation.model.Mode
import app.pivot.orientation.model.Side
import app.pivot.orientation.model.primaryFor
import app.pivot.orientation.model.sidesFor
import app.pivot.orientation.service.PivotTile
import app.pivot.orientation.ui.components.Group
import app.pivot.orientation.ui.components.GroupDivider
import app.pivot.orientation.ui.components.OrientationGlyph
import app.pivot.orientation.ui.components.SectionHeader
import app.pivot.orientation.ui.components.SwitchRow

private const val SLOTS = 3

@Composable
fun ShortcutsScreen(
    state: PivotState,
    padding: PaddingValues,
    onSetSlot: (slot: Int, mode: Mode) -> Unit,
    onKeepNotification: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Int?>(null) }

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
                "Three modes become buttons in the notification and cycle on the Quick Settings tile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        item { NotificationPreview(state) }

        item { SectionHeader("Buttons") }
        item {
            Group {
                for (slot in 0 until SLOTS) {
                    val mode = state.shortcuts.getOrNull(slot)
                    if (slot > 0) GroupDivider()
                    ListItem(
                        leadingContent = {
                            if (mode != null) {
                                OrientationGlyph(
                                    sides = mode.sidesFor(state.allowedMask),
                                    primary = mode.primaryFor(state.allowedMask),
                                    frozen = mode.locked,
                                    modifier = Modifier.size(40.dp),
                                )
                            } else {
                                Spacer(Modifier.size(40.dp))
                            }
                        },
                        overlineContent = { Text("Button ${slot + 1}") },
                        headlineContent = { Text(mode?.title ?: "Empty") },
                        supportingContent = { Text(mode?.hint ?: "Tap to choose a mode") },
                        trailingContent = { TextButton(onClick = { editing = slot }) { Text("Change") } },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { editing = slot },
                    )
                }
            }
        }

        item { SectionHeader("Where they appear") }
        item {
            Group {
                SwitchRow(
                    title = "Show controls in notification",
                    supporting = "Keeps the notification so you can switch from the shade, even in Auto.",
                    checked = state.keepNotification,
                    onChange = onKeepNotification,
                )
                GroupDivider()
                ListItem(
                    headlineContent = { Text("Quick Settings tile") },
                    supportingContent = {
                        Text(
                            if (Build.VERSION.SDK_INT >= 33) {
                                "One tap moves to the next button."
                            } else {
                                "Edit your Quick Settings panel and drag Pivot in. One tap moves to the next button."
                            },
                        )
                    },
                    trailingContent = if (Build.VERSION.SDK_INT >= 33) {
                        { FilledTonalButton(onClick = { requestTile(context) }) { Text("Add") } }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    editing?.let { slot ->
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                "Button ${slot + 1}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(Mode.entries.toList(), key = { it.id }) { mode ->
                    ListItem(
                        leadingContent = {
                            OrientationGlyph(
                                sides = mode.sidesFor(state.allowedMask),
                                primary = mode.primaryFor(state.allowedMask),
                                frozen = mode.locked,
                                modifier = Modifier.size(40.dp),
                            )
                        },
                        headlineContent = { Text(mode.title) },
                        supportingContent = { Text(mode.hint) },
                        trailingContent = if (state.shortcuts.getOrNull(slot) == mode) {
                            { Icon(Icons.Rounded.Check, contentDescription = "Current", tint = MaterialTheme.colorScheme.primary) }
                        } else {
                            null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onSetSlot(slot, mode)
                            editing = null
                        },
                    )
                }
            }
        }
    }
}

/** Android 13 and newer can ask the system to add the tile with one confirmation. */
private fun requestTile(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < 33) return
    val bar = context.getSystemService(StatusBarManager::class.java) ?: return
    runCatching {
        bar.requestAddTileService(
            ComponentName(context, PivotTile::class.java),
            "Pivot",
            TileIcon.createWithResource(context, R.drawable.ic_stat_pivot),
            context.mainExecutor,
        ) { _ -> }
    }
}

/** Mirrors the real notification so the three buttons are easy to picture. */
@Composable
private fun NotificationPreview(state: PivotState) {
    val scheme = MaterialTheme.colorScheme
    val body = if (state.mode == Mode.CUSTOM) {
        "Allowed: " + Side.fromMask(state.allowedMask).joinToString(", ") { it.label }
    } else {
        state.mode.hint
    }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHighest),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).background(scheme.primary, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("Pivot", style = MaterialTheme.typography.labelMedium)
                Text(
                    "  ·  Orientation controls",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Rotation: ${state.mode.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                state.shortcuts.forEach { mode ->
                    Text(
                        if (mode == state.mode) "${mode.title} ✓" else mode.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                    )
                }
            }
        }
    }
}
