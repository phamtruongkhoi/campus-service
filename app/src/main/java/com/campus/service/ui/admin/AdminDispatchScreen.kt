package com.campus.service.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.CommentSection
import com.campus.service.ui.common.DropdownField
import com.campus.service.ui.common.EmptyState
import com.campus.service.ui.common.IconInfoRow
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.ScreenHeaderRow
import com.campus.service.ui.common.SectionTitle
import com.campus.service.ui.common.SkeletonList
import com.campus.service.ui.common.TicketDescriptionCard
import com.campus.service.ui.common.TicketDetailHeader
import com.campus.service.ui.common.TicketImagesCard
import com.campus.service.ui.common.TicketListCard
import com.campus.service.ui.common.TicketTimelineCard
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
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        ScreenHeaderRow(
            title = "Điều phối phiếu",
            subtitle = "${state.visible.size} phiếu",
            live = state.live,
            loading = state.loading,
            onRefresh = { vm.refresh() }
        )
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

        val items = state.visible
        when {
            state.loading && items.isEmpty() -> SkeletonList()
            items.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Inbox,
                title = "Không có phiếu",
                message = "Không có phiếu nào ở trạng thái này."
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items, key = { it.id }) { ticket ->
                    TicketListCard(
                        ticket = ticket,
                        onClick = { vm.openDetail(ticket) },
                        lines = listOf(
                            "KTV: ${ticket.assigneeLabel}",
                            "${ticket.creatorLabel} • ${formatDateTime(ticket.createdAt)}"
                        )
                    )
                }
            }
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
                IconInfoRow(Icons.Outlined.Person, "Người gửi", ticket.creatorLabel)
                IconInfoRow(Icons.Outlined.Place, "Khu vực", ticket.areaLabel)
                IconInfoRow(Icons.Outlined.Category, "Loại lỗi", ticket.categoryLabel)
                IconInfoRow(Icons.Outlined.Schedule, "Gửi lúc", formatDateTime(ticket.createdAt))
                IconInfoRow(Icons.Outlined.Engineering, "KTV hiện tại", ticket.assigneeLabel)
                IconInfoRow(Icons.Outlined.Event, "ETA hiện tại", formatDateTime(ticket.eta))
                IconInfoRow(Icons.Outlined.Public, "Công khai", if (ticket.isPublic) "Có" else "Không")
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(text = "Điều phối")
                Spacer(Modifier.height(10.dp))
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
                    Text(
                        "Hiển thị trên bảng tin công khai",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
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
                    Text(
                        "Phiếu đã đóng, không thể chỉnh sửa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TicketTimelineCard(events = state.events, current = ticket.statusEnum, live = state.live)
            TicketDescriptionCard(ticket)
            TicketImagesCard(ticket)
            CommentSection(
                ticketId = ticket.id,
                canPost = ticket.statusEnum != TicketStatus.CLOSED
            )
        }
    }
}

private fun techLabel(profile: Profile): String =
    profile.fullName?.ifBlank { null } ?: profile.email ?: profile.id