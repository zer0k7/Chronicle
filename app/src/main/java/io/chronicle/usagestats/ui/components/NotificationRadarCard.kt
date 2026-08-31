package io.chronicle.usagestats.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.PermissionHelper
import io.chronicle.usagestats.domain.model.FragmentationRisk
import io.chronicle.usagestats.domain.model.NotificationRadarData

@Composable
fun NotificationRadarCard(
    data: NotificationRadarData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (!data.isListenerPermissionGranted) {
        PermissionPromptCard(
            modifier = modifier,
            onGrantClick = {
                PermissionHelper.openNotificationListenerSettings(context)
            }
        )
        return
    }

    var selectedHour by remember { mutableStateOf<Int?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary

    val riskColor = when (data.fragmentationRisk) {
        FragmentationRisk.LOW -> Color(0xFF10B981)
        FragmentationRisk.MODERATE -> MaterialTheme.colorScheme.tertiary
        FragmentationRisk.ELEVATED -> Color(0xFFF59E0B)
        FragmentationRisk.HIGH -> MaterialTheme.colorScheme.error
    }

    val riskLabel = when (data.fragmentationRisk) {
        FragmentationRisk.LOW -> stringResource(R.string.notification_radar_risk_low)
        FragmentationRisk.MODERATE -> stringResource(R.string.notification_radar_risk_moderate)
        FragmentationRisk.ELEVATED -> stringResource(R.string.notification_radar_risk_elevated)
        FragmentationRisk.HIGH -> stringResource(R.string.notification_radar_risk_high)
    }

    ChronicleCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = primaryColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.notification_radar_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.notification_radar_total_pings, data.totalPings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Fragmentation Score Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = riskColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${data.fragmentationScore}/100",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score & State Explanation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.notification_radar_score_label).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = riskLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = riskColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.notification_radar_disruptive_pings, data.disruptivePings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (data.peakHour != null) {
                        Text(
                            text = "Peak at ${String.format("%02d:00", data.peakHour)} (${data.peakHourCount} pings)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 24-Hour Hourly Ping Density Canvas
            Text(
                text = stringResource(R.string.notification_radar_hourly_chart_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.notification_radar_hourly_chart_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val maxHourlyPings = (data.hourlySlots.maxOfOrNull { it.pingCount } ?: 1).coerceAtLeast(1)
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val tertiaryColor = MaterialTheme.colorScheme.tertiary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val colWidth = size.width / 24f
                            val hourTapped = (offset.x / colWidth).toInt().coerceIn(0, 23)
                            selectedHour = if (selectedHour == hourTapped) null else hourTapped
                        }
                    }
            ) {
                val colWidth = size.width / 24f
                val barWidth = colWidth * 0.65f
                val maxBarHeight = size.height - 18.dp.toPx()

                data.hourlySlots.forEach { slot ->
                    val xOffset = slot.hour * colWidth + (colWidth - barWidth) / 2f
                    val barHeight = if (slot.pingCount > 0) {
                        ((slot.pingCount.toFloat() / maxHourlyPings.toFloat()) * maxBarHeight).coerceAtLeast(4.dp.toPx())
                    } else {
                        2.dp.toPx()
                    }
                    val yOffset = maxBarHeight - barHeight

                    val isSelected = selectedHour == slot.hour
                    val barColor = when {
                        isSelected -> tertiaryColor
                        slot.pingCount > 0 -> primaryColor
                        else -> surfaceVariant.copy(alpha = 0.5f)
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(xOffset, yOffset),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }

            // Interactive Hour Inspection Pill
            AnimatedVisibility(visible = selectedHour != null) {
                selectedHour?.let { hour ->
                    val slot = data.hourlySlots.getOrNull(hour)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%02d:00 - %02d:00 IST", hour, (hour + 1) % 24),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${slot?.pingCount ?: 0} pings${if (slot?.topAppLabel != null) " - Top: ${slot.topAppLabel}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Bait & Switch Reflex Alerts
            if (data.baitAndSwitchEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.notification_radar_bait_switch_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                data.baitAndSwitchEvents.forEach { event ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.notification_radar_bait_switch_desc,
                                    event.triggerAppLabel,
                                    event.occurrenceCount,
                                    event.destinationAppLabel
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Top Disruptor Apps
            if (data.topDisruptors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.notification_radar_top_disruptors_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                data.topDisruptors.take(4).forEach { disruptor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = disruptor.appLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${disruptor.pingCount} pings (${disruptor.percentage.toInt()}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (disruptor.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = primaryColor,
                                trackColor = surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPromptCard(
    modifier: Modifier = Modifier,
    onGrantClick: () -> Unit
) {
    ChronicleCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.notification_radar_permission_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.notification_radar_permission_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onGrantClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.notification_radar_grant_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
