package io.chronicle.usagestats.domain.usecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import io.chronicle.usagestats.domain.model.DataUsageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ExportDataPdfUseCase(
    private val context: Context
) {
    private val pageWidth = 595 // Standard A4 width in points
    private val pageHeight = 842 // Standard A4 height in points
    private val margin = 40f

    suspend fun execute(
        summary: DailyDataUsageSummary,
        startDateMillis: Long,
        endDateMillis: Long
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val apps = summary.appUsageList
            val appsPerPage = 16
            val totalPages = if (apps.isEmpty()) 1 else ((apps.size + appsPerPage - 1) / appsPerPage)

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                // Header
                drawHeader(canvas, paint, startDateMillis, endDateMillis)

                if (pageIndex == 0) {
                    // Summary Cards & Ratio Bar on first page
                    drawSummaryCards(canvas, paint, summary)
                }

                // Table Items for this page
                val startAppIndex = pageIndex * appsPerPage
                val endAppIndex = minOf(startAppIndex + appsPerPage, apps.size)
                val pageApps = if (apps.isNotEmpty()) apps.subList(startAppIndex, endAppIndex) else emptyList()

                val tableStartY = if (pageIndex == 0) 270f else 100f
                drawTable(canvas, paint, pageApps, summary.grandTotalBytes, tableStartY, startAppIndex)

                // Footer
                drawFooter(canvas, paint, pageIndex + 1, totalPages)
                pdfDocument.finishPage(page)
            }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val fileName = "chronicle_data_dossier_${System.currentTimeMillis()}.pdf"
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
        startMillis: Long,
        endMillis: Long
    ) {
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CHRONICLE", margin, margin + 15f, paint)

        paint.color = Color.parseColor("#0284C7")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NETWORK TELEMETRY & BANDWIDTH DOSSIER", margin, margin + 30f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateText = "Period: ${DateTimeUtils.formatDateRange(startMillis, endMillis)}"
        val tzText = "Timezone: IST (UTC+5:30)"
        val dateWidth = paint.measureText(dateText)
        val tzWidth = paint.measureText(tzText)

        canvas.drawText(dateText, pageWidth - margin - dateWidth, margin + 15f, paint)
        canvas.drawText(tzText, pageWidth - margin - tzWidth, margin + 28f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1.5f
        canvas.drawLine(margin, margin + 40f, pageWidth - margin, margin + 40f, paint)
    }

    private fun drawSummaryCards(
        canvas: Canvas,
        paint: Paint,
        summary: DailyDataUsageSummary
    ) {
        val cardTop = margin + 55f
        val cardWidth = (pageWidth - (margin * 2) - 30f) / 4f
        val cardHeight = 65f

        val metrics = listOf(
            Triple("TOTAL BANDWIDTH", DataSizeUtils.formatBytes(summary.grandTotalBytes), "#0284C7"),
            Triple("MOBILE DATA", DataSizeUtils.formatBytes(summary.totalMobileBytes), "#6366F1"),
            Triple("WI-FI DATA", DataSizeUtils.formatBytes(summary.totalWifiBytes), "#0D9488"),
            Triple("HOTSPOT SHARED", DataSizeUtils.formatBytes(summary.totalHotspotBytes), "#D97706")
        )

        metrics.forEachIndexed { index, (title, value, colorHex) ->
            val left = margin + (index * (cardWidth + 10f))
            val right = left + cardWidth

            paint.color = Color.parseColor("#F8FAFC")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(left, cardTop, right, cardTop + cardHeight, 6f, 6f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(left, cardTop, right, cardTop + cardHeight, 6f, 6f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(title, left + 8f, cardTop + 20f, paint)

            paint.color = Color.parseColor(colorHex)
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(value, left + 8f, cardTop + 45f, paint)
        }

        // Bandwidth Ratio Bar
        val barTop = cardTop + cardHeight + 15f
        val barWidth = pageWidth - (margin * 2)
        val total = summary.totalWifiBytes + summary.totalMobileBytes

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BANDWIDTH DISTRIBUTION", margin, barTop - 4f, paint)

        if (total > 0) {
            val wifiRatio = (summary.totalWifiBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val mobileRatio = (summary.totalMobileBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val ratioText = String.format(Locale.ENGLISH, "Wi-Fi: %.1f%% | Mobile SIM: %.1f%%", wifiRatio * 100f, mobileRatio * 100f)
            val ratioTextWidth = paint.measureText(ratioText)
            paint.color = Color.parseColor("#0284C7")
            canvas.drawText(ratioText, pageWidth - margin - ratioTextWidth, barTop - 4f, paint)

            val wifiWidth = barWidth * wifiRatio
            val mobileWidth = barWidth * mobileRatio

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0D9488")
            canvas.drawRoundRect(margin, barTop, margin + wifiWidth, barTop + 8f, 3f, 3f, paint)

            paint.color = Color.parseColor("#6366F1")
            canvas.drawRoundRect(margin + wifiWidth, barTop, margin + wifiWidth + mobileWidth, barTop + 8f, 3f, 3f, paint)
        }
    }

    private fun drawTable(
        canvas: Canvas,
        paint: Paint,
        apps: List<DataUsageInfo>,
        grandTotalBytes: Long,
        startY: Float,
        startIndex: Int
    ) {
        val colRank = margin + 5f
        val colApp = margin + 30f
        val colMobile = margin + 175f
        val colWifi = margin + 250f
        val colRx = margin + 325f
        val colTx = margin + 400f
        val colTotal = margin + 475f

        // Table Header
        paint.color = Color.parseColor("#0F172A")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(margin, startY, pageWidth - margin, startY + 22f, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("#", colRank, startY + 14f, paint)
        canvas.drawText("APPLICATION", colApp, startY + 14f, paint)
        canvas.drawText("MOBILE", colMobile, startY + 14f, paint)
        canvas.drawText("WI-FI", colWifi, startY + 14f, paint)
        canvas.drawText("DOWNLOAD (RX)", colRx, startY + 14f, paint)
        canvas.drawText("UPLOAD (TX)", colTx, startY + 14f, paint)
        canvas.drawText("TOTAL", colTotal, startY + 14f, paint)

        // Rows
        var currentY = startY + 28f
        val rowHeight = 22f

        apps.forEachIndexed { index, app ->
            val isEven = index % 2 == 0
            if (isEven) {
                paint.color = Color.parseColor("#F8FAFC")
                paint.style = Paint.Style.FILL
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint)
            }

            val textY = currentY + 14f
            val rankNumber = (startIndex + index + 1).toString()

            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(rankNumber, colRank, textY, paint)

            paint.color = if (app.isRemoved) Color.parseColor("#EF4444") else Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val appLabel = if (app.appLabel.length > 22) app.appLabel.take(20) + "..." else app.appLabel
            canvas.drawText(appLabel, colApp, textY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#334155")
            canvas.drawText(DataSizeUtils.formatBytes(app.mobileTotalBytes), colMobile, textY, paint)
            canvas.drawText(DataSizeUtils.formatBytes(app.wifiTotalBytes), colWifi, textY, paint)
            canvas.drawText(DataSizeUtils.formatBytes(app.wifiRxBytes + app.mobileRxBytes), colRx, textY, paint)
            canvas.drawText(DataSizeUtils.formatBytes(app.wifiTxBytes + app.mobileTxBytes), colTx, textY, paint)

            paint.color = Color.parseColor("#0284C7")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(DataSizeUtils.formatBytes(app.totalBytes), colTotal, textY, paint)

            currentY += rowHeight
        }
    }

    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        currentPage: Int,
        totalPages: Int
    ) {
        val footerY = pageHeight - margin + 15f

        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(margin, footerY - 15f, pageWidth - margin, footerY - 15f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated by Chronicle • Confidential & Privacy-First", margin, footerY, paint)

        val pageStr = "Page $currentPage of $totalPages"
        val pageWidthCalculated = paint.measureText(pageStr)
        canvas.drawText(pageStr, pageWidth - margin - pageWidthCalculated, footerY, paint)
    }
}
