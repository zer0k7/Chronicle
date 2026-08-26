package io.chronicle.usagestats.ui.datausage

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataPeriod
import io.chronicle.usagestats.domain.model.DataUsageInfo
import io.chronicle.usagestats.domain.model.NetworkTypeFilter
import io.chronicle.usagestats.domain.model.UserSettings
import io.chronicle.usagestats.ui.components.AppIconView
import io.chronicle.usagestats.ui.components.ChronicleCard
import io.chronicle.usagestats.ui.components.ChronicleDatePickerDialog
import io.chronicle.usagestats.ui.components.ChronicleDateRangePickerDialog
import io.chronicle.usagestats.ui.components.ChronicleSnackbar
import io.chronicle.usagestats.ui.components.DataAppDetailBottomSheet
import io.chronicle.usagestats.ui.components.SnackbarType
import io.chronicle.usagestats.ui.theme.ColorRemoved
import io.chronicle.usagestats.ui.theme.ColorSuccess
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageScreen(
    viewModel: DataUsageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val referenceDate by viewModel.referenceDate.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val dataSummary by viewModel.dataSummary.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showCustomRangePicker by remember { mutableStateOf(false) }
    var selectedAppForDetail by remember { mutableStateOf<DataUsageInfo?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.checkPermission()
    }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            delay(3000)
            snackbarMessage = null
        }
    }

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

    if (showCustomRangePicker) {
        ChronicleDateRangePickerDialog(
            initialStartEpochMillis = referenceDate - (6 * 86400000L),
            initialEndEpochMillis = referenceDate,
            onDismissRequest = { showCustomRangePicker = false },
            onDateRangeSelected = { start, end ->
                showCustomRangePicker = false
                viewModel.exportPdf(start, end) { uri ->
                    if (uri != null) {
                        shareUri(context, uri, "application/pdf")
                    } else {
                        snackbarMessage = "Failed to export PDF"
                    }
                }
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
                    text = "Export Network Telemetry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 1. PDF Dossier Export
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showExportSheet = false
                            val (start, end) = getPeriodRange(selectedPeriod, referenceDate)
                            viewModel.exportPdf(start, end) { uri ->
                                if (uri != null) {
                                    shareUri(context, uri, "application/pdf")
                                } else {
                                    snackbarMessage = "Failed to generate PDF dossier"
                                }
                            }
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
                                text = "PDF Telemetry Dossier",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Comprehensive vector PDF with tabular Rx/Tx breakdown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. High-Res Infographic Image
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showExportSheet = false
                            val (start, end) = getPeriodRange(selectedPeriod, referenceDate)
                            viewModel.exportImage(start, end, saveToGallery = false) { uri ->
                                if (uri != null) {
                                    shareUri(context, uri, "image/png")
                                } else {
                                    snackbarMessage = "Failed to generate infographic card"
                                }
                            }
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
                                text = "Infographic Image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "1080x1440 shareable telemetry summary card",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Custom Date Range Export
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showExportSheet = false
                            showCustomRangePicker = true
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
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Range Export",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select any custom start and end date (1d, 2d, 3d, or weeks)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    selectedAppForDetail?.let { app ->
        DataAppDetailBottomSheet(
            app = app,
            grandTotalBytes = dataSummary?.grandTotalBytes ?: 1L,
            onDismissRequest = { selectedAppForDetail = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.data_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.timeline_date_picker),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showExportSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Export Data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Period Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periods = listOf(
                    DataPeriod.DAY to stringResource(R.string.data_period_day),
                    DataPeriod.WEEK to stringResource(R.string.data_period_week),
                    DataPeriod.MONTH to stringResource(R.string.data_period_month),
                    DataPeriod.BILLING_CYCLE to stringResource(R.string.data_period_cycle)
                )

                periods.forEach { (period, label) ->
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
                            selectedLabelColor = MaterialTheme.colorScheme.primary
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
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                val periodLabel = when (selectedPeriod) {
                    DataPeriod.DAY -> DateTimeUtils.formatDate(referenceDate)
                    DataPeriod.WEEK -> {
                        val start = DateTimeUtils.getStartOfWeek(referenceDate)
                        val end = DateTimeUtils.getEndOfWeek(referenceDate)
                        "${DateTimeUtils.formatDate(start)} – ${DateTimeUtils.formatDate(end)}"
                    }
                    DataPeriod.MONTH -> DateTimeUtils.formatMonth(referenceDate)
                    DataPeriod.BILLING_CYCLE -> {
                        val cycleDay = userSettings.billingCycleStartDay
                        "Cycle from Day $cycleDay (${DateTimeUtils.formatMonth(referenceDate)})"
                    }
                }

                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = { viewModel.navigateNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Sync / Refresh / Export Progress
            AnimatedVisibility(
                visible = isRefreshing || isExporting,
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

            if (!hasPermission) {
                // Permission Required Banner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ChronicleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.data_permission_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = stringResource(R.string.data_permission_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { PermissionHelper.openUsageStatsSettings(context) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = stringResource(R.string.data_permission_button))
                            }
                        }
                    }
                }
            } else {
                val summary = dataSummary
                if (summary == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Data Hero Summary Card (Correct mathematical totals)
                        item {
                            DataHeroCard(
                                summary = summary,
                                userSettings = userSettings,
                                selectedPeriod = selectedPeriod
                            )
                        }

                        // 1b. Carrier Quota Depletion Forecast (Day view & budget enabled)
                        if (selectedPeriod == DataPeriod.DAY && summary.depletionForecast != null && userSettings.dailyDataBudgetMb > 0) {
                            item {
                                DataDepletionForecastCard(
                                    forecast = summary.depletionForecast
                                )
                            }
                        }

                        // 2. 24-Hour Hourly Network Bar Chart (Day view)
                        if (selectedPeriod == DataPeriod.DAY && summary.hourlyDataPoints.isNotEmpty()) {
                            item {
                                HourlyDataBarChart(
                                    hourlyDataPoints = summary.hourlyDataPoints,
                                    selectedHour = filter.selectedHour,
                                    onHourSelected = { viewModel.selectHour(it) }
                                )
                            }
                        }

                        // 3. Multi-Day Historical Bar Chart (Week/Month views)
                        if (selectedPeriod != DataPeriod.DAY && summary.multiDayDataPoints.isNotEmpty()) {
                            item {
                                WeeklyDataBarChart(
                                    multiDayDataPoints = summary.multiDayDataPoints
                                )
                            }
                        }

                        // 4. Category Bandwidth Distribution Bar
                        if (summary.categoryShares.isNotEmpty()) {
                            item {
                                CategoryDataDistributionBar(
                                    categoryShares = summary.categoryShares
                                )
                            }
                        }

                        // 5. Overnight Sleep Data & Drain Leak Detector (Day view)
                        if (selectedPeriod == DataPeriod.DAY && summary.sleepDataLeaks.isNotEmpty()) {
                            item {
                                SleepDataLeakCard(
                                    sleepLeaks = summary.sleepDataLeaks,
                                    totalSleepBytes = summary.totalSleepBytes
                                )
                            }
                        }

                        // 6. Hotspot / Tethering Card (if data exists)
                        if (summary.totalHotspotBytes > 0) {
                            item {
                                HotspotTetheringCard(hotspotBytes = summary.totalHotspotBytes)
                            }
                        }

                        // 7. Bandwidth Ratio Bar
                        if (summary.grandTotalBytes > 0) {
                            item {
                                BandwidthRatioCard(summary = summary)
                            }
                        }

                        // 8. Network Type Filter Bar
                        item {
                            NetworkTypeFilterRow(
                                selectedType = filter.networkType,
                                onTypeSelected = { viewModel.setNetworkTypeFilter(it) }
                            )
                        }

                        // 9. Search Bar
                        item {
                            OutlinedTextField(
                                value = filter.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.data_search_hint),
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
                        }

                        // 10. App List Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Applications (${summary.appUsageList.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        // 11. App Rows
                        val maxBytes = summary.appUsageList.firstOrNull()?.totalBytes ?: 1L
                        if (summary.appUsageList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.data_no_records),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(summary.appUsageList) { app ->
                                AppDataRow(
                                    app = app,
                                    maxBytes = maxBytes,
                                    grandTotalBytes = summary.grandTotalBytes,
                                    onClick = { selectedAppForDetail = app }
                                )
                            }
                        }

                        // Bottom navigation padding
                        item { Spacer(modifier = Modifier.height(88.dp)) }
                    }
                }
            }
        }

        snackbarMessage?.let { msg ->
            ChronicleSnackbar(
                visible = true,
                message = msg,
                type = SnackbarType.INFO,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun DataHeroCard(
    summary: DailyDataUsageSummary,
    userSettings: UserSettings,
    selectedPeriod: DataPeriod
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    ChronicleCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stringResource(R.string.data_total_bandwidth).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Grand Total strictly = Mobile Data + Wi-Fi Data
            Text(
                text = DataSizeUtils.formatBytes(summary.grandTotalBytes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mobile vs Wi-Fi pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mobile SIM Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SignalCellularAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.data_mobile_used),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DataSizeUtils.formatBytes(summary.totalMobileBytes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Wi-Fi Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.data_wifi_used),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DataSizeUtils.formatBytes(summary.totalWifiBytes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Daily Data Budget Indicator (if Day view & quota enabled > 0)
            if (selectedPeriod == DataPeriod.DAY && userSettings.dailyDataBudgetMb > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                val budgetBytes = userSettings.dailyDataBudgetMb * 1024L * 1024L
                val progress = (summary.totalMobileBytes.toFloat() / budgetBytes.toFloat()).coerceIn(0f, 1f)
                val pct = (progress * 100).toInt()

                val progressColor = when {
                    progress < 0.60f -> ColorSuccess
                    progress < 0.90f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(
                        R.string.data_budget_progress,
                        DataSizeUtils.formatBytes(summary.totalMobileBytes),
                        DataSizeUtils.formatBytes(budgetBytes),
                        pct
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HotspotTetheringCard(hotspotBytes: Long) {
    ChronicleCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.WifiTethering,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.data_hotspot_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.data_hotspot_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = DataSizeUtils.formatBytes(hotspotBytes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun BandwidthRatioCard(summary: DailyDataUsageSummary) {
    val total = summary.totalWifiBytes + summary.totalMobileBytes
    if (total <= 0) return

    val wifiRatio = (summary.totalWifiBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val mobileRatio = (summary.totalMobileBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    ChronicleCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.data_ratio_title).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = String.format(Locale.ENGLISH, "Wi-Fi %.0f%% • Mobile %.0f%%", wifiRatio * 100, mobileRatio * 100),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                if (wifiRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(wifiRatio)
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                if (mobileRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(mobileRatio)
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkTypeFilterRow(
    selectedType: NetworkTypeFilter,
    onTypeSelected: (NetworkTypeFilter) -> Unit
) {
    val filters = listOf(
        NetworkTypeFilter.ALL to stringResource(R.string.data_filter_all),
        NetworkTypeFilter.MOBILE to stringResource(R.string.data_filter_mobile),
        NetworkTypeFilter.WIFI to stringResource(R.string.data_filter_wifi),
        NetworkTypeFilter.HOTSPOT to stringResource(R.string.data_filter_hotspot)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (type, label) ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
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
}

@Composable
private fun AppDataRow(
    app: DataUsageInfo,
    maxBytes: Long,
    grandTotalBytes: Long,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
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
                if (app.isHotspot) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.WifiTethering,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    AppIconView(
                        packageName = app.packageName,
                        appName = app.appLabel,
                        isRemoved = app.isRemoved,
                        size = 40.dp
                    )
                }

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
                        if (app.isWifiPreferred) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Wi-Fi Only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    val pctOfTotal = if (grandTotalBytes > 0) {
                        String.format(Locale.ENGLISH, "%.1f%%", (app.totalBytes.toFloat() / grandTotalBytes.toFloat()) * 100f)
                    } else "0.0%"

                    val details = buildString {
                        append(pctOfTotal)
                        if (app.mobileTotalBytes > 0) {
                            append(" • Mobile ${DataSizeUtils.formatBytes(app.mobileTotalBytes)}")
                        }
                        if (app.wifiTotalBytes > 0) {
                            append(" • Wi-Fi ${DataSizeUtils.formatBytes(app.wifiTotalBytes)}")
                        }
                    }

                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = DataSizeUtils.formatBytes(app.totalBytes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            val ratio = if (maxBytes > 0) {
                (app.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.01f, 1f)
            } else 0.01f

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (app.isRemoved) ColorRemoved else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // Rx/Tx breakdown
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val rxTotal = app.wifiRxBytes + app.mobileRxBytes
                val txTotal = app.wifiTxBytes + app.mobileTxBytes

                Text(
                    text = stringResource(R.string.data_download_rx, DataSizeUtils.formatBytes(rxTotal)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Text(
                    text = stringResource(R.string.data_upload_tx, DataSizeUtils.formatBytes(txTotal)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun getPeriodRange(period: DataPeriod, referenceDate: Long): Pair<Long, Long> {
    return when (period) {
        DataPeriod.DAY -> referenceDate to DateTimeUtils.getEndOfDay(referenceDate)
        DataPeriod.WEEK -> DateTimeUtils.getStartOfWeek(referenceDate) to DateTimeUtils.getEndOfWeek(referenceDate)
        DataPeriod.MONTH, DataPeriod.BILLING_CYCLE -> DateTimeUtils.getStartOfMonth(referenceDate) to DateTimeUtils.getEndOfMonth(referenceDate)
    }
}

private fun shareUri(context: android.content.Context, uri: Uri, mimeType: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Network Report"))
}
