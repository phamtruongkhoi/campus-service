package com.campus.service.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.theme.Campus
import com.campus.service.util.formatDateTime

private data class TimelineItem(
    val label: String,
    val time: String?,
    val note: String?,
    val accent: Color?,   // null = bước chưa tới
)

/**
 * Tiến trình xử lý của phiếu, theo thứ tự thời gian thật.
 * - Mỗi lần đổi trạng thái là một mốc có màu theo trạng thái, kèm giờ.
 * - Lần Resolved -> In progress được hiện là "Mở lại phiếu" kèm lý do của sinh viên.
 * - Các bước phía sau trạng thái hiện tại hiện mờ (chưa tới).
 */
@Composable
fun StatusTimeline(events: List<TicketEvent>, current: TicketStatus) {
    val colors = MaterialTheme.colorScheme
    val extra = Campus.extra

    val ordered = events.sortedBy { it.createdAt }
    val items = mutableListOf<TimelineItem>()

    if (ordered.isEmpty()) {
        // Chưa có sự kiện: suy ra từ trạng thái hiện tại
        TicketStatus.entries.filter { it.ordinal <= current.ordinal }.forEach { s ->
            items += TimelineItem(s.label, null, null, extra.status(s).accent)
        }
    } else {
        ordered.forEachIndexed { index, event ->
            val status = TicketStatus.from(event.status)
            val previous = ordered.getOrNull(index - 1)?.status
            val reopened = status == TicketStatus.IN_PROGRESS && previous == TicketStatus.RESOLVED.dbValue
            items += TimelineItem(
                label = if (reopened) "Mở lại phiếu" else status.label,
                time = formatDateTime(event.createdAt),
                note = event.note?.ifBlank { null },
                accent = if (reopened) colors.error else extra.status(status).accent
            )
        }
    }
    TicketStatus.entries.filter { it.ordinal > current.ordinal }.forEach { s ->
        items += TimelineItem(s.label, null, null, null)
    }

    Column {
        items.forEachIndexed { index, item ->
            val last = index == items.lastIndex
            val reached = item.accent != null
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier.width(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .then(
                                if (reached) Modifier.background(item.accent!!)
                                else Modifier.border(2.dp, colors.outline, CircleShape)
                            )
                    )
                    if (!last) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(colors.outlineVariant)
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(start = 8.dp, bottom = if (last) 0.dp else 12.dp)
                ) {
                    Text(
                        text = item.label,
                        style = if (reached) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.bodyMedium,
                        color = if (reached) colors.onSurface else colors.outline
                    )
                    if (item.time != null) {
                        Text(
                            text = item.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    if (item.note != null) {
                        Text(
                            text = "Lý do: ${item.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
