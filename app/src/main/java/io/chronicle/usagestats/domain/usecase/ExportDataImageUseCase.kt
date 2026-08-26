package io.chronicle.usagestats.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.DailyDataUsageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Locale

class ExportDataImageUseCase(
    private val context: Context
) {
    private val imageWidth = 1080
    private val imageHeight = 1440

    suspend fun execute(
        summary: DailyDataUsageSummary,
        startDateMillis: Long,
        endDateMillis: Long,
        saveToGallery: Boolean = false
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            renderInfographic(canvas, summary, startDateMillis, endDateMillis)

            val uri = if (saveToGallery) {
                saveBitmapToMediaStore(bitmap)
            } else {
                saveBitmapToCache(bitmap)
            }

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun renderInfographic(
        canvas: Canvas,
        summary: DailyDataUsageSummary,
        startMillis: Long,
        endMillis: Long
    ) {
        canvas.drawColor(Color.parseColor("#0F172A"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 60f

        // Brand Title
        paint.color = Color.WHITE
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CHRONICLE", margin, margin + 40f, paint)

        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NETWORK TELEMETRY SUMMARY", margin, margin + 70f, paint)

        // Date Range
        val dateText = DateTimeUtils.formatDateRange(startMillis, endMillis)
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateWidth = paint.measureText(dateText)
        canvas.drawText(dateText, imageWidth - margin - dateWidth, margin + 50f, paint)

        // Hero Card
        val heroRect = RectF(margin, margin + 100f, imageWidth - margin, margin + 350f)
        paint.color = Color.parseColor("#1E293B")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(heroRect, 24f, 24f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL BANDWIDTH CONSUMED", margin + 30f, heroRect.top + 50f, paint)

        paint.color = Color.WHITE
        paint.textSize = 64f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(DataSizeUtils.formatBytes(summary.grandTotalBytes), margin + 30f, heroRect.top + 130f, paint)

        // Hero Sub-pills
        val subPillY = heroRect.top + 170f
        val pillWidth = (heroRect.width() - 80f) / 3f

        val heroPills = listOf(
            Triple("MOBILE DATA", DataSizeUtils.formatBytes(summary.totalMobileBytes), "#818CF8"),
            Triple("WI-FI DATA", DataSizeUtils.formatBytes(summary.totalWifiBytes), "#2DD4BF"),
            Triple("HOTSPOT SHARED", DataSizeUtils.formatBytes(summary.totalHotspotBytes), "#FBBF24")
        )

        heroPills.forEachIndexed { i, (label, valStr, colorHex) ->
            val pLeft = margin + 30f + (i * (pillWidth + 10f))
            val pRect = RectF(pLeft, subPillY, pLeft + pillWidth, subPillY + 54f)
            paint.color = Color.parseColor("#0F172A")
            canvas.drawRoundRect(pRect, 12f, 12f, paint)

            paint.color = Color.parseColor("#64748B")
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(label, pLeft + 10f, pRect.top + 20f, paint)

            paint.color = Color.parseColor(colorHex)
            paint.textSize = 18f
            canvas.drawText(valStr, pLeft + 10f, pRect.top + 44f, paint)
        }

        // Top 6 Bandwidth Consumers Header
        val listTop = heroRect.bottom + 40f
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOP BANDWIDTH APPLICATIONS", margin, listTop, paint)

        // App Rows
        var rowY = listTop + 30f
        val topApps = summary.appUsageList.take(6)
        val maxBytes = topApps.firstOrNull()?.totalBytes ?: 1L

        topApps.forEachIndexed { index, app ->
            val rowRect = RectF(margin, rowY, imageWidth - margin, rowY + 110f)
            paint.color = Color.parseColor("#1E293B")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rowRect, 16f, 16f, paint)

            paint.color = if (app.isRemoved) Color.parseColor("#F87171") else Color.WHITE
            paint.textSize = 24f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${index + 1}. ${app.appLabel}", margin + 24f, rowRect.top + 42f, paint)

            val details = buildString {
                if (app.mobileTotalBytes > 0) append("Mobile ${DataSizeUtils.formatBytes(app.mobileTotalBytes)}  ")
                if (app.wifiTotalBytes > 0) append("Wi-Fi ${DataSizeUtils.formatBytes(app.wifiTotalBytes)}  ")
                append("• Rx ${DataSizeUtils.formatBytes(app.wifiRxBytes + app.mobileRxBytes)} / Tx ${DataSizeUtils.formatBytes(app.wifiTxBytes + app.mobileTxBytes)}")
            }

            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(details, margin + 24f, rowRect.top + 70f, paint)

            // Total right aligned
            val totalStr = DataSizeUtils.formatBytes(app.totalBytes)
            paint.color = Color.parseColor("#38BDF8")
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val totalW = paint.measureText(totalStr)
            canvas.drawText(totalStr, imageWidth - margin - 24f - totalW, rowRect.top + 46f, paint)

            // Progress bar
            val ratio = (app.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.02f, 1f)
            val barW = (rowRect.width() - 48f) * ratio
            val barRect = RectF(margin + 24f, rowRect.bottom - 16f, margin + 24f + barW, rowRect.bottom - 8f)
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRoundRect(barRect, 4f, 4f, paint)

            rowY += 122f
        }

        // Footer
        val footerY = imageHeight - margin
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Chronicle • Privacy-First Telemetry", margin, footerY, paint)
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(cacheDir, "chronicle_data_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap): Uri {
        val filename = "chronicle_data_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Chronicle")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to insert media row")

        resolver.openOutputStream(uri)?.use { stream: OutputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        return uri
    }
}
