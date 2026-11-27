package com.musa.azanmy.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.musa.azanmy.BuildConfig
import com.musa.azanmy.data.AppReleaseInfo
import com.musa.azanmy.service.AppUpdateService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppReleaseInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()

    data class Downloading(
        val releaseInfo: AppReleaseInfo,
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val statusMessage: String = "Memuat turun APK..."
    ) : UpdateDownloadState()

    data class ReadyToInstall(
        val releaseInfo: AppReleaseInfo,
        val apkFilePath: String,
        val autoLaunchConsumed: Boolean = false
    ) : UpdateDownloadState()

    data class Error(
        val message: String,
        val releaseInfo: AppReleaseInfo? = null
    ) : UpdateDownloadState()
}

object AppUpdater {

    private const val VERSION_JSON_URL =
        "https://raw.githubusercontent.com/MuzRC/AzanMY/main/version.json"

    private const val MAX_RETRY_ATTEMPTS = 4

    private val checkClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _availableRelease = MutableStateFlow<AppReleaseInfo?>(null)
    val availableRelease: StateFlow<AppReleaseInfo?> = _availableRelease.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    @Volatile
    private var activeDownloadCall: Call? = null

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val urlWithBust = "$VERSION_JSON_URL?t=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(urlWithBust)
                .header("User-Agent", "AzanMY-Updater/${BuildConfig.VERSION_NAME}")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            checkClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error(
                        "Pelayan GitHub memulangkan ralat HTTP ${response.code}"
                    )
                }
                val body = response.body?.string()
                    ?: return@withContext UpdateCheckResult.Error("Maklumat versi kosong.")

                val json = JSONObject(body)
                val remoteCode = json.optInt("versionCode", 0)
                val remoteName = json.optString("versionName", "1.0.0")
                val apkUrl = json.optString("apkUrl", "")
                val releaseNotes = json.optString("releaseNotes", "Kemas kini penambahbaikan sistem.")
                val updatedAt = json.optString("updatedAt", "")

                if (remoteCode > BuildConfig.VERSION_CODE && apkUrl.isNotBlank()) {
                    val info = AppReleaseInfo(
                        versionCode = remoteCode,
                        versionName = remoteName,
                        apkUrl = apkUrl,
                        releaseNotes = releaseNotes,
                        updatedAt = updatedAt
                    )
                    _availableRelease.value = info
                    UpdateCheckResult.UpdateAvailable(info)
                } else {
                    _availableRelease.value = null
                    UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "Gagal menyemak kemas kini.")
        }
    }

    fun startBackgroundDownload(context: Context, releaseInfo: AppReleaseInfo) {
        if (_downloadState.value is UpdateDownloadState.Downloading) return
        _availableRelease.value = releaseInfo
        _downloadState.value = UpdateDownloadState.Downloading(
            releaseInfo = releaseInfo,
            progress = 0,
            downloadedBytes = 0L,
            totalBytes = -1L,
            statusMessage = "Menyambung ke pelayan GitHub..."
        )
        AppUpdateService.startDownload(context, releaseInfo)
    }

    fun cancelBackgroundDownload(context: Context) {
        AppUpdateService.cancelDownload(context)
    }

    fun abortActiveNetworkCall() {
        try {
            activeDownloadCall?.cancel()
        } catch (_: Exception) {
        } finally {
            activeDownloadCall = null
        }
    }

    fun markCancelled() {
        _downloadState.value = UpdateDownloadState.Idle
    }

    fun markError(message: String, releaseInfo: AppReleaseInfo? = null) {
        _downloadState.value = UpdateDownloadState.Error(
            message = message,
            releaseInfo = releaseInfo ?: _availableRelease.value
        )
    }

    fun markReadyToInstall(releaseInfo: AppReleaseInfo, apkFilePath: String) {
        _availableRelease.value = releaseInfo
        _downloadState.value = UpdateDownloadState.ReadyToInstall(
            releaseInfo = releaseInfo,
            apkFilePath = apkFilePath,
            autoLaunchConsumed = false
        )
    }

    suspend fun downloadApkWithResume(
        context: Context,
        releaseInfo: AppReleaseInfo,
        onProgressUpdate: (progress: Int, downloadedBytes: Long, totalBytes: Long, statusMessage: String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val baseDir = context.getExternalFilesDir(null) ?: context.cacheDir
        val updatesDir = File(baseDir, "updates").apply {
            if (!exists()) mkdirs()
        }

        val safeVersion = releaseInfo.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(updatesDir, "AzanMY-v$safeVersion.apk")
        val partFile = File(updatesDir, "AzanMY-v$safeVersion.apk.part")

        // Buang fail lama selain .part untuk versi semasa
        updatesDir.listFiles()?.forEach { file ->
            if (file.name != partFile.name) {
                file.delete()
            }
        }

        // Buang .part yang terlalu lama (> 30 minit) supaya tidak rosak
        if (partFile.exists()) {
            val ageMs = System.currentTimeMillis() - partFile.lastModified()
            if (ageMs > 30 * 60 * 1000L) {
                partFile.delete()
            }
        }

        var lastException: Exception? = null

        for (attempt in 1..MAX_RETRY_ATTEMPTS) {
            coroutineContext.ensureActive()

            try {
                val existingBytes = if (partFile.exists()) partFile.length() else 0L
                val statusStart = if (attempt == 1) {
                    if (existingBytes > 0L) "Menyambung muat turun..." else "Memuat turun APK..."
                } else {
                    "Menyambung semula muat turun (cubaan $attempt/$MAX_RETRY_ATTEMPTS)..."
                }

                updateDownloadProgress(
                    releaseInfo = releaseInfo,
                    progress = 0,
                    downloadedBytes = existingBytes,
                    totalBytes = -1L,
                    statusMessage = statusStart,
                    callback = onProgressUpdate
                )

                val requestBuilder = Request.Builder()
                    .url(releaseInfo.apkUrl)
                    .header("User-Agent", "AzanMY-Updater/${BuildConfig.VERSION_NAME}")
                    .get()

                if (existingBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                val call = downloadClient.newCall(requestBuilder.build())
                activeDownloadCall = call

                call.execute().use { response ->
                    if (response.code == 416) {
                        partFile.delete()
                        throw IOException("Julat fail tidak sepadan, memulakan semula muat turun.")
                    }

                    if (!response.isSuccessful && response.code != 206) {
                        throw IllegalStateException("Gagal muat turun APK (HTTP ${response.code})")
                    }

                    val body = response.body
                        ?: throw IllegalStateException("Fail APK kosong daripada pelayan.")

                    val isResume = response.code == 206 && existingBytes > 0L
                    val remainingLength = body.contentLength()
                    val totalFromRange = response.header("Content-Range")
                        ?.substringAfterLast("/")
                        ?.toLongOrNull()

                    val totalBytes = when {
                        isResume && totalFromRange != null && totalFromRange > 0L -> totalFromRange
                        isResume && remainingLength > 0L -> existingBytes + remainingLength
                        remainingLength > 0L -> remainingLength
                        else -> -1L
                    }

                    var downloaded = if (isResume) existingBytes else 0L
                    var lastPercent = if (totalBytes > 0L) {
                        ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    updateDownloadProgress(
                        releaseInfo = releaseInfo,
                        progress = lastPercent,
                        downloadedBytes = downloaded,
                        totalBytes = totalBytes,
                        statusMessage = "Memuat turun APK...",
                        callback = onProgressUpdate
                    )

                    body.byteStream().use { input ->
                        FileOutputStream(partFile, isResume).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            var lastEmitBytes = downloaded

                            while (input.read(buffer).also { read = it } != -1) {
                                coroutineContext.ensureActive()
                                output.write(buffer, 0, read)
                                downloaded += read

                                val percent = if (totalBytes > 0L) {
                                    ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                } else {
                                    0
                                }

                                val bytesDelta = downloaded - lastEmitBytes
                                if (percent != lastPercent || bytesDelta >= 128 * 1024L) {
                                    lastPercent = percent
                                    lastEmitBytes = downloaded
                                    updateDownloadProgress(
                                        releaseInfo = releaseInfo,
                                        progress = percent,
                                        downloadedBytes = downloaded,
                                        totalBytes = totalBytes,
                                        statusMessage = "Memuat turun APK...",
                                        callback = onProgressUpdate
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    if (totalBytes > 0L && downloaded < totalBytes) {
                        throw IOException("Sambungan terputus pada $downloaded / $totalBytes bait.")
                    }

                    if (outFile.exists()) {
                        outFile.delete()
                    }
                    if (!partFile.renameTo(outFile)) {
                        partFile.copyTo(outFile, overwrite = true)
                        partFile.delete()
                    }

                    updateDownloadProgress(
                        releaseInfo = releaseInfo,
                        progress = 100,
                        downloadedBytes = outFile.length(),
                        totalBytes = outFile.length(),
                        statusMessage = "Muat turun selesai",
                        callback = onProgressUpdate
                    )

                    return@withContext outFile
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    val waitSeconds = attempt * 2
                    updateDownloadProgress(
                        releaseInfo = releaseInfo,
                        progress = (_downloadState.value as? UpdateDownloadState.Downloading)?.progress ?: 0,
                        downloadedBytes = if (partFile.exists()) partFile.length() else 0L,
                        totalBytes = (_downloadState.value as? UpdateDownloadState.Downloading)?.totalBytes ?: -1L,
                        statusMessage = "Menyambung semula dalam ${waitSeconds}s (cubaan ${attempt + 1}/$MAX_RETRY_ATTEMPTS)...",
                        callback = onProgressUpdate
                    )
                    delay(waitSeconds * 1000L)
                }
            } finally {
                activeDownloadCall = null
            }
        }

        throw lastException ?: IOException("Gagal memuat turun kemas kini APK.")
    }

    private fun updateDownloadProgress(
        releaseInfo: AppReleaseInfo,
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        statusMessage: String,
        callback: (Int, Long, Long, String) -> Unit
    ) {
        _downloadState.value = UpdateDownloadState.Downloading(
            releaseInfo = releaseInfo,
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            statusMessage = statusMessage
        )
        callback(progress, downloadedBytes, totalBytes, statusMessage)
    }

    fun consumeAutoInstallIfReady(context: Context): Boolean {
        val state = _downloadState.value as? UpdateDownloadState.ReadyToInstall ?: return false
        if (state.autoLaunchConsumed) return false
        val apkFile = File(state.apkFilePath)
        if (!apkFile.exists()) {
            _downloadState.value = UpdateDownloadState.Idle
            return false
        }
        _downloadState.value = state.copy(autoLaunchConsumed = true)
        return launchApkInstaller(context, apkFile)
    }

    fun installReadyApk(context: Context): Boolean {
        val state = _downloadState.value as? UpdateDownloadState.ReadyToInstall ?: return false
        val apkFile = File(state.apkFilePath)
        if (!apkFile.exists()) {
            _downloadState.value = UpdateDownloadState.Error(
                message = "Fail APK yang dimuat turun tidak ditemui. Sila muat turun semula.",
                releaseInfo = state.releaseInfo
            )
            return false
        }
        _downloadState.value = state.copy(autoLaunchConsumed = true)
        return launchApkInstaller(context, apkFile)
    }

    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        return try {
            val installIntent = createInstallIntent(context, apkFile)
            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        return when {
            bytes >= mb -> String.format(Locale.ENGLISH, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.ENGLISH, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
