package io.chronicle.usagestats.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import io.chronicle.usagestats.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val GITHUB_REPO = "zer0k7/Chronicle"
        private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        private const val BUFFER_SIZE = 8192
        private const val TIMEOUT_MILLIS = 15000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible.asStateFlow()

    fun showDialog() {
        _isDialogVisible.value = true
    }

    fun dismissDialog() {
        _isDialogVisible.value = false
    }

    fun checkForUpdates(silent: Boolean = false) {
        scope.launch {
            if (_updateState.value is UpdateState.Downloading) {
                // Don't interrupt active download
                return@launch
            }

            _updateState.value = UpdateState.Checking

            val updateInfo = fetchLatestReleaseInfo()
            if (updateInfo != null && updateInfo.isAvailable) {
                // Check if the APK has already been downloaded
                val cachedApk = getDownloadedApkFile(updateInfo.latestVersion)
                if (cachedApk != null && cachedApk.exists() && cachedApk.length() > 0) {
                    _updateState.value = UpdateState.ReadyToInstall(updateInfo, cachedApk)
                } else {
                    _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                }
                _isDialogVisible.value = true
            } else if (updateInfo != null && !updateInfo.isAvailable) {
                if (!silent) {
                    _updateState.value = UpdateState.UpToDate
                    _isDialogVisible.value = true
                } else {
                    _updateState.value = UpdateState.Idle
                }
            } else {
                if (!silent) {
                    _updateState.value = UpdateState.Error("Unable to check for updates. Please verify your connection.")
                    _isDialogVisible.value = true
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    fun startDownload(info: AppUpdateInfo) {
        if (downloadJob?.isActive == true) return

        downloadJob = scope.launch {
            try {
                val updateDir = File(context.cacheDir, "apk_updates").apply { mkdirs() }
                // Clear any older update files
                updateDir.listFiles()?.forEach { file ->
                    if (file.name != "chronicle-v${info.latestVersion}.apk") {
                        file.delete()
                    }
                }

                val destinationFile = File(updateDir, "chronicle-v${info.latestVersion}.apk")
                val url = URL(info.apkDownloadUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = TIMEOUT_MILLIS
                    readTimeout = TIMEOUT_MILLIS
                    setRequestProperty("User-Agent", "Chronicle-Android-App")
                    connect()
                }

                if (connection.responseCode !in 200..299) {
                    _updateState.value = UpdateState.Error("Server returned code ${connection.responseCode}")
                    return@launch
                }

                val totalBytes = if (connection.contentLengthLong > 0) {
                    connection.contentLengthLong
                } else {
                    info.apkSize
                }

                var downloadedBytes = 0L
                _updateState.value = UpdateState.Downloading(info, 0, 0L, totalBytes)

                connection.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                _updateState.value = UpdateState.Downloading(
                                    info = info,
                                    progress = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                            }
                        }
                    }
                }

                if (destinationFile.exists() && destinationFile.length() > 0) {
                    _updateState.value = UpdateState.ReadyToInstall(info, destinationFile)
                    _isDialogVisible.value = true
                } else {
                    _updateState.value = UpdateState.Error("Downloaded file is empty.")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Download failed.")
            }
        }
    }

    fun installApk(apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Open file or settings
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkFile.absolutePath)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                _updateState.value = UpdateState.Error("Unable to launch package installer.")
            }
        }
    }

    private fun getDownloadedApkFile(version: String): File? {
        val updateDir = File(context.cacheDir, "apk_updates")
        val apkFile = File(updateDir, "chronicle-v$version.apk")
        return if (apkFile.exists() && apkFile.length() > 0) apkFile else null
    }

    private suspend fun fetchLatestReleaseInfo(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Chronicle-Android-App")
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
            }

            if (connection.responseCode != 200) return@withContext null

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)

            val latestTag = json.getString("tag_name").removePrefix("v")
            val releaseNotes = json.optString("body", "")
            val currentVersion = getAppVersionName()

            var apkUrl: String? = null
            var apkSize = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.getString("browser_download_url")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl == null) {
                apkUrl = json.getString("html_url")
            }

            val isNewer = compareVersions(latestTag, currentVersion) > 0

            AppUpdateInfo(
                isAvailable = isNewer,
                currentVersion = currentVersion,
                latestVersion = latestTag,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                apkSize = apkSize
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun getAppVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) return num1.compareTo(num2)
        }
        return 0
    }
}
