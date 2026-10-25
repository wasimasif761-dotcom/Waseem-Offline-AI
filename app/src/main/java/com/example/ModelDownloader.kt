package com.example

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Float,             // 0.0f .. 1.0f
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedFormatted: String,      // e.g. "4.2 MB/s"
        val statusText: String           // e.g. "Downloading model..."
    ) : DownloadState()
    data class Completed(val file: File, val sizeFormatted: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * High-performance In-App Model Downloader using OkHttp with streaming progress,
 * speed tracking, atomic file rename, and cancel/retry support.
 */
class ModelDownloader(private val context: Context) {

    companion object {
        // Direct HuggingFace links for Qwen quantized on-device inference models
        const val DEFAULT_MODEL_NAME = "qwen_model.bin"

        // Qwen 2.5 1.5B Instruct quantized model (~1.2 GB)
        const val URL_QWEN_1_5B =
            "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task"

        // Qwen 2.5 0.5B Instruct fast mobile model (~350 MB - optimized for 4GB RAM devices)
        const val URL_QWEN_0_5B =
            "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_seq128_q8_ekv1280.task"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var activeDownloadJob: Job? = null

    val targetFile: File
        get() {
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            return File(baseDir, DEFAULT_MODEL_NAME)
        }

    fun isModelDownloaded(): Boolean {
        return targetFile.exists() && targetFile.length() > 10 * 1024 * 1024 // at least 10 MB
    }

    fun getModelFileSizeFormatted(): String {
        if (!targetFile.exists()) return "0 MB"
        val bytes = targetFile.length()
        return formatBytes(bytes)
    }

    fun startDownload(
        scope: CoroutineScope,
        url: String = URL_QWEN_1_5B,
        onSuccess: (() -> Unit)? = null
    ) {
        if (activeDownloadJob?.isActive == true) return

        activeDownloadJob = scope.launch(Dispatchers.IO) {
            val destination = targetFile
            val tempFile = File(destination.parentFile, "${destination.name}.download_tmp")

            try {
                _downloadState.value = DownloadState.Downloading(
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    speedFormatted = "Connecting...",
                    statusText = "Establishing connection to HuggingFace..."
                )

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WaseemOfflineAI/1.0 (Android; OnDeviceLLM)")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("Server returned HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw Exception("Empty response body from server")
                val contentLength = body.contentLength()
                val totalBytes = if (contentLength > 0) contentLength else 1_200_000_000L // fallback estimate ~1.2GB

                if (tempFile.exists()) tempFile.delete()

                var bytesCopied = 0L
                var lastTime = System.currentTimeMillis()
                var bytesSinceLastSample = 0L
                var speedText = "0 MB/s"

                val buffer = ByteArray(64 * 1024)
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                outputStream.use { out ->
                    inputStream.use { input ->
                        var read = input.read(buffer)
                        while (read != -1) {
                            out.write(buffer, 0, read)
                            bytesCopied += read
                            bytesSinceLastSample += read

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastTime
                            if (elapsed >= 500) {
                                val bytesPerSec = (bytesSinceLastSample * 1000) / elapsed
                                speedText = formatSpeed(bytesPerSec)
                                lastTime = now
                                bytesSinceLastSample = 0L

                                val progress = if (totalBytes > 0) {
                                    (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                _downloadState.value = DownloadState.Downloading(
                                    progress = progress,
                                    downloadedBytes = bytesCopied,
                                    totalBytes = totalBytes,
                                    speedFormatted = speedText,
                                    statusText = "${formatBytes(bytesCopied)} / ${formatBytes(totalBytes)}"
                                )
                            }

                            read = input.read(buffer)
                        }
                        out.flush()
                    }
                }

                // Atomic rename
                if (destination.exists()) destination.delete()
                val renamed = tempFile.renameTo(destination)
                if (!renamed) {
                    tempFile.copyTo(destination, overwrite = true)
                    tempFile.delete()
                }

                val finalFormatted = formatBytes(destination.length())
                _downloadState.value = DownloadState.Completed(destination, finalFormatted)

                withContext(Dispatchers.Main) {
                    onSuccess?.invoke()
                }

            } catch (e: CancellationException) {
                if (tempFile.exists()) tempFile.delete()
                _downloadState.value = DownloadState.Idle
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Download failed")
            }
        }
    }

    fun cancelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        _downloadState.value = DownloadState.Idle
    }

    fun deleteModel(): Boolean {
        return try {
            if (targetFile.exists()) {
                val deleted = targetFile.delete()
                _downloadState.value = DownloadState.Idle
                deleted
            } else true
        } catch (e: Exception) {
            false
        }
    }

    private fun formatBytes(bytes: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
            bytes >= 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
            bytes >= 1024 -> "${df.format(bytes / 1024.0)} KB"
            else -> "$bytes B"
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        val df = DecimalFormat("#.#")
        return when {
            bytesPerSec >= 1024 * 1024 -> "${df.format(bytesPerSec / (1024.0 * 1024.0))} MB/s"
            bytesPerSec >= 1024 -> "${df.format(bytesPerSec / 1024.0)} KB/s"
            else -> "$bytesPerSec B/s"
        }
    }
}
