package com.discocock.launcher.net

import com.discocock.launcher.data.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class DeviceCodeStart(
    val userCode: String,
    val verificationUrl: String,
    val deviceCode: String,
    val intervalMs: Long,
)

sealed interface DevicePollResult {
    data object Pending : DevicePollResult
    data object Expired : DevicePollResult
    data class Success(val accessToken: String, val refreshToken: String) : DevicePollResult
    data class Error(val message: String) : DevicePollResult
}

/**
 * Microsoft account login via OAuth device-code flow:
 * MSA device code -> token -> Xbox Live (XBL) -> XSTS -> Minecraft services.
 * Pure HTTP, no WebView needed — friendly on phones.
 */
class MicrosoftAuth {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun startDeviceCode(clientId: String): DeviceCodeStart = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("client_id", clientId)
            .add("scope", "XboxLive.signin offline_access")
            .build()
        val obj = parseJson(postForm(MSA_DEVICE_CODE_URL, form))
        DeviceCodeStart(
            userCode = obj.str("user_code"),
            verificationUrl = obj.str("verification_uri"),
            deviceCode = obj.str("device_code"),
            intervalMs = (obj["interval"]?.jsonPrimitive?.content?.toLongOrNull() ?: 5L) * 1000L,
        )
    }

    suspend fun pollDeviceCode(clientId: String, deviceCode: String): DevicePollResult =
        withContext(Dispatchers.IO) {
            val form = FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .add("client_id", clientId)
                .add("device_code", deviceCode)
                .build()
            val body = postForm(MSA_TOKEN_URL, form)
            val obj = json.parseToJsonElement(body).jsonObject
            val error = obj["error"]?.jsonPrimitive?.content
            when (error) {
                null -> DevicePollResult.Success(
                    accessToken = obj.str("access_token"),
                    refreshToken = obj.str("refresh_token"),
                )
                "authorization_pending" -> DevicePollResult.Pending
                "expired_token", "authorization_expired" -> DevicePollResult.Expired
                else -> DevicePollResult.Error(obj.str("error_description").ifBlank { error })
            }
        }

    /** XBL -> XSTS -> Minecraft -> profile. Returns a ready Microsoft account. */
    suspend fun completeLogin(accessToken: String, refreshToken: String): Account =
        withContext(Dispatchers.IO) {
            // 1. Xbox Live user token
            val xblBody = buildJsonObject {
                put("RpsTicket", "d=$accessToken")
                put("Properties", buildJsonObject {
                    put("AuthMethod", "RPS")
                    put("SiteName", "user.auth.xboxlive.com")
                    put("RpsTicket", "d=$accessToken")
                })
            }.toString()
            val xbl = parseJson(postJson(XBL_AUTH_URL, xblBody))
            val xblToken = xbl.str("Token")
            val xblUhs = xbl["DisplayClaims"]?.jsonObject?.get("xui")?.jsonArray
                ?.firstOrNull()?.jsonObject?.str("uhs")
                ?: throw IOException("XBL yanıtı geçersiz (uhs yok)")

            // 2. XSTS token
            val xstsBody = buildJsonObject {
                put("Properties", buildJsonObject {
                    put("SandboxId", "RETAIL")
                    put("UserTokens", JsonArray(listOf(JsonPrimitive(xblToken))))
                })
            }.toString()
            val xsts = parseJson(postJson(XSTS_AUTH_URL, xstsBody))
            val xerr = xsts["XErr"]?.jsonPrimitive?.content?.toLongOrNull()
            if (xerr != null) {
                throw IOException(
                    when (xerr) {
                        2148916233L -> "Bu Microsoft hesabında Xbox profili yok"
                        2148916238L -> "Hesap çocuk hesabı; izin gerekli (XErr=2148916238)"
                        else -> "Xbox girişi başarısız (XErr=$xerr)"
                    },
                )
            }
            val xstsToken = xsts.str("Token")
            val uhs = xsts["DisplayClaims"]?.jsonObject?.get("xui")?.jsonArray
                ?.firstOrNull()?.jsonObject?.str("uhs") ?: xblUhs

            // 3. Minecraft services token
            val mcBody = buildJsonObject {
                put("identityToken", "XBL3.0 x=$uhs;$xstsToken")
            }.toString()
            val mc = parseJson(postJson(MC_LOGIN_URL, mcBody))
            val mcToken = mc.str("access_token")

            // 4. Profile
            val profile = parseJson(getJsonBearer(MC_PROFILE_URL, mcToken))
            val mcUuid = profile.str("id")
            val mcName = profile.str("name")
            if (mcUuid.isBlank() || mcName.isBlank()) {
                throw IOException("Minecraft profili alınamadı (oyun satın alınmamış olabilir)")
            }

            Account(
                id = "microsoft:$mcUuid",
                type = Account.TYPE_MICROSOFT,
                username = mcName,
                uuid = mcUuid,
                accessToken = mcToken,
                refreshToken = refreshToken,
                expiresAt = System.currentTimeMillis(),
            )
        }

    private fun parseJson(body: String) = json.parseToJsonElement(body).jsonObject

    private fun JsonObject.str(key: String): String =
        (this[key] as? JsonPrimitive)?.content ?: ""

    private fun postForm(url: String, form: FormBody): String {
        val request = Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).post(form).build()
        Http.client.newCall(request).execute().use { response ->
            return response.body?.string()
                ?: throw IOException("Boş yanıt: $url")
        }
    }

    private fun postJson(url: String, jsonBody: String): String {
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).post(body).build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
            return response.body?.string() ?: throw IOException("Boş yanıt: $url")
        }
    }

    private fun getJsonBearer(url: String, token: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Http.USER_AGENT)
            .header("Authorization", "Bearer $token")
            .build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
            return response.body?.string() ?: throw IOException("Boş yanıt: $url")
        }
    }

    companion object {
        const val MSA_DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
        const val MSA_TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
        const val XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
        const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
        const val MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
        const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"
    }
}
