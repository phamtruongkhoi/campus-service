package com.campus.service.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketStatus
import com.campus.service.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TicketHistoryUiState(
    val ownerId: String? = null,
    val loading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val error: String? = null,
    val selectedId: String? = null,
    val filter: TicketStatus? = null
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
            try {
                val tickets = TicketRepository.getMyTickets()
                _state.update { it.copy(loading = false, tickets = tickets) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = "Không tải được danh sách phiếu: ${e.message}")
                }
            }
        }
    }

    fun setFilter(status: TicketStatus?) {
        _state.update { it.copy(filter = status) }
    }

    fun openDetail(ticket: Ticket) {
        _state.update { it.copy(selectedId = ticket.id) }
    }

    fun closeDetail() {
        _state.update { it.copy(selectedId = null) }
    }
}