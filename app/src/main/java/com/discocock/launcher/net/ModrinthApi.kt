package com.discocock.launcher.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

data class ModrinthProject(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val downloads: Long,
    val iconUrl: String,
)

data class ModrinthVersionFile(val url: String, val filename: String, val primary: Boolean)

data class ModrinthVersion(
    val id: String,
    val name: String,
    val versionNumber: String,
    val gameVersions: List<String>,
    val files: List<ModrinthVersionFile>,
)

/**
 * Modrinth public API — no key needed, matching the desktop launcher's
 * store philosophy (CurseForge stays key-based, Modrinth is open).
 */
class ModrinthApi {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchModpacks(query: String, limit: Int = 20): List<ModrinthProject> =
        withContext(Dispatchers.IO) {
            val facets = URLEncoder.encode("""[["project_type:modpack"]]""", "UTF-8")
            val q = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.modrinth.com/v2/search?query=$q&limit=$limit&facets=$facets"
            val body = get(url)
            val hits = json.parseToJsonElement(body).jsonObject["hits"]?.jsonArray ?: return@withContext emptyList()
            hits.mapNotNull { element ->
                val obj = element.jsonObject
                val id = obj["project_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                ModrinthProject(
                    id = id,
                    slug = obj["slug"]?.jsonPrimitive?.content ?: id,
                    title = obj["title"]?.jsonPrimitive?.content ?: id,
                    description = obj["description"]?.jsonPrimitive?.content ?: "",
                    downloads = obj["downloads"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    iconUrl = obj["icon_url"]?.jsonPrimitive?.content ?: "",
                )
            }
        }

    suspend fun versions(projectId: String): List<ModrinthVersion> = withContext(Dispatchers.IO) {
        val body = get("https://api.modrinth.com/v2/project/$projectId/version")
        val list = json.parseToJsonElement(body).jsonArray
        list.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            ModrinthVersion(
                id = id,
                name = obj["name"]?.jsonPrimitive?.content ?: id,
                versionNumber = obj["version_number"]?.jsonPrimitive?.content ?: "",
                gameVersions = obj["game_versions"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.content } ?: emptyList(),
                files = obj["files"]?.jsonArray?.mapNotNull { fileElement ->
                    val fileObj = fileElement.jsonObject
                    val fileUrl = fileObj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    ModrinthVersionFile(
                        url = fileUrl,
                        filename = fileObj["filename"]?.jsonPrimitive?.content ?: "modpack",
                        primary = fileObj["primary"]?.jsonPrimitive?.content == "true",
                    )
                } ?: emptyList(),
            )
        }
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Http.USER_AGENT)
            .build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
            return response.body?.string() ?: throw IOException("Boş yanıt: $url")
        }
    }
}
