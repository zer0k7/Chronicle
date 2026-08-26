package io.chronicle.usagestats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.chronicle.usagestats.R
import io.chronicle.usagestats.core.util.DateTimeUtils
import io.chronicle.usagestats.domain.model.AppCategory

private fun getCategoryColor(category: AppCategory): Color {
    return when (category) {
        AppCategory.PRODUCTIVITY -> Color(0xFF3B82F6) // Blue
        AppCategory.SOCIAL -> Color(0xFF8B5CF6) // Violet
        AppCategory.ENTERTAINMENT -> Color(0xFFF59E0B) // Amber
        AppCategory.GAMES, AppCategory.GAMING -> Color(0xFFEF4444) // Red
        AppCategory.COMMUNICATION -> Color(0xFF06B6D4) // Cyan
        AppCategory.UTILITIES -> Color(0xFF10B981) // Emerald Green
        AppCategory.SYSTEM -> Color(0xFF6B7280) // Gray
        AppCategory.REMOVED -> Color(0xFF9CA3AF) // Slate
        else -> Color(0xFF64748B)
    }
}

private fun getCategoryNameRes(category: AppCategory): Int {
    return when (category) {
        AppCategory.PRODUCTIVITY -> R.string.category_name_productivity
        AppCategory.SOCIAL -> R.string.category_name_social
        AppCategory.ENTERTAINMENT -> R.string.category_name_entertainment
        AppCategory.GAMES -> R.string.category_name_games
        AppCategory.COMMUNICATION -> R.string.category_name_communication
        AppCategory.UTILITIES -> R.string.category_name_utilities
        AppCategory.SYSTEM -> R.string.category_name_system
        else -> R.string.category_name_other
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDistributionBar(
    categoryBreakdown: Map<AppCategory, Long>,
    productivityScore: Int,
    modifier: Modifier = Modifier
) {
    val totalTime = categoryBreakdown.values.sum().coerceAtLeast(1L)
    val sortedCategories = categoryBreakdown.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }

    if (sortedCategories.isEmpty()) return

    ChronicleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Productivity Score badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.category_distribution_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.productivity_score_label, productivityScore),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment horizontal bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                sortedCategories.forEach { (cat, duration) ->
                    val weight = (duration.toFloat() / totalTime).coerceAtLeast(0.01f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(12.dp)
                            .background(getCategoryColor(cat))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Legend Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedCategories.take(6).forEach { (cat, duration) ->
                    val pct = ((duration.toDouble() / totalTime) * 100).toInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(cat))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(getCategoryNameRes(cat)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "($pct% • ${DateTimeUtils.formatDuration(duration)})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
