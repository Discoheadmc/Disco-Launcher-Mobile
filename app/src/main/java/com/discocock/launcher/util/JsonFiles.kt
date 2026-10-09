package com.discocock.launcher.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

/** Small JSON-file-backed persistence helpers shared by the repositories. */
object JsonFiles {

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    inline fun <reified T> read(file: File, default: T): T {
        if (!file.exists()) return default
        return runCatching {
            json.decodeFromString(kotlinx.serialization.serializer<T>(), file.readText())
        }.getOrNull() ?: default
    }

    inline fun <reified T> write(file: File, value: T) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(kotlinx.serialization.serializer<T>(), value))
    }
}
