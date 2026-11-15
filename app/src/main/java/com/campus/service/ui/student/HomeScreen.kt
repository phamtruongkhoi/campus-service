package com.campus.service.ui.student

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
import com.campus.service.data.model.PublicIncident
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.StatusBadge
import com.campus.service.util.formatDateTime

@Composable
fun HomeScreen(vm: HomeViewModel) {
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { vm.observeChanges() }

    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Bảng tin sự cố", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Các hạng mục đang được bảo trì trong trường. Vui lòng kiểm tra trước khi gửi phiếu mới để tránh trùng lặp.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(4.dp))
        LiveIndicator(state.live)
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

        if (state.incidents.isEmpty() && !state.loading) {
            Text("Hiện không có sự cố nào đang được công khai.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.incidents, key = { it.ticketId }) { incident ->
                    IncidentCard(incident)
                }
            }
        }
    }
}

@Composable
private fun IncidentCard(incident: PublicIncident) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    incident.areaLabel,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                StatusBadge(incident.statusEnum)
            }
            Spacer(Modifier.height(4.dp))
            Text(incident.categoryName, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (incident.eta.isNullOrBlank()) "Dự kiến xong: chưa có"
                else "Dự kiến xong: ${formatDateTime(incident.eta)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Cập nhật: ${formatDateTime(incident.updatedAt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}