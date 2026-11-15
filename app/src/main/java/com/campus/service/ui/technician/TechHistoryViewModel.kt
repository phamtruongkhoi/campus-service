package com.campus.service.ui.technician

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.model.TicketStatus
import com.campus.service.data.repository.TechnicianTicketRepository
import com.campus.service.data.repository.TicketEventRepository
import com.campus.service.data.repository.TicketRealtime
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TechHistoryUiState(
    val loading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val selectedId: String? = null,
    val events: List<TicketEvent> = emptyList(),
    val error: String? = null,
    val live: Boolean = false
) {
    /** Việc đã giải quyết: Đã khắc phục hoặc Đã đóng, mới cập nhật nhất trước. */
    val done: List<Ticket>
        get() = tickets
            .filter {
                it.statusEnum == TicketStatus.RESOLVED || it.statusEnum == TicketStatus.CLOSED
            }
            .sortedByDescending { it.updatedAt ?: it.createdAt }

    val closedCount: Int get() = done.count { it.statusEnum == TicketStatus.CLOSED }

    val averageRating: String
        get() {
            val ratings = done.mapNotNull { it.rating }
            return if (ratings.isEmpty()) "—"
            else String.format(Locale.US, "%.1f/5", ratings.average())
        }

    val selected: Ticket? get() = tickets.firstOrNull { it.id == selectedId }
}

class TechHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(TechHistoryUiState())
    val state: StateFlow<TechHistoryUiState> = _state.asStateFlow()

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
        _state.update { it.copy(selectedId = ticket.id, events = emptyList()) }
        viewModelScope.launch { loadEvents(ticket.id) }
    }

    fun closeDetail() {
        _state.update { it.copy(selectedId = null, events = emptyList()) }
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
            // Không có timeline thì vẫn xem được chi tiết
        }
    }
}