package app.pivot.orientation.model

import android.content.pm.ActivityInfo
import kotlin.math.abs

/** One of the four physical directions a phone screen can face. */
enum class Side(val bit: Int, val label: String, val angle: Float, val constant: Int) {
    PORTRAIT(1, "Portrait", 0f, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    REVERSE_PORTRAIT(2, "Reverse portrait", 180f, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT),
    LANDSCAPE(4, "Landscape", -90f, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    REVERSE_LANDSCAPE(8, "Reverse landscape", 90f, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

    companion object {
        const val ALL = 15

        fun fromMask(mask: Int): List<Side> = entries.filter { (mask and it.bit) != 0 }

        fun maskOf(sides: Collection<Side>): Int = sides.fold(0) { acc, s -> acc or s.bit }
    }
}

/** Shortest distance between two angles in degrees, 0..180. */
fun angularDistance(a: Float, b: Float): Float {
    val d = ((a - b) % 360f + 540f) % 360f - 180f
    return abs(d)
}

/** Everything the user can pick. [sides] and [primary] only drive the glyph drawing. */
enum class Mode(
    val id: String,
    val title: String,
    val hint: String,
    val sides: Set<Side>,
    val primary: Side?,
    val locked: Boolean = false,
) {
    AUTO("auto", "Auto", "Rotation follows the system setting", Side.entries.toSet(), Side.PORTRAIT),
    PORTRAIT("portrait", "Portrait", "Always upright, never rotates", setOf(Side.PORTRAIT), Side.PORTRAIT),
    LANDSCAPE("landscape", "Landscape", "Always sideways, left edge up", setOf(Side.LANDSCAPE), Side.LANDSCAPE),
    REVERSE_PORTRAIT("reverse_portrait", "Reverse portrait", "Always upside down", setOf(Side.REVERSE_PORTRAIT), Side.REVERSE_PORTRAIT),
    REVERSE_LANDSCAPE("reverse_landscape", "Reverse landscape", "Always sideways, right edge up", setOf(Side.REVERSE_LANDSCAPE), Side.REVERSE_LANDSCAPE),
    SENSOR_PORTRAIT("sensor_portrait", "Portrait flip", "Upright or upside down, by sensor", setOf(Side.PORTRAIT, Side.REVERSE_PORTRAIT), Side.PORTRAIT),
    SENSOR_LANDSCAPE("sensor_landscape", "Landscape flip", "Either sideways direction, by sensor", setOf(Side.LANDSCAPE, Side.REVERSE_LANDSCAPE), Side.LANDSCAPE),
    FULL_SENSOR("full_sensor", "All four", "Every direction, by sensor", Side.entries.toSet(), Side.PORTRAIT),
    LOCKED("locked", "Freeze", "Hold whatever the screen shows now", emptySet(), null, locked = true),
    CUSTOM("custom", "Custom", "Choose exactly which directions are allowed", setOf(Side.PORTRAIT, Side.LANDSCAPE), Side.PORTRAIT);

    companion object {
        fun fromId(id: String?): Mode? = entries.firstOrNull { it.id == id }
    }
}

/** Sides to draw for a mode. Custom mode uses the user's own selection. */
fun Mode.sidesFor(customMask: Int): Set<Side> =
    if (this == Mode.CUSTOM) Side.fromMask(customMask).toSet() else sides

fun Mode.primaryFor(customMask: Int): Side? =
    if (this == Mode.CUSTOM) Side.fromMask(customMask).firstOrNull() else primary

/** How the engine should realise a mode with the least possible work. */
sealed interface Plan {
    /** No overlay at all. The system decides. */
    data object Release : Plan

    /** One static overlay value. Zero CPU after it is applied. */
    data class Fixed(val constant: Int) : Plan

    /** The system cannot express this set, so a screen-on-only sensor picks the side. */
    data class Adaptive(val allowed: Int) : Plan
}

object OrientationPlanner {
    fun plan(mode: Mode, customMask: Int): Plan = when (mode) {
        Mode.AUTO -> Plan.Release
        Mode.PORTRAIT -> Plan.Fixed(Side.PORTRAIT.constant)
        Mode.LANDSCAPE -> Plan.Fixed(Side.LANDSCAPE.constant)
        Mode.REVERSE_PORTRAIT -> Plan.Fixed(Side.REVERSE_PORTRAIT.constant)
        Mode.REVERSE_LANDSCAPE -> Plan.Fixed(Side.REVERSE_LANDSCAPE.constant)
        Mode.SENSOR_PORTRAIT -> Plan.Fixed(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
        Mode.SENSOR_LANDSCAPE -> Plan.Fixed(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        Mode.FULL_SENSOR -> Plan.Fixed(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
        Mode.LOCKED -> Plan.Fixed(ActivityInfo.SCREEN_ORIENTATION_LOCKED)
        Mode.CUSTOM -> custom(customMask)
    }

    private fun custom(rawMask: Int): Plan {
        val mask = rawMask and Side.ALL
        val sides = Side.fromMask(mask)
        val portraits = Side.PORTRAIT.bit or Side.REVERSE_PORTRAIT.bit
        val landscapes = Side.LANDSCAPE.bit or Side.REVERSE_LANDSCAPE.bit
        return when {
            sides.isEmpty() -> Plan.Release
            sides.size == 1 -> Plan.Fixed(sides.first().constant)
            mask == Side.ALL -> Plan.Fixed(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
            mask == portraits -> Plan.Fixed(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
            mask == landscapes -> Plan.Fixed(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
            else -> Plan.Adaptive(mask)
        }
    }
}
