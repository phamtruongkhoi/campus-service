package com.campus.service.ui.technician

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.campus.service.ui.common.HorizontalBarChart
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.StatusBadge

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Maintenance Radar", style = MaterialTheme.typography.headlineMedium)
            LiveIndicator(state.live)
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
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Sự cố đang mở", state.active.size, Modifier.weight(1f))
                StatCard("Chưa phân công", state.unassignedCount, Modifier.weight(1f))
                StatCard("Đang xử lý", state.inProgressCount, Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Khối lượng công việc", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    HorizontalBarChart(
                        items = state.workload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Text("Theo tòa nhà / khu vực", style = MaterialTheme.typography.titleMedium)
        }

        if (groups.isEmpty() && !state.loading) {
            item { Text("Hiện không có sự cố nào đang xử lý.") }
        }

        items(groups, key = { it.building }) { group ->
            BuildingCard(group)
        }

        item { Spacer(Modifier.height(16.dp)) }
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
private fun BuildingCard(group: BuildingGroup) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    group.building + if (group.mine) "  (có việc của bạn)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text("${group.tickets.size} sự cố", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (group.colleagues.isEmpty()) "Chưa có kỹ thuật viên nào tại đây"
                else "Kỹ thuật viên tại đây: " + group.colleagues.joinToString(", "),
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.tickets.forEach { ticket -> RadarRow(ticket) }
            }
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
            Text(ticket.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${ticket.areas?.name ?: "—"} • ${ticket.categoryLabel} • " +
                        "Ưu tiên ${ticket.priorityEnum.label}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Phụ trách: ${ticket.assigneeLabel}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.padding(horizontal = 4.dp))
        StatusBadge(ticket.statusEnum)
    }
}