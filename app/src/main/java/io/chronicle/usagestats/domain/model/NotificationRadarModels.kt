package io.chronicle.usagestats.domain.model

enum class FragmentationRisk {
    LOW,
    MODERATE,
    ELEVATED,
    HIGH
}

data class HourlyPingSlot(
    val hour: Int,
    val pingCount: Int,
    val topAppPackage: String? = null,
    val topAppLabel: String? = null
)

data class DisruptorApp(
    val packageName: String,
    val appLabel: String,
    val pingCount: Int,
    val percentage: Float,
    val category: AppCategory = AppCategory.OTHER
)

data class BaitAndSwitchEvent(
    val triggerAppPackage: String,
    val triggerAppLabel: String,
    val destinationAppPackage: String,
    val destinationAppLabel: String,
    val occurrenceCount: Int
)

data class NotificationRadarData(
    val totalPings: Int,
    val disruptivePings: Int,
    val fragmentationScore: Int, // 0..100
    val fragmentationRisk: FragmentationRisk,
    val hourlySlots: List<HourlyPingSlot>,
    val topDisruptors: List<DisruptorApp>,
    val baitAndSwitchEvents: List<BaitAndSwitchEvent>,
    val peakHour: Int? = null,
    val peakHourCount: Int = 0,
    val isListenerPermissionGranted: Boolean = true
)
