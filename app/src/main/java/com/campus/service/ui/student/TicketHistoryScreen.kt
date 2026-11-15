package com.campus.service.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.StatusBadge
import com.campus.service.ui.common.StatusTimeline
import com.campus.service.ui.common.StorageImage
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
            onBack = { vm.closeDetail() }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text("Lịch sử phiếu", style = MaterialTheme.typography.headlineMedium)
        LiveIndicator(state.live)
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { vm.refresh(userId) }, enabled = !state.loading) {
                Text("Làm mới")
            }
            if (state.loading) {
                Spacer(Modifier.padding(horizontal = 8.dp))
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        val items = state.visible
        if (items.isEmpty() && !state.loading) {
            Text(
                if (state.tickets.isEmpty()) "Bạn chưa gửi phiếu nào."
                else "Không có phiếu nào ở trạng thái này."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { ticket ->
                    TicketCard(ticket = ticket, onClick = { vm.openDetail(ticket) })
                }
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: Ticket, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    ticket.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                StatusBadge(ticket.statusEnum)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${ticket.areaLabel} • ${ticket.categoryLabel}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(formatDateTime(ticket.createdAt), style = MaterialTheme.typography.bodySmall)
            if (ticket.statusEnum == TicketStatus.RESOLVED) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Đã khắc phục xong, bấm để nghiệm thu",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
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
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("< Quay lại danh sách") }

        Text(ticket.title, style = MaterialTheme.typography.headlineSmall)
        LiveIndicator(live)
        Spacer(Modifier.height(8.dp))
        StatusBadge(ticket.statusEnum)
        Spacer(Modifier.height(16.dp))

        InfoRow("Khu vực", ticket.areaLabel)
        InfoRow("Loại lỗi", ticket.categoryLabel)
        InfoRow("Mức ưu tiên", ticket.priorityEnum.label)
        InfoRow("Gửi lúc", formatDateTime(ticket.createdAt))
        InfoRow("Dự kiến xong (ETA)", formatDateTime(ticket.eta))
        InfoRow("Cập nhật lần cuối", formatDateTime(ticket.updatedAt))

        // Nghiệm thu: chỉ khi phiếu đã khắc phục và chưa có đánh giá
        if (ticket.statusEnum == TicketStatus.RESOLVED) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            AcceptanceForm(
                key = ticket.id,
                accepting = accepting,
                error = actionError,
                onAccept = onAccept
            )
        } else if (info != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            MessageText(error = null, info = info)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Tiến trình xử lý", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        StatusTimeline(events = events, current = ticket.statusEnum)

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Mô tả", style = MaterialTheme.typography.titleSmall)
        Text(ticket.description?.ifBlank { "(không có)" } ?: "(không có)")

        if (ticket.images.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Ảnh đính kèm (${ticket.images.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ticket.images.forEach { image ->
                StorageImage(
                    path = image.path,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (ticket.rating != null || !ticket.feedback.isNullOrBlank()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Đánh giá của bạn", style = MaterialTheme.typography.titleSmall)
            if (ticket.rating != null) Text("${ticket.rating}/5 sao")
            if (!ticket.feedback.isNullOrBlank()) Text(ticket.feedback)
        }
    }
}

@Composable
private fun AcceptanceForm(
    key: String,
    accepting: Boolean,
    error: String?,
    onAccept: (Int, String) -> Unit
) {
    var rating by remember(key) { mutableIntStateOf(0) }
    var feedback by remember(key) { mutableStateOf("") }

    Text("Nghiệm thu", style = MaterialTheme.typography.titleMedium)
    Text(
        "Sự cố đã được báo khắc phục. Vui lòng đánh giá để đóng phiếu.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { star ->
            FilterChip(
                selected = rating == star,
                onClick = { rating = star },
                enabled = !accepting,
                label = { Text("$star ★") }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = feedback,
        onValueChange = { feedback = it },
        label = { Text("Nhận xét (không bắt buộc)") },
        enabled = !accepting,
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    MessageText(error = error, info = null)
    if (rating == 0) {
        Text("Chọn số sao để nghiệm thu.", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
    }
    LoadingButton(
        text = "Nghiệm thu và đóng phiếu",
        loading = accepting,
        onClick = { onAccept(rating, feedback) }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}