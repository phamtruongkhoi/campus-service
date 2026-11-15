package com.campus.service.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketStatus
import com.campus.service.data.repository.TicketEventRepository
import com.campus.service.data.repository.TicketRealtime
import com.campus.service.data.repository.TicketRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TicketHistoryUiState(
    val ownerId: String? = null,
    val loading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val error: String? = null,
    val selectedId: String? = null,
    val filter: TicketStatus? = null,
    val events: List<TicketEvent> = emptyList(),
    val live: Boolean = false
) {
    val selected: Ticket? get() = tickets.firstOrNull { it.id == selectedId }
    val visible: List<Ticket>
        get() = if (filter == null) tickets else tickets.filter { it.statusEnum == filter }
}

class TicketHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(TicketHistoryUiState())
    val state: StateFlow<TicketHistoryUiState> = _state.asStateFlow()

    /** Tải lại danh sách; nếu đổi tài khoản thì xóa dữ liệu của tài khoản trước. */
    fun refresh(userId: String) {
        if (_state.value.ownerId != userId) {
            _state.value = TicketHistoryUiState(ownerId = userId)
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            reload()
            _state.update { it.copy(loading = false) }
        }
    }

    /**
     * Lắng nghe thay đổi realtime. Hàm này chạy cho tới khi bị hủy
     * (gọi trong LaunchedEffect của màn hình để rời màn hình là dừng).
     */
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
                it.copy(
                    live = false,
                    error = "Mất kết nối cập nhật trực tiếp: ${e.message}"
                )
            }
        } finally {
            _state.update { it.copy(live = false) }
        }
    }

    private suspend fun reload() {
        try {
            val tickets = TicketRepository.getMyTickets()
            _state.update { it.copy(tickets = tickets, error = null) }
            _state.value.selectedId?.let { loadEvents(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                it.copy(error = "Không tải được danh sách phiếu: ${e.message}")
            }
        }
    }

    private suspend fun loadEvents(ticketId: String) {
        try {
            val events = TicketEventRepository.getEvents(ticketId)
            _state.update { if (it.selectedId == ticketId) it.copy(events = events) else it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Giữ timeline cũ nếu tải lỗi
        }
    }

    fun setFilter(status: TicketStatus?) {
        _state.update { it.copy(filter = status) }
    }

    fun openDetail(ticket: Ticket) {
        _state.update { it.copy(selectedId = ticket.id, events = emptyList()) }
        viewModelScope.launch { loadEvents(ticket.id) }
    }

    fun closeDetail() {
        _state.update { it.copy(selectedId = null, events = emptyList()) }
    }
}