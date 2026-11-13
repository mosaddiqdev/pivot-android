package app.pivot.orientation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.pivot.orientation.model.Side
import kotlin.math.cos
import kotlin.math.sin

/**
 * A phone outline turned to the main direction, with four dots around it. A lit dot means
 * the phone may end up with its top edge pointing that way. A dashed outline means frozen.
 */
@Composable
fun OrientationGlyph(
    sides: Set<Side>,
    primary: Side?,
    modifier: Modifier = Modifier,
    frozen: Boolean = false,
    body: Color = MaterialTheme.colorScheme.onSurface,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = center
        val phoneW = s * 0.34f
        val phoneH = s * 0.58f
        val stroke = s * 0.055f

        rotate(degrees = primary?.angle ?: 0f, pivot = c) {
            val dash = if (frozen) PathEffect.dashPathEffect(floatArrayOf(stroke * 1.6f, stroke * 1.4f)) else null
            drawRoundRect(
                color = body,
                topLeft = Offset(c.x - phoneW / 2f, c.y - phoneH / 2f),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(phoneW * 0.22f),
                style = Stroke(width = stroke, pathEffect = dash),
            )
            val slotY = c.y - phoneH / 2f + phoneH * 0.1f
            drawLine(
                color = body,
                start = Offset(c.x - phoneW * 0.14f, slotY),
                end = Offset(c.x + phoneW * 0.14f, slotY),
                strokeWidth = stroke * 0.8f,
                cap = StrokeCap.Round,
            )
        }

        val radius = s * 0.44f
        Side.entries.forEach { side ->
            val a = Math.toRadians(side.angle.toDouble())
            val at = Offset(c.x + (radius * sin(a)).toFloat(), c.y - (radius * cos(a)).toFloat())
            drawCircle(
                color = if (side in sides) accent else body.copy(alpha = 0.18f),
                radius = s * 0.05f,
                center = at,
            )
        }
    }
}
