package com.campus.service.ui.student

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Area
import com.campus.service.data.model.Category
import com.campus.service.data.repository.TicketRepository
import com.campus.service.util.CompressedImage
import com.campus.service.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MAX_IMAGES = 3

data class CreateTicketUiState(
    val loadingOptions: Boolean = false,
    val areas: List<Area> = emptyList(),
    val categories: List<Category> = emptyList(),
    val images: List<CompressedImage> = emptyList(),
    val processingImages: Boolean = false,
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

    fun addImages(context: Context, uris: List<Uri>) {
        val remaining = MAX_IMAGES - _state.value.images.size
        if (remaining <= 0 || uris.isEmpty()) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _state.update { it.copy(processingImages = true, error = null, message = null) }
            val compressed = withContext(Dispatchers.IO) {
                uris.take(remaining).mapNotNull { uri ->
                    try {
                        ImageCompressor.compress(appContext, uri)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            _state.update {
                val combined = (it.images + compressed).take(MAX_IMAGES)
                it.copy(
                    processingImages = false,
                    images = combined,
                    error = if (compressed.size < uris.take(remaining).size)
                        "Có ảnh không đọc được và đã bị bỏ qua." else null
                )
            }
        }
    }

    fun removeImage(index: Int) {
        _state.update {
            if (index in it.images.indices) {
                it.copy(images = it.images.filterIndexed { i, _ -> i != index })
            } else it
        }
    }

    fun submit(area: Area, category: Category, title: String, description: String) {
        val images = _state.value.images
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null, message = null) }
            try {
                TicketRepository.createTicket(
                    areaId = area.id,
                    categoryId = category.id,
                    title = title.trim(),
                    description = description.trim(),
                    images = images.map { it.bytes }
                )
                _state.update {
                    it.copy(
                        submitting = false,
                        images = emptyList(),
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