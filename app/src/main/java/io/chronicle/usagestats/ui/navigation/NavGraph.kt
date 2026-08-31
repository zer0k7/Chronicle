package io.chronicle.usagestats.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import io.chronicle.usagestats.ui.components.ChronicleNavigationRail
import io.chronicle.usagestats.ui.components.FloatingNavigationBar
import io.chronicle.usagestats.ui.datausage.DataUsageScreen
import io.chronicle.usagestats.ui.limits.AppLimitsScreen
import io.chronicle.usagestats.ui.onboarding.OnboardingScreen
import io.chronicle.usagestats.ui.report.ReportScreen
import io.chronicle.usagestats.ui.settings.SettingsScreen
import io.chronicle.usagestats.ui.splash.SplashScreen
import io.chronicle.usagestats.ui.timeline.TimelineScreen

@Composable
fun ChronicleNavGraph(
    navController: NavHostController,
    isOnboardingCompleted: Boolean,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    targetRoute: String? = null
) {
    val startDestination = if (isOnboardingCompleted) Screen.Timeline.route else Screen.Splash.route

    var currentRoute by rememberSaveable { mutableStateOf(Screen.Timeline.route) }

    androidx.compose.runtime.LaunchedEffect(targetRoute, isOnboardingCompleted) {
        if (targetRoute != null && isOnboardingCompleted) {
            navController.navigate(targetRoute) {
                popUpTo(Screen.Timeline.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            currentRoute = targetRoute
        }
    }

    val showNavBar = currentRoute in listOf(
        Screen.Timeline.route,
        Screen.Report.route,
        Screen.DataUsage.route,
        Screen.Settings.route
    )

    val isExpandedOrMedium = widthSizeClass != WindowWidthSizeClass.Compact

    if (isExpandedOrMedium && showNavBar) {
        // Large / Foldable / Tablet two-pane layout with Navigation Rail
        Row(modifier = Modifier.fillMaxSize()) {
            ChronicleNavigationRail(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Timeline.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    currentRoute = route
                }
            )

            Box(modifier = Modifier.weight(1f)) {
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

                    composable(Screen.DataUsage.route) {
                        currentRoute = Screen.DataUsage.route
                        DataUsageScreen()
                    }

                    composable(Screen.Settings.route) {
                        currentRoute = Screen.Settings.route
                        SettingsScreen(
                            onNavigateToAppLimits = {
                                navController.navigate(Screen.AppLimits.route)
                            }
                        )
                    }

                    composable(Screen.AppLimits.route) {
                        currentRoute = Screen.AppLimits.route
                        AppLimitsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    } else {
        // Compact phone layout with floating pill bottom navigation bar
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

                composable(Screen.DataUsage.route) {
                    currentRoute = Screen.DataUsage.route
                    DataUsageScreen()
                }

                composable(Screen.Settings.route) {
                    currentRoute = Screen.Settings.route
                    SettingsScreen(
                        onNavigateToAppLimits = {
                            navController.navigate(Screen.AppLimits.route)
                        }
                    )
                }

                composable(Screen.AppLimits.route) {
                    currentRoute = Screen.AppLimits.route
                    AppLimitsScreen(
                        onBack = { navController.popBackStack() }
                    )
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
}
