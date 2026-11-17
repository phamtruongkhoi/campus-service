package com.campus.service.ui.technician

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Ticket
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.EmptyState
import com.campus.service.ui.common.IconInfoRow
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.ScreenHeaderRow
import com.campus.service.ui.common.SkeletonList
import com.campus.service.ui.common.StatTile
import com.campus.service.ui.common.TicketDescriptionCard
import com.campus.service.ui.common.TicketDetailHeader
import com.campus.service.ui.common.TicketImagesCard
import com.campus.service.ui.common.TicketListCard
import com.campus.service.ui.common.TicketRatingCard
import com.campus.service.ui.common.TicketTimelineCard
import com.campus.service.ui.theme.Campus
import com.campus.service.util.formatDateTime

@Composable
fun TechHistoryScreen(userId: String, vm: TechHistoryViewModel) {
    LaunchedEffect(userId) { vm.refresh(userId) }
    LaunchedEffect(userId) { vm.observeChanges(userId) }

    val state by vm.state.collectAsState()
    val selected = state.selected

    BackHandler(enabled = selected != null) { vm.closeDetail() }

    if (selected != null) {
        HistoryDetail(ticket = selected, state = state, onBack = { vm.closeDetail() })
        return
    }

    val done = state.done

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            ScreenHeaderRow(
                title = "Lịch sử công việc",
                subtitle = "${done.size} việc đã hoàn thành",
                live = state.live,
                loading = state.loading,
                onRefresh = { vm.refresh(userId) }
            )
            MessageText(error = state.error, info = null)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.Outlined.CheckCircle,
                    label = "Đã giải quyết",
                    value = "${done.size}",
                    tint = Campus.extra.resolved.accent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Outlined.TaskAlt,
                    label = "Đã nghiệm thu",
                    value = "${state.closedCount}",
                    tint = Campus.extra.closed.accent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Outlined.StarBorder,
                    label = "Đánh giá TB",
                    value = state.averageRating,
                    tint = Campus.extra.priorityMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        when {
            state.loading && done.isEmpty() -> item { SkeletonList() }
            done.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = "Chưa có lịch sử",
                    message = "Bạn chưa hoàn thành sự cố nào."
                )
            }
            else -> items(done, key = { it.id }) { ticket ->
                TicketListCard(
                    ticket = ticket,
                    onClick = { vm.openDetail(ticket) },
                    showPriority = false,
                    lines = listOf(
                        if (ticket.rating != null) "Đánh giá: ${ticket.rating}/5 sao"
                        else "Chưa có đánh giá",
                        "Cập nhật: ${formatDateTime(ticket.updatedAt ?: ticket.createdAt)}"
                    )
                )
            }
        }
    }
}

@Composable
private fun HistoryDetail(ticket: Ticket, state: TechHistoryUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TicketDetailHeader(
            title = ticket.title,
            status = ticket.statusEnum,
            onBack = onBack
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
                if (ticket.resolvedAt != null) {
                    IconInfoRow(
                        Icons.Outlined.CheckCircle,
                        "Khắc phục lúc",
                        formatDateTime(ticket.resolvedAt)
                    )
                }
                IconInfoRow(Icons.Outlined.Update, "Cập nhật", formatDateTime(ticket.updatedAt))
            }

            TicketRatingCard(ticket, title = "Đánh giá của sinh viên", showEmpty = true)
            TicketTimelineCard(events = state.events, current = ticket.statusEnum, live = state.live)
            TicketDescriptionCard(ticket)
            TicketImagesCard(ticket, title = "Ảnh hiện trường")
        }
    }
}