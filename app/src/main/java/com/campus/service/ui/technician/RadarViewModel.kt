package com.campus.service.ui.technician

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

/** Các sự cố đang xử lý trong một tòa nhà (khu vực lân cận = cùng tòa nhà). */
data class BuildingGroup(
    val building: String,
    val tickets: List<Ticket>,
    val colleagues: List<String>,
    val mine: Boolean
)

data class RadarUiState(
    val userId: String = "",
    val loading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val error: String? = null,
    val live: Boolean = false
) {
    /** Sự cố chưa xong: chờ tiếp nhận, đã phân công, đang xử lý. */
    val active: List<Ticket>
        get() = tickets.filter {
            it.statusEnum == TicketStatus.OPEN ||
                    it.statusEnum == TicketStatus.ASSIGNED ||
                    it.statusEnum == TicketStatus.IN_PROGRESS
        }

    val unassignedCount: Int get() = active.count { it.assignedTo == null }
    val inProgressCount: Int get() = active.count { it.statusEnum == TicketStatus.IN_PROGRESS }

    /** Số việc đang gánh của từng kỹ thuật viên, kèm cột "Chưa phân công". */
    val workload: List<Pair<String, Int>>
        get() {
            val perTech = active
                .filter { it.assignedTo != null }
                .groupBy { it.assignedTo }
                .map { (techId, list) ->
                    val name = list.first().assigneeLabel
                    val label = if (techId == userId) "$name (Bạn)" else name
                    label to list.size
                }
            val unassigned = unassignedCount
            val all = if (unassigned > 0) perTech + ("Chưa phân công" to unassigned) else perTech
            return all.sortedByDescending { it.second }
        }

    val groups: List<BuildingGroup>
        get() = active
            .groupBy { it.areas?.building?.ifBlank { null } ?: "Khu khác" }
            .map { (building, list) ->
                val colleagues = list
                    .filter { it.assignedTo != null }
                    .distinctBy { it.assignedTo }
                    .map { if (it.assignedTo == userId) "Bạn" else it.assigneeLabel }
                BuildingGroup(
                    building = building,
                    tickets = list.sortedWith(
                        compareByDescending<Ticket> { it.priorityEnum.ordinal }
                            .thenBy { it.createdAt }
                    ),
                    colleagues = colleagues,
                    mine = list.any { it.assignedTo == userId }
                )
            }
            .sortedWith(compareByDescending<BuildingGroup> { it.mine }.thenByDescending { it.tickets.size })
}

class RadarViewModel : ViewModel() {

    private val _state = MutableStateFlow(RadarUiState())
    val state: StateFlow<RadarUiState> = _state.asStateFlow()

    fun refresh(userId: String) {
        _state.update { it.copy(userId = userId, loading = true, error = null) }
        viewModelScope.launch {
            reload()
            _state.update { it.copy(loading = false) }
        }
    }

    @OptIn(FlowPreview::class)
    suspend fun observeChanges(userId: String) {
        _state.update { it.copy(userId = userId) }
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