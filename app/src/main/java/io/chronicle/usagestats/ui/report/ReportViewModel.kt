package io.chronicle.usagestats.ui.report

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import io.chronicle.usagestats.domain.model.ReportFilter
import io.chronicle.usagestats.domain.usecase.ExportPdfReportUseCase
import io.chronicle.usagestats.domain.usecase.ExportReportImageUseCase
import io.chronicle.usagestats.domain.usecase.GetReportUseCase
import io.chronicle.usagestats.domain.usecase.SyncUsageDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val exportSuccess: Boolean = false
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getReportUseCase: GetReportUseCase,
    private val syncUsageDataUseCase: SyncUsageDataUseCase,
    private val exportPdfReportUseCase: ExportPdfReportUseCase,
    private val exportReportImageUseCase: ExportReportImageUseCase
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
        _selectedDate.value = DateTimeUtils.getStartOfDay(epochMillis)
    }

    fun setSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun setCategory(category: AppCategory) {
        _filter.value = _filter.value.copy(selectedCategory = category)
    }

    fun exportPdf(context: Context) {
        val summary = reportData.value ?: return
        val date = _selectedDate.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
            val result = exportPdfReportUseCase.execute(
                summary = summary,
                startDateMillis = date,
                endDateMillis = DateTimeUtils.getEndOfDay(date)
            )
            result.fold(
                onSuccess = { uri ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share PDF Report"))
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = "PDF report generated successfully.",
                        exportSuccess = true
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = "Unable to export PDF report.",
                        exportSuccess = false
                    )
                }
            )
        }
    }

    fun exportImage(context: Context, saveToGallery: Boolean = false) {
        val summary = reportData.value ?: return
        val date = _selectedDate.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
            val result = exportReportImageUseCase.execute(
                summary = summary,
                startDateMillis = date,
                endDateMillis = DateTimeUtils.getEndOfDay(date),
                saveToGallery = saveToGallery
            )
            result.fold(
                onSuccess = { uri ->
                    if (!saveToGallery) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Report Image"))
                    }
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = if (saveToGallery) "Report saved to gallery." else "Report image generated.",
                        exportSuccess = true
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = "Unable to export report image.",
                        exportSuccess = false
                    )
                }
            )
        }
    }

    fun clearExportMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }
}
