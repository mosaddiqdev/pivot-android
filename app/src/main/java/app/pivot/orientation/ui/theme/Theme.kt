package app.pivot.orientation.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Charcoal monochrome tokens named after the shadcn/ui set so the mapping is obvious.
 * Semantic hues (success, warning, destructive, info) are reserved for status only.
 */
@Immutable
data class PivotColors(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val popover: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val muted: Color,
    val mutedForeground: Color,
    val accent: Color,
    val border: Color,
    val input: Color,
    val ring: Color,
    val success: Color,
    val warning: Color,
    val destructive: Color,
    val info: Color,
)

private val CharcoalDark = PivotColors(
    background = Color(0xFF0D0D0F),
    foreground = Color(0xFFF5F5F6),
    card = Color(0xFF141416),
    popover = Color(0xFF1A1A1D),
    primary = Color(0xFFF5F5F6),
    primaryForeground = Color(0xFF151517),
    secondary = Color(0xFF212125),
    muted = Color(0xFF1B1B1E),
    mutedForeground = Color(0xFF9D9DA6),
    accent = Color(0xFF26262A),
    border = Color(0xFF2C2C31),
    input = Color(0xFF3A3A41),
    ring = Color(0xFF8A8A94),
    success = Color(0xFF3FCF8E),
    warning = Color(0xFFF2B24C),
    destructive = Color(0xFFF26B6B),
    info = Color(0xFF6CA9FF),
)

@Immutable
class PivotTypography {
    private val sans = FontFamily.SansSerif

    val display = TextStyle(fontFamily = sans, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp)
    val title = TextStyle(fontFamily = sans, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
    val heading = TextStyle(fontFamily = sans, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.1).sp)
    val body = TextStyle(fontFamily = sans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val small = TextStyle(fontFamily = sans, fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontFamily = sans, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.7.sp)
}

private val LocalPivotColors = staticCompositionLocalOf { CharcoalDark }
private val LocalPivotTypography = staticCompositionLocalOf { PivotTypography() }

object Pivot {
    val colors: PivotColors
        @Composable @ReadOnlyComposable get() = LocalPivotColors.current

    val type: PivotTypography
        @Composable @ReadOnlyComposable get() = LocalPivotTypography.current
}

@Composable
fun PivotTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPivotColors provides CharcoalDark,
        LocalPivotTypography provides PivotTypography(),
    ) {
        Box(Modifier.fillMaxSize().background(CharcoalDark.background)) {
            content()
        }
    }
}
