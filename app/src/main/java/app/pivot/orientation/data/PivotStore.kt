package app.pivot.orientation.data

import android.content.Context
import android.content.SharedPreferences
import app.pivot.orientation.model.Mode
import app.pivot.orientation.model.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PivotState(
    val mode: Mode = Mode.AUTO,
    val allowedMask: Int = Side.PORTRAIT.bit or Side.LANDSCAPE.bit,
    val shortcuts: List<Mode> = listOf(Mode.PORTRAIT, Mode.LANDSCAPE, Mode.AUTO),
    val keepNotification: Boolean = true,
    val startOnBoot: Boolean = true,
    val haptics: Boolean = true,
)

/**
 * Tiny synchronous store. SharedPreferences is used on purpose: the service and the
 * quick settings tile need instant reads, and it costs no coroutine or disk work
 * until a value actually changes.
 */
object PivotStore {
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(PivotState())
    val state: StateFlow<PivotState> get() = _state

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("pivot", Context.MODE_PRIVATE)
        _state.value = read()
    }

    @Synchronized
    fun update(block: (PivotState) -> PivotState) {
        val next = block(_state.value)
        if (next == _state.value) return
        _state.value = next
        write(next)
    }

    private fun read(): PivotState {
        val d = PivotState()
        val shortcuts = prefs.getString("shortcuts", null)
            ?.split(",")
            ?.mapNotNull { Mode.fromId(it) }
            ?.distinct()
            ?.take(3)
            ?.takeIf { it.isNotEmpty() }
            ?: d.shortcuts
        return PivotState(
            mode = Mode.fromId(prefs.getString("mode", null)) ?: d.mode,
            allowedMask = (prefs.getInt("mask", d.allowedMask) and Side.ALL).takeIf { it != 0 } ?: d.allowedMask,
            shortcuts = shortcuts,
            keepNotification = prefs.getBoolean("keep_notification", d.keepNotification),
            startOnBoot = prefs.getBoolean("start_on_boot", d.startOnBoot),
            haptics = prefs.getBoolean("haptics", d.haptics),
        )
    }

    private fun write(s: PivotState) {
        prefs.edit()
            .putString("mode", s.mode.id)
            .putInt("mask", s.allowedMask)
            .putString("shortcuts", s.shortcuts.joinToString(",") { it.id })
            .putBoolean("keep_notification", s.keepNotification)
            .putBoolean("start_on_boot", s.startOnBoot)
            .putBoolean("haptics", s.haptics)
            .apply()
    }
}
