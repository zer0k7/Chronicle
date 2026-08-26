package io.chronicle.usagestats.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Timeline : Screen("timeline")
    object Report : Screen("report")
    object DataUsage : Screen("data_usage")
    object Settings : Screen("settings")
    object AppLimits : Screen("app_limits")
}
