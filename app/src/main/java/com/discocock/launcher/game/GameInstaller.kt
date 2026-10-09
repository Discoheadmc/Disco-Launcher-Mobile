package com.discocock.launcher.game

import com.discocock.launcher.net.MojangApi
import com.discocock.launcher.util.JsonFiles
import com.discocock.launcher.util.LauncherPaths
import com.discocock.launcher.util.NetFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException

/** Metadata stored next to an installed version, consumed by the launch flow. */
@Serializable
data class InstalledVersionInfo(
    val versionId: String,
    val mainClass: String,
    val assetIndexId: String,
)

class GameInstaller(private val paths: LauncherPaths, private val mojang: MojangApi) {

    sealed interface Progress {
        data object Idle : Progress
        data class Working(val stage: String, val current: Int, val total: Int) : Progress
        data class Done(val message: String) : Progress
        data class Failed(val message: String) : Progress
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _progress = MutableStateFlow<Progress>(Progress.Idle)
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    /**
     * Installs a vanilla version: client jar, libraries and (optionally) assets.
     * Inherits-from (modded) versions come with the mod-loader milestone.
     */
    suspend fun installVanilla(
        versionId: String,
        versionUrl: String? = null,
        includeAssets: Boolean = true,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = versionUrl
                ?: mojang.versionManifest().firstOrNull { it.id == versionId }?.url
                ?: throw IOException("$versionId manifestte bulunamadı")

            _progress.value = Progress.Working("Sürüm bilgisi alınıyor", 0, 1)
            val versionJson = mojang.versionJson(url)
            val versionDir = File(paths.versionsDir, versionId)

            // 1. Client jar
            val clientUrl = versionJson["downloads"]?.jsonObject?.get("client")
                ?.jsonObject?.get("url")?.jsonPrimitive?.content
                ?: throw IOException("Sürüm JSON'unda client jar bağlantısı yok")
            val clientJar = File(versionDir, "$versionId.jar")
            NetFiles.download(clientUrl, clientJar) { _, _ ->
                _progress.value = Progress.Working("Client jar indiriliyor", 1, 3)
            }

            // 2. Libraries (artifact jars only; natives land with the runtime milestone)
            val libraries = versionJson["libraries"]?.jsonArray ?: JsonArray(emptyList())
            val applicable = libraries.filter { allowedOnLinux(it.jsonObject["rules"] as? JsonArray) }
            var libIndex = 0
            for (lib in applicable) {
                libIndex++
                _progress.value = Progress.Working("Kütüphaneler", libIndex, applicable.size)
                val artifact = lib.jsonObject["downloads"]?.jsonObject?.get("artifact")?.jsonObject
                val libUrl = artifact?.get("url")?.jsonPrimitive?.content
                val libPath = artifact?.get("path")?.jsonPrimitive?.content
                if (libUrl.isNullOrBlank() || libPath.isNullOrBlank()) continue
                val dest = File(paths.librariesDir, libPath)
                if (dest.exists() && dest.length() > 0L) continue
                NetFiles.download(libUrl, dest)
            }

            // 3. Assets (hash-addressed, from resources.download.minecraft.net)
            val mainClass = versionJson["mainClass"]?.jsonPrimitive?.content
                ?: "net.minecraft.client.main.Main"
            val assetIndexId = versionJson["assetIndex"]?.jsonObject
                ?.get("id")?.jsonPrimitive?.content ?: "legacy"
            if (includeAssets) {
                val assetIndexUrl = versionJson["assetIndex"]?.jsonObject
                    ?.get("url")?.jsonPrimitive?.content
                if (!assetIndexUrl.isNullOrBlank()) {
                    val indexFile = File(paths.assetsDir, "indexes/$assetIndexId.json")
                    if (!indexFile.exists() || indexFile.length() == 0L) {
                        NetFiles.download(assetIndexUrl, indexFile)
                    }
                    val objects = json.parseToJsonElement(indexFile.readText())
                        .jsonObject["objects"]?.jsonObject
                    val entries = objects?.values?.toList() ?: emptyList()
                    var assetIndex = 0
                    for (entry in entries) {
                        assetIndex++
                        val hash = entry.jsonObject["hash"]?.jsonPrimitive?.content ?: continue
                        val dest = File(paths.assetsDir, "objects/${hash.take(2)}/$hash")
                        if (dest.exists() && dest.length() > 0L) continue
                        if (assetIndex % 25 == 0 || assetIndex == entries.size) {
                            _progress.value = Progress.Working("Varlıklar", assetIndex, entries.size)
                        }
                        NetFiles.download("https://resources.download.minecraft.net/${hash.take(2)}/$hash", dest)
                    }
                }
            }

            JsonFiles.write(
                File(versionDir, "disco-version.json"),
                InstalledVersionInfo(versionId, mainClass, assetIndexId),
            )
            _progress.value = Progress.Done("$versionId kuruldu")
            Result.success(clientJar)
        } catch (e: Exception) {
            _progress.value = Progress.Failed(e.message ?: "Bilinmeyen hata")
            Result.failure(e)
        }
    }

    suspend fun installedInfo(versionId: String): InstalledVersionInfo? = withContext(Dispatchers.IO) {
        val file = File(File(paths.versionsDir, versionId), "disco-version.json")
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString<InstalledVersionInfo>(file.readText()) }.getOrNull()
    }

    /**
     * Simplified vanilla rule evaluation for the "linux" platform: unconditional
     * and linux rules apply (last match wins), feature-gated rules are skipped.
     */
    private fun allowedOnLinux(rules: JsonArray?): Boolean {
        if (rules == null || rules.isEmpty()) return true
        var allowed = false
        for (rule in rules) {
            val obj = rule.jsonObject
            val action = obj["action"]?.jsonPrimitive?.content ?: continue
            if (obj["features"] != null) continue
            val os = (obj["os"] as? JsonObject)?.get("name")?.jsonPrimitive?.content
            if (os == null || os == "linux") {
                allowed = action == "allow"
            }
        }
        return allowed
    }
}
