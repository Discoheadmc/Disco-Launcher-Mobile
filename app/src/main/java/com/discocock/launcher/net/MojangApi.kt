package com.discocock.launcher.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.IOException

data class ManifestVersion(
    val id: String,
    val type: String,
    val url: String,
    val releaseTime: String,
)

/** Mojang piston-meta access: version manifest + per-version JSON. */
class MojangApi {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun versionManifest(): List<ManifestVersion> = withContext(Dispatchers.IO) {
        val body = get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
        val root = json.parseToJsonElement(body).jsonObject
        val versions = root["versions"]?.jsonArray ?: JsonArray(emptyList())
        versions.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            ManifestVersion(
                id = id,
                type = obj["type"]?.jsonPrimitive?.content ?: "release",
                url = obj["url"]?.jsonPrimitive?.content ?: "",
                releaseTime = obj["releaseTime"]?.jsonPrimitive?.content ?: "",
            )
        }
    }

    suspend fun versionJson(versionUrl: String): JsonObject = withContext(Dispatchers.IO) {
        json.parseToJsonElement(get(versionUrl)).jsonObject
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
            return response.body?.string() ?: throw IOException("Boş yanıt: $url")
        }
    }
}
