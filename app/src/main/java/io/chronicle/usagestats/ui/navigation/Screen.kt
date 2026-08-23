package io.chronicle.usagestats.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Timeline : Screen("timeline")
    object Report : Screen("report")
    object Settings : Screen("settings")
}
