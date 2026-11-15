package com.campus.service.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketEvent
import com.campus.service.data.repository.AdminTicketRepository
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

/** Bộ lọc: null nghĩa là "Tất cả". Giá trị so khớp với dbValue (trạng thái, ưu tiên) hoặc nhãn hiển thị (loại lỗi, khu vực). */
data class HistoryFilters(
    val query: String = "",
    val status: String? = null,
    val category: String? = null,
    val area: String? = null,
    val priority: String? = null
) {
    val active: Boolean
        get() = query.isNotBlank() || status != null || category != null ||
                area != null || priority != null
}

data class AdminHistoryUiState(
    val loading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val filters: HistoryFilters = HistoryFilters(),
    val selectedId: String? = null,
    val events: List<TicketEvent> = emptyList(),
    val error: String? = null,
    val live: Boolean = false
) {
    val selected: Ticket? get() = tickets.firstOrNull { it.id == selectedId }

    val categories: List<String>
        get() = tickets.map { it.categoryLabel }.distinct().sorted()

    val areas: List<String>
        get() = tickets.map { it.areaLabel }.distinct().sorted()

    val visible: List<Ticket>
        get() {
            val q = filters.query.trim().lowercase()
            return tickets.filter { t ->
                (filters.status == null || t.status == filters.status) &&
                        (filters.priority == null || t.priority == filters.priority) &&
                        (filters.category == null || t.categoryLabel == filters.category) &&
                        (filters.area == null || t.areaLabel == filters.area) &&
                        (q.isEmpty() ||
                                t.title.lowercase().contains(q) ||
                                (t.description ?: "").lowercase().contains(q) ||
                                t.creatorLabel.lowercase().contains(q) ||
                                t.areaLabel.lowercase().contains(q))
            }
        }
}

class AdminHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminHistoryUiState())
    val state: StateFlow<AdminHistoryUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            reload()
            _state.update { it.copy(loading = false) }
        }
    }

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

    fun setQuery(value: String) = updateFilters { it.copy(query = value) }
    fun setStatus(value: String?) = updateFilters { it.copy(status = value) }
    fun setCategory(value: String?) = updateFilters { it.copy(category = value) }
    fun setArea(value: String?) = updateFilters { it.copy(area = value) }
    fun setPriority(value: String?) = updateFilters { it.copy(priority = value) }
    fun clearFilters() = updateFilters { HistoryFilters() }

    private fun updateFilters(change: (HistoryFilters) -> HistoryFilters) {
        _state.update { it.copy(filters = change(it.filters)) }
    }

    fun openDetail(ticket: Ticket) {
        _state.update { it.copy(selectedId = ticket.id, events = emptyList()) }
        viewModelScope.launch { loadEvents(ticket.id) }
    }

    fun closeDetail() {
        _state.update { it.copy(selectedId = null, events = emptyList()) }
    }

    private suspend fun reload() {
        try {
            val tickets = AdminTicketRepository.getAllTickets()
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