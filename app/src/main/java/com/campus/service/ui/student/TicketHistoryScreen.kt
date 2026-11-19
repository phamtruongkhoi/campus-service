package com.campus.service.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketStatus
import com.campus.service.data.repository.TicketAcceptanceRepository
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.AppTextField
import com.campus.service.ui.common.EmptyState
import com.campus.service.ui.common.GradientHeader
import com.campus.service.ui.common.IconInfoRow
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.PriorityTag
import com.campus.service.ui.common.SectionTitle
import com.campus.service.ui.common.SkeletonList
import com.campus.service.ui.common.StatusBadge
import com.campus.service.ui.common.StatusTimeline
import com.campus.service.ui.common.StorageImage
import com.campus.service.ui.theme.Campus
import com.campus.service.util.formatDateTime

@Composable
fun TicketHistoryScreen(userId: String, vm: TicketHistoryViewModel) {
    // Tải lại mỗi lần mở tab Lịch sử
    LaunchedEffect(userId) { vm.refresh(userId) }
    // Lắng nghe realtime trong lúc màn hình này đang hiển thị
    LaunchedEffect(userId) { vm.observeChanges() }

    val state by vm.state.collectAsState()
    val selected = state.selected

    BackHandler(enabled = selected != null) { vm.closeDetail() }

    if (selected != null) {
        TicketDetail(
            ticket = selected,
            events = state.events,
            live = state.live,
            accepting = state.accepting,
            actionError = state.actionError,
            info = state.info,
            onAccept = { rating, feedback -> vm.accept(selected, rating, feedback) },
            onReopen = { reason -> vm.reopen(selected, reason) },
            onBack = { vm.closeDetail() }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        // Tiêu đề + trạng thái realtime + nút làm mới
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Lịch sử phiếu", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${state.tickets.size} phiếu đã gửi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LiveIndicator(state.live)
            IconButton(onClick = { vm.refresh(userId) }, enabled = !state.loading) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Làm mới")
            }
        }
        Spacer(Modifier.height(8.dp))

        // Bộ lọc theo trạng thái
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.filter == null,
                onClick = { vm.setFilter(null) },
                label = { Text("Tất cả") }
            )
            TicketStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.filter == status,
                    onClick = { vm.setFilter(status) },
                    label = { Text(status.label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        MessageText(error = state.error, info = null)

        val items = state.visible
        when {
            state.loading && state.tickets.isEmpty() -> SkeletonList()
            items.isEmpty() -> {
                if (state.tickets.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Inbox,
                        title = "Chưa có phiếu nào",
                        message = "Bạn chưa gửi phiếu nào. Vào tab Tạo phiếu để báo sự cố."
                    )
                } else {
                    EmptyState(
                        icon = Icons.Outlined.FilterList,
                        title = "Không có phiếu phù hợp",
                        message = "Không có phiếu nào ở trạng thái này."
                    )
                }
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
            ) {
                items(items, key = { it.id }) { ticket ->
                    TicketCard(ticket = ticket, onClick = { vm.openDetail(ticket) })
                }
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: Ticket, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
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
        Spacer(Modifier.height(2.dp))
        Text(
            formatDateTime(ticket.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (ticket.statusEnum == TicketStatus.RESOLVED) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Campus.extra.success,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Đã khắc phục, bấm để nghiệm thu hoặc mở lại",
                    style = MaterialTheme.typography.labelMedium,
                    color = Campus.extra.success
                )
            }
        }
    }
}

@Composable
private fun TicketDetail(
    ticket: Ticket,
    events: List<TicketEvent>,
    live: Boolean,
    accepting: Boolean,
    actionError: String?,
    info: String?,
    onAccept: (Int, String) -> Unit,
    onReopen: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            Text(
                ticket.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            StatusBadge(ticket.statusEnum)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thông tin chung
            AppCard(modifier = Modifier.fillMaxWidth()) {
                IconInfoRow(Icons.Outlined.Place, "Khu vực", ticket.areaLabel)
                IconInfoRow(Icons.Outlined.Category, "Loại lỗi", ticket.categoryLabel)
                IconInfoRow(Icons.Outlined.Schedule, "Gửi lúc", formatDateTime(ticket.createdAt))
                IconInfoRow(Icons.Outlined.Event, "Dự kiến xong", formatDateTime(ticket.eta))
                IconInfoRow(Icons.Outlined.Update, "Cập nhật", formatDateTime(ticket.updatedAt))
                Spacer(Modifier.height(8.dp))
                PriorityTag(ticket.priorityEnum)
            }

            // Xác nhận kết quả: chỉ khi phiếu đã khắc phục (nghiệm thu hoặc mở lại)
            if (ticket.statusEnum == TicketStatus.RESOLVED) {
                ResolvedActions(
                    key = ticket.id,
                    accepting = accepting,
                    error = actionError,
                    onAccept = onAccept,
                    onReopen = onReopen
                )
            } else if (info != null) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    MessageText(error = null, info = info)
                }
            }

            // Timeline (có mốc "Mở lại phiếu" kèm lý do)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Tiến trình xử lý", trailing = { LiveIndicator(live) })
                Spacer(Modifier.height(8.dp))
                StatusTimeline(events = events, current = ticket.statusEnum)
            }

            // Mô tả
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Mô tả")
                Spacer(Modifier.height(6.dp))
                Text(
                    ticket.description?.ifBlank { "(không có)" } ?: "(không có)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Ảnh đính kèm
            if (ticket.images.isNotEmpty()) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "Ảnh đính kèm (${ticket.images.size})")
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

            // Đánh giá đã gửi
            if (ticket.rating != null || !ticket.feedback.isNullOrBlank()) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "Đánh giá của bạn")
                    Spacer(Modifier.height(6.dp))
                    if (ticket.rating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarRow(rating = ticket.rating, onSelect = null, enabled = false)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${ticket.rating}/5",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    if (!ticket.feedback.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(ticket.feedback, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** Hàng 5 ngôi sao. onSelect = null nghĩa là chỉ hiển thị, không bấm được. */
@Composable
private fun StarRow(rating: Int, onSelect: ((Int) -> Unit)?, enabled: Boolean) {
    val filled = Campus.extra.priorityMedium
    val empty = MaterialTheme.colorScheme.outline
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { star ->
            val on = star <= rating
            val icon = if (on) Icons.Filled.Star else Icons.Outlined.StarBorder
            val tint = if (on) filled else empty
            if (onSelect == null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                IconButton(onClick = { onSelect(star) }, enabled = enabled) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "$star sao",
                        tint = tint,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * Khi phiếu đã khắc phục, sinh viên chọn một trong hai:
 * - Đạt: chọn sao, nhận xét, nghiệm thu và đóng phiếu.
 * - Chưa đạt: nhập lý do (10 đến 300 ký tự), phiếu quay lại Đang xử lý.
 */
@Composable
private fun ResolvedActions(
    key: String,
    accepting: Boolean,
    error: String?,
    onAccept: (Int, String) -> Unit,
    onReopen: (String) -> Unit
) {
    var reopenMode by remember(key) { mutableStateOf(false) }
    var rating by remember(key) { mutableIntStateOf(0) }
    var feedback by remember(key) { mutableStateOf("") }
    var reason by remember(key) { mutableStateOf("") }

    val min = TicketAcceptanceRepository.REOPEN_MIN
    val max = TicketAcceptanceRepository.REOPEN_MAX

    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Xác nhận kết quả")
        Spacer(Modifier.height(4.dp))
        Text(
            "Kỹ thuật viên đã báo khắc phục. Bạn kiểm tra rồi chọn một trong hai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !reopenMode,
                onClick = { reopenMode = false },
                enabled = !accepting,
                label = { Text("Đạt, nghiệm thu") }
            )
            FilterChip(
                selected = reopenMode,
                onClick = { reopenMode = true },
                enabled = !accepting,
                label = { Text("Chưa đạt, mở lại") }
            )
        }
        Spacer(Modifier.height(8.dp))

        if (!reopenMode) {
            StarRow(rating = rating, onSelect = { rating = it }, enabled = !accepting)
            if (rating == 0) {
                Text(
                    "Chọn số sao để nghiệm thu.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = "Nhận xét (không bắt buộc)",
                singleLine = false,
                minLines = 2,
                readOnly = accepting,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            MessageText(error = error, info = null)
            LoadingButton(
                text = "Nghiệm thu và đóng phiếu",
                loading = accepting,
                onClick = { onAccept(rating, feedback) }
            )
        } else {
            Text(
                "Phiếu sẽ quay lại trạng thái Đang xử lý, cùng kỹ thuật viên cũ. " +
                        "Lý do của bạn hiện trong tiến trình xử lý.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = reason,
                onValueChange = { if (it.length <= max) reason = it },
                label = "Lý do chưa đạt",
                singleLine = false,
                minLines = 3,
                readOnly = accepting,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${reason.trim().length}/$max ký tự (tối thiểu $min)",
                style = MaterialTheme.typography.labelMedium,
                color = if (reason.trim().length in 1 until min)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            MessageText(error = error, info = null)
            LoadingButton(
                text = "Mở lại phiếu",
                loading = accepting,
                onClick = { onReopen(reason) }
            )
        }
    }
}
