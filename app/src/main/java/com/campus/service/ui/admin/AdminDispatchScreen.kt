package com.campus.service.ui.admin

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Profile
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketPriority
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.common.DropdownField
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.StatusBadge
import com.campus.service.ui.common.StatusTimeline
import com.campus.service.ui.common.StorageImage
import com.campus.service.util.formatDateTime

private data class TechOption(val id: String?, val label: String)

@Composable
fun AdminDispatchScreen(vm: AdminDispatchViewModel) {
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { vm.observeChanges() }

    val state by vm.state.collectAsState()
    val selected = state.selected

    BackHandler(enabled = selected != null) { vm.closeDetail() }

    if (selected != null) {
        DispatchDetail(vm = vm, ticket = selected)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Điều phối phiếu", style = MaterialTheme.typography.headlineMedium)
        LiveIndicator(state.live)
        Spacer(Modifier.height(8.dp))

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
            OutlinedButton(onClick = { vm.refresh() }, enabled = !state.loading) {
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
            Text("Không có phiếu nào ở trạng thái này.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { ticket ->
                    DispatchCard(ticket = ticket, onClick = { vm.openDetail(ticket) })
                }
            }
        }
    }
}

@Composable
private fun DispatchCard(ticket: Ticket, onClick: () -> Unit) {
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
private fun DispatchDetail(vm: AdminDispatchViewModel, ticket: Ticket) {
    val state by vm.state.collectAsState()

    val techOptions = remember(state.technicians) {
        listOf(TechOption(null, "(Chưa phân công)")) +
                state.technicians.map { TechOption(it.id, techLabel(it)) }
    }

    // Giữ giá trị đang chỉnh khi realtime làm mới danh sách (khóa theo mã phiếu)
    var priority by remember(ticket.id) { mutableStateOf(ticket.priorityEnum) }
    var technicianId by remember(ticket.id) { mutableStateOf(ticket.assignedTo) }
    var eta by remember(ticket.id) { mutableStateOf(ETA_CHOICES.first()) }
    var isPublic by remember(ticket.id) { mutableStateOf(ticket.isPublic) }

    val editable = ticket.statusEnum != TicketStatus.CLOSED
    val currentTech = techOptions.firstOrNull { it.id == technicianId } ?: techOptions.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = { vm.closeDetail() }) { Text("< Quay lại danh sách") }

        Text(ticket.title, style = MaterialTheme.typography.headlineSmall)
        LiveIndicator(state.live)
        Spacer(Modifier.height(8.dp))
        StatusBadge(ticket.statusEnum)
        Spacer(Modifier.height(12.dp))

        InfoRow("Người gửi", ticket.creatorLabel)
        InfoRow("Khu vực", ticket.areaLabel)
        InfoRow("Loại lỗi", ticket.categoryLabel)
        InfoRow("Gửi lúc", formatDateTime(ticket.createdAt))
        InfoRow("KTV hiện tại", ticket.assigneeLabel)
        InfoRow("ETA hiện tại", formatDateTime(ticket.eta))
        InfoRow("Công khai", if (ticket.isPublic) "Có" else "Không")

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Điều phối", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        DropdownField(
            label = "Mức ưu tiên",
            items = TicketPriority.entries.toList(),
            selected = priority,
            itemLabel = { it.label },
            onSelected = { priority = it },
            enabled = editable,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "Kỹ thuật viên",
            items = techOptions,
            selected = currentTech,
            itemLabel = { it.label },
            onSelected = { technicianId = it.id },
            enabled = editable,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "Thời gian dự kiến xong (ETA)",
            items = ETA_CHOICES,
            selected = eta,
            itemLabel = { it.label },
            onSelected = { eta = it },
            enabled = editable,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hiển thị trên bảng tin công khai")
            Switch(checked = isPublic, onCheckedChange = { isPublic = it }, enabled = editable)
        }
        Spacer(Modifier.height(12.dp))

        MessageText(error = state.error, info = state.info)
        if (editable) {
            LoadingButton(
                text = "Lưu thay đổi",
                loading = state.saving,
                onClick = { vm.save(ticket, priority, technicianId, eta, isPublic) }
            )
        } else {
            Text("Phiếu đã đóng, không thể chỉnh sửa.")
        }

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
    }
}

private fun techLabel(profile: Profile): String =
    profile.fullName?.ifBlank { null } ?: profile.email ?: profile.id

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