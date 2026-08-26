package io.chronicle.usagestats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.ui.theme.ColorSuccess

/**
 * Circular progress ring showing daily screen time usage vs the configured goal.
 * Changes color based on proximity to the goal:
 * - Green: under 60% of goal
 * - Yellow/Tertiary: 60-90% of goal
 * - Red/Error: over 90% of goal
 */
@Composable
fun GoalProgressRing(
    currentUsageMillis: Long,
    goalMinutes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp
) {
    val goalMillis = goalMinutes * 60L * 1000L
    val progress = if (goalMillis > 0) {
        (currentUsageMillis.toFloat() / goalMillis).coerceIn(0f, 1.5f)
    } else 0f

    val percentInt = (progress * 100).toInt()

    val ringColor = when {
        progress < 0.6f -> ColorSuccess
        progress < 0.9f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val goalFormatted = DateTimeUtils.formatDuration(goalMillis)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val sweepAngle = (progress.coerceAtMost(1f) * 360f)

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Progress arc
                if (sweepAngle > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidth.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Center percentage text
            Text(
                text = "$percentInt%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
        }

        Text(
            text = stringResource(R.string.goal_progress_format, percentInt, goalFormatted),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
