package com.campus.service.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketStatus
import com.campus.service.util.formatDateTime

/** Tiến trình 5 bước của phiếu; bước đã qua có dấu ● và thời điểm, bước chưa tới có ○. */
@Composable
fun StatusTimeline(events: List<TicketEvent>, current: TicketStatus) {
    val colors = MaterialTheme.colorScheme
    Column {
        TicketStatus.entries.forEach { status ->
            val event = events.lastOrNull { it.status == status.dbValue }
            val reached = event != null || status.ordinal <= current.ordinal
            val isCurrent = status == current

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = if (reached) "●" else "○",
                    color = if (reached) colors.primary else colors.outline,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = status.label,
                        style = if (isCurrent) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.bodyMedium,
                        color = if (reached) colors.onSurface else colors.outline
                    )
                    if (event != null) {
                        Text(
                            text = formatDateTime(event.createdAt),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}