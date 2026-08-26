package io.chronicle.usagestats.ui.datausage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.domain.model.HourlyDataPoint
import io.chronicle.usagestats.ui.components.ChronicleCard
import java.util.Locale

@Composable
fun HourlyDataBarChart(
    hourlyDataPoints: List<HourlyDataPoint>,
    selectedHour: Int?,
    onHourSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    val maxBytes = remember(hourlyDataPoints) {
        hourlyDataPoints.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1024 * 1024L) ?: (1024 * 1024L)
    }

    ChronicleCard(modifier = modifier.fillMaxWidth()) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "HOURLY NETWORK TRAFFIC",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedHour != null) {
                    val point = hourlyDataPoints.getOrNull(selectedHour)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = primaryColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.ENGLISH, "%02d:00 • %s", selectedHour, DataSizeUtils.formatBytes(point?.totalBytes ?: 0L)),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                tint = primaryColor,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onHourSelected(null) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 24 Hour Bar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .pointerInput(hourlyDataPoints) {
                            detectTapGestures { offset ->
                                val barWidth = size.width / 24f
                                val tappedHour = (offset.x / barWidth).toInt().coerceIn(0, 23)
                                if (selectedHour == tappedHour) {
                                    onHourSelected(null)
                                } else {
                                    onHourSelected(tappedHour)
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val barWidth = (width / 24f) * 0.65f
                    val slotWidth = width / 24f

                    // Grid line
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 1f
                    )

                    for (h in 0..23) {
                        val point = hourlyDataPoints.getOrNull(h) ?: HourlyDataPoint(hour = h)
                        val slotCenterX = (h * slotWidth) + (slotWidth / 2f)
                        val barLeft = slotCenterX - (barWidth / 2f)

                        val isSelected = selectedHour == h
                        val isDimmed = selectedHour != null && !isSelected

                        val wifiHeight = (point.wifiBytes.toFloat() / maxBytes.toFloat()) * height
                        val mobileHeight = (point.mobileBytes.toFloat() / maxBytes.toFloat()) * height

                        val totalBarHeight = (wifiHeight + mobileHeight).coerceAtLeast(3f)

                        // Draw background slot highlight
                        if (isSelected) {
                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.12f),
                                topLeft = Offset(h * slotWidth, 0f),
                                size = Size(slotWidth, height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }

                        // Draw Wi-Fi bar (bottom)
                        if (wifiHeight > 0f) {
                            val wColor = if (isDimmed) primaryColor.copy(alpha = 0.25f) else primaryColor
                            drawRoundRect(
                                color = wColor,
                                topLeft = Offset(barLeft, height - wifiHeight),
                                size = Size(barWidth, wifiHeight),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }

                        // Draw Mobile bar (stacked on top of Wi-Fi)
                        if (mobileHeight > 0f) {
                            val mColor = if (isDimmed) secondaryColor.copy(alpha = 0.25f) else secondaryColor
                            drawRoundRect(
                                color = mColor,
                                topLeft = Offset(barLeft, height - wifiHeight - mobileHeight),
                                size = Size(barWidth, mobileHeight),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }

                        // Draw empty placeholder dot if 0 traffic
                        if (point.totalBytes == 0L) {
                            drawCircle(
                                color = if (isDimmed) surfaceVariant.copy(alpha = 0.3f) else surfaceVariant,
                                radius = 2f,
                                center = Offset(slotCenterX, height - 3f)
                            )
                        }
                    }
                }

                // Time labels below canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val keyHours = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "23:59")
                    keyHours.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = secondaryColor,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Mobile SIM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(16.dp))

                Surface(
                    shape = CircleShape,
                    color = primaryColor,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Wi-Fi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
