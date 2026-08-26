package io.chronicle.usagestats.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.ui.components.AppDetailBottomSheet
import io.chronicle.usagestats.ui.components.AppIconView
import io.chronicle.usagestats.ui.components.ChronicleCard
import io.chronicle.usagestats.ui.components.ChronicleDatePickerDialog
import io.chronicle.usagestats.ui.components.HourlyBarChart
import io.chronicle.usagestats.ui.theme.ColorRemoved
import io.chronicle.usagestats.ui.theme.ColorSuccess
import kotlin.math.abs

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val referenceDate by viewModel.referenceDate.collectAsStateWithLifecycle()
    val timelineData by viewModel.timelineData.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedHour by viewModel.selectedHour.collectAsStateWithLifecycle()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsStateWithLifecycle()
    val selectedAppDetail by viewModel.selectedAppDetail.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        ChronicleDatePickerDialog(
            initialDateEpochMillis = referenceDate,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { millis ->
                viewModel.selectDate(millis)
                showDatePicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.timeline_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row {
                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(R.string.timeline_date_picker),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { viewModel.refreshData() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Period Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimelinePeriod.entries.forEach { period ->
                val label = when (period) {
                    TimelinePeriod.DAY -> stringResource(R.string.timeline_period_day)
                    TimelinePeriod.WEEK -> stringResource(R.string.timeline_period_week)
                    TimelinePeriod.MONTH -> stringResource(R.string.timeline_period_month)
                    TimelinePeriod.YEAR -> stringResource(R.string.timeline_period_year)
                }
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { viewModel.selectPeriod(period) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Date Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigatePrevious() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            val periodLabel = when (selectedPeriod) {
                TimelinePeriod.DAY -> DateTimeUtils.formatDate(referenceDate)
                TimelinePeriod.WEEK -> {
                    val start = DateTimeUtils.getStartOfWeek(referenceDate)
                    val end = DateTimeUtils.getEndOfWeek(referenceDate)
                    "${DateTimeUtils.formatDate(start)} – ${DateTimeUtils.formatDate(end)}"
                }
                TimelinePeriod.MONTH -> DateTimeUtils.formatMonth(referenceDate)
                TimelinePeriod.YEAR -> DateTimeUtils.formatYear(referenceDate)
            }

            Text(
                text = periodLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            val isFuture = DateTimeUtils.isTodayOrFuture(referenceDate, selectedPeriod)
            IconButton(
                onClick = { viewModel.navigateNext() },
                enabled = !isFuture,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (!isFuture) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        }

        // Sync / Refresh progress
        AnimatedVisibility(
            visible = isRefreshing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        val data = timelineData
        if (data == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (data.topApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.timeline_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Determine displayed apps (either all day or filtered by tapped hour)
            val displayedApps = if (selectedPeriod == TimelinePeriod.DAY && selectedHour != null) {
                val hourSlot = data.hourlySlots.getOrNull(selectedHour!!)
                hourSlot?.appBreakdown ?: emptyList()
            } else {
                data.topApps
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Total Screen Time & Trend Comparison Card with Goal Progress
                item {
                    TimelineSummaryCard(
                        data = data,
                        dailyGoalMinutes = dailyGoalMinutes
                    )
                }

                // 2. Interactive 24-Hour Timeline Bar Chart (Day view only)
                if (selectedPeriod == TimelinePeriod.DAY && data.hourlySlots.isNotEmpty()) {
                    item {
                        HourlyBarChart(
                            hourlySlots = data.hourlySlots,
                            selectedHour = selectedHour,
                            onHourSelected = { hour -> viewModel.selectHour(hour) }
                        )
                    }
                }

                // 3. Weekly Bar Chart (Week view only)
                if (selectedPeriod == TimelinePeriod.WEEK && data.dailySummaries.isNotEmpty()) {
                    item {
                        ChronicleCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = stringResource(R.string.timeline_weekly_overview),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                WeeklyBarChart(
                                    dailySummaries = data.dailySummaries,
                                    referenceDate = referenceDate
                                )
                            }
                        }
                    }
                }

                // 4. Most Used Apps Header
                item {
                    val headerTitle = if (selectedPeriod == TimelinePeriod.DAY && selectedHour != null) {
                        val startHourStr = String.format("%02d:00", selectedHour)
                        val endHourStr = String.format("%02d:00", (selectedHour!! + 1) % 24)
                        "Apps used $startHourStr - $endHourStr"
                    } else {
                        stringResource(R.string.timeline_most_used)
                    }

                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 5. App Rows with clickable drill-down
                val maxDuration = displayedApps.firstOrNull()?.totalTimeForegroundMillis ?: 1L
                items(displayedApps) { app ->
                    AppUsageRow(
                        app = app,
                        maxDuration = maxDuration,
                        onClick = { viewModel.selectApp(app.packageName) }
                    )
                }

                // Bottom padding for floating nav bar
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }

    // App Detail Bottom Sheet
    selectedAppDetail?.let { detail ->
        AppDetailBottomSheet(
            appDetail = detail,
            onDismissRequest = { viewModel.selectApp(null) },
            onSaveOverride = { override -> viewModel.saveAppOverride(override) }
        )
    }
}

@Composable
private fun TimelineSummaryCard(data: TimelineData, dailyGoalMinutes: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary

    ChronicleCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.timeline_total_screen_time).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Trend Comparison Badge
                val trend = data.trendComparison
                if (trend != null && trend.previousPeriodDurationMillis > 0) {
                    val isLess = trend.deltaDurationMillis < 0
                    val deltaFormatted = DateTimeUtils.formatDuration(abs(trend.deltaDurationMillis))
                    val pctFormatted = String.format("%.0f%%", abs(trend.percentageChange))
                    val badgeColor = if (isLess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    val trendText = if (isLess) "-$deltaFormatted (-$pctFormatted)" else "+$deltaFormatted (+$pctFormatted)"

                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = trendText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateTimeUtils.formatDuration(data.totalDurationMillis),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val goalMillis = dailyGoalMinutes * 60 * 1000L
            val progress = if (goalMillis > 0) {
                (data.totalDurationMillis.toFloat() / goalMillis.toFloat()).coerceIn(0f, 1f)
            } else 0f
            
            val progressColor = when {
                progress < 0.60f -> ColorSuccess
                progress < 0.90f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val pct = (progress * 100).toInt()
            val goalFormatted = DateTimeUtils.formatDuration(goalMillis)
            Text(
                text = stringResource(R.string.goal_progress_format, pct, goalFormatted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(
                        text = stringResource(R.string.timeline_apps_active).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${data.activeAppCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val topApp = data.topApps.firstOrNull()
                if (topApp != null) {
                    Column {
                        Text(
                            text = stringResource(R.string.timeline_most_used).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = topApp.appLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(
    app: AppUsageInfo,
    maxDuration: Long,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconView(
                    packageName = app.packageName,
                    appName = app.appLabel,
                    isRemoved = app.isRemoved
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (app.isRemoved) ColorRemoved else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (app.isDistraction) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    val subtext = buildString {
                        if (app.launchCount > 0) {
                            append("${app.launchCount} launches")
                        }
                        if (app.avgSessionDurationMillis > 0) {
                            if (isNotEmpty()) append(" • ")
                            append("avg ${DateTimeUtils.formatDuration(app.avgSessionDurationMillis)}")
                        }
                        if (app.dailyLimitMinutes != null) {
                            if (isNotEmpty()) append(" • ")
                            append("limit ${app.dailyLimitMinutes}m")
                        }
                        if (app.isRemoved) {
                            if (isNotEmpty()) append(" • ")
                            append("Removed")
                        }
                    }

                    if (subtext.isNotEmpty()) {
                        Text(
                            text = subtext,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (app.isRemoved) ColorRemoved else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = DateTimeUtils.formatDuration(app.totalTimeForegroundMillis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))

            val ratio = if (maxDuration > 0) {
                (app.totalTimeForegroundMillis.toFloat() / maxDuration).coerceIn(0.01f, 1f)
            } else 0.01f

            val barColor = when {
                app.isRemoved -> ColorRemoved
                app.isDistraction -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(
    dailySummaries: List<DailyUsageSummary>,
    referenceDate: Long,
    modifier: Modifier = Modifier
) {
    val summariesMap = dailySummaries.associateBy {
        DateTimeUtils.toZonedDateTime(it.dateEpochMillis).dayOfWeek
    }

    val maxDaily = dailySummaries.maxOfOrNull { it.totalScreenTimeMillis } ?: 1L
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val todayZdt = DateTimeUtils.nowInIst()
    val isCurrentWeek = DateTimeUtils.isSameWeek(referenceDate, System.currentTimeMillis())

    val daysOfWeek = listOf(
        java.time.DayOfWeek.MONDAY,
        java.time.DayOfWeek.TUESDAY,
        java.time.DayOfWeek.WEDNESDAY,
        java.time.DayOfWeek.THURSDAY,
        java.time.DayOfWeek.FRIDAY,
        java.time.DayOfWeek.SATURDAY,
        java.time.DayOfWeek.SUNDAY
    )

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        val chartHeight = size.height - 40f
        val slotWidth = size.width / 7f
        val barWidth = slotWidth * 0.45f

        // Draw horizontal grid line at 50%
        drawLine(
            color = outlineColor.copy(alpha = 0.5f),
            start = Offset(0f, chartHeight / 2f),
            end = Offset(size.width, chartHeight / 2f),
            strokeWidth = 1f
        )

        daysOfWeek.forEachIndexed { index, dow ->
            val summary = summariesMap[dow]
            val duration = summary?.totalScreenTimeMillis ?: 0L
            val heightRatio = if (maxDaily > 0) (duration.toFloat() / maxDaily.toFloat()).coerceIn(0f, 1f) else 0f
            val x = index * slotWidth + (slotWidth - barWidth) / 2f
            val barHeight = maxOf(4f, heightRatio * chartHeight)
            val isToday = isCurrentWeek && dow == todayZdt.dayOfWeek

            // Bar background slot
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, chartHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Active bar
            if (duration > 0) {
                drawRoundRect(
                    color = if (isToday) primaryColor else primaryColor.copy(alpha = 0.7f),
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Day label
            val label = dow.name.take(3)
            textPaint.color = if (isToday) primaryColor.hashCode() else onSurfaceVariant.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barWidth / 2f,
                size.height - 6f,
                textPaint
            )
        }
    }
}
