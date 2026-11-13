package app.pivot.orientation.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.pivot.orientation.model.Side
import app.pivot.orientation.ui.theme.Pivot
import kotlin.math.max

/**
 * Turns a target angle into one that always takes the short way round,
 * so going from 270 to 0 degrees never spins the phone three quarters of a turn.
 */
@Composable
private fun rememberContinuousAngle(target: Float): Float {
    var accumulated by remember { mutableFloatStateOf(target) }
    val next = remember(target) {
        val delta = ((target - accumulated) % 360f + 540f) % 360f - 180f
        accumulated + delta
    }
    SideEffect { accumulated = next }
    return next
}

/**
 * The signature visual of the app. A phone outline that physically turns to the selected
 * direction. Allowed directions appear as faint ghosts with their own notch.
 * It only animates when the selection changes, so it costs nothing at rest.
 */
@Composable
fun PhoneGlyph(
    modifier: Modifier,
    primary: Side?,
    sides: Set<Side>,
    locked: Boolean = false,
    dashedGhosts: Boolean = false,
) {
    val c = Pivot.colors
    val target = rememberContinuousAngle(primary?.angle ?: 0f)
    val angle by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "glyphAngle",
    )

    Canvas(modifier) {
        val m = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)
        val pw = m * 0.40f
        val ph = m * 0.74f
        val stroke = max(1.5f, m * 0.028f)
        val topLeft = Offset(cx - pw / 2f, cy - ph / 2f)
        val corner = CornerRadius(pw * 0.22f)
        val ghosts = sides.filter { it != primary }

        fun DrawScope.outline(alpha: Float, fill: Boolean, dashed: Boolean) {
            if (fill) drawRoundRect(color = c.card, topLeft = topLeft, size = Size(pw, ph), cornerRadius = corner)
            drawRoundRect(
                color = c.foreground.copy(alpha = alpha),
                topLeft = topLeft,
                size = Size(pw, ph),
                cornerRadius = corner,
                style = Stroke(
                    width = stroke,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(m * 0.03f, m * 0.035f)) else null,
                ),
            )
        }

        fun DrawScope.notch(alpha: Float) {
            drawRoundRect(
                color = c.foreground.copy(alpha = alpha),
                topLeft = Offset(cx - pw * 0.14f, cy - ph / 2f + ph * 0.05f),
                size = Size(pw * 0.28f, ph * 0.028f),
                cornerRadius = CornerRadius(ph * 0.014f),
            )
        }

        // Ghost bodies first, then the active phone on top, then every notch.
        ghosts.forEach { side ->
            rotate(side.angle, center) { outline(alpha = 0.26f, fill = false, dashed = dashedGhosts) }
        }
        rotate(angle, center) {
            outline(alpha = 1f, fill = true, dashed = false)
            notch(1f)
            if (locked) {
                val lw = pw * 0.46f
                val lh = pw * 0.36f
                drawRoundRect(
                    color = c.foreground,
                    topLeft = Offset(cx - lw / 2f, cy),
                    size = Size(lw, lh),
                    cornerRadius = CornerRadius(lw * 0.14f),
                )
                val r = lw * 0.32f
                drawArc(
                    color = c.foreground,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = stroke),
                )
            }
        }
        ghosts.forEach { side ->
            rotate(side.angle, center) { notch(0.4f) }
        }
    }
}
