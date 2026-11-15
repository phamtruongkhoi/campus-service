package com.campus.service.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketStatus
import com.campus.service.data.repository.AdminTicketRepository
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

data class AdminDashboardUiState(
    val loading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val error: String? = null,
    val live: Boolean = false
) {
    val total: Int get() = tickets.size

    val countByStatus: Map<TicketStatus, Int>
        get() = TicketStatus.entries.associateWith { status ->
            tickets.count { it.statusEnum == status }
        }

    val countByCategory: List<Pair<String, Int>>
        get() = tickets.groupingBy { it.categoryLabel }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

    val recent: List<Ticket>
        get() = tickets.sortedByDescending { it.createdAt }.take(5)
}

class AdminDashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardUiState())
    val state: StateFlow<AdminDashboardUiState> = _state.asStateFlow()

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
            val tickets = AdminTicketRepository.getAllTickets()
            _state.update { it.copy(tickets = tickets, error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(error = "Không tải được dữ liệu: ${e.message}") }
        }
    }
}