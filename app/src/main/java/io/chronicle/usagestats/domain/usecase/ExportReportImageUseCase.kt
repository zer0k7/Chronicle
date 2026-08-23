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
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.DailyUsageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ExportReportImageUseCase(
    private val context: Context
) {

    private val imageWidth = 1080
    private val imageHeight = 1440

    suspend fun execute(
        summary: DailyUsageSummary,
        startDateMillis: Long,
        endDateMillis: Long,
        saveToGallery: Boolean = false
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Render Card
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
        summary: DailyUsageSummary,
        startMillis: Long,
        endMillis: Long
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        paint.color = Color.parseColor("#0D0F12")
        canvas.drawRect(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat(), paint)

        // Top Accent Bar
        paint.color = Color.parseColor("#00E5FF")
        canvas.drawRect(0f, 0f, imageWidth.toFloat(), 12f, paint)

        // Header - App Brand
        paint.color = Color.parseColor("#FFFFFF")
        paint.textSize = 42f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CHRONICLE", 80f, 100f, paint)

        paint.color = Color.parseColor("#00E5FF")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DIGITAL USAGE REPORT", 80f, 135f, paint)

        // Date Range (Right)
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateText = DateTimeUtils.formatDateRange(startMillis, endMillis)
        val dateWidth = paint.measureText(dateText)
        canvas.drawText(dateText, imageWidth - 80f - dateWidth, 100f, paint)

        val tzText = "IST (UTC+5:30)"
        val tzWidth = paint.measureText(tzText)
        paint.textSize = 18f
        canvas.drawText(tzText, imageWidth - 80f - tzWidth, 132f, paint)

        // Main Stat Card
        val cardRect = RectF(80f, 180f, imageWidth - 80f, 440f)
        paint.color = Color.parseColor("#161B22")
        canvas.drawRoundRect(cardRect, 24f, 24f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL SCREEN TIME", 120f, 240f, paint)

        paint.color = Color.parseColor("#FFFFFF")
        paint.textSize = 68f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(DateTimeUtils.formatDuration(summary.totalScreenTimeMillis), 120f, 330f, paint)

        paint.color = Color.parseColor("#00E5FF")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${summary.appCount} active applications recorded", 120f, 390f, paint)

        // Top Applications Section Header
        paint.color = Color.parseColor("#FFFFFF")
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Top Applications", 80f, 510f, paint)

        // Top 5 Apps List with Progress Bars
        val topApps = summary.apps.take(5)
        var rowY = 570f
        val maxDuration = topApps.firstOrNull()?.totalTimeForegroundMillis ?: 1L

        for (app in topApps) {
            // App Box
            val appCard = RectF(80f, rowY, imageWidth - 80f, rowY + 110f)
            paint.color = Color.parseColor("#161B22")
            canvas.drawRoundRect(appCard, 16f, 16f, paint)

            // App Label
            paint.color = if (app.isRemoved) Color.parseColor("#EF4444") else Color.parseColor("#FFFFFF")
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val label = if (app.isRemoved) "${app.appLabel} (Removed)" else app.appLabel
            val truncatedLabel = if (label.length > 24) label.take(22) + "…" else label
            canvas.drawText(truncatedLabel, 110f, rowY + 45f, paint)

            // Duration
            paint.color = Color.parseColor("#00E5FF")
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val durText = DateTimeUtils.formatDuration(app.totalTimeForegroundMillis)
            val durWidth = paint.measureText(durText)
            canvas.drawText(durText, imageWidth - 110f - durWidth, rowY + 45f, paint)

            // Progress Bar Background
            val barY = rowY + 70f
            val barHeight = 12f
            val maxBarWidth = (imageWidth - 220f)
            val barBg = RectF(110f, barY, 110f + maxBarWidth, barY + barHeight)
            paint.color = Color.parseColor("#21262D")
            canvas.drawRoundRect(barBg, 6f, 6f, paint)

            // Progress Bar Fill
            val ratio = if (maxDuration > 0) app.totalTimeForegroundMillis.toFloat() / maxDuration.toFloat() else 0f
            val fillWidth = maxBarWidth * ratio.coerceIn(0.05f, 1.0f)
            val barFill = RectF(110f, barY, 110f + fillWidth, barY + barHeight)
            paint.color = if (app.isRemoved) Color.parseColor("#EF4444") else Color.parseColor("#00E5FF")
            canvas.drawRoundRect(barFill, 6f, 6f, paint)

            rowY += 135f
        }

        // Footer
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated locally with Chronicle", 80f, imageHeight - 60f, paint)

        val istTimestamp = DateTimeUtils.nowInIst().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", java.util.Locale.ENGLISH))
        val istText = "$istTimestamp IST"
        val istWidth = paint.measureText(istText)
        canvas.drawText(istText, imageWidth - 80f - istWidth, imageHeight - 60f, paint)
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(imagesDir, "chronicle_snapshot_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap): Uri {
        val fileName = "chronicle_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Chronicle")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        context.contentResolver.openOutputStream(uri).use { out: OutputStream? ->
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        return uri
    }
}
