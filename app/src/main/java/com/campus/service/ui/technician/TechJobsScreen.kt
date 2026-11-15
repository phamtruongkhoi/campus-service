package com.campus.service.ui.technician

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
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.StatusBadge
import com.campus.service.ui.common.StatusTimeline
import com.campus.service.ui.common.StorageImage
import com.campus.service.util.formatDateTime

@Composable
fun TechJobsScreen(userId: String, vm: TechJobsViewModel) {
    LaunchedEffect(userId) { vm.refresh(userId) }
    LaunchedEffect(userId) { vm.observeChanges(userId) }

    val state by vm.state.collectAsState()
    val selected = state.selected

    BackHandler(enabled = selected != null) { vm.closeDetail() }

    if (selected != null) {
        JobDetail(vm = vm, ticket = selected)
        return
    }

    val jobs = state.active

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Công việc của tôi", style = MaterialTheme.typography.headlineMedium)
        LiveIndicator(state.live)
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

        val waiting = jobs.count { it.statusEnum == TicketStatus.ASSIGNED }
        val working = jobs.count { it.statusEnum == TicketStatus.IN_PROGRESS }
        Text(
            "Cần nhận: $waiting • Đang xử lý: $working",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(8.dp))

        if (jobs.isEmpty() && !state.loading) {
            Text("Hiện chưa có việc nào được giao cho bạn.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(jobs, key = { it.id }) { ticket ->
                    JobCard(ticket = ticket, onClick = { vm.openDetail(ticket) })
                }
            }
        }
    }
}

@Composable
private fun JobCard(ticket: Ticket, onClick: () -> Unit) {
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
                "Ưu tiên: ${ticket.priorityEnum.label} • ETA: ${formatDateTime(ticket.eta)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun JobDetail(vm: TechJobsViewModel, ticket: Ticket) {
    val state by vm.state.collectAsState()

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

        InfoRow("Khu vực", ticket.areaLabel)
        InfoRow("Loại lỗi", ticket.categoryLabel)
        InfoRow("Mức ưu tiên", ticket.priorityEnum.label)
        InfoRow("Người báo", ticket.creatorLabel)
        InfoRow("Gửi lúc", formatDateTime(ticket.createdAt))
        InfoRow("ETA", formatDateTime(ticket.eta))

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        MessageText(error = state.error, info = state.info)

        when (ticket.statusEnum) {
            TicketStatus.ASSIGNED -> LoadingButton(
                text = "Nhận việc và bắt đầu xử lý",
                loading = state.saving,
                onClick = { vm.advance(ticket) }
            )
            TicketStatus.IN_PROGRESS -> LoadingButton(
                text = "Báo đã khắc phục xong",
                loading = state.saving,
                onClick = { vm.advance(ticket) }
            )
            TicketStatus.RESOLVED ->
                Text("Đã báo khắc phục. Chờ sinh viên nghiệm thu.")
            else -> Text("Phiếu này không còn thao tác.")
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
            Text("Ảnh hiện trường (${ticket.images.size})", style = MaterialTheme.typography.titleSmall)
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