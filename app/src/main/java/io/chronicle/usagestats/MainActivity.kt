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
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.ui.navigation.ChronicleNavGraph
import io.chronicle.usagestats.ui.theme.ChronicleTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settings by userPreferencesRepository.userSettingsFlow
                .collectAsStateWithLifecycle(initialValue = null)

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
                }
            }
        }
    }
}
