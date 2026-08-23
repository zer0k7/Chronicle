package io.chronicle.usagestats.domain.usecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.AppUsageInfo
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ExportPdfReportUseCase(
    private val context: Context
) {

    private val pageWidth = 595 // Standard A4 width in points (72 dpi)
    private val pageHeight = 842 // Standard A4 height in points (72 dpi)
    private val margin = 40f

    suspend fun execute(
        summary: DailyUsageSummary,
        startDateMillis: Long,
        endDateMillis: Long
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val apps = summary.apps
            val appsPerPage = 18
            val totalPages = if (apps.isEmpty()) 1 else ((apps.size + appsPerPage - 1) / appsPerPage)

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                canvas.drawColor(Color.WHITE)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                // Header
                drawHeader(canvas, paint, pageIndex + 1, totalPages, startDateMillis, endDateMillis)

                if (pageIndex == 0) {
                    // Summary Card on first page
                    drawSummaryCard(canvas, paint, summary)
                }

                // Table Items for this page
                val startAppIndex = pageIndex * appsPerPage
                val endAppIndex = minOf(startAppIndex + appsPerPage, apps.size)
                val pageApps = if (apps.isNotEmpty()) apps.subList(startAppIndex, endAppIndex) else emptyList()

                val tableStartY = if (pageIndex == 0) 230f else 110f
                drawTable(canvas, paint, pageApps, summary.totalScreenTimeMillis, tableStartY)

                // Footer
                drawFooter(canvas, paint, pageIndex + 1, totalPages)

                pdfDocument.finishPage(page)
            }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val fileName = "chronicle_report_${System.currentTimeMillis()}.pdf"
            val outputFile = File(reportsDir, fileName)

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        paint: Paint,
        currentPage: Int,
        totalPages: Int,
        startMillis: Long,
        endMillis: Long
    ) {
        // App Title
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CHRONICLE", margin, margin + 15f, paint)

        paint.color = Color.parseColor("#0284C7")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("USAGE ANALYTICS REPORT", margin, margin + 30f, paint)

        // Date Info (Right Aligned)
        paint.color = Color.parseColor("#475569")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateText = "Period: ${DateTimeUtils.formatDateRange(startMillis, endMillis)}"
        val tzText = "Timezone: IST (UTC+5:30)"
        val dateWidth = paint.measureText(dateText)
        val tzWidth = paint.measureText(tzText)

        canvas.drawText(dateText, pageWidth - margin - dateWidth, margin + 15f, paint)
        canvas.drawText(tzText, pageWidth - margin - tzWidth, margin + 28f, paint)

        // Divider Line
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1.5f
        canvas.drawLine(margin, margin + 40f, pageWidth - margin, margin + 40f, paint)
    }

    private fun drawSummaryCard(canvas: Canvas, paint: Paint, summary: DailyUsageSummary) {
        val cardTop = 95f
        val cardHeight = 110f
        val cardRight = pageWidth - margin

        // Background Box
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(margin, cardTop, cardRight, cardTop + cardHeight, 8f, 8f, paint)

        // Border
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(margin, cardTop, cardRight, cardTop + cardHeight, 8f, 8f, paint)

        // Card Content
        paint.style = Paint.Style.FILL

        // Metric 1: Total Screen Time
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL SCREEN TIME", margin + 20f, cardTop + 30f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(DateTimeUtils.formatDuration(summary.totalScreenTimeMillis), margin + 20f, cardTop + 60f, paint)

        // Metric 2: Active Applications
        val col2X = margin + 200f
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("APPLICATIONS", col2X, cardTop + 30f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${summary.appCount} Apps", col2X, cardTop + 60f, paint)

        // Metric 3: Top Application
        val col3X = margin + 350f
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("MOST USED APP", col3X, cardTop + 30f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val topLabel = summary.topAppLabel ?: "N/A"
        val truncatedTop = if (topLabel.length > 18) topLabel.take(16) + "…" else topLabel
        canvas.drawText(truncatedTop, col3X, cardTop + 55f, paint)
    }

    private fun drawTable(
        canvas: Canvas,
        paint: Paint,
        apps: List<AppUsageInfo>,
        totalDuration: Long,
        startY: Float
    ) {
        var currentY = startY

        // Table Header
        paint.color = Color.parseColor("#F1F5F9")
        paint.style = Paint.Style.FILL
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 24f, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colApp = margin + 10f
        val colCategory = margin + 200f
        val colLaunches = margin + 320f
        val colTime = margin + 390f
        val colPct = margin + 460f

        canvas.drawText("APPLICATION", colApp, currentY + 16f, paint)
        canvas.drawText("CATEGORY", colCategory, currentY + 16f, paint)
        canvas.drawText("LAUNCHES", colLaunches, currentY + 16f, paint)
        canvas.drawText("ACTIVE TIME", colTime, currentY + 16f, paint)
        canvas.drawText("% SHARE", colPct, currentY + 16f, paint)

        currentY += 24f

        // Table Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f

        for ((index, app) in apps.withIndex()) {
            val rowHeight = 22f
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                paint.style = Paint.Style.FILL
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint)
            }

            // Text
            paint.color = if (app.isRemoved) Color.parseColor("#DC2626") else Color.parseColor("#0F172A")
            val label = if (app.isRemoved) "${app.appLabel} (Removed)" else app.appLabel
            val truncatedLabel = if (label.length > 28) label.take(26) + "…" else label
            canvas.drawText(truncatedLabel, colApp, currentY + 15f, paint)

            paint.color = Color.parseColor("#64748B")
            canvas.drawText(app.category.name, colCategory, currentY + 15f, paint)
            canvas.drawText("${app.launchCount}", colLaunches, currentY + 15f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(DateTimeUtils.formatDuration(app.totalTimeForegroundMillis), colTime, currentY + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val pct = if (totalDuration > 0) {
                String.format(java.util.Locale.ENGLISH, "%.1f%%", (app.totalTimeForegroundMillis.toFloat() / totalDuration) * 100f)
            } else "0.0%"
            paint.color = Color.parseColor("#64748B")
            canvas.drawText(pct, colPct, currentY + 15f, paint)

            currentY += rowHeight
        }
    }

    private fun drawFooter(canvas: Canvas, paint: Paint, currentPage: Int, totalPages: Int) {
        val footerY = pageHeight - margin

        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(margin, footerY - 15f, pageWidth - margin, footerY - 15f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated by Chronicle - Privacy-First Usage Analytics", margin, footerY, paint)

        val pageStr = "Page $currentPage of $totalPages"
        val pageStrWidth = paint.measureText(pageStr)
        canvas.drawText(pageStr, pageWidth - margin - pageStrWidth, footerY, paint)
    }
}
