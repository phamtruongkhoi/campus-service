package com.campus.service.ui.technician

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketStatus
import com.campus.service.data.repository.TechnicianTicketRepository
import com.campus.service.data.repository.TicketEventRepository
import com.campus.service.data.repository.TicketRealtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TechJobsUiState(
    val loading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val selectedId: String? = null,
    val events: List<TicketEvent> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val live: Boolean = false
) {
    /** Phiếu hiển thị trong danh sách công việc: chưa hoàn thành. Ưu tiên cao trước, cũ trước. */
    val active: List<Ticket>
        get() = tickets
            .filter {
                it.statusEnum == TicketStatus.ASSIGNED || it.statusEnum == TicketStatus.IN_PROGRESS
            }
            .sortedWith(
                compareByDescending<Ticket> { it.priorityEnum.ordinal }
                    .thenBy { it.createdAt }
            )

    /** Tìm trong toàn bộ phiếu để vẫn xem được phiếu vừa báo khắc phục. */
    val selected: Ticket? get() = tickets.firstOrNull { it.id == selectedId }
}

class TechJobsViewModel : ViewModel() {

    private val _state = MutableStateFlow(TechJobsUiState())
    val state: StateFlow<TechJobsUiState> = _state.asStateFlow()

    private var technicianId: String? = null

    fun refresh(userId: String) {
        technicianId = userId
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            reload()
            _state.update { it.copy(loading = false) }
        }
    }

    @OptIn(FlowPreview::class)
    suspend fun observeChanges(userId: String) {
        technicianId = userId
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

    fun openDetail(ticket: Ticket) {
        _state.update { it.copy(selectedId = ticket.id, events = emptyList(), info = null, error = null) }
        viewModelScope.launch { loadEvents(ticket.id) }
    }

    fun closeDetail() {
        _state.update { it.copy(selectedId = null, events = emptyList(), info = null) }
    }

    /** Đã phân công -> Đang xử lý; Đang xử lý -> Đã khắc phục. */
    fun advance(ticket: Ticket) {
        val next = when (ticket.statusEnum) {
            TicketStatus.ASSIGNED -> TicketStatus.IN_PROGRESS
            TicketStatus.IN_PROGRESS -> TicketStatus.RESOLVED
            else -> return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, info = null) }
            try {
                TechnicianTicketRepository.updateStatus(ticket.id, next.dbValue)
                reload()
                _state.update { it.copy(info = "Đã chuyển sang: ${next.label}") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Không cập nhật được: ${e.message}") }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private suspend fun reload() {
        val id = technicianId ?: return
        try {
            val tickets = TechnicianTicketRepository.getAssignedTickets(id)
            _state.update { it.copy(tickets = tickets, error = null) }
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
            // Không có timeline thì vẫn thao tác được
        }
    }
}