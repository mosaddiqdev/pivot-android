package app.pivot.orientation.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class EngineSnapshot(
    val overlayAttached: Boolean = false,
    val sensorListening: Boolean = false,
)

/** Read-only window into what the engine is doing, shown in the app. */
object EngineStatus {
    private val _state = MutableStateFlow(EngineSnapshot())
    val state: StateFlow<EngineSnapshot> get() = _state

    fun set(overlayAttached: Boolean, sensorListening: Boolean) {
        _state.value = EngineSnapshot(overlayAttached, sensorListening)
    }
}
