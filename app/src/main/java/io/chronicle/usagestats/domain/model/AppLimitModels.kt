package io.chronicle.usagestats.domain.model

// Bypass challenge type -- how hard it is to get 5 extra minutes
enum class LimitBypassMode {
    NO_BYPASS,           // Locked until IST midnight - no way out
    TYPING_CHALLENGE,    // Type 200-char paragraph perfectly, case-sensitive
    MATH_CHALLENGE       // Solve 5 hard multiplication problems in sequence
}

// Per-app limit configuration stored in Room
data class AppLimitConfig(
    val packageName: String,
    val appLabel: String = "",
    val dailyLimitMinutes: Int,       // 1-1440 range
    val isEnabled: Boolean = true,
    val bypassMode: LimitBypassMode = LimitBypassMode.NO_BYPASS,
    val temporaryUnlockUntilMillis: Long? = null  // if challenge completed, unlocked until this time
)

// Live enforcement state combining limit config with actual today usage
data class AppLimitStatus(
    val packageName: String,
    val appLabel: String,
    val dailyLimitMinutes: Int,
    val usedTodayMinutes: Int,
    val isBlocked: Boolean,
    val remainingMinutes: Int,
    val bypassMode: LimitBypassMode,
    val temporaryUnlockUntilMillis: Long?
)
