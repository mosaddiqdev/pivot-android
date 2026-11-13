package app.pivot.orientation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pivot.orientation.model.Mode
import app.pivot.orientation.model.Side
import app.pivot.orientation.model.primaryFor
import app.pivot.orientation.model.sidesFor
import app.pivot.orientation.ui.theme.Pivot

/** Whether taps give haptic feedback. Driven by the user setting. */
val LocalHaptics = staticCompositionLocalOf { true }

// Text ------------------------------------------------------------------------------------

@Composable
fun PText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Pivot.colors.foreground,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

// Interaction -----------------------------------------------------------------------------

/** Press feedback in the shadcn spirit: a quick scale, no ripple. */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHaptics.current
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
        ) {
            if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        }
}

// Button ----------------------------------------------------------------------------------

enum class ButtonVariant { Default, Secondary, Outline, Destructive }

@Composable
fun PButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    compact: Boolean = false,
) {
    val c = Pivot.colors
    val shape = RoundedCornerShape(10.dp)
    val bg: Color
    val fg: Color
    val outline: Color?
    when (variant) {
        ButtonVariant.Default -> { bg = c.primary; fg = c.primaryForeground; outline = null }
        ButtonVariant.Secondary -> { bg = c.secondary; fg = c.foreground; outline = null }
        ButtonVariant.Outline -> { bg = Color.Transparent; fg = c.foreground; outline = c.input }
        ButtonVariant.Destructive -> { bg = c.destructive; fg = c.primaryForeground; outline = null }
    }
    Box(
        modifier = modifier
            .pressable(onClick = onClick)
            .heightIn(min = if (compact) 40.dp else 48.dp)
            .clip(shape)
            .background(bg)
            .then(if (outline != null) Modifier.border(1.dp, outline, shape) else Modifier)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        PText(text, Pivot.type.heading, color = fg, maxLines = 1)
    }
}

// Surfaces --------------------------------------------------------------------------------

@Composable
fun PCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pivot.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.border, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun PSeparator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Pivot.colors.border))
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    PText(text.uppercase(), Pivot.type.label, modifier, color = Pivot.colors.mutedForeground)
}

// Badge -----------------------------------------------------------------------------------

enum class BadgeTone { Neutral, Success, Warning, Destructive, Info }

@Composable
fun PBadge(text: String, tone: BadgeTone = BadgeTone.Neutral, modifier: Modifier = Modifier) {
    val c = Pivot.colors
    val hue = when (tone) {
        BadgeTone.Neutral -> c.mutedForeground
        BadgeTone.Success -> c.success
        BadgeTone.Warning -> c.warning
        BadgeTone.Destructive -> c.destructive
        BadgeTone.Info -> c.info
    }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (tone == BadgeTone.Neutral) c.secondary else hue.copy(alpha = 0.14f))
            .border(1.dp, if (tone == BadgeTone.Neutral) c.border else hue.copy(alpha = 0.35f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        PText(text, Pivot.type.small, color = if (tone == BadgeTone.Neutral) c.foreground else hue, maxLines = 1)
    }
}

// Switch ----------------------------------------------------------------------------------

@Composable
fun PSwitchVisual(checked: Boolean) {
    val c = Pivot.colors
    val track by animateColorAsState(if (checked) c.primary else c.input, label = "track")
    val thumb by animateColorAsState(if (checked) c.primaryForeground else c.foreground, label = "thumb")
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, spring(stiffness = Spring.StiffnessMedium), label = "thumbX")
    Box(
        Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(track)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(20.dp)
                .clip(CircleShape)
                .background(thumb),
        )
    }
}

@Composable
fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHaptics.current
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                role = Role.Switch,
                onValueChange = {
                    if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onChange(it)
                },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PText(title, Pivot.type.heading)
            PText(description, Pivot.type.small, color = Pivot.colors.mutedForeground)
        }
        Spacer(Modifier.width(16.dp))
        PSwitchVisual(checked)
    }
}

// Chip ------------------------------------------------------------------------------------

@Composable
fun PChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pivot.colors
    val shape = RoundedCornerShape(10.dp)
    val bg by animateColorAsState(if (selected) c.primary else c.card, label = "chipBg")
    val fg by animateColorAsState(if (selected) c.primaryForeground else c.foreground, label = "chipFg")
    Box(
        modifier = modifier
            .pressable(role = Role.Checkbox, onClick = onClick)
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) c.primary else c.border, shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        PText(text, Pivot.type.small, color = fg, maxLines = 1, textAlign = TextAlign.Center)
    }
}

// Mode tile -------------------------------------------------------------------------------

@Composable
fun ModeTile(
    mode: Mode,
    customMask: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pivot.colors
    val shape = RoundedCornerShape(16.dp)
    val bg by animateColorAsState(if (selected) c.accent else c.card, label = "tileBg")
    val edge by animateColorAsState(if (selected) c.foreground else c.border, label = "tileEdge")
    Box(
        modifier = modifier
            .pressable(role = Role.RadioButton, onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, edge, shape),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PhoneGlyph(
                modifier = Modifier.size(56.dp),
                primary = mode.primaryFor(customMask),
                sides = mode.sidesFor(customMask),
                locked = mode.locked,
                dashedGhosts = mode == Mode.AUTO,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                PText(mode.title, Pivot.type.heading, maxLines = 1)
                PText(mode.hint, Pivot.type.small, color = c.mutedForeground, maxLines = 2)
            }
        }
        if (selected) {
            CheckBadge(Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
    }
}

@Composable
private fun CheckBadge(modifier: Modifier = Modifier) {
    val c = Pivot.colors
    Canvas(modifier.size(20.dp).clip(CircleShape).background(c.foreground)) {
        val w = size.width
        val path = Path().apply {
            moveTo(w * 0.28f, w * 0.52f)
            lineTo(w * 0.44f, w * 0.68f)
            lineTo(w * 0.72f, w * 0.34f)
        }
        drawPath(
            path = path,
            color = c.primaryForeground,
            style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** A single direction toggle used by the custom allowed-directions picker. */
@Composable
fun SideTile(
    side: Side,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pivot.colors
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(if (selected) c.accent else c.card, label = "sideBg")
    val edge by animateColorAsState(if (selected) c.foreground else c.border, label = "sideEdge")
    Column(
        modifier = modifier
            .pressable(role = Role.Checkbox, onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, edge, shape)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhoneGlyph(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer { alpha = if (selected) 1f else 0.45f },
            primary = side,
            sides = setOf(side),
        )
        PText(
            side.label.replace("Reverse ", "Rev. "),
            Pivot.type.small,
            color = if (selected) c.foreground else c.mutedForeground,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
