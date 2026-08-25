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
import java.util.Locale

@Composable
fun AdvancedPsychologyCard(
    insights: HabitInsights,
    modifier: Modifier = Modifier
) {
    val lifeClock = insights.lifeClock
    val dopamineDebt = insights.dopamineDebt
    val phantom = insights.phantomUnlocks

    if (lifeClock == null && dopamineDebt == null && phantom == null) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    ChronicleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Psychological Habit Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Life Clock Projection
            if (lifeClock != null && lifeClock.yearsLostBy75 > 0.1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.life_clock_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.ENGLISH, "%.1f full years of life", lifeClock.yearsLostBy75),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (lifeClock.yearsLostBy75 > 10.0) errorColor else primaryColor
                        )
                        Text(
                            text = stringResource(R.string.life_clock_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        color = (if (lifeClock.yearsLostBy75 > 10.0) errorColor else primaryColor).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.ENGLISH, "%.1f%% of Life", lifeClock.consciousPercentage),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (lifeClock.yearsLostBy75 > 10.0) errorColor else primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. Dopamine Debt & Fasting
            if (dopamineDebt != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.dopamine_debt_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )

                    if (dopamineDebt.debtMillis > 0) {
                        val debtStr = DateTimeUtils.formatDuration(dopamineDebt.debtMillis)
                        Text(
                            text = stringResource(R.string.dopamine_debt_format, debtStr, dopamineDebt.recommendedFastMinutes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = tertiaryColor
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.dopamine_debt_balanced),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    }

                    Text(
                        text = stringResource(R.string.dopamine_debt_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // 3. Phantom Reflex Unlocks
            if (phantom != null && phantom.count > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.phantom_unlocks_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.phantom_unlocks_format, phantom.count, phantom.totalQuickChecks),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.phantom_unlocks_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
