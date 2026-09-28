package dirtyfrag.galaxy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory log bus. UI shows the last 3 lines by default and the full list when expanded. */
object LogBus {

    private const val MAX = 2000

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun log(message: String) {
        val trimmed = message.trimEnd('\n')
        if (trimmed.isEmpty()) return
        android.util.Log.i("dirtyfrag", trimmed)
        _lines.value = (_lines.value + trimmed).takeLast(MAX)
    }

    fun clear() {
        _lines.value = emptyList()
    }

    fun text(): String = _lines.value.joinToString("\n")
}
