package io.chronicle.usagestats.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.chronicle.usagestats.domain.repository.UsageRepository
import java.io.File
import javax.inject.Inject

class ExportCsvUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend operator fun invoke(
        context: Context,
        startDateEpochMillis: Long,
        endDateEpochMillis: Long
    ): File {
        val file = usageRepository.exportUsageToCsv(startDateEpochMillis, endDateEpochMillis)

        val uri = FileProvider.getUriForFile(
            context,
            "io.chronicle.usagestats.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Export Chronicle Usage CSV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
        return file
    }
}
