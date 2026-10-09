package com.discocock.launcher.game

import com.discocock.launcher.util.JsonFiles
import com.discocock.launcher.util.LauncherPaths
import com.discocock.launcher.util.NetFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@Serializable
data class InstalledRuntime(
    val name: String,
    val dirName: String,
    /** Path of the java binary, relative to the runtime dir. */
    val javaBinary: String,
    val version: String = "",
)

@Serializable
data class RuntimeCatalogEntry(
    val name: String,
    val url: String,
    val javaBinary: String,
    val version: String = "",
)

@Serializable
private data class RuntimeStore(
    val runtimes: List<InstalledRuntime> = emptyList(),
)

/**
 * Downloads, extracts and manages Java runtime packages (.zip or .tar.xz,
 * e.g. PojavLauncherTeam's openjdk builds for Android). The launch flow boots
 * the java binary from here via a plain process (Phase 2 moves to JNI).
 */
class RuntimeManager(private val paths: LauncherPaths) {

    private val mutex = Mutex()
    private val storeFile get() = File(paths.root, "runtimes.json")

    private val _runtimes = MutableStateFlow<List<InstalledRuntime>>(emptyList())
    val runtimes: StateFlow<List<InstalledRuntime>> = _runtimes.asStateFlow()

    suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                _runtimes.value = JsonFiles.read(storeFile, RuntimeStore()).runtimes
            }
        }
    }

    suspend fun install(
        entry: RuntimeCatalogEntry,
        onProgress: (stage: String, downloaded: Long, total: Long) -> Unit = { _, _, _ -> },
    ): Result<InstalledRuntime> = withContext(Dispatchers.IO) {
        try {
            val archive = File(paths.runtimesDir, "tmp-${System.currentTimeMillis()}.pkg")
            NetFiles.download(entry.url, archive) { downloaded, total ->
                onProgress("İndiriliyor", downloaded, total)
            }
            val target = File(paths.runtimesDir, entry.dirName)
            target.deleteRecursively()
            target.mkdirs()
            onProgress("Ayıklanıyor", 0, 1)
            when {
                entry.url.endsWith(".zip", ignoreCase = true) ->
                    NetFiles.unzip(archive, target)
                entry.url.endsWith(".tar.xz", ignoreCase = true) ||
                    entry.url.endsWith(".txz", ignoreCase = true) ->
                    untarXz(archive, target)
                else -> throw IOException("Desteklenmeyen arşiv türü: ${entry.url.substringAfterLast('/')}")
            }
            archive.delete()

            val runtime = InstalledRuntime(
                name = entry.name,
                dirName = entry.dirName,
                javaBinary = entry.javaBinary,
                version = entry.version,
            )
            mutex.withLock {
                val current = JsonFiles.read(storeFile, RuntimeStore())
                val next = RuntimeStore(current.runtimes.filterNot { it.name == entry.name } + runtime)
                JsonFiles.write(storeFile, next)
                _runtimes.value = next.runtimes
            }
            Result.success(runtime)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun remove(name: String) {
        withContext(Dispatchers.IO) {
            File(paths.runtimesDir, name).deleteRecursively()
            mutex.withLock {
                val current = JsonFiles.read(storeFile, RuntimeStore())
                val next = RuntimeStore(current.runtimes.filterNot { it.name == name })
                JsonFiles.write(storeFile, next)
                _runtimes.value = next.runtimes
            }
        }
    }

    /** The first installed runtime is used for launches (per-version selection comes later). */
    fun activeRuntime(): InstalledRuntime? = _runtimes.value.firstOrNull()

    fun javaBinaryFile(runtime: InstalledRuntime): File {
        val file = File(File(paths.runtimesDir, runtime.dirName), runtime.javaBinary)
        if (file.exists()) file.setExecutable(true)
        return file
    }

    private fun untarXz(archive: File, target: File) {
        TarArchiveInputStream(
            XZCompressorInputStream(BufferedInputStream(FileInputStream(archive))),
        ).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                val out = File(target, entry.name)
                if (!out.canonicalPath.startsWith(target.canonicalPath)) {
                    throw IOException("Arşiv girişi hedef dışına çıkıyor: ${entry.name}")
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { tar.copyTo(it) }
                    // Preserve the owner-executable bit (java, javac, ...)
                    if (entry.mode and 256 != 0) out.setExecutable(true)
                }
                entry = tar.nextTarEntry
            }
        }
    }
}
