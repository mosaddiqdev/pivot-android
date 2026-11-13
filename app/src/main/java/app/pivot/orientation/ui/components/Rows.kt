@file:OptIn(ExperimentalMaterial3Api::class)

package app.pivot.orientation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.pivot.orientation.ui.theme.okColor

enum class Level { Ok, Warn, Info }

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
    )
}

/** Rounded container that groups related list rows. */
@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(PaddingValues(vertical = 4.dp)),
            content = content,
        )
    }
}

@Composable
fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

@Composable
fun SwitchRow(
    title: String,
    supporting: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}

/** One health check: a status mark, a title, a sentence, and an optional fix button. */
@Composable
fun StatusRow(
    level: Level,
    title: String,
    supporting: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val (icon, tint) = when (level) {
        Level.Ok -> Icons.Rounded.CheckCircle to okColor()
        Level.Warn -> Icons.Rounded.Warning to MaterialTheme.colorScheme.error
        Level.Info -> Icons.Rounded.Info to MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListItem(
        leadingContent = { Icon(icon, contentDescription = level.name, tint = tint) },
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        trailingContent = actionLabel?.let { label -> { TextButton(onClick = onAction) { Text(label) } } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
