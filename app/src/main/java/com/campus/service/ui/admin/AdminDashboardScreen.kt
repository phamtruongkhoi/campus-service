package com.campus.service.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.EmptyState
import com.campus.service.ui.common.GradientHeader
import com.campus.service.ui.common.HorizontalBarChart
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.OverdueTag
import com.campus.service.ui.common.SectionTitle
import com.campus.service.ui.common.SkeletonList
import com.campus.service.ui.common.StatTile
import com.campus.service.ui.common.StatusBadge
import com.campus.service.ui.common.StatusDonutChart
import com.campus.service.ui.theme.Campus
import com.campus.service.util.formatDateTime
import com.campus.service.util.formatDuration

@Composable
fun AdminDashboardScreen(vm: AdminDashboardViewModel) {
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { vm.observeChanges() }

    val state by vm.state.collectAsState()
    val byStatus = state.countByStatus
    val extra = Campus.extra

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(bottomPadding = 28.dp) {
            Text(
                "Tổng quan hệ thống",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${state.total}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 44.sp),
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "phiếu trong hệ thống",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveIndicator(state.live)
                IconButton(onClick = { vm.refresh() }, enabled = !state.loading) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Làm mới")
                }
            }
            MessageText(error = state.error, info = null)

            if (state.loading && state.tickets.isEmpty()) {
                SkeletonList()
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        icon = Icons.Outlined.Inbox,
                        label = "Chờ tiếp nhận",
                        value = "${byStatus[TicketStatus.OPEN] ?: 0}",
                        tint = extra.open.accent,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Outlined.Build,
                        label = "Đang xử lý",
                        value = "${(byStatus[TicketStatus.ASSIGNED] ?: 0) + (byStatus[TicketStatus.IN_PROGRESS] ?: 0)}",
                        tint = extra.inProgress.accent,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        icon = Icons.Outlined.Warning,
                        label = "Quá hạn",
                        value = "${state.overdueCount}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Outlined.Timer,
                        label = "Xử lý trung bình",
                        value = state.averageProcessingMillis?.let { formatDuration(it) } ?: "—",
                        tint = extra.resolved.accent,
                        modifier = Modifier.weight(1f)
                    )
                }

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "Phân bố theo trạng thái")
                    Spacer(Modifier.height(12.dp))
                    StatusDonutChart(counts = byStatus)
                }

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "Số phiếu theo loại lỗi")
                    Spacer(Modifier.height(12.dp))
                    HorizontalBarChart(
                        items = state.countByCategory,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "Phiếu mới nhất")
                    Spacer(Modifier.height(8.dp))
                    if (state.recent.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.Inbox,
                            title = "Chưa có phiếu nào",
                            message = "Phiếu sinh viên gửi sẽ hiện ở đây."
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.recent.forEach { ticket -> RecentRow(ticket) }
                    }
                }
            }
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
            Text(ticket.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "${ticket.areaLabel} • ${formatDateTime(ticket.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ticket.isOverdue) {
                Spacer(Modifier.height(4.dp))
                OverdueTag()
            }
        }
        Spacer(Modifier.width(8.dp))
        StatusBadge(ticket.statusEnum)
    }
}