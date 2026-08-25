package io.chronicle.usagestats.ui.report

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.ExportDateRange
import io.chronicle.usagestats.domain.model.ExportFormat
import io.chronicle.usagestats.domain.model.ReportFilter
import io.chronicle.usagestats.domain.repository.UsageRepository
import io.chronicle.usagestats.domain.usecase.ExportPdfReportUseCase
import io.chronicle.usagestats.domain.usecase.ExportReportImageUseCase
import io.chronicle.usagestats.domain.usecase.GetReportUseCase
import io.chronicle.usagestats.domain.usecase.SyncUsageDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val exportSuccess: Boolean = false,
    val showExportDialog: Boolean = false
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getReportUseCase: GetReportUseCase,
    private val syncUsageDataUseCase: SyncUsageDataUseCase,
    private val exportPdfReportUseCase: ExportPdfReportUseCase,
    private val exportReportImageUseCase: ExportReportImageUseCase,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateTimeUtils.getStartOfDay())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _filter = MutableStateFlow(ReportFilter())
    val filter: StateFlow<ReportFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    val reportData: StateFlow<DailyUsageSummary?> = combine(_selectedDate, _filter) { date, filter ->
            Pair(date, filter)
        }
        .flatMapLatest { (date, filter) ->
            getReportUseCase.getDailyReport(date, filter)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            try {
                syncUsageDataUseCase.syncToday()
            } catch (_: Exception) { }
        }
    }

    fun selectDate(epochMillis: Long) {
        val startOfDay = DateTimeUtils.getStartOfDay(epochMillis)
        _selectedDate.value = startOfDay
        viewModelScope.launch {
            try {
                syncUsageDataUseCase.syncDate(startOfDay)
            } catch (_: Exception) { }
        }
    }

    fun setSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun setCategory(category: AppCategory) {
        _filter.value = _filter.value.copy(selectedCategory = category)
    }

    fun showExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }

    fun hideExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    fun exportWithScope(
        context: Context,
        range: ExportDateRange,
        format: ExportFormat,
        startMillis: Long,
        endMillis: Long
    ) {
        hideExportDialog()
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)

        viewModelScope.launch {
            try {
                if (range == ExportDateRange.TODAY) {
                    val summary = reportData.value ?: return@launch
                    if (format == ExportFormat.PDF) {
                        val result = exportPdfReportUseCase.execute(
                            summary = summary,
                            startDateMillis = startMillis,
                            endDateMillis = endMillis
                        )
                        handleExportResult(context, result, "application/pdf", "Share PDF Report")
                    } else {
                        val result = exportReportImageUseCase.execute(
                            summary = summary,
                            startDateMillis = startMillis,
                            endDateMillis = endMillis,
                            saveToGallery = false
                        )
                        handleExportResult(context, result, "image/png", "Share Report Image")
                    }
                } else {
                    // Multi-day Range Dossier
                    val rangeReport = usageRepository.getRangeReport(startMillis, endMillis).first()
                    if (format == ExportFormat.PDF) {
                        val result = exportPdfReportUseCase.executeRange(rangeReport)
                        handleExportResult(context, result, "application/pdf", "Share Analytics Dossier")
                    } else {
                        // Aggregate into summary for image
                        val aggregateSummary = DailyUsageSummary(
                            dateEpochMillis = startMillis,
                            totalScreenTimeMillis = rangeReport.totalScreenTimeMillis,
                            topAppPackage = rangeReport.topApps.firstOrNull()?.packageName,
                            topAppLabel = rangeReport.topApps.firstOrNull()?.appLabel,
                            appCount = rangeReport.topApps.size,
                            apps = rangeReport.topApps
                        )
                        val result = exportReportImageUseCase.execute(
                            summary = aggregateSummary,
                            startDateMillis = startMillis,
                            endDateMillis = endMillis,
                            saveToGallery = false
                        )
                        handleExportResult(context, result, "image/png", "Share Report Image")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "Unable to export report: ${e.localizedMessage ?: "Unknown error"}",
                    exportSuccess = false
                )
            }
        }
    }

    private fun handleExportResult(
        context: Context,
        result: Result<android.net.Uri>,
        mimeType: String,
        chooserTitle: String
    ) {
        result.fold(
            onSuccess = { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "Export generated successfully.",
                    exportSuccess = true
                )
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "Unable to generate export file.",
                    exportSuccess = false
                )
            }
        )
    }

    fun exportPdf(context: Context) {
        showExportDialog()
    }

    fun exportImage(context: Context, saveToGallery: Boolean = false) {
        if (saveToGallery) {
            val summary = reportData.value ?: return
            val date = _selectedDate.value
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
                val result = exportReportImageUseCase.execute(
                    summary = summary,
                    startDateMillis = date,
                    endDateMillis = DateTimeUtils.getEndOfDay(date),
                    saveToGallery = true
                )
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportMessage = "Report saved to gallery.",
                            exportSuccess = true
                        )
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportMessage = "Unable to save report image.",
                            exportSuccess = false
                        )
                    }
                )
            }
        } else {
            showExportDialog()
        }
    }

    fun clearExportMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }
}
