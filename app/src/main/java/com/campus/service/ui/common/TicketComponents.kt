package com.campus.service.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.theme.Campus

/** Nhãn "Quá hạn" (ETA đã qua mà phiếu chưa xong). */
@Composable
fun OverdueTag(modifier: Modifier = Modifier) {
    val fg = MaterialTheme.colorScheme.onErrorContainer
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text("Quá hạn", style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/** Hàng tiêu đề của màn hình dạng tab: tiêu đề, phụ đề, trạng thái realtime, nút làm mới. */
@Composable
fun ScreenHeaderRow(
    title: String,
    subtitle: String?,
    live: Boolean,
    loading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LiveIndicator(live)
        IconButton(onClick = onRefresh, enabled = !loading) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Làm mới")
        }
    }
}

/** Thẻ phiếu dùng chung cho danh sách (Điều phối, Lịch sử, Công việc...). */
@Composable
fun TicketListCard(
    ticket: Ticket,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lines: List<String> = emptyList(),
    showPriority: Boolean = true
) {
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                ticket.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            StatusBadge(ticket.statusEnum)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${ticket.areaLabel} • ${ticket.categoryLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        lines.forEach { line ->
            Spacer(Modifier.height(2.dp))
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showPriority || ticket.isOverdue) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showPriority) PriorityTag(ticket.priorityEnum)
                if (ticket.isOverdue) OverdueTag()
            }
        }
    }
}

/** Dải tiêu đề gradient của màn chi tiết phiếu, có nút quay lại. */
@Composable
fun TicketDetailHeader(
    title: String,
    status: TicketStatus,
    onBack: () -> Unit,
    overdue: Boolean = false
) {
    GradientHeader(bottomPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại danh sách",
                    tint = Color.White
                )
            }
            Text(
                "Chi tiết phiếu",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(status)
            if (overdue) OverdueTag()
        }
    }
}

@Composable
fun TicketTimelineCard(events: List<TicketEvent>, current: TicketStatus, live: Boolean) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Tiến trình xử lý", trailing = { LiveIndicator(live) })
        Spacer(Modifier.height(8.dp))
        StatusTimeline(events = events, current = current)
    }
}

@Composable
fun TicketDescriptionCard(ticket: Ticket) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Mô tả")
        Spacer(Modifier.height(6.dp))
        Text(
            ticket.description?.ifBlank { "(không có)" } ?: "(không có)",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Thẻ ảnh đính kèm; không hiện gì nếu phiếu không có ảnh. */
@Composable
fun TicketImagesCard(ticket: Ticket, title: String = "Ảnh đính kèm") {
    if (ticket.images.isEmpty()) return
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "$title (${ticket.images.size})")
        Spacer(Modifier.height(8.dp))
        ticket.images.forEach { image ->
            StorageImage(
                path = image.path,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Hàng sao chỉ để hiển thị. */
@Composable
fun RatingStars(rating: Int) {
    val filled = Campus.extra.priorityMedium
    val empty = MaterialTheme.colorScheme.outline
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { star ->
            val on = star <= rating
            Icon(
                imageVector = if (on) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (on) filled else empty,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Thẻ đánh giá của sinh viên. showEmpty = true thì hiện cả khi chưa có đánh giá. */
@Composable
fun TicketRatingCard(ticket: Ticket, title: String, showEmpty: Boolean = false) {
    val hasRating = ticket.rating != null
    val hasFeedback = !ticket.feedback.isNullOrBlank()
    if (!hasRating && !hasFeedback && !showEmpty) return
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = title)
        Spacer(Modifier.height(6.dp))
        if (!hasRating && !hasFeedback) {
            Text(
                "Chưa có đánh giá.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (ticket.rating != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingStars(ticket.rating)
                Spacer(Modifier.width(8.dp))
                Text("${ticket.rating}/5", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (hasFeedback) {
            Spacer(Modifier.height(6.dp))
            Text(ticket.feedback.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}