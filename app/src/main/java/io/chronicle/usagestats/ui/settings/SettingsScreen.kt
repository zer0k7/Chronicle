package io.chronicle.usagestats.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.chronicle.usagestats.BuildConfig
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.domain.model.AccentColorPreset
import io.chronicle.usagestats.domain.model.ThemeMode
import io.chronicle.usagestats.ui.components.ChronicleDialog
import io.chronicle.usagestats.ui.components.ChronicleTimePickerDialog
import io.chronicle.usagestats.ui.theme.ColorSuccess
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateToAppLimits: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val isUpdateDialogVisible by viewModel.isUpdateDialogVisible.collectAsStateWithLifecycle()
    
    var showTimePicker by remember { mutableStateOf(false) }
    var showFocusStartPicker by remember { mutableStateOf(false) }
    var showFocusEndPicker by remember { mutableStateOf(false) }
    var showResetTimePicker by remember { mutableStateOf(false) }
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showWeekendGoalDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showDailyDataBudgetDialog by remember { mutableStateOf(false) }
    var showMonthlyDataBudgetDialog by remember { mutableStateOf(false) }
    var showBillingCycleDialog by remember { mutableStateOf(false) }

    if (showDailyDataBudgetDialog) {
        DailyDataBudgetDialog(
            initialMb = settings.dailyDataBudgetMb,
            onDismiss = { showDailyDataBudgetDialog = false },
            onConfirm = { mb ->
                viewModel.setDailyDataBudgetMb(mb)
                showDailyDataBudgetDialog = false
            }
        )
    }

    if (showMonthlyDataBudgetDialog) {
        MonthlyDataBudgetDialog(
            initialGb = settings.monthlyDataBudgetGb,
            onDismiss = { showMonthlyDataBudgetDialog = false },
            onConfirm = { gb ->
                viewModel.setMonthlyDataBudgetGb(gb)
                showMonthlyDataBudgetDialog = false
            }
        )
    }

    if (showBillingCycleDialog) {
        BillingCycleDayDialog(
            initialDay = settings.billingCycleStartDay,
            onDismiss = { showBillingCycleDialog = false },
            onConfirm = { day ->
                viewModel.setBillingCycleStartDay(day)
                showBillingCycleDialog = false
            }
        )
    }

    if (showTimePicker) {
        ChronicleTimePickerDialog(
            initialHour = settings.dailyNotificationHour,
            initialMinute = settings.dailyNotificationMinute,
            onDismissRequest = { showTimePicker = false },
            onTimeSelected = { h, m ->
                viewModel.setDailyNotificationTime(h, m, context)
                showTimePicker = false
            }
        )
    }

    if (showFocusStartPicker) {
        ChronicleTimePickerDialog(
            initialHour = settings.focusStartHour,
            initialMinute = settings.focusStartMinute,
            onDismissRequest = { showFocusStartPicker = false },
            onTimeSelected = { h, m ->
                viewModel.setFocusSchedule(h, m, settings.focusEndHour, settings.focusEndMinute)
                showFocusStartPicker = false
            }
        )
    }

    if (showFocusEndPicker) {
        ChronicleTimePickerDialog(
            initialHour = settings.focusEndHour,
            initialMinute = settings.focusEndMinute,
            onDismissRequest = { showFocusEndPicker = false },
            onTimeSelected = { h, m ->
                viewModel.setFocusSchedule(settings.focusStartHour, settings.focusStartMinute, h, m)
                showFocusEndPicker = false
            }
        )
    }

    if (showResetTimePicker) {
        ChronicleTimePickerDialog(
            initialHour = settings.dailyResetHour,
            initialMinute = 0,
            onDismissRequest = { showResetTimePicker = false },
            onTimeSelected = { h, _ ->
                viewModel.setDailyResetHour(h)
                showResetTimePicker = false
            }
        )
    }

    if (showDailyGoalDialog) {
        DailyGoalDialog(
            title = stringResource(R.string.settings_daily_goal),
            initialMinutes = settings.dailyGoalMinutes,
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = { mins ->
                viewModel.setDailyGoalMinutes(mins)
                showDailyGoalDialog = false
            }
        )
    }

    if (showWeekendGoalDialog) {
        DailyGoalDialog(
            title = stringResource(R.string.settings_weekend_goal),
            initialMinutes = settings.weekendGoalMinutes,
            onDismiss = { showWeekendGoalDialog = false },
            onConfirm = { mins ->
                viewModel.setWeekendGoalMinutes(mins)
                showWeekendGoalDialog = false
            }
        )
    }

    if (showRetentionDialog) {
        RetentionPeriodDialog(
            currentDays = settings.dataRetentionDays,
            onDismiss = { showRetentionDialog = false },
            onConfirm = { days ->
                viewModel.setDataRetentionDays(days)
                showRetentionDialog = false
            }
        )
    }

    if (showClearDataDialog) {
        ChronicleDialog(
            title = stringResource(R.string.settings_clear_data),
            description = stringResource(R.string.settings_clear_data_desc),
            onDismissRequest = { showClearDataDialog = false },
            primaryButtonText = stringResource(android.R.string.ok),
            onPrimaryClick = {
                viewModel.clearUsageData(context)
                showClearDataDialog = false
            },
            secondaryButtonText = stringResource(android.R.string.cancel),
            onSecondaryClick = { showClearDataDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        // Header
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // --- Appearance Section ---
        item {
            SectionHeader(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_section_appearance)
            )
        }

        item {
            SettingsLabel(stringResource(R.string.settings_theme_label))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = mode == settings.themeMode
                    val label = when (mode) {
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        ThemeMode.AMOLED -> stringResource(R.string.settings_theme_amoled)
                        ThemeMode.DYNAMIC -> "Monet"
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setThemeMode(mode) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            SettingsLabel(stringResource(R.string.settings_accent_label))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccentColorPreset.entries.forEach { preset ->
                    val isSelected = preset == settings.accentColor
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(preset.primaryColorLong))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setAccentColor(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // --- Screen Time Budgets ---
        item {
            SectionHeader(
                icon = Icons.Outlined.Timer,
                title = stringResource(R.string.settings_daily_goal)
            )
        }

        item {
            SettingsClickRow(
                title = stringResource(R.string.settings_daily_goal),
                value = formatMinutesToHoursMins(settings.dailyGoalMinutes),
                onClick = { showDailyGoalDialog = true }
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_weekend_override),
                description = stringResource(R.string.settings_weekend_override_desc),
                checked = settings.weekendGoalEnabled,
                onCheckedChange = { viewModel.setWeekendGoalEnabled(it) }
            )
        }

        if (settings.weekendGoalEnabled) {
            item {
                SettingsClickRow(
                    title = stringResource(R.string.settings_weekend_goal),
                    value = formatMinutesToHoursMins(settings.weekendGoalMinutes),
                    onClick = { showWeekendGoalDialog = true }
                )
            }
        }

        item {
            SettingsClickRow(
                title = stringResource(R.string.app_limits_title),
                value = "",
                onClick = onNavigateToAppLimits
            )
        }

        // --- Focus Mode ---
        item {
            SectionHeader(
                icon = Icons.Outlined.DoNotDisturb,
                title = stringResource(R.string.settings_focus_mode_toggle)
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_focus_mode_toggle),
                description = stringResource(R.string.settings_focus_mode_desc),
                checked = settings.focusModeEnabled,
                onCheckedChange = { viewModel.setFocusModeEnabled(it) }
            )
        }

        if (settings.focusModeEnabled) {
            item {
                val start = DateTimeUtils.formatTime(settings.focusStartHour, settings.focusStartMinute)
                val end = DateTimeUtils.formatTime(settings.focusEndHour, settings.focusEndMinute)
                SettingsClickRow(
                    title = stringResource(R.string.settings_focus_schedule),
                    value = "$start to $end",
                    onClick = { showFocusStartPicker = true }
                )
            }
        }

        // --- Notifications Section ---
        item {
            SectionHeader(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.settings_section_notifications)
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_notification_daily_summary),
                description = stringResource(R.string.settings_notification_daily_summary_desc),
                checked = settings.dailyNotificationEnabled,
                onCheckedChange = { viewModel.setDailyNotificationEnabled(it, context) }
            )
        }

        item {
            SettingsClickRow(
                title = stringResource(R.string.settings_notification_time),
                value = DateTimeUtils.formatTime(settings.dailyNotificationHour, settings.dailyNotificationMinute),
                onClick = { showTimePicker = true }
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_notification_badge),
                description = stringResource(R.string.settings_notification_badge_desc),
                checked = settings.badgeEnabled,
                onCheckedChange = { viewModel.setBadgeEnabled(it) }
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_reality_check),
                description = stringResource(R.string.settings_reality_check_desc),
                checked = settings.realityCheckEnabled,
                onCheckedChange = { viewModel.setRealityCheckEnabled(it) }
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_milestone_notifications),
                description = stringResource(R.string.settings_milestone_notifications_desc),
                checked = settings.milestoneNotificationsEnabled,
                onCheckedChange = { viewModel.setMilestoneNotificationsEnabled(it) }
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_weekend_mute),
                description = stringResource(R.string.settings_weekend_mute_desc),
                checked = settings.weekendNotificationsMuted,
                onCheckedChange = { viewModel.setWeekendNotificationsMuted(it) }
            )
        }

        // --- General ---
        item {
            SectionHeader(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.settings_first_day_of_week)
            )
        }

        item {
            SettingsLabel(stringResource(R.string.settings_first_day_of_week))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val days = listOf("SUNDAY" to "Sunday", "MONDAY" to "Monday")
                days.forEach { (value, label) ->
                    val isSelected = value == settings.firstDayOfWeek
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setFirstDayOfWeek(value) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsClickRow(
                title = stringResource(R.string.settings_daily_reset_time),
                value = DateTimeUtils.formatTime(settings.dailyResetHour, 0),
                onClick = { showResetTimePicker = true }
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_show_removed_apps),
                description = stringResource(R.string.settings_show_removed_apps_desc),
                checked = settings.showRemovedApps,
                onCheckedChange = { viewModel.setShowRemovedApps(it) }
            )
        }

        // --- Data Management ---
        item {
            SectionHeader(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.settings_export_csv)
            )
        }

        item {
            SettingsClickRow(
                title = stringResource(R.string.settings_export_csv),
                value = "CSV",
                onClick = { viewModel.exportCsvData(context) }
            )
        }

        item {
            SettingsClickRow(
                title = stringResource(R.string.settings_clear_data),
                value = "",
                onClick = { showClearDataDialog = true }
            )
        }

        item {
            val periodStr = when (settings.dataRetentionDays) {
                30 -> "30 Days"
                90 -> "90 Days"
                365 -> "1 Year"
                else -> "Forever"
            }
            SettingsClickRow(
                title = stringResource(R.string.settings_retention_period),
                value = periodStr,
                onClick = { showRetentionDialog = true }
            )
        }

        // --- Accessibility ---
        item {
            SectionHeader(
                icon = Icons.Outlined.Accessibility,
                title = stringResource(R.string.settings_compact_view)
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_compact_view),
                description = stringResource(R.string.settings_compact_view_desc),
                checked = settings.compactView,
                onCheckedChange = { viewModel.setCompactView(it) }
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_high_contrast),
                description = stringResource(R.string.settings_high_contrast_desc),
                checked = settings.highContrast,
                onCheckedChange = { viewModel.setHighContrast(it) }
            )
        }

        // --- Data & Network Budgets ---
        item {
            SectionHeader(
                icon = Icons.Outlined.SignalCellularAlt,
                title = stringResource(R.string.settings_section_data_budgets)
            )
        }

        item {
            val formattedMb = if (settings.dailyDataBudgetMb <= 0) {
                "No Limit"
            } else if (settings.dailyDataBudgetMb >= 1024) {
                String.format(java.util.Locale.ENGLISH, "%.1f GB", settings.dailyDataBudgetMb / 1024f)
            } else {
                "${settings.dailyDataBudgetMb} MB"
            }
            SettingsClickRow(
                title = stringResource(R.string.settings_daily_data_budget),
                value = formattedMb,
                onClick = { showDailyDataBudgetDialog = true }
            )
        }

        item {
            val formattedGb = if (settings.monthlyDataBudgetGb <= 0) "No Limit" else "${settings.monthlyDataBudgetGb} GB"
            SettingsClickRow(
                title = stringResource(R.string.settings_monthly_data_budget),
                value = formattedGb,
                onClick = { showMonthlyDataBudgetDialog = true }
            )
        }

        item {
            SettingsClickRow(
                title = stringResource(R.string.settings_billing_cycle_day),
                value = stringResource(R.string.settings_billing_cycle_day_desc, settings.billingCycleStartDay),
                onClick = { showBillingCycleDialog = true }
            )
        }

        item {
            SettingsToggleRow(
                title = stringResource(R.string.settings_data_alerts),
                description = stringResource(R.string.settings_data_alerts_desc),
                checked = settings.dataAlertsEnabled,
                onCheckedChange = { viewModel.setDataAlertsEnabled(it) }
            )
        }

        item {
            SettingsToggleRow(
                title = "Live Network Speed Meter",
                description = "Shows real-time download and upload speed in the notification shade",
                checked = settings.liveNetworkSpeedMeterEnabled,
                onCheckedChange = { viewModel.setLiveNetworkSpeedMeterEnabled(it, context) }
            )
        }

        // --- Permissions Section ---
        item {
            SectionHeader(
                icon = Icons.Outlined.Security,
                title = stringResource(R.string.settings_section_permissions)
            )
        }

        item {
            PermissionStatusRow(
                title = stringResource(R.string.settings_permission_usage),
                isGranted = PermissionHelper.hasUsageStatsPermission(context),
                onClick = { PermissionHelper.openUsageStatsSettings(context) }
            )
        }
        item {
            PermissionStatusRow(
                title = stringResource(R.string.settings_permission_notifications),
                isGranted = PermissionHelper.hasNotificationPermission(context),
                onClick = { PermissionHelper.openAppSettings(context) }
            )
        }
        item {
            PermissionStatusRow(
                title = stringResource(R.string.settings_permission_storage),
                isGranted = PermissionHelper.hasStoragePermission(context),
                onClick = { PermissionHelper.openAppSettings(context) }
            )
        }

        // --- About Section ---
        item {
            SectionHeader(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.settings_section_about)
            )
        }

        item {
            val versionSubtitle = when (val state = updateState) {
                is io.chronicle.usagestats.core.updater.UpdateState.Checking -> stringResource(R.string.update_checking)
                is io.chronicle.usagestats.core.updater.UpdateState.UpdateAvailable -> stringResource(R.string.update_available_title) + " (v${state.info.latestVersion})"
                is io.chronicle.usagestats.core.updater.UpdateState.Downloading -> stringResource(R.string.update_downloading, state.progress)
                is io.chronicle.usagestats.core.updater.UpdateState.ReadyToInstall -> stringResource(R.string.update_ready_title)
                else -> stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME)
            }
            SettingsClickRow(
                title = stringResource(R.string.update_check_button),
                value = versionSubtitle,
                onClick = { viewModel.checkForUpdates() }
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.settings_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatMinutesToHoursMins(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return buildString {
        if (h > 0) append("${h}h ")
        append("${m}m")
    }.trim()
}

@Composable
private fun DailyGoalDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(initialMinutes.toFloat()) }
    val currentMinutes = sliderValue.roundToInt()

    ChronicleDialog(
        title = title,
        onDismissRequest = onDismiss,
        primaryButtonText = stringResource(android.R.string.ok),
        onPrimaryClick = { onConfirm(currentMinutes) },
        secondaryButtonText = stringResource(android.R.string.cancel),
        onSecondaryClick = onDismiss,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatMinutesToHoursMins(currentMinutes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 30f..480f,
                    steps = 29
                )
            }
        }
    )
}

@Composable
private fun RetentionPeriodDialog(
    currentDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDays by remember { mutableStateOf(currentDays) }
    
    val options = listOf(
        -1 to "Forever",
        30 to "30 Days",
        90 to "90 Days",
        365 to "1 Year"
    )

    ChronicleDialog(
        title = stringResource(R.string.settings_retention_period),
        onDismissRequest = onDismiss,
        primaryButtonText = stringResource(android.R.string.ok),
        onPrimaryClick = { onConfirm(selectedDays) },
        secondaryButtonText = stringResource(android.R.string.cancel),
        onSecondaryClick = onDismiss,
        content = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (days, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (days == selectedDays),
                                onClick = { selectedDays = days },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (days == selectedDays),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
        )
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsClickRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PermissionStatusRow(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = if (isGranted) stringResource(R.string.settings_permission_status_granted) else stringResource(R.string.settings_permission_status_missing),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isGranted) ColorSuccess else MaterialTheme.colorScheme.error
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyDataBudgetDialog(
    initialMb: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var currentMb by remember { mutableStateOf(initialMb) }
    val presets = listOf(
        0 to "No Limit",
        512 to "500 MB",
        1024 to "1.0 GB",
        1536 to "1.5 GB",
        2048 to "2.0 GB",
        2560 to "2.5 GB",
        3072 to "3.0 GB",
        5120 to "5.0 GB"
    )

    ChronicleDialog(
        title = stringResource(R.string.settings_daily_data_budget),
        onDismissRequest = onDismiss,
        primaryButtonText = stringResource(android.R.string.ok),
        onPrimaryClick = { onConfirm(currentMb) },
        secondaryButtonText = stringResource(android.R.string.cancel),
        onSecondaryClick = onDismiss,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val formatted = if (currentMb <= 0) {
                    "No Daily Quota"
                } else if (currentMb >= 1024) {
                    String.format(java.util.Locale.ENGLISH, "%.1f GB", currentMb / 1024f)
                } else {
                    "$currentMb MB"
                }
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (mb, label) ->
                        val isSelected = currentMb == mb
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentMb = mb },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (currentMb > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Slider(
                        value = currentMb.toFloat().coerceIn(256f, 5120f),
                        onValueChange = { currentMb = it.roundToInt() },
                        valueRange = 256f..5120f,
                        steps = 18
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthlyDataBudgetDialog(
    initialGb: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var currentGb by remember { mutableStateOf(initialGb) }
    val presets = listOf(
        0 to "No Limit",
        10 to "10 GB",
        25 to "25 GB",
        50 to "50 GB",
        75 to "75 GB",
        100 to "100 GB",
        150 to "150 GB",
        200 to "200 GB"
    )

    ChronicleDialog(
        title = stringResource(R.string.settings_monthly_data_budget),
        onDismissRequest = onDismiss,
        primaryButtonText = stringResource(android.R.string.ok),
        onPrimaryClick = { onConfirm(currentGb) },
        secondaryButtonText = stringResource(android.R.string.cancel),
        onSecondaryClick = onDismiss,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val formatted = if (currentGb <= 0) "No Monthly Quota" else "$currentGb GB"
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (gb, label) ->
                        val isSelected = currentGb == gb
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentGb = gb },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (currentGb > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Slider(
                        value = currentGb.toFloat().coerceIn(5f, 200f),
                        onValueChange = { currentGb = it.roundToInt() },
                        valueRange = 5f..200f,
                        steps = 38
                    )
                }
            }
        }
    )
}

@Composable
private fun BillingCycleDayDialog(
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(initialDay.toFloat()) }
    val currentDay = sliderValue.roundToInt().coerceIn(1, 31)

    ChronicleDialog(
        title = stringResource(R.string.settings_billing_cycle_day),
        onDismissRequest = onDismiss,
        primaryButtonText = stringResource(android.R.string.ok),
        onPrimaryClick = { onConfirm(currentDay) },
        secondaryButtonText = stringResource(android.R.string.cancel),
        onSecondaryClick = onDismiss,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Day $currentDay of every month",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..31f,
                    steps = 29
                )
            }
        }
    )
}
