package com.discocock.launcher.game

import android.content.Context
import com.discocock.launcher.download.DownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * App-scoped download manager: long installs keep running while the user
 * navigates between screens or the screen turns off (via [DownloadService]),
 * and every download can be stopped on demand.
 */
class DownloadManager(
    private val context: Context,
    private val installer: GameInstaller,
    private val runtimes: RuntimeManager,
    private val mrpackInstaller: MrpackInstaller,
) {

    enum class Kind { VERSION, RUNTIME, MODPACK }

    data class ActiveDownload(
        val id: String,
        val title: String,
        val kind: Kind,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val meta = ConcurrentHashMap<String, ActiveDownload>()

    private val _active = MutableStateFlow<List<ActiveDownload>>(emptyList())
    val active: StateFlow<List<ActiveDownload>> = _active.asStateFlow()

    /** Installs a vanilla version in the background (client jar, libraries, assets). */
    fun installVersion(versionId: String, versionUrl: String? = null) {
        val id = "version:$versionId"
        start(id, ActiveDownload(id, "Sürüm: $versionId", Kind.VERSION)) {
            installer.installVanilla(versionId, versionUrl, includeAssets = true)
        }
    }

    /** Installs a JRE package in the background. */
    fun installRuntime(entry: RuntimeCatalogEntry) {
        val id = "runtime:${entry.name}"
        start(id, ActiveDownload(id, "Runtime: ${entry.name}", Kind.RUNTIME)) {
            runtimes.install(entry)
        }
    }

    /** Ensures a JRE exists, auto-picking the device-matched package. */
    fun ensureRuntime(preferredName: String = "") {
        val id = "runtime:auto"
        start(id, ActiveDownload(id, "Java runtime", Kind.RUNTIME)) {
            runtimes.ensureRuntimeInstalled(preferredName)
        }
    }

    /** Installs a downloaded .mrpack into the instance dir, in the background. */
    fun installModpack(mrpack: File, instanceDir: File, instanceName: String) {
        val id = "modpack:${mrpack.nameWithoutExtension}"
        start(id, ActiveDownload(id, "Mod paketi → $instanceName", Kind.MODPACK)) {
            mrpackInstaller.install(mrpack, instanceDir)
        }
    }

    /** Stops a single download; partial files are cleaned up by the download layer. */
    fun cancel(id: String) {
        jobs[id]?.cancel()
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
    }

    fun isActive(id: String): Boolean = jobs.containsKey(id)

    private fun start(id: String, info: ActiveDownload, block: suspend () -> Unit) {
        if (jobs.containsKey(id)) return
        val wasEmpty = jobs.isEmpty()
        val job = scope.launch {
            try {
                block()
            } finally {
                jobs.remove(id)
                meta.remove(id)
                refresh()
            }
        }
        jobs[id] = job
        meta[id] = info
        refresh()
        if (wasEmpty) {
            // Ekran kapalıyken de inmeye devam etmesi için ön plan servisi.
            runCatching { DownloadService.start(context) }
        }
    }

    private fun refresh() {
        _active.value = jobs.keys.mapNotNull { meta[it] }
    }
}
