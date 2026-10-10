package com.discocock.launcher.game

import com.discocock.launcher.util.NetFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Installs .mrpack modpacks: resolves the files listed in modrinth.index.json
 * and places overrides into the instance directory (basic store install).
 */
class MrpackInstaller {

    sealed interface MrpackProgress {
        data object Idle : MrpackProgress
        data class Working(val stage: String, val current: Int, val total: Int) : MrpackProgress
        data class Done(val message: String) : MrpackProgress
        data class Failed(val message: String) : MrpackProgress
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _progress = MutableStateFlow<MrpackProgress>(MrpackProgress.Idle)
    val progress: StateFlow<MrpackProgress> = _progress.asStateFlow()

    suspend fun install(
        mrpack: File,
        instanceDir: File,
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            ZipFile(mrpack).use { zip ->
                val indexEntry = zip.getEntry("modrinth.index.json")
                    ?: throw IOException("modrinth.index.json yok — geçerli bir .mrpack değil")
                val index = json.parseToJsonElement(
                    zip.getInputStream(indexEntry).bufferedReader().readText(),
                ).jsonObject

                val downloadFiles = index["files"]?.jsonArray ?: JsonArray(emptyList())
                val overrideEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.startsWith("overrides/") }
                    .toList()
                val total = overrideEntries.size + downloadFiles.size
                var step = 0

                // 1. overrides/ -> instance dir
                for (entry in overrideEntries) {
                    step++
                    if (step % 10 == 0 || step == total) {
                        _progress.value = MrpackProgress.Working("Overrides", step, total)
                    }
                    val relative = entry.name.removePrefix("overrides/")
                    val out = File(instanceDir, relative)
                    if (!out.canonicalPath.startsWith(instanceDir.canonicalPath)) {
                        throw IOException("Override hedef dışına çıkıyor: ${entry.name}")
                    }
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.getInputStream(entry).copyTo(it) }
                }

                // 2. files[] -> downloads by path
                for (fileElement in downloadFiles) {
                    step++
                    val obj = fileElement.jsonObject
                    val path = obj["path"]?.jsonPrimitive?.content ?: continue
                    val url = obj["downloads"]?.jsonArray
                        ?.firstOrNull()?.jsonPrimitive?.content
                    if (url.isNullOrBlank()) continue
                    _progress.value = MrpackProgress.Working("Modlar", step, total)
                    val out = File(instanceDir, path)
                    if (!out.canonicalPath.startsWith(instanceDir.canonicalPath)) {
                        throw IOException("Mod yolu hedef dışına çıkıyor: $path")
                    }
                    out.parentFile?.mkdirs()
                    NetFiles.download(url, out)
                }

                val count = step
                _progress.value = MrpackProgress.Done("$count dosya kuruldu")
                Result.success(count)
            }
        } catch (e: Exception) {
            _progress.value = MrpackProgress.Failed(
                if (e is CancellationException) "Kurulum durduruldu" else (e.message ?: "Kurulum başarısız"),
            )
            Result.failure(e)
        }
    }
}
