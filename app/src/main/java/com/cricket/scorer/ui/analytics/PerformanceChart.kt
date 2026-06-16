package com.cricket.scorer.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.cricket.scorer.data.model.MatchPerformance

@Composable
fun PerformanceChart(
    performances: List<MatchPerformance>,
    isBatting: Boolean, // true = runs, false = wickets
    modifier: Modifier = Modifier
) {
    val themeColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textColor = MaterialTheme.colorScheme.onSurface

    val data = performances.take(5).reversed() // Show chronologically left-to-right

    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No recent matches", style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.5f))
        }
        return
    }

    val maxVal = if (isBatting) {
        val maxRuns = data.maxOf { it.runsScored }
        if (maxRuns < 30) 30f else maxRuns.toFloat()
    } else {
        val maxWkts = data.maxOf { it.wicketsTaken }
        if (maxWkts < 3) 3f else maxWkts.toFloat()
    }

    Column(modifier = modifier) {
        Text(
            text = if (isBatting) "Runs Trend (Last 5 matches)" else "Wickets Trend (Last 5 matches)",
            style = MaterialTheme.typography.titleMedium,
            color = themeColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val width = size.width
            val height = size.height
            val padding = 40f

            val chartWidth = width - 2 * padding
            val chartHeight = height - 2 * padding

            // Draw Y-axis grid lines (3 divisions)
            for (i in 0..3) {
                val y = padding + chartHeight * (1f - i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 2f
                )
                // Draw label
                val labelVal = (maxVal * (i / 3f)).toInt()
                drawContext.canvas.nativeCanvas.drawText(
                    labelVal.toString(),
                    10f,
                    y + 8f,
                    android.graphics.Paint().apply {
                        color = textColor.toArgb()
                        textSize = 28f
                    }
                )
            }

            // Draw bars
            val barCount = data.size
            val barWidth = (chartWidth / barCount) * 0.4f
            val spacing = (chartWidth / barCount) * 0.6f

            data.forEachIndexed { index, perf ->
                val scoreVal = if (isBatting) perf.runsScored.toFloat() else perf.wicketsTaken.toFloat()
                val barHeight = (scoreVal / maxVal) * chartHeight
                
                val x = padding + spacing / 2 + index * (barWidth + spacing)
                val y = padding + chartHeight - barHeight

                drawRoundRect(
                    color = themeColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Label on top of the bar
                drawContext.canvas.nativeCanvas.drawText(
                    scoreVal.toInt().toString(),
                    x + barWidth / 2 - 12f,
                    y - 12f,
                    android.graphics.Paint().apply {
                        color = textColor.toArgb()
                        textSize = 28f
                        isFakeBoldText = true
                    }
                )

                // X-axis label (Match indices)
                drawContext.canvas.nativeCanvas.drawText(
                    "M${index + 1}",
                    x + barWidth / 2 - 20f,
                    padding + chartHeight + 36f,
                    android.graphics.Paint().apply {
                        color = textColor.copy(alpha = 0.6f).toArgb()
                        textSize = 24f
                    }
                )
            }
        }
    }
}
