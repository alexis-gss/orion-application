package com.orion.app.cinema.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orion.app.core.ui.components.SectionTitle
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.orion.app.R

@Composable
fun FinancialInfoSection(budget: Long?, revenue: Long?) {
    val hasBudget = budget != null && budget > 0
    val hasRevenue = revenue != null && revenue > 0

    if (!hasBudget && !hasRevenue) return

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    // Profit / loss calculation
    val profit = if (hasBudget && hasRevenue) revenue!! - budget!! else null

    // Colors with opacity (alpha): adapt correctly to both Light Mode and Dark Mode
    val (profitColor, profitContainerColor) = when {
        profit == null -> MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.surfaceContainerHigh
        profit >= 0 -> Color(0xFF4CAF50) to Color(0xFF4CAF50).copy(alpha = 0.15f) // Green
        else -> Color(0xFFE53935) to Color(0xFFE53935).copy(alpha = 0.15f)        // Red
    }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        SectionTitle(
            title = stringResource(R.string.finance_title),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // LazyRow lets the cards keep a minimum width without being compressed
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasBudget) {
                item {
                    FinancialCard(
                        label = stringResource(R.string.finance_budget),
                        value = currencyFormatter.format(budget)
                    )
                }
            }

            if (hasRevenue) {
                item {
                    FinancialCard(
                        label = stringResource(R.string.finance_revenue),
                        value = currencyFormatter.format(revenue)
                    )
                }
            }

            if (profit != null) {
                item {
                    FinancialCard(
                        label = if (profit >= 0) stringResource(R.string.finance_profit) else stringResource(R.string.finance_loss),
                        value = (if (profit > 0) "+" else "") + currencyFormatter.format(profit),
                        valueColor = profitColor,
                        containerColor = profitContainerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    Card(
        modifier = modifier.widthIn(min = 110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                // Switched to a smaller size (labelLarge or bodySmall) for large numbers
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}