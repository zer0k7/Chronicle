package io.chronicle.usagestats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.HourlyUsageSlot

@Composable
fun HourlyBarChart(
    hourlySlots: List<HourlyUsageSlot>,
    selectedHour: Int?,
    onHourSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightColor = MaterialTheme.colorScheme.tertiary

    ChronicleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Selected Hour indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.hourly_timeline_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedHour != null && selectedHour in 0..23) {
                        val slot = hourlySlots.getOrNull(selectedHour)
                        val durationText = slot?.let { DateTimeUtils.formatDuration(it.totalDurationMillis) } ?: "0m"
                        val startHourStr = String.format("%02d:00", selectedHour)
                        val endHourStr = String.format("%02d:00", (selectedHour + 1) % 24)
                        Text(
                            text = stringResource(R.string.hourly_slot_selected, startHourStr, endHourStr) + " • $durationText",
                            style = MaterialTheme.typography.bodySmall,
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.hourly_timeline_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                    }
                }

                if (selectedHour != null) {
                    TextButton(
                        onClick = { onHourSelected(null) }
                    ) {
                        Text(
                            text = stringResource(R.string.hourly_slot_clear),
                            style = MaterialTheme.typography.labelMedium,
                            color = primaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 24-Column Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .pointerInput(hourlySlots) {
                        detectTapGestures { offset ->
                            val columnWidth = size.width / 24f
                            val tappedIndex = (offset.x / columnWidth).toInt().coerceIn(0, 23)
                            if (selectedHour == tappedIndex) {
                                onHourSelected(null) // deselect
                            } else {
                                onHourSelected(tappedIndex)
                            }
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 20.dp.toPx() // reserve bottom for labels
                val maxMinutes = 60f // max 60 mins in an hour
                val barSpacing = 2.dp.toPx()
                val totalBars = 24
                val barWidth = ((canvasWidth - (barSpacing * (totalBars - 1))) / totalBars).coerceAtLeast(2f)

                // Background track and active bars
                for (i in 0 until totalBars) {
                    val x = i * (barWidth + barSpacing)
                    val slot = hourlySlots.getOrNull(i)
                    val durationMinutes = slot?.let { it.totalDurationMillis / 60000f } ?: 0f
                    val fraction = (durationMinutes / maxMinutes).coerceIn(0f, 1f)

                    val barHeight = if (fraction > 0f) {
                        (fraction * canvasHeight).coerceAtLeast(4.dp.toPx())
                    } else {
                        2.dp.toPx() // subtle baseline dot
                    }

                    val isSelected = selectedHour == i
                    val barColor = when {
                        isSelected -> highlightColor
                        durationMinutes > 0f -> primaryColor
                        else -> outlineVariant
                    }

                    // Background slot pillar (dim)
                    drawRoundRect(
                        color = surfaceVariant.copy(alpha = 0.5f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, canvasHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Active usage fill (aligned to bottom)
                    val y = canvasHeight - barHeight
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            // Hour Markers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("00:00", "06:00", "12:00", "18:00", "23:59").forEach { marker ->
                    Text(
                        text = marker,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = onSurfaceVariant
                    )
                }
            }
        }
    }
}
