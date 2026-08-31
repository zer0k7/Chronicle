package io.chronicle.usagestats

import android.content.Intent
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
import io.chronicle.usagestats.domain.model.AccentColorPreset
import io.chronicle.usagestats.domain.model.ThemeMode
import io.chronicle.usagestats.domain.model.UserSettings
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

    private var targetRouteState = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetRouteState.value = intent.getStringExtra("target_route")
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        targetRouteState.value = intent.getStringExtra("target_route")

        enableEdgeToEdge()

        try {
            // Check for updates on every app launch in background
            appUpdateManager.checkForUpdates(silent = true)
        } catch (_: Exception) { }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settings by userPreferencesRepository.userSettingsFlow
                .collectAsStateWithLifecycle(initialValue = null)
            val updateState by appUpdateManager.updateState
                .collectAsStateWithLifecycle()
            val isUpdateDialogVisible by appUpdateManager.isDialogVisible
                .collectAsStateWithLifecycle()
            val targetRoute by targetRouteState

            val currentSettings = settings ?: UserSettings(
                themeMode = ThemeMode.DARK,
                accentColor = AccentColorPreset.SAPPHIRE,
                dailyNotificationEnabled = true,
                dailyNotificationHour = 21,
                dailyNotificationMinute = 0,
                badgeEnabled = true,
                isOnboardingCompleted = true
            )

            ChronicleTheme(
                themeMode = currentSettings.themeMode,
                accentColor = currentSettings.accentColor
            ) {
                val navController = rememberNavController()
                ChronicleNavGraph(
                    navController = navController,
                    isOnboardingCompleted = currentSettings.isOnboardingCompleted,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    targetRoute = targetRoute
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
