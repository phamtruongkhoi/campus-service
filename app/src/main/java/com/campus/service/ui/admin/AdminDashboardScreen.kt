package com.campus.service.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.common.HorizontalBarChart
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.StatusBadge
import com.campus.service.ui.common.StatusDonutChart
import com.campus.service.util.formatDateTime

@Composable
fun AdminDashboardScreen(vm: AdminDashboardViewModel) {
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { vm.observeChanges() }

    val state by vm.state.collectAsState()
    val byStatus = state.countByStatus

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tổng quan hệ thống", style = MaterialTheme.typography.headlineMedium)
        LiveIndicator(state.live)
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

        // Ba thẻ số liệu
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Tổng số phiếu", state.total, Modifier.weight(1f))
            StatCard(
                "Chờ tiếp nhận",
                byStatus[TicketStatus.OPEN] ?: 0,
                Modifier.weight(1f)
            )
            StatCard(
                "Đang xử lý",
                (byStatus[TicketStatus.ASSIGNED] ?: 0) + (byStatus[TicketStatus.IN_PROGRESS] ?: 0),
                Modifier.weight(1f)
            )
        }

        SectionCard("Phân bố theo trạng thái") {
            StatusDonutChart(counts = byStatus)
        }

        SectionCard("Số phiếu theo loại lỗi") {
            HorizontalBarChart(
                items = state.countByCategory,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard("Phiếu mới nhất") {
            if (state.recent.isEmpty()) {
                Text("Chưa có phiếu nào.")
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.recent.forEach { ticket -> RecentRow(ticket) }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RecentRow(ticket: Ticket) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(ticket.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${ticket.areaLabel} • ${formatDateTime(ticket.createdAt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.padding(horizontal = 4.dp))
        StatusBadge(ticket.statusEnum)
    }
}