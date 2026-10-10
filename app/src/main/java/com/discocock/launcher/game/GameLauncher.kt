package com.discocock.launcher.game

import com.discocock.launcher.util.LauncherPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Boots the game as a child process of the extracted JRE (works because
 * targetSdk 28 keeps exec() allowed in app storage; see ROADMAP for the
 * Phase 2 JNI runtime that removes this constraint).
 */
class GameLauncher(@Suppress("unused") private val paths: LauncherPaths) {

    sealed interface LaunchState {
        data object Idle : LaunchState
        data class Running(val log: List<String>) : LaunchState
        data class Exited(val code: Int, val log: List<String>) : LaunchState
        data class Failed(val message: String) : LaunchState
    }

    private val _state = MutableStateFlow<LaunchState>(LaunchState.Idle)
    val state: StateFlow<LaunchState> = _state.asStateFlow()

    private var process: Process? = null
    private val logLines = ArrayDeque<String>()

    suspend fun launch(spec: GameLaunchSpec): LaunchState = withContext(Dispatchers.IO) {
        if (process?.isAlive == true) {
            return@withContext fail("Oyun zaten çalışıyor")
        }
        synchronized(logLines) { logLines.clear() }
        try {
            val proc = ProcessBuilder(spec.command)
                .directory(spec.workingDir)
                .redirectErrorStream(true)
                .apply { environment().putAll(spec.env) }
                .start()
            process = proc
            _state.value = LaunchState.Running(emptyList())

            Thread {
                proc.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        synchronized(logLines) {
                            logLines.addLast(line)
                            while (logLines.size > MAX_LOG_LINES) logLines.removeFirst()
                        }
                        _state.value = LaunchState.Running(logSnapshot())
                    }
                }
                val code = runCatching { proc.exitValue() }.getOrDefault(-1)
                _state.value = LaunchState.Exited(code, logSnapshot())
            }.start()

            LaunchState.Running(emptyList())
        } catch (e: Exception) {
            fail(e.message ?: "Oyun başlatılamadı")
        }
    }

    fun logSnapshot(): List<String> = synchronized(logLines) { logLines.toList() }

    /** Resets the state back to Idle (used to dismiss failure dialogs). */
    fun acknowledge() {
        _state.value = LaunchState.Idle
    }

    fun stop() {
        process?.destroy()
    }

    private fun fail(message: String): LaunchState {
        val state = LaunchState.Failed(message)
        _state.value = state
        return state
    }

    companion object {
        private const val MAX_LOG_LINES = 400
    }
}
