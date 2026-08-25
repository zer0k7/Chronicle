package io.chronicle.usagestats.ui.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import io.chronicle.usagestats.ui.components.AppIconView
import io.chronicle.usagestats.ui.components.ChronicleCard
import io.chronicle.usagestats.ui.components.ChronicleDatePickerDialog
import io.chronicle.usagestats.ui.components.ChronicleSnackbar
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

    // Material 3 Modal Bottom Sheet for Export/Share
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.report_export_action),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Option 1: Paginated PDF
                FilledTonalButton(
                    onClick = {
                        viewModel.exportPdf(context)
                        showExportSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.export_type_pdf),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Option 2: Share Infographic Image
                FilledTonalButton(
                    onClick = {
                        viewModel.exportImage(context, saveToGallery = false)
                        showExportSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.export_type_image),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Option 3: Save Infographic to Storage
                FilledTonalButton(
                    onClick = {
                        viewModel.exportImage(context, saveToGallery = true)
                        showExportSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SaveAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.export_action_save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Column {
                    Text(
                        text = stringResource(R.string.report_title),
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

                Row {
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showExportSheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.report_export_action),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Export Loading bar
            AnimatedVisibility(visible = uiState.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Search Bar
            OutlinedTextField(
                value = filter.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.report_search_hint)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    AppCategory.ALL, AppCategory.PRODUCTIVITY, AppCategory.SOCIAL,
                    AppCategory.ENTERTAINMENT, AppCategory.GAMES, AppCategory.COMMUNICATION,
                    AppCategory.UTILITIES, AppCategory.SYSTEM, AppCategory.REMOVED, AppCategory.OTHER
                )
                items(categories) { category ->
                    val label = when (category) {
                        AppCategory.ALL -> stringResource(R.string.category_all)
                        AppCategory.PRODUCTIVITY -> stringResource(R.string.category_name_productivity)
                        AppCategory.SOCIAL -> stringResource(R.string.category_name_social)
                        AppCategory.ENTERTAINMENT -> stringResource(R.string.category_name_entertainment)
                        AppCategory.GAMES -> stringResource(R.string.category_name_games)
                        AppCategory.COMMUNICATION -> stringResource(R.string.category_name_communication)
                        AppCategory.UTILITIES -> stringResource(R.string.category_name_utilities)
                        AppCategory.SYSTEM -> stringResource(R.string.category_name_system)
                        AppCategory.REMOVED -> stringResource(R.string.category_removed)
                        AppCategory.OTHER -> stringResource(R.string.category_name_other)
                    }
                    FilterChip(
                        selected = filter.selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (category == AppCategory.REMOVED)
                                ColorRemoved.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            selectedLabelColor = if (category == AppCategory.REMOVED)
                                ColorRemoved else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Report Content
            val data = reportData
            if (data == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (data.apps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.timeline_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary Header
                    item { ReportSummaryHeader(data) }

                    // App Rows
                    items(data.apps) { app ->
                        ReportAppRow(app, data.totalScreenTimeMillis)
                    }

                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = stringResource(R.string.report_total_time).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = DateTimeUtils.formatDuration(summary.totalScreenTimeMillis),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${summary.appCount} apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ReportAppRow(app: AppUsageInfo, totalDuration: Long) {
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

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (app.isRemoved) ColorRemoved else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val pct = if (totalDuration > 0) {
                        String.format(Locale.ENGLISH, "%.1f%%", (app.totalTimeForegroundMillis.toFloat() / totalDuration) * 100f)
                    } else "0.0%"

                    Text(
                        text = "$pct • ${app.launchCount} launches" + if (app.isRemoved) " • Removed" else "",
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

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (app.isRemoved) ColorRemoved else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
