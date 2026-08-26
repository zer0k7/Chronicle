package io.chronicle.usagestats.ui.datausage

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.chronicle.usagestats.domain.model.DailyDataPoint
import io.chronicle.usagestats.ui.components.ChronicleCard

@Composable
fun WeeklyDataBarChart(
    multiDayDataPoints: List<DailyDataPoint>,
    modifier: Modifier = Modifier
) {
    if (multiDayDataPoints.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    val maxBytes = remember(multiDayDataPoints) {
        multiDayDataPoints.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1024 * 1024L) ?: (1024 * 1024L)
    }

    ChronicleCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "HISTORICAL DAILY BANDWIDTH",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val barCount = multiDayDataPoints.size
                    val slotWidth = width / barCount
                    val barWidth = slotWidth * 0.55f

                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 1f
                    )

                    multiDayDataPoints.forEachIndexed { index, point ->
                        val slotCenterX = (index * slotWidth) + (slotWidth / 2f)
                        val barLeft = slotCenterX - (barWidth / 2f)

                        val wifiHeight = (point.wifiBytes.toFloat() / maxBytes.toFloat()) * height
                        val mobileHeight = (point.mobileBytes.toFloat() / maxBytes.toFloat()) * height

                        // Wi-Fi
                        if (wifiHeight > 0f) {
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(barLeft, height - wifiHeight),
                                size = Size(barWidth, wifiHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }

                        // Mobile
                        if (mobileHeight > 0f) {
                            drawRoundRect(
                                color = secondaryColor,
                                topLeft = Offset(barLeft, height - wifiHeight - mobileHeight),
                                size = Size(barWidth, mobileHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                // First and Last date label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = multiDayDataPoints.first().dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = multiDayDataPoints.last().dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
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
