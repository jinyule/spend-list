package com.spendlist.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendlist.app.R
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.ui.theme.StatusColors
import com.spendlist.app.util.DateFormatter
import com.spendlist.app.util.MoneyFormatter
import java.math.BigDecimal

@Composable
fun SubscriptionCard(
    subscription: Subscription,
    onClick: () -> Unit,
    convertedAmount: BigDecimal? = null,
    primaryCurrency: Currency? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left accent bar — highlights EXPIRED subscriptions in the list.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        if (subscription.status == SubscriptionStatus.EXPIRED)
                            MaterialTheme.colorScheme.error
                        else
                            Color.Transparent
                    )
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subscription.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Name + info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatRenewalDate(subscription),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount + status
            Column(horizontalAlignment = Alignment.End) {
                val displayAmount = convertedAmount ?: subscription.amount
                val displayCurrency = primaryCurrency ?: subscription.currency
                // When converted, amount is monthly average, so always show "/mo"
                val suffix = if (convertedAmount != null) {
                    stringResource(R.string.home_per_month)
                } else {
                    formatCycleSuffix(subscription.billingCycle)
                }
                Text(
                    text = "${MoneyFormatter.format(displayAmount, displayCurrency)}$suffix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(subscription.status)
            }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SubscriptionStatus) {
    val (color, textResId) = when (status) {
        SubscriptionStatus.ACTIVE -> StatusColors.Active to R.string.status_active
        SubscriptionStatus.CANCELLED -> StatusColors.Cancelled to R.string.status_cancelled
        SubscriptionStatus.EXPIRED -> StatusColors.Expired to R.string.status_expired
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = stringResource(textResId),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun formatRenewalDate(subscription: Subscription): String {
    return DateFormatter.format(subscription.nextRenewalDate)
}

@Composable
private fun formatCycleSuffix(cycle: BillingCycle): String {
    return when (cycle) {
        is BillingCycle.Monthly -> stringResource(R.string.home_per_month)
        is BillingCycle.Quarterly -> stringResource(R.string.home_per_quarter)
        is BillingCycle.Yearly -> stringResource(R.string.home_per_year)
        is BillingCycle.Custom -> "/${cycle.days}d"
    }
}

