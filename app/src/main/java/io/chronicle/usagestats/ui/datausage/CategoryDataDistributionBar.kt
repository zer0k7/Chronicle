package io.chronicle.usagestats.ui.datausage

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.chronicle.usagestats.core.util.DataSizeUtils
import io.chronicle.usagestats.domain.model.AppCategory
import io.chronicle.usagestats.domain.model.CategoryDataShare
import io.chronicle.usagestats.ui.components.ChronicleCard
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDataDistributionBar(
    categoryShares: List<CategoryDataShare>,
    modifier: Modifier = Modifier
) {
    if (categoryShares.isEmpty()) return

    val totalBytes = categoryShares.sumOf { it.totalBytes }
    if (totalBytes <= 0) return

    val categoryColors = mapOf(
        AppCategory.SOCIAL to Color(0xFF6366F1),
        AppCategory.ENTERTAINMENT to Color(0xFFEF4444),
        AppCategory.PRODUCTIVITY to Color(0xFF10B981),
        AppCategory.GAMING to Color(0xFFF59E0B),
        AppCategory.COMMUNICATION to Color(0xFF06B6D4),
        AppCategory.EDUCATION to Color(0xFF8B5CF6),
        AppCategory.NEWS to Color(0xFFEC4899),
        AppCategory.SYSTEM to Color(0xFF64748B),
        AppCategory.OTHER to Color(0xFF94A3B8),
        AppCategory.REMOVED to Color(0xFFE11D48)
    )

    ChronicleCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "CATEGORY BANDWIDTH DISTRIBUTION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            // Multi-segment proportional bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                categoryShares.forEach { share ->
                    val weight = (share.totalBytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.01f)
                    val color = categoryColors[share.category] ?: MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(10.dp)
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Legend FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryShares.take(6).forEach { share ->
                    val color = categoryColors[share.category] ?: MaterialTheme.colorScheme.primary
                    val catName = share.category.name.lowercase().replaceFirstChar { it.uppercase() }
                    val shareStr = String.format(Locale.ENGLISH, "%.0f%% (%s)", share.percentage, DataSizeUtils.formatBytes(share.totalBytes))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$catName: $shareStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
