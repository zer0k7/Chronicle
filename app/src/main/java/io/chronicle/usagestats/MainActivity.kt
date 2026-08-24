package io.chronicle.usagestats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.chronicle.usagestats.core.updater.AppUpdateManager
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.ui.components.UpdateDialog
import io.chronicle.usagestats.ui.navigation.ChronicleNavGraph
import io.chronicle.usagestats.ui.theme.ChronicleTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Check for updates on every app launch in background
        appUpdateManager.checkForUpdates(silent = true)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settings by userPreferencesRepository.userSettingsFlow
                .collectAsStateWithLifecycle(initialValue = null)
            val updateState by appUpdateManager.updateState
                .collectAsStateWithLifecycle()
            val isUpdateDialogVisible by appUpdateManager.isDialogVisible
                .collectAsStateWithLifecycle()

            val currentSettings = settings

            if (currentSettings != null) {
                ChronicleTheme(
                    themeMode = currentSettings.themeMode,
                    accentColor = currentSettings.accentColor
                ) {
                    val navController = rememberNavController()
                    ChronicleNavGraph(
                        navController = navController,
                        isOnboardingCompleted = currentSettings.isOnboardingCompleted,
                        widthSizeClass = windowSizeClass.widthSizeClass
                    )

                    // Global in-app update dialog
                    UpdateDialog(
                        updateState = updateState,
                        isVisible = isUpdateDialogVisible,
                        onDismiss = { appUpdateManager.dismissDialog() },
                        onDownload = { info -> appUpdateManager.startDownload(info) },
                        onInstall = { file -> appUpdateManager.installApk(file) },
                        onRetry = { appUpdateManager.checkForUpdates(silent = false) }
                    )
                }
            }
        }
    }
}
