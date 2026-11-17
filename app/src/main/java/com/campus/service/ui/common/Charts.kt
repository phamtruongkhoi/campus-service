package com.campus.service.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.theme.Campus

/** Màu cố định cũ theo trạng thái (giữ lại cho nơi khác còn gọi). Biểu đồ dùng màu token qua Campus.extra. */
fun statusColor(status: TicketStatus): Color = when (status) {
    TicketStatus.OPEN -> Color(0xFF9E9E9E)
    TicketStatus.ASSIGNED -> Color(0xFF1E88E5)
    TicketStatus.IN_PROGRESS -> Color(0xFFFB8C00)
    TicketStatus.RESOLVED -> Color(0xFF43A047)
    TicketStatus.CLOSED -> Color(0xFF546E7A)
}

/** Biểu đồ vành khuyên số phiếu theo trạng thái, kèm chú thích có số lượng. */
@Composable
fun StatusDonutChart(counts: Map<TicketStatus, Int>, modifier: Modifier = Modifier) {
    val total = counts.values.sum()
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val extra = Campus.extra
    val colors: Map<TicketStatus, Color> = counts.keys.associateWith { extra.status(it).accent }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val strokeWidth = 22.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(diameter, diameter)

                if (total == 0) {
                    drawArc(
                        color = emptyColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    var start = -90f
                    counts.forEach { (status, count) ->
                        if (count > 0) {
                            val sweep = 360f * count / total
                            drawArc(
                                color = colors[status] ?: emptyColor,
                                startAngle = start,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                            start += sweep
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$total", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "phiếu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            counts.forEach { (status, count) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[status] ?: emptyColor, RoundedCornerShape(3.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${status.label}: $count", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Biểu đồ cột ngang: mỗi dòng gồm nhãn, thanh và số lượng. */
@Composable
fun HorizontalBarChart(
    items: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val maxValue = (items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (items.isEmpty()) {
            Text(
                "Chưa có dữ liệu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(120.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(trackColor, RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(value.toFloat() / maxValue)
                            .height(12.dp)
                            .background(barColor, RoundedCornerShape(6.dp))
                    )
                }
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}