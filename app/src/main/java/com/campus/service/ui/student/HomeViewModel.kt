package com.campus.service.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.PublicIncident
import com.campus.service.data.repository.IncidentRepository
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

data class HomeUiState(
    val loading: Boolean = false,
    val incidents: List<PublicIncident> = emptyList(),
    val error: String? = null,
    val live: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

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
            TicketRealtime.incidentChanges()
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
            val incidents = IncidentRepository.getPublicIncidents()
            _state.update { it.copy(incidents = incidents, error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(error = "Không tải được bảng tin: ${e.message}") }
        }
    }
}