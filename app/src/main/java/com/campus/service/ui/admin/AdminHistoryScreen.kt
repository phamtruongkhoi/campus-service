package com.campus.service.ui.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.MaterialTheme
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
import com.campus.service.data.model.TicketPriority
import com.campus.service.data.model.TicketStatus
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.AppTextField
import com.campus.service.ui.common.DropdownField
import com.campus.service.ui.common.EmptyState
import com.campus.service.ui.common.IconInfoRow
import com.campus.service.ui.common.MessageText
import com.campus.service.ui.common.ScreenHeaderRow
import com.campus.service.ui.common.SkeletonList
import com.campus.service.ui.common.TicketDescriptionCard
import com.campus.service.ui.common.TicketDetailHeader
import com.campus.service.ui.common.TicketImagesCard
import com.campus.service.ui.common.TicketListCard
import com.campus.service.ui.common.TicketRatingCard
import com.campus.service.ui.common.TicketTimelineCard
import com.campus.service.util.formatDateTime

private data class FilterOption(val value: String?, val label: String)

private val ALL_OPTION = FilterOption(null, "Tất cả")

@Composable
fun AdminHistoryScreen(vm: AdminHistoryViewModel) {
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { vm.observeChanges() }

    val state by vm.state.collectAsState()
    val selected = state.selected

    BackHandler(enabled = selected != null) { vm.closeDetail() }

    if (selected != null) {
        HistoryDetail(ticket = selected, state = state, onBack = { vm.closeDetail() })
        return
    }

    val filters = state.filters
    val statusOptions = listOf(ALL_OPTION) +
            TicketStatus.entries.map { FilterOption(it.dbValue, it.label) }
    val priorityOptions = listOf(ALL_OPTION) +
            TicketPriority.entries.map { FilterOption(it.dbValue, it.label) }
    val categoryOptions = listOf(ALL_OPTION) + state.categories.map { FilterOption(it, it) }
    val areaOptions = listOf(ALL_OPTION) + state.areas.map { FilterOption(it, it) }

    val visible = state.visible

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            ScreenHeaderRow(
                title = "Lịch sử toàn trường",
                subtitle = "${state.tickets.size} phiếu",
                live = state.live,
                loading = state.loading,
                onRefresh = { vm.refresh() }
            )
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = filters.query,
                onValueChange = vm::setQuery,
                label = "Tìm theo tiêu đề, mô tả, người gửi, khu vực",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Trạng thái",
                    items = statusOptions,
                    selected = statusOptions.firstOrNull { it.value == filters.status } ?: ALL_OPTION,
                    itemLabel = { it.label },
                    onSelected = { vm.setStatus(it.value) },
                    modifier = Modifier.weight(1f)
                )
                DropdownField(
                    label = "Ưu tiên",
                    items = priorityOptions,
                    selected = priorityOptions.firstOrNull { it.value == filters.priority } ?: ALL_OPTION,
                    itemLabel = { it.label },
                    onSelected = { vm.setPriority(it.value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Loại lỗi",
                    items = categoryOptions,
                    selected = categoryOptions.firstOrNull { it.value == filters.category } ?: ALL_OPTION,
                    itemLabel = { it.label },
                    onSelected = { vm.setCategory(it.value) },
                    modifier = Modifier.weight(1f)
                )
                DropdownField(
                    label = "Khu vực",
                    items = areaOptions,
                    selected = areaOptions.firstOrNull { it.value == filters.area } ?: ALL_OPTION,
                    itemLabel = { it.label },
                    onSelected = { vm.setArea(it.value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            MessageText(error = state.error, info = null)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tìm thấy ${visible.size} / ${state.tickets.size} phiếu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (filters.active) {
                    TextButton(onClick = { vm.clearFilters() }) { Text("Xóa bộ lọc") }
                }
            }
        }
        if (state.loading && state.tickets.isEmpty()) {
            item { SkeletonList() }
        } else if (visible.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Không có kết quả",
                    message = "Không có phiếu nào khớp bộ lọc."
                )
            }
        }
        items(visible, key = { it.id }) { ticket ->
            TicketListCard(
                ticket = ticket,
                onClick = { vm.openDetail(ticket) },
                lines = listOf(
                    "KTV: ${ticket.assigneeLabel}",
                    "${ticket.creatorLabel} • ${formatDateTime(ticket.createdAt)}"
                )
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HistoryDetail(ticket: Ticket, state: AdminHistoryUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TicketDetailHeader(
            title = ticket.title,
            status = ticket.statusEnum,
            onBack = onBack,
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
                IconInfoRow(Icons.Outlined.Flag, "Mức ưu tiên", ticket.priorityEnum.label)
                IconInfoRow(Icons.Outlined.Engineering, "Kỹ thuật viên", ticket.assigneeLabel)
                IconInfoRow(Icons.Outlined.Schedule, "Gửi lúc", formatDateTime(ticket.createdAt))
                IconInfoRow(Icons.Outlined.Event, "ETA", formatDateTime(ticket.eta))
                if (ticket.resolvedAt != null) {
                    IconInfoRow(
                        Icons.Outlined.CheckCircle,
                        "Khắc phục lúc",
                        formatDateTime(ticket.resolvedAt)
                    )
                }
                IconInfoRow(Icons.Outlined.Public, "Công khai", if (ticket.isPublic) "Có" else "Không")
                IconInfoRow(Icons.Outlined.Update, "Cập nhật", formatDateTime(ticket.updatedAt))
            }

            TicketTimelineCard(events = state.events, current = ticket.statusEnum, live = state.live)
            TicketDescriptionCard(ticket)
            TicketImagesCard(ticket)
            TicketRatingCard(ticket, title = "Đánh giá của sinh viên")
        }
    }
}