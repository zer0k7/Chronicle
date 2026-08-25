package io.chronicle.usagestats.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.TimelineData
import io.chronicle.usagestats.domain.model.TimelinePeriod
import io.chronicle.usagestats.ui.components.AppIconView
import io.chronicle.usagestats.ui.components.CategoryDistributionBar
import io.chronicle.usagestats.ui.components.ChronicleCard
import io.chronicle.usagestats.ui.components.ChronicleDatePickerDialog
import io.chronicle.usagestats.ui.components.HabitsCard
import io.chronicle.usagestats.ui.components.HourlyBarChart
import io.chronicle.usagestats.ui.theme.ColorRemoved
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
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Date Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val dateLabel = timelineData?.let {
                DateTimeUtils.formatPeriodLabel(selectedPeriod, it.startEpochMillis, it.endEpochMillis)
            } ?: DateTimeUtils.formatDate(referenceDate)

            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { viewModel.navigateNext() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Loading indicator
        AnimatedVisibility(visible = isRefreshing, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content
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
                // 1. Total Screen Time & Trend Comparison Card
                item {
                    TimelineSummaryCard(data)
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

                // 3. Category Distribution Bar & Productivity Score
                val habits = data.habitInsights
                if (habits != null && habits.categoryBreakdown.isNotEmpty()) {
                    item {
                        CategoryDistributionBar(
                            categoryBreakdown = habits.categoryBreakdown,
                            productivityScore = habits.productivityScore
                        )
                    }
                }

                // 4. Habits & Sleep Routine Card (Day view only)
                if (selectedPeriod == TimelinePeriod.DAY && habits != null) {
                    item {
                        HabitsCard(insights = habits)
                    }
                    if (habits.wakingLifeImpact != null) {
                        item {
                            io.chronicle.usagestats.ui.components.WakingLifeCard(insights = habits)
                        }
                    }
                    if (habits.lifeClock != null || habits.dopamineDebt != null || habits.phantomUnlocks != null) {
                        item {
                            io.chronicle.usagestats.ui.components.AdvancedPsychologyCard(insights = habits)
                        }
                    }
                }

                // 5. Weekly Bar Chart (Week view only)
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

                // 6. Most Used Apps Header
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

                // 7. App Rows
                val maxDuration = displayedApps.firstOrNull()?.totalTimeForegroundMillis ?: 1L
                items(displayedApps) { app ->
                    AppUsageRow(
                        app = app,
                        maxDuration = maxDuration
                    )
                }

                // Bottom padding for floating nav bar
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun TimelineSummaryCard(data: TimelineData) {
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
private fun AppUsageRow(app: AppUsageInfo, maxDuration: Long) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = app.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (app.isRemoved) ColorRemoved else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtext = buildString {
                        if (app.launchCount > 0) {
                            append("${app.launchCount} launches")
                        }
                        if (app.avgSessionDurationMillis > 0) {
                            if (isNotEmpty()) append(" • ")
                            append("avg ${DateTimeUtils.formatDuration(app.avgSessionDurationMillis)}")
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

            Spacer(modifier = Modifier.height(10.dp))

            val ratio = if (maxDuration > 0) {
                (app.totalTimeForegroundMillis.toFloat() / maxDuration.toFloat()).coerceIn(0.02f, 1f)
            } else 0.02f

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (app.isRemoved) ColorRemoved else MaterialTheme.colorScheme.primary,
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
    val maxDuration = dailySummaries.maxOfOrNull { it.totalScreenTimeMillis } ?: 1L
    val twoHoursMs = 2 * 60 * 60 * 1000L
    val yAxisMax = ((maxDuration / twoHoursMs) + 1) * twoHoursMs
    val yAxisSteps = (yAxisMax / twoHoursMs).toInt().coerceIn(1, 6)

    val currentDayOfWeek = DateTimeUtils.toZonedDateTime(referenceDate).dayOfWeek
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val daysOfWeek = java.time.DayOfWeek.entries

    val summaryByDay = dailySummaries.associateBy {
        DateTimeUtils.toZonedDateTime(it.dateEpochMillis).dayOfWeek
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val yAxisPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(start = 40.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    ) {
        val chartWidth = size.width
        val chartHeight = size.height - 40f
        val barWidth = chartWidth / 7f * 0.6f
        val barSpacing = chartWidth / 7f

        for (i in 0..yAxisSteps) {
            val y = chartHeight - (chartHeight * i / yAxisSteps)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )
            val hours = (yAxisMax * i / yAxisSteps) / (60 * 60 * 1000L)
            drawContext.canvas.nativeCanvas.drawText(
                "${hours}h",
                -8f,
                y + 8f,
                yAxisPaint
            )
        }

        for (i in daysOfWeek.indices) {
            val dayOfWeek = daysOfWeek[i]
            val summary = summaryByDay[dayOfWeek]
            val duration = summary?.totalScreenTimeMillis ?: 0L
            val barHeight = if (yAxisMax > 0) {
                (duration.toFloat() / yAxisMax) * chartHeight
            } else 0f

            val x = i * barSpacing + (barSpacing - barWidth) / 2f
            val isCurrentDay = dayOfWeek == currentDayOfWeek
            val barColor = if (isCurrentDay) primaryColor else mutedColor

            if (barHeight > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            drawContext.canvas.nativeCanvas.drawText(
                dayLabels[i],
                x + barWidth / 2f,
                chartHeight + 30f,
                textPaint
            )
        }
    }
}
