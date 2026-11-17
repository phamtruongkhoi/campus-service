package com.campus.service.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.campus.service.data.model.PublicIncident
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.EmptyState
import com.campus.service.ui.common.GradientHeader
import com.campus.service.ui.common.IconInfoRow
import com.campus.service.ui.common.LiveIndicator
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.StatusBadge
import com.campus.service.util.formatDateTime

@Composable
fun HomeScreen(vm: HomeViewModel) {
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { vm.observeChanges() }

    val state by vm.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Bảng tin sự cố",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    IconButton(onClick = { vm.refresh() }, enabled = !state.loading) {
                        Icon(Icons.Outlined.Refresh, "Làm mới", tint = Color.White)
                    }
                }
                Text(
                    "Các hạng mục đang được xử lý trong trường. Hãy kiểm tra trước khi gửi phiếu mới để tránh trùng lặp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(10.dp))
                LiveIndicator(state.live)
            }
        }

        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (state.error != null) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                MessageText(error = state.error, info = null)
            }
        }

        if (state.incidents.isEmpty() && !state.loading) {
            EmptyState(
                icon = Icons.Outlined.CheckCircle,
                title = "Mọi thứ đang ổn",
                message = "Hiện không có sự cố nào đang được công khai."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.incidents, key = { it.ticketId }) { incident ->
                    IncidentCard(incident)
                }
            }
        }
    }
}

@Composable
private fun IncidentCard(incident: PublicIncident) {
    AppCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    incident.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                StatusBadge(incident.statusEnum)
            }
            Spacer(Modifier.height(10.dp))
            IconInfoRow(Icons.Outlined.Place, "Vị trí", incident.areaLabel)
            Spacer(Modifier.height(6.dp))
            IconInfoRow(
                Icons.Outlined.Schedule,
                "Dự kiến xong",
                if (incident.eta.isNullOrBlank()) "Chưa có" else formatDateTime(incident.eta)
            )
            Spacer(Modifier.height(6.dp))
            IconInfoRow(Icons.Outlined.Update, "Cập nhật", formatDateTime(incident.updatedAt))
        }
    }
}