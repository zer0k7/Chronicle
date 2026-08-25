package io.chronicle.usagestats.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.HabitInsights
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
fun HabitsCard(
    insights: HabitInsights,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    ChronicleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title & Focus Quality Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.habits_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val (focusTextRes, focusColor) = when {
                    insights.fragmentationScore < 30 -> Pair(R.string.habits_fragmentation_focused, primaryColor)
                    insights.fragmentationScore < 60 -> Pair(R.string.habits_fragmentation_moderate, MaterialTheme.colorScheme.tertiary)
                    else -> Pair(R.string.habits_fragmentation_fragmented, MaterialTheme.colorScheme.error)
                }

                Surface(
                    color = focusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(focusTextRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = focusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2x2 Metric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Pickups
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.habits_unlocks),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                    Text(
                        text = "${insights.deviceUnlocks}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Column 2: Avg Session Duration
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.habits_avg_session),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                    Text(
                        text = DateTimeUtils.formatDuration(insights.avgSessionDurationMillis),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Sleep & Routine Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // First & Last Interaction
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.habits_first_unlock) + " • " + stringResource(R.string.habits_last_lock),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                    val firstStr = insights.firstUnlockEpochMillis?.let {
                        DateTimeUtils.toZonedDateTime(it).format(TIME_FORMATTER)
                    } ?: "—"
                    val lastStr = insights.lastLockEpochMillis?.let {
                        DateTimeUtils.toZonedDateTime(it).format(TIME_FORMATTER)
                    } ?: "—"

                    Text(
                        text = "$firstStr → $lastStr",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Bedtime Screen Time
                if (insights.bedtimeUsageMillis > 0) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.habits_bedtime_usage),
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                        Text(
                            text = DateTimeUtils.formatDuration(insights.bedtimeUsageMillis),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
