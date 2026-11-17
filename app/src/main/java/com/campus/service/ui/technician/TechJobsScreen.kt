package com.campus.service.ui.technician

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.EmptyState
import com.campus.service.ui.common.IconInfoRow
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.ScreenHeaderRow
import com.campus.service.ui.common.SectionTitle
import com.campus.service.ui.common.SkeletonList
import com.campus.service.ui.common.StatTile
import com.campus.service.ui.common.TicketDescriptionCard
import com.campus.service.ui.common.TicketDetailHeader
import com.campus.service.ui.common.TicketImagesCard
import com.campus.service.ui.common.TicketListCard
import com.campus.service.ui.common.TicketTimelineCard
import com.campus.service.ui.theme.Campus
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
    val waiting = jobs.count { it.statusEnum == TicketStatus.ASSIGNED }
    val working = jobs.count { it.statusEnum == TicketStatus.IN_PROGRESS }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            ScreenHeaderRow(
                title = "Công việc của tôi",
                subtitle = "${jobs.size} việc đang giao",
                live = state.live,
                loading = state.loading,
                onRefresh = { vm.refresh(userId) }
            )
            MessageText(error = state.error, info = null)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.Outlined.Inbox,
                    label = "Cần nhận",
                    value = "$waiting",
                    tint = Campus.extra.assigned.accent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Outlined.Build,
                    label = "Đang xử lý",
                    value = "$working",
                    tint = Campus.extra.inProgress.accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        when {
            state.loading && jobs.isEmpty() -> item { SkeletonList() }
            jobs.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Chưa có việc",
                    message = "Hiện chưa có việc nào được giao cho bạn."
                )
            }
            else -> items(jobs, key = { it.id }) { ticket ->
                TicketListCard(
                    ticket = ticket,
                    onClick = { vm.openDetail(ticket) },
                    lines = listOf("ETA: ${formatDateTime(ticket.eta)}")
                )
            }
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
    ) {
        TicketDetailHeader(
            title = ticket.title,
            status = ticket.statusEnum,
            onBack = { vm.closeDetail() },
            overdue = ticket.isOverdue
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                IconInfoRow(Icons.Outlined.Place, "Khu vực", ticket.areaLabel)
                IconInfoRow(Icons.Outlined.Category, "Loại lỗi", ticket.categoryLabel)
                IconInfoRow(Icons.Outlined.Flag, "Mức ưu tiên", ticket.priorityEnum.label)
                IconInfoRow(Icons.Outlined.Person, "Người báo", ticket.creatorLabel)
                IconInfoRow(Icons.Outlined.Schedule, "Gửi lúc", formatDateTime(ticket.createdAt))
                IconInfoRow(Icons.Outlined.Event, "ETA", formatDateTime(ticket.eta))
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Thao tác")
                Spacer(Modifier.height(8.dp))
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
                    TicketStatus.RESOLVED -> Text(
                        "Đã báo khắc phục. Chờ sinh viên nghiệm thu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Campus.extra.success
                    )
                    else -> Text(
                        "Phiếu này không còn thao tác.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TicketTimelineCard(events = state.events, current = ticket.statusEnum, live = state.live)
            TicketDescriptionCard(ticket)
            TicketImagesCard(ticket, title = "Ảnh hiện trường")
        }
    }
}