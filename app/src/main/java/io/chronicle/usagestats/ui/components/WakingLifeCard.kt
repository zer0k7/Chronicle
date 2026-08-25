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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.HabitInsights
import java.time.Year
import java.util.Locale

@Composable
fun WakingLifeCard(
    insights: HabitInsights,
    modifier: Modifier = Modifier
) {
    val wakingImpact = insights.wakingLifeImpact ?: return
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val currentYear = Year.now().value

    ChronicleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title & Annual Days Lost Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.waking_life_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (wakingImpact.annualProjectedDays > 0) {
                    val badgeColor = if (wakingImpact.wakingPercentage > 35.0) errorColor else primaryColor
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.waking_life_annual_projection, wakingImpact.annualProjectedDays, currentYear),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Percentage of Waking Life Bar
            val pctFloat = (wakingImpact.wakingPercentage / 100.0).toFloat().coerceIn(0.01f, 1f)
            val barColor = if (wakingImpact.wakingPercentage > 35.0) errorColor else primaryColor

            Text(
                text = String.format(Locale.ENGLISH, "%.1f%%", wakingImpact.wakingPercentage),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
            Text(
                text = stringResource(R.string.waking_life_pct_label, wakingImpact.wakingPercentage),
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { pctFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // Ghost Opens & Morning Bed Doomscroll Rows
            if (insights.ghostOpens != null || insights.morningDoomscroll != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Ghost Reflex Opens (< 30s)
                    insights.ghostOpens?.let { ghost ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ghost_opens_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant
                            )
                            val topApp = ghost.topGhostAppLabel ?: "—"
                            Text(
                                text = stringResource(R.string.ghost_opens_format, ghost.totalGhostOpens, topApp, ghost.topGhostAppOpens),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Morning Bed Screen Time (within 45m of waking)
                    insights.morningDoomscroll?.let { doom ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.morning_doomscroll_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant
                            )
                            val doomDurationStr = DateTimeUtils.formatDuration(doom.durationMillis)
                            val topApp = doom.topAppLabel ?: "—"
                            Text(
                                text = stringResource(R.string.morning_doomscroll_format, doomDurationStr, topApp),
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
}
