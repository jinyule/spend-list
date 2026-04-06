package com.spendlist.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class LineChartEntry(
    val label: String,
    val value: Float
)

@Composable
fun SimpleLineChart(
    entries: List<LineChartEntry>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return
    val maxValue = entries.maxOf { it.value }.coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val padding = 16.dp.toPx()
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2
            val step = chartWidth / (entries.size - 1).coerceAtLeast(1)

            // Draw grid lines
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            for (i in 0..4) {
                val y = padding + chartHeight * (1 - i / 4f)
                drawLine(gridColor, Offset(padding, y), Offset(size.width - padding, y))
            }

            // Draw line path
            val path = Path()
            val points = entries.mapIndexed { index, entry ->
                val x = padding + index * step
                val y = padding + chartHeight * (1 - entry.value / maxValue)
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                path.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    path.lineTo(points[i].x, points[i].y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw dots
                points.forEach { point ->
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            entries.forEach { entry ->
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
