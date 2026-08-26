package io.chronicle.usagestats.ui.report

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.ui.components.AppDetailBottomSheet
import io.chronicle.usagestats.ui.components.AppIconView
import io.chronicle.usagestats.ui.components.ChronicleCard
import io.chronicle.usagestats.ui.components.ChronicleDatePickerDialog
import io.chronicle.usagestats.ui.components.ChronicleSnackbar
import io.chronicle.usagestats.ui.components.DisciplineStreaksCard
import io.chronicle.usagestats.ui.components.SnackbarType
import io.chronicle.usagestats.ui.theme.ColorRemoved
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val reportData by viewModel.reportData.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedAppDetail by viewModel.selectedAppDetail.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-dismiss snackbar
    LaunchedEffect(uiState.exportMessage) {
        if (uiState.exportMessage != null) {
            delay(3000)
            viewModel.clearExportMessage()
        }
    }

    if (showDatePicker) {
        ChronicleDatePickerDialog(
            initialDateEpochMillis = selectedDate,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { millis ->
                viewModel.selectDate(millis)
                showDatePicker = false
            }
        )
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.export_options_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 1. Paginated PDF
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showExportSheet = false
                            viewModel.showExportDialog()
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_type_pdf),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Scope options: Today, 7-Day, 30-Day, Custom",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Share Infographic Image
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showExportSheet = false
                            viewModel.showExportDialog()
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_type_image),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "High-resolution shareable card",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Save to Storage directly
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showExportSheet = false
                            viewModel.exportImage(context, saveToGallery = true)
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.SaveAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_action_save),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Save PNG directly to Pictures folder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp)
        ) {
            // Header: Title & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.nav_report),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = DateTimeUtils.formatDate(selectedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    FilledTonalButton(
                        onClick = { showExportSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.button_export),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Export Progress Bar
            AnimatedVisibility(visible = uiState.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = filter.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.report_search_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    AppCategory.ALL,
                    AppCategory.PRODUCTIVITY,
                    AppCategory.SOCIAL,
                    AppCategory.ENTERTAINMENT,
                    AppCategory.GAMES,
                    AppCategory.COMMUNICATION,
                    AppCategory.UTILITIES,
                    AppCategory.SYSTEM,
                    AppCategory.REMOVED,
                    AppCategory.OTHER
                )
                items(categories) { category ->
                    val label = when (category) {
                        AppCategory.ALL -> stringResource(R.string.category_all)
                        AppCategory.PRODUCTIVITY -> stringResource(R.string.category_productivity)
                        AppCategory.SOCIAL -> stringResource(R.string.category_social)
                        AppCategory.ENTERTAINMENT -> stringResource(R.string.category_entertainment)
                        AppCategory.GAMES, AppCategory.GAMING -> stringResource(R.string.category_games)
                        AppCategory.COMMUNICATION -> "Communication"
                        AppCategory.UTILITIES -> stringResource(R.string.category_utilities)
                        AppCategory.SYSTEM -> stringResource(R.string.category_system)
                        AppCategory.REMOVED -> stringResource(R.string.category_removed)
                        else -> stringResource(R.string.category_other)
                    }
                    FilterChip(
                        selected = filter.selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (filter.selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val data = reportData
            if (data == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (data.apps.isEmpty()) {
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
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary Header
                    item { ReportSummaryHeader(data) }

                    // Category Distribution Bar & Productivity Score
                    val habits = data.habitInsights
                    if (habits != null && habits.categoryBreakdown.isNotEmpty()) {
                        item { AnalyticsSectionHeader(title = stringResource(R.string.section_usage_breakdown)) }
                        item {
                            io.chronicle.usagestats.ui.components.CategoryDistributionBar(
                                categoryBreakdown = habits.categoryBreakdown,
                                productivityScore = habits.productivityScore
                            )
                        }
                    }

                    // Habits & Routine Card + Discipline Streaks
                    if (habits != null) {
                        item { AnalyticsSectionHeader(title = stringResource(R.string.section_habits_routine)) }
                        habits.disciplineStreaks?.let { streaks ->
                            item {
                                DisciplineStreaksCard(streaks = streaks)
                            }
                        }
                        item {
                            io.chronicle.usagestats.ui.components.HabitsCard(insights = habits)
                        }
                        if (habits.wakingLifeImpact != null) {
                            item { AnalyticsSectionHeader(title = stringResource(R.string.section_screen_life_impact)) }
                            item {
                                io.chronicle.usagestats.ui.components.WakingLifeCard(insights = habits)
                            }
                        }
                    }

                    // Advanced Psychological Habit Metrics Card
                    if (habits != null) {
                        item { AnalyticsSectionHeader(title = stringResource(R.string.section_psychological_metrics)) }
                        item {
                            io.chronicle.usagestats.ui.components.AdvancedPsychologyCard(insights = habits)
                        }
                    }

                    // Predictive Screen Time Forecast & Burnout Risk
                    data.forecast?.let { forecast ->
                        item { AnalyticsSectionHeader(title = "PREDICTIVE FORECAST & COGNITIVE STRAIN") }
                        item {
                            io.chronicle.usagestats.ui.components.ScreenTimeForecastCard(
                                forecast = forecast,
                                dailyGoalMinutes = 150
                            )
                        }
                    }

                    // Continuous Doomscroll Radar & 20-20-20 Eye Rest
                    if (data.doomscrollSessions.isNotEmpty()) {
                        item { AnalyticsSectionHeader(title = "CONTINUOUS FOCUS & EYE REST") }
                        item {
                            io.chronicle.usagestats.ui.components.DoomscrollRadarCard(
                                sessions = data.doomscrollSessions
                            )
                        }
                    }

                    // Distraction Cascade & App Habit Loops
                    if (data.habitLoops.isNotEmpty()) {
                        item { AnalyticsSectionHeader(title = "DISTRACTION CASCADE & HABIT LOOPS") }
                        item {
                            io.chronicle.usagestats.ui.components.DistractionCascadeCard(
                                habitLoops = data.habitLoops
                            )
                        }
                    }

                    // App vs App Comparison Card
                    if (data.apps.size >= 2) {
                        item { AnalyticsSectionHeader(title = stringResource(R.string.section_app_comparison)) }
                        item {
                            io.chronicle.usagestats.ui.components.AppComparisonCard(apps = data.apps)
                        }
                    }

                    // App List Header
                    item { AnalyticsSectionHeader(title = stringResource(R.string.section_all_apps)) }

                    // App Rows with drill-down
                    items(data.apps) { app ->
                        ReportAppRow(
                            app = app,
                            totalDuration = data.totalScreenTimeMillis,
                            onClick = { viewModel.selectApp(app.packageName) }
                        )
                    }

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

        // Export Scope & Format Dialog
        if (uiState.showExportDialog) {
            io.chronicle.usagestats.ui.components.ExportOptionsDialog(
                currentDate = selectedDate,
                onDismiss = { viewModel.hideExportDialog() },
                onExport = { range, format, startMillis, endMillis ->
                    viewModel.exportWithScope(context, range, format, startMillis, endMillis)
                }
            )
        }

        // Snackbar
        ChronicleSnackbar(
            visible = uiState.exportMessage != null,
            message = uiState.exportMessage ?: "",
            type = if (uiState.exportSuccess) SnackbarType.SUCCESS else SnackbarType.ERROR,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReportSummaryHeader(summary: DailyUsageSummary) {
    ChronicleCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.timeline_total_screen_time).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = DateTimeUtils.formatDuration(summary.totalScreenTimeMillis),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.timeline_apps_active).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${summary.appCount}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ReportAppRow(
    app: AppUsageInfo,
    totalDuration: Long,
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

                Spacer(Modifier.width(12.dp))

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
                    val pct = if (totalDuration > 0) {
                        String.format(Locale.ENGLISH, "%.1f%%", (app.totalTimeForegroundMillis.toFloat() / totalDuration) * 100f)
                    } else "0.0%"

                    val details = buildString {
                        append(pct)
                        if (app.launchCount > 0) append(" • ${app.launchCount} launches")
                        if (app.dailyLimitMinutes != null) append(" • limit ${app.dailyLimitMinutes}m")
                        if (app.isRemoved) append(" • Removed")
                    }

                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (app.isRemoved) ColorRemoved else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = DateTimeUtils.formatDuration(app.totalTimeForegroundMillis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))

            val ratio = if (totalDuration > 0) {
                (app.totalTimeForegroundMillis.toFloat() / totalDuration).coerceIn(0.01f, 1f)
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
private fun AnalyticsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}
