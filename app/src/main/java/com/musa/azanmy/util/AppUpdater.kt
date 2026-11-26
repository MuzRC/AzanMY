package com.musa.azanmy.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.musa.azanmy.BuildConfig
import com.musa.azanmy.data.AppReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppReleaseInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object AppUpdater {

    private const val VERSION_JSON_URL =
        "https://raw.githubusercontent.com/MuzRC/AzanMY/main/version.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val urlWithBust = "$VERSION_JSON_URL?t=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(urlWithBust)
                .header("User-Agent", "AzanMY-Updater/${BuildConfig.VERSION_NAME}")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
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
                    UpdateCheckResult.UpdateAvailable(
                        AppReleaseInfo(
                            versionCode = remoteCode,
                            versionName = remoteName,
                            apkUrl = apkUrl,
                            releaseNotes = releaseNotes,
                            updatedAt = updatedAt
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "Gagal menyemak kemas kini.")
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        releaseInfo: AppReleaseInfo,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(releaseInfo.apkUrl)
                .header("User-Agent", "AzanMY-Updater/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal muat turun APK (HTTP ${response.code})")
                    )
                }

                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("Fail APK kosong."))

                val totalBytes = body.contentLength()
                val baseDir = context.getExternalFilesDir(null) ?: context.cacheDir
                val updatesDir = File(baseDir, "updates").apply {
                    if (!exists()) mkdirs()
                }
                updatesDir.listFiles()?.forEach { it.delete() }

                val outFile = File(updatesDir, "AzanMY-v${releaseInfo.versionName}.apk")

                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var read: Int
                        var lastPercent = -1

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                val percent = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    withContext(Dispatchers.Main) {
                                        onProgress(percent)
                                    }
                                }
                            }
                        }
                        output.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    onProgress(100)
                    launchApkInstaller(context, outFile)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun launchApkInstaller(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    }
}
