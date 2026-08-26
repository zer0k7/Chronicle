package io.chronicle.usagestats.domain.model

enum class ThemeMode {
    LIGHT,
    DARK,
    AMOLED,
    DYNAMIC
}

enum class AccentColorPreset(
    val primaryColorLong: Long,
    val secondaryColorLong: Long,
    val tertiaryColorLong: Long,
    val colorNameResKey: String
) {
    SAPPHIRE(
        primaryColorLong = 0xFF00E5FF,
        secondaryColorLong = 0xFF0284C7,
        tertiaryColorLong = 0xFF38BDF8,
        colorNameResKey = "accent_sapphire"
    ),
    EMERALD(
        primaryColorLong = 0xFF10B981,
        secondaryColorLong = 0xFF059669,
        tertiaryColorLong = 0xFF34D399,
        colorNameResKey = "accent_emerald"
    ),
    CRIMSON(
        primaryColorLong = 0xFFEF4444,
        secondaryColorLong = 0xFFDC2626,
        tertiaryColorLong = 0xFFF87171,
        colorNameResKey = "accent_crimson"
    ),
    AMBER(
        primaryColorLong = 0xFFF59E0B,
        secondaryColorLong = 0xFFD97706,
        tertiaryColorLong = 0xFFFBBF24,
        colorNameResKey = "accent_amber"
    ),
    AMETHYST(
        primaryColorLong = 0xFF8B5CF6,
        secondaryColorLong = 0xFF7C3AED,
        tertiaryColorLong = 0xFFA78BFA,
        colorNameResKey = "accent_amethyst"
    ),
    SLATE(
        primaryColorLong = 0xFF64748B,
        secondaryColorLong = 0xFF475569,
        tertiaryColorLong = 0xFF94A3B8,
        colorNameResKey = "accent_slate"
    )
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColorPreset = AccentColorPreset.SAPPHIRE,
    val dailyNotificationEnabled: Boolean = true,
    val dailyNotificationHour: Int = 21,
    val dailyNotificationMinute: Int = 0,
    val badgeEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    // Screen Time Budgets
    val dailyGoalMinutes: Int = 150,
    val weekendGoalEnabled: Boolean = false,
    val weekendGoalMinutes: Int = 240,
    // Focus Mode
    val focusModeEnabled: Boolean = false,
    val focusStartHour: Int = 9,
    val focusStartMinute: Int = 0,
    val focusEndHour: Int = 17,
    val focusEndMinute: Int = 0,
    // Enhanced Notifications
    val realityCheckEnabled: Boolean = true,
    val milestoneNotificationsEnabled: Boolean = true,
    val weekendNotificationsMuted: Boolean = false,
    // General
    val firstDayOfWeek: String = "MONDAY",
    val dailyResetHour: Int = 0,
    val showRemovedApps: Boolean = true,
    // Accessibility
    val compactView: Boolean = false,
    val highContrast: Boolean = false,
    // Data Management
    val dataRetentionDays: Int = -1,
    // Network & Data Budgets
    val dailyDataBudgetMb: Int = 2048,
    val monthlyDataBudgetGb: Int = 50,
    val billingCycleStartDay: Int = 1,
    val dataAlertsEnabled: Boolean = true
)
