package com.discocock.launcher.util

import com.discocock.launcher.net.Http
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/** Network download + archive extraction helpers. */
object NetFiles {

    fun download(
        url: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        dest.parentFile?.mkdirs()
        val request = Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
            val body = response.body ?: throw IOException("Boş gövde: $url")
            val total = body.contentLength()
            var downloaded = 0L
            body.byteStream().use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        }
    }

    /** Extracts a .zip, refusing entries that would escape the target dir. */
    fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(targetDir, entry.name)
                if (!out.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw IOException("Zip girişi hedef dışına çıkıyor: ${entry.name}")
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zis.copyTo(it) }
                }
                entry = zis.nextEntry
            }
        }
    }
}
