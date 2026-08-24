package io.chronicle.usagestats.core.updater

import java.io.File

data class AppUpdateInfo(
    val isAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSize: Long = 0L
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateState()
    data class Downloading(
        val info: AppUpdateInfo,
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class ReadyToInstall(val info: AppUpdateInfo, val apkFile: File) : UpdateState()
    data object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}
