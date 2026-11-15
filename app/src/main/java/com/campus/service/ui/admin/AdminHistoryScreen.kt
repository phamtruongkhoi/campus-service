package com.campus.service.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketPriority
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.common.DropdownField
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.StatusBadge
import com.campus.service.ui.common.StatusTimeline
import com.campus.service.ui.common.StorageImage
import com.campus.service.util.formatDateTime

private data class FilterOption(val value: String?, val label: String)

private val ALL_OPTION = FilterOption(null, "Tất cả")

@Composable
fun AdminHistoryScreen(vm: AdminHistoryViewModel) {
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { vm.observeChanges() }

    val state by vm.state.collectAsState()
    val selected = state.selected

    BackHandler(enabled = selected != null) { vm.closeDetail() }

    if (selected != null) {
        HistoryDetail(ticket = selected, state = state, onBack = { vm.closeDetail() })
        return
    }

    val filters = state.filters
    val statusOptions = listOf(ALL_OPTION) +
            TicketStatus.entries.map { FilterOption(it.dbValue, it.label) }
    val priorityOptions = listOf(ALL_OPTION) +
            TicketPriority.entries.map { FilterOption(it.dbValue, it.label) }
    val categoryOptions = listOf(ALL_OPTION) + state.categories.map { FilterOption(it, it) }
    val areaOptions = listOf(ALL_OPTION) + state.areas.map { FilterOption(it, it) }

    val visible = state.visible

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Lịch sử toàn trường", style = MaterialTheme.typography.headlineMedium)
            LiveIndicator(state.live)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = filters.query,
                onValueChange = vm::setQuery,
                label = { Text("Tìm theo tiêu đề, mô tả, người gửi, khu vực") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Trạng thái",
                    items = statusOptions,
                    selected = statusOptions.firstOrNull { it.value == filters.status } ?: ALL_OPTION,
                    itemLabel = { it.label },
                    onSelected = { vm.setStatus(it.value) },
                    modifier = Modifier.weight(1f)
                )
                DropdownField(
                    label = "Ưu tiên",
                    items = priorityOptions,
                    selected = priorityOptions.firstOrNull { it.value == filters.priority } ?: ALL_OPTION,
                    itemLabel = { it.label },
                    onSelected = { vm.setPriority(it.value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Loại lỗi",
                    items = categoryOptions,
                    selected = categoryOptions.firstOrNull { it.value == filters.category } ?: ALL_OPTION,
                    itemLabel = { it.label },
                    onSelected = { vm.setCategory(it.value) },
                    modifier = Modifier.weight(1f)
                )
                DropdownField(
                    label = "Khu vực",
                    items = areaOptions,
                    selected = areaOptions.firstOrNull { it.value == filters.area } ?: ALL_OPTION,
                    itemLabel = { it.label },
                    onSelected = { vm.setArea(it.value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            MessageText(error = state.error, info = null)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tìm thấy ${visible.size} / ${state.tickets.size} phiếu",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.loading) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        Spacer(Modifier.padding(horizontal = 4.dp))
                    }
                    if (filters.active) {
                        TextButton(onClick = { vm.clearFilters() }) { Text("Xóa bộ lọc") }
                    }
                    OutlinedButton(onClick = { vm.refresh() }, enabled = !state.loading) {
                        Text("Làm mới")
                    }
                }
            }
        }
        if (visible.isEmpty() && !state.loading) {
            item { Text("Không có phiếu nào khớp bộ lọc.") }
        }
        items(visible, key = { it.id }) { ticket ->
            HistoryCard(ticket = ticket, onClick = { vm.openDetail(ticket) })
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HistoryCard(ticket: Ticket, onClick: () -> Unit) {
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
            Text(
                "Ưu tiên: ${ticket.priorityEnum.label} • KTV: ${ticket.assigneeLabel}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${ticket.creatorLabel} • ${formatDateTime(ticket.createdAt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun HistoryDetail(ticket: Ticket, state: AdminHistoryUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("< Quay lại danh sách") }

        Text(ticket.title, style = MaterialTheme.typography.headlineSmall)
        LiveIndicator(state.live)
        Spacer(Modifier.height(8.dp))
        StatusBadge(ticket.statusEnum)
        Spacer(Modifier.height(12.dp))

        InfoRow("Người gửi", ticket.creatorLabel)
        InfoRow("Khu vực", ticket.areaLabel)
        InfoRow("Loại lỗi", ticket.categoryLabel)
        InfoRow("Mức ưu tiên", ticket.priorityEnum.label)
        InfoRow("Kỹ thuật viên", ticket.assigneeLabel)
        InfoRow("Gửi lúc", formatDateTime(ticket.createdAt))
        InfoRow("ETA", formatDateTime(ticket.eta))
        InfoRow("Công khai", if (ticket.isPublic) "Có" else "Không")
        InfoRow("Cập nhật lần cuối", formatDateTime(ticket.updatedAt))

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Tiến trình xử lý", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        StatusTimeline(events = state.events, current = ticket.statusEnum)

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
            Text("Đánh giá của sinh viên", style = MaterialTheme.typography.titleSmall)
            if (ticket.rating != null) Text("${ticket.rating}/5 sao")
            if (!ticket.feedback.isNullOrBlank()) Text(ticket.feedback)
        }
    }
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
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}