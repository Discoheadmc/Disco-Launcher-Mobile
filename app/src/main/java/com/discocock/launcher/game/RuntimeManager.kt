package com.discocock.launcher.game

import android.os.Build
import com.discocock.launcher.util.JsonFiles
import com.discocock.launcher.util.LauncherPaths
import com.discocock.launcher.util.NetFiles
import kotlinx.coroutines.CancellationException
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

/**
 * Shared runtime catalog — PojavLauncherTeam's android-openjdk JRE builds
 * (verified release assets; the tarballs extract to `bin/java`, `lib/…`).
 * Custom packages can override the java path via the custom URL flow.
 */
val RUNTIME_CATALOG: List<RuntimeCatalogEntry> = listOf(
    RuntimeCatalogEntry(
        name = "jre17-arm64",
        version = "JRE 17 · android arm64",
        url = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre17-ec28559/jre17-arm64-20210825-release.tar.xz",
        javaBinary = "bin/java",
    ),
    RuntimeCatalogEntry(
        name = "jre17-arm",
        version = "JRE 17 · android arm (32-bit)",
        url = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre17-ec28559/jre17-arm-20210914-release.tar.xz",
        javaBinary = "bin/java",
    ),
    RuntimeCatalogEntry(
        name = "jre17-x86_64",
        version = "JRE 17 · android x86_64",
        url = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre17-ec28559/jre17-x86_64-20210825-release.tar.xz",
        javaBinary = "bin/java",
    ),
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

    sealed interface RuntimeProgress {
        data object Idle : RuntimeProgress
        data class Working(val stage: String, val downloaded: Long, val total: Long) : RuntimeProgress
        data class Done(val message: String) : RuntimeProgress
        data class Failed(val message: String) : RuntimeProgress
    }

    private val mutex = Mutex()
    private val storeFile get() = File(paths.root, "runtimes.json")

    private val _runtimes = MutableStateFlow<List<InstalledRuntime>>(emptyList())
    val runtimes: StateFlow<List<InstalledRuntime>> = _runtimes.asStateFlow()

    private val _progress = MutableStateFlow<RuntimeProgress>(RuntimeProgress.Idle)
    val progress: StateFlow<RuntimeProgress> = _progress.asStateFlow()

    suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val store = JsonFiles.read(storeFile, RuntimeStore())
                // Drop entries whose java binary is missing (e.g. records from an
                // old layout/location) so they get re-installed automatically.
                val valid = store.runtimes.filter { javaBinaryFile(it).exists() }
                if (valid.size != store.runtimes.size) {
                    JsonFiles.write(storeFile, RuntimeStore(valid))
                }
                _runtimes.value = valid
            }
        }
    }

    suspend fun install(
        entry: RuntimeCatalogEntry,
        onProgress: (stage: String, downloaded: Long, total: Long) -> Unit = { _, _, _ -> },
    ): Result<InstalledRuntime> = withContext(Dispatchers.IO) {
        try {
            _progress.value = RuntimeProgress.Working("İndiriliyor", 0, 0)
            val archive = File(paths.runtimesDir, "tmp-${System.currentTimeMillis()}.pkg")
            NetFiles.download(entry.url, archive) { downloaded, total ->
                onProgress("İndiriliyor", downloaded, total)
                _progress.value = RuntimeProgress.Working("İndiriliyor", downloaded, total)
            }
            val target = File(paths.runtimesDir, entry.name)
            target.deleteRecursively()
            target.mkdirs()
            _progress.value = RuntimeProgress.Working("Ayıklanıyor", 0, 1)
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
                dirName = entry.name,
                javaBinary = entry.javaBinary,
                version = entry.version,
            )
            mutex.withLock {
                val current = JsonFiles.read(storeFile, RuntimeStore())
                val next = RuntimeStore(current.runtimes.filterNot { it.name == entry.name } + runtime)
                JsonFiles.write(storeFile, next)
                _runtimes.value = next.runtimes
            }
            _progress.value = RuntimeProgress.Done("${entry.name} kuruldu")
            Result.success(runtime)
        } catch (e: Exception) {
            _progress.value = RuntimeProgress.Failed(
                if (e is CancellationException) "İndirme durduruldu" else (e.message ?: "Runtime kurulumu başarısız"),
            )
            Result.failure(e)
        }
    }

    /**
     * Makes sure a JRE is installed: returns the existing/preferred runtime,
     * or automatically downloads the device-recommended package when none is
     * present. Called by the instance-creation flow.
     */
    suspend fun ensureRuntimeInstalled(preferredName: String = ""): Result<InstalledRuntime> =
        withContext(Dispatchers.IO) {
            val existing = activeRuntime(preferredName)
            if (existing != null) return@withContext Result.success(existing)
            val entry = recommendedEntryForDevice()
                ?: return@withContext Result.failure(
                    IllegalStateException("Cihaz mimarisi için otomatik JRE paketi bulunamadı — Runtime ekranından elle kur"),
                )
            install(entry)
        }

    /** Picks the catalog entry matching the device's primary ABI. */
    fun recommendedEntryForDevice(): RuntimeCatalogEntry? {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return null
        return when {
            abi.contains("arm64") -> RUNTIME_CATALOG.firstOrNull { it.name == "jre17-arm64" }
            abi.contains("armeabi") -> RUNTIME_CATALOG.firstOrNull { it.name == "jre17-arm" }
            abi.contains("x86_64") || abi.contains("x86") ->
                RUNTIME_CATALOG.firstOrNull { it.name == "jre17-x86_64" }
            else -> null
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

    /**
     * The runtime used for launches: the preferred one when set and installed,
     * otherwise the first installed.
     */
    fun activeRuntime(preferredName: String = ""): InstalledRuntime? {
        val list = _runtimes.value
        if (preferredName.isNotBlank()) {
            list.firstOrNull { it.name == preferredName }?.let { return it }
        }
        return list.firstOrNull()
    }

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
                when {
                    entry.isDirectory -> out.mkdirs()
                    // Symbolic links: recreate them (JRE trees rely on them
                    // for lib/server, lib/jli, ...); fall back to a copy.
                    entry.isSymbolicLink -> {
                        val linkTarget = entry.linkName
                        out.parentFile?.mkdirs()
                        if (out.exists()) out.delete()
                        try {
                            java.nio.file.Files.createSymbolicLink(
                                out.toPath(),
                                java.nio.file.Paths.get(linkTarget),
                            )
                        } catch (e: Exception) {
                            val resolved = File(out.parentFile, linkTarget)
                            if (resolved.exists()) resolved.copyTo(out, overwrite = true)
                        }
                    }
                    else -> {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { tar.copyTo(it) }
                    }
                }
                entry = tar.nextTarEntry
            }
        }
        // The java launcher and its helpers must be executable.
        File(target, "bin").takeIf { it.isDirectory }?.listFiles()?.forEach {
            if (it.isFile) it.setExecutable(true, false)
        }
    }
}
