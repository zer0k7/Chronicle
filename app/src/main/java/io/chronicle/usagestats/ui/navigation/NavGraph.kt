package io.chronicle.usagestats.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.chronicle.usagestats.ui.components.FloatingNavigationBar
import io.chronicle.usagestats.ui.onboarding.OnboardingScreen
import io.chronicle.usagestats.ui.report.ReportScreen
import io.chronicle.usagestats.ui.settings.SettingsScreen
import io.chronicle.usagestats.ui.splash.SplashScreen
import io.chronicle.usagestats.ui.timeline.TimelineScreen

@Composable
fun ChronicleNavGraph(
    navController: NavHostController,
    isOnboardingCompleted: Boolean
) {
    val startDestination = if (isOnboardingCompleted) Screen.Timeline.route else Screen.Splash.route

    var currentRoute by rememberSaveable { mutableStateOf(Screen.Timeline.route) }

    val showNavBar = currentRoute in listOf(
        Screen.Timeline.route,
        Screen.Report.route,
        Screen.Settings.route
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.Splash.route) {
                currentRoute = Screen.Splash.route
                SplashScreen(
                    onSplashFinished = {
                        if (isOnboardingCompleted) {
                            navController.navigate(Screen.Timeline.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                currentRoute = Screen.Onboarding.route
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Timeline.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Timeline.route) {
                currentRoute = Screen.Timeline.route
                TimelineScreen()
            }

            composable(Screen.Report.route) {
                currentRoute = Screen.Report.route
                ReportScreen()
            }

            composable(Screen.Settings.route) {
                currentRoute = Screen.Settings.route
                SettingsScreen()
            }
        }

        if (showNavBar) {
            FloatingNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Timeline.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    currentRoute = route
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
