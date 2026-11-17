package com.campus.service.ui.technician

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Ticket
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.EmptyState
import com.campus.service.ui.common.HorizontalBarChart
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.OverdueTag
import com.campus.service.ui.common.ScreenHeaderRow
import com.campus.service.ui.common.SectionTitle
import com.campus.service.ui.common.SkeletonList
import com.campus.service.ui.common.StatTile
import com.campus.service.ui.common.StatusBadge
import com.campus.service.ui.theme.Campus

@Composable
fun RadarScreen(userId: String, vm: RadarViewModel) {
    LaunchedEffect(userId) { vm.refresh(userId) }
    LaunchedEffect(userId) { vm.observeChanges(userId) }

    val state by vm.state.collectAsState()
    val groups = state.groups

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            ScreenHeaderRow(
                title = "Maintenance Radar",
                subtitle = "Sự cố toàn trường theo thời gian thực",
                live = state.live,
                loading = state.loading,
                onRefresh = { vm.refresh(userId) }
            )
            MessageText(error = state.error, info = null)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.Outlined.Warning,
                    label = "Đang mở",
                    value = "${state.active.size}",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Outlined.Inbox,
                    label = "Chưa giao",
                    value = "${state.unassignedCount}",
                    tint = Campus.extra.open.accent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Outlined.Build,
                    label = "Đang xử lý",
                    value = "${state.inProgressCount}",
                    tint = Campus.extra.inProgress.accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Khối lượng công việc")
                Spacer(Modifier.height(12.dp))
                HorizontalBarChart(
                    items = state.workload,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { SectionTitle(text = "Theo tòa nhà / khu vực") }

        when {
            state.loading && groups.isEmpty() -> item { SkeletonList() }
            groups.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Không có sự cố",
                    message = "Hiện không có sự cố nào đang xử lý."
                )
            }
            else -> items(groups, key = { it.building }) { group ->
                BuildingCard(group)
            }
        }
    }
}

@Composable
private fun BuildingCard(group: BuildingGroup) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(group.building, style = MaterialTheme.typography.titleMedium)
                if (group.mine) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Có việc của bạn",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Text("${group.tickets.size} sự cố", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (group.colleagues.isEmpty()) "Chưa có kỹ thuật viên nào tại đây"
            else "Kỹ thuật viên tại đây: " + group.colleagues.joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            group.tickets.forEach { ticket -> RadarRow(ticket) }
        }
    }
}

@Composable
private fun RadarRow(ticket: Ticket) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(ticket.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "${ticket.areas?.name ?: "—"} • ${ticket.categoryLabel} • " +
                        "Ưu tiên ${ticket.priorityEnum.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Phụ trách: ${ticket.assigneeLabel}",
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