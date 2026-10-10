package com.discocock.launcher.util

import com.discocock.launcher.net.Http
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/** Network download + archive extraction helpers. */
object NetFiles {

    /**
     * Downloads to [dest] with progress. Cancellation-aware: cancelling the
     * calling coroutine aborts the in-flight HTTP call, deletes the partial
     * file and rethrows so callers can surface "stopped" state.
     */
    suspend fun download(
        url: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): Unit = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        val request = Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build()
        val call = Http.client.newCall(request)
        // Abort the HTTP call when the calling job is cancelled (also covers
        // reads blocked on a stalled connection).
        val handle = coroutineContext[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
                val body = response.body ?: throw IOException("Boş gövde: $url")
                val total = body.contentLength()
                var downloaded = 0L
                body.byteStream().use { input ->
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (!isActive) throw CancellationException("İndirme durduruldu")
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Partial files must not be mistaken for complete downloads later.
            dest.delete()
            throw e
        } finally {
            handle?.dispose()
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
