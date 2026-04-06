package com.spendlist.app.ui.screen.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendlist.app.R
import com.spendlist.app.ui.component.*
import java.text.DecimalFormat

@Composable
private fun resolvedCategoryName(name: String, nameResKey: String?): String {
    if (nameResKey == null) return name
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(nameResKey, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else name
}

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.stats_tab_category),
        stringResource(R.string.stats_tab_trend),
        stringResource(R.string.stats_tab_compare)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            when (selectedTab) {
                0 -> CategoryTab(uiState)
                1 -> TrendTab(uiState)
                2 -> CompareTab(uiState)
            }
        }
    }
}

@Composable
private fun CategoryTab(uiState: StatsUiState) {
    val formatter = DecimalFormat("#,##0.00")
    val total = uiState.categorySpending.sumOf { it.amount.toDouble() }

    if (uiState.categorySpending.isEmpty()) {
        EmptyStats()
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DonutChart(
                entries = uiState.categorySpending.map { spending ->
                    DonutChartEntry(
                        label = resolvedCategoryName(spending.categoryName, spending.categoryNameResKey),
                        value = spending.amount.toFloat(),
                        color = Color(spending.color)
                    )
                },
                centerText = "${uiState.primaryCurrency.symbol}${formatter.format(total)}",
                centerSubText = stringResource(R.string.home_per_month),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        items(uiState.categorySpending) { spending ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color dot
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(Color(spending.color))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = resolvedCategoryName(spending.categoryName, spending.categoryNameResKey),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${uiState.primaryCurrency.symbol}${formatter.format(spending.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${spending.percentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendTab(uiState: StatsUiState) {
    if (uiState.monthlyTrend.isEmpty()) {
        EmptyStats()
        return
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SimpleLineChart(
            entries = uiState.monthlyTrend.map { monthly ->
                LineChartEntry(
                    label = "${monthly.yearMonth.monthValue}",
                    value = monthly.amount.toFloat()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    }
}

@Composable
private fun CompareTab(uiState: StatsUiState) {
    if (uiState.categorySpending.isEmpty()) {
        EmptyStats()
        return
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SimpleBarChart(
            entries = uiState.categorySpending.map { spending ->
                BarChartEntry(
                    label = resolvedCategoryName(spending.categoryName, spending.categoryNameResKey),
                    value = spending.amount.toFloat(),
                    color = Color(spending.color)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    }
}

@Composable
private fun EmptyStats() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
