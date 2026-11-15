package com.campus.service.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Profile
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketPriority
import com.campus.service.data.model.TicketStatus
import com.campus.service.data.repository.AdminTicketRepository
import com.campus.service.data.repository.TicketEventRepository
import com.campus.service.data.repository.TicketRealtime
import java.time.OffsetDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lựa chọn ETA: keep = giữ nguyên, days = null và keep = false nghĩa là xóa ETA. */
data class EtaChoice(val label: String, val days: Int?, val keep: Boolean = false)

val ETA_CHOICES = listOf(
    EtaChoice("Giữ nguyên", null, keep = true),
    EtaChoice("Trong 1 ngày", 1),
    EtaChoice("Trong 2 ngày", 2),
    EtaChoice("Trong 3 ngày", 3),
    EtaChoice("Trong 7 ngày", 7),
    EtaChoice("Xóa ETA", null)
)

data class DispatchUiState(
    val loading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val technicians: List<Profile> = emptyList(),
    val filter: TicketStatus? = TicketStatus.OPEN,
    val selectedId: String? = null,
    val events: List<TicketEvent> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val live: Boolean = false
) {
    val selected: Ticket? get() = tickets.firstOrNull { it.id == selectedId }

    /** Ưu tiên cao xếp trước, cùng mức thì mới nhất trước. */
    val visible: List<Ticket>
        get() = tickets
            .filter { filter == null || it.statusEnum == filter }
            .sortedWith(
                compareByDescending<Ticket> { it.priorityEnum.ordinal }
                    .thenByDescending { it.createdAt }
            )
}

class AdminDispatchViewModel : ViewModel() {

    private val _state = MutableStateFlow(DispatchUiState())
    val state: StateFlow<DispatchUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            reload()
            _state.update { it.copy(loading = false) }
        }
    }

    /** Chạy cho tới khi bị hủy (gọi trong LaunchedEffect của màn hình). */
    @OptIn(FlowPreview::class)
    suspend fun observeChanges() {
        try {
            TicketRealtime.changes()
                .onEach { _state.update { it.copy(live = true) } }
                .debounce(400)
                .collect { reload() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                it.copy(live = false, error = "Mất kết nối cập nhật trực tiếp: ${e.message}")
            }
        } finally {
            _state.update { it.copy(live = false) }
        }
    }

    fun setFilter(status: TicketStatus?) {
        _state.update { it.copy(filter = status) }
    }

    fun openDetail(ticket: Ticket) {
        _state.update { it.copy(selectedId = ticket.id, events = emptyList(), info = null, error = null) }
        viewModelScope.launch { loadEvents(ticket.id) }
    }

    fun closeDetail() {
        _state.update { it.copy(selectedId = null, events = emptyList(), info = null) }
    }

    fun save(
        ticket: Ticket,
        priority: TicketPriority,
        technicianId: String?,
        eta: EtaChoice,
        isPublic: Boolean
    ) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, info = null) }
            try {
                val current = ticket.statusEnum
                val newStatus = when {
                    technicianId != null && current == TicketStatus.OPEN -> TicketStatus.ASSIGNED
                    technicianId == null && current == TicketStatus.ASSIGNED -> TicketStatus.OPEN
                    else -> current
                }
                val newEta: String? = when {
                    eta.keep -> ticket.eta
                    eta.days != null -> OffsetDateTime.now().plusDays(eta.days.toLong()).toString()
                    else -> null
                }
                AdminTicketRepository.updateDispatch(
                    ticketId = ticket.id,
                    priority = priority.dbValue,
                    assignedTo = technicianId,
                    eta = newEta,
                    isPublic = isPublic,
                    status = newStatus.dbValue
                )
                reload()
                _state.update { it.copy(info = "Đã lưu thay đổi.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Không lưu được: ${e.message}") }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private suspend fun reload() {
        try {
            val tickets = AdminTicketRepository.getAllTickets()
            _state.update { it.copy(tickets = tickets, error = null) }
            if (_state.value.technicians.isEmpty()) {
                val techs = AdminTicketRepository.getTechnicians()
                _state.update { it.copy(technicians = techs) }
            }
            _state.value.selectedId?.let { loadEvents(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(error = "Không tải được dữ liệu: ${e.message}") }
        }
    }

    private suspend fun loadEvents(ticketId: String) {
        try {
            val events = TicketEventRepository.getEvents(ticketId)
            _state.update { if (it.selectedId == ticketId) it.copy(events = events) else it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Không có timeline thì vẫn dùng được form điều phối
        }
    }
}