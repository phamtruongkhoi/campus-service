package com.campus.service.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Area
import com.campus.service.data.model.Category
import com.campus.service.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateTicketUiState(
    val loadingOptions: Boolean = false,
    val areas: List<Area> = emptyList(),
    val categories: List<Category> = emptyList(),
    val submitting: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val formVersion: Int = 0
)

class CreateTicketViewModel : ViewModel() {

    private val _state = MutableStateFlow(CreateTicketUiState())
    val state: StateFlow<CreateTicketUiState> = _state.asStateFlow()

    /** Gọi khi mở màn hình; chỉ tải nếu chưa có dữ liệu. */
    fun loadOptionsIfNeeded() {
        val s = _state.value
        if (s.loadingOptions || (s.areas.isNotEmpty() && s.categories.isNotEmpty())) return
        viewModelScope.launch {
            _state.update { it.copy(loadingOptions = true, error = null) }
            try {
                val areas = TicketRepository.getAreas()
                val categories = TicketRepository.getCategories()
                _state.update {
                    it.copy(loadingOptions = false, areas = areas, categories = categories)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loadingOptions = false,
                        error = "Không tải được danh sách khu vực/loại lỗi: ${e.message}"
                    )
                }
            }
        }
    }

    fun showError(text: String) {
        _state.update { it.copy(error = text, message = null) }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, message = null) }
    }

    fun submit(area: Area, category: Category, title: String, description: String) {
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null, message = null) }
            try {
                TicketRepository.createTicket(
                    areaId = area.id,
                    categoryId = category.id,
                    title = title.trim(),
                    description = description.trim()
                )
                _state.update {
                    it.copy(
                        submitting = false,
                        message = "Đã gửi phiếu yêu cầu thành công.",
                        formVersion = it.formVersion + 1
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(submitting = false, error = "Không gửi được phiếu: ${e.message}")
                }
            }
        }
    }
}