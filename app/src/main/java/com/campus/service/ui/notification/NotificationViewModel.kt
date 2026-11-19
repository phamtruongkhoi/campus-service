package com.campus.service.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Profile
import com.campus.service.data.model.Ticket
import com.campus.service.data.model.TicketStatus
import com.campus.service.data.model.UserRole
import com.campus.service.data.repository.AdminTicketRepository
import com.campus.service.data.repository.TechnicianTicketRepository
import com.campus.service.data.repository.TicketRealtime
import com.campus.service.data.repository.TicketRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Thông báo trong app: theo dõi realtime, so sánh trạng thái phiếu với lần trước
 * và phát ra các câu thông báo ngắn. Chỉ hoạt động khi app đang mở.
 */
class NotificationViewModel : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var watchedUserId: String? = null
    private var job: Job? = null

    fun watch(profile: Profile) {
        if (watchedUserId == profile.id && job?.isActive == true) return
        job?.cancel()
        watchedUserId = profile.id
        job = viewModelScope.launch { observe(profile) }
    }

    fun stop() {
        job?.cancel()
        job = null
        watchedUserId = null
    }

    @OptIn(FlowPreview::class)
    private suspend fun observe(profile: Profile) {
        var known: Map<String, Ticket>? = null
        try {
            TicketRealtime.changes()
                .debounce(500)
                .collect {
                    val current = try {
                        fetch(profile)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        return@collect
                    }
                    val previous = known
                    // Lần tải đầu tiên chỉ ghi nhận trạng thái hiện có, không thông báo
                    if (previous != null) {
                        messagesFor(profile.userRole, previous, current).forEach {
                            _events.tryEmit(it)
                        }
                    }
                    known = current.associateBy { it.id }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Mất kết nối cập nhật trực tiếp: bỏ qua, các màn hình đã tự báo lỗi
        }
    }

    private suspend fun fetch(profile: Profile): List<Ticket> = when (profile.userRole) {
        UserRole.STUDENT -> TicketRepository.getMyTickets()
        UserRole.TECHNICIAN -> TechnicianTicketRepository.getAssignedTickets(profile.id)
        UserRole.ADMIN -> AdminTicketRepository.getAllTickets()
    }

    private fun messagesFor(
        role: UserRole,
        previous: Map<String, Ticket>,
        current: List<Ticket>
    ): List<String> {
        val messages = mutableListOf<String>()
        current.forEach { ticket ->
            val old = previous[ticket.id]?.statusEnum
            val now = ticket.statusEnum
            val title = "“${ticket.title}”"
            // Sinh viên mở lại phiếu: Resolved -> In progress
            val reopened = old == TicketStatus.RESOLVED && now == TicketStatus.IN_PROGRESS
            when (role) {
                UserRole.STUDENT -> {
                    // Không báo cho chính người vừa mở lại
                    if (old != null && old != now && now != TicketStatus.CLOSED && !reopened) {
                        messages += "Phiếu $title chuyển sang: ${now.label}"
                    }
                }
                UserRole.TECHNICIAN -> {
                    if (reopened) {
                        messages += "Sinh viên đã mở lại phiếu $title, cần xử lý lại"
                    } else if (old == null && now == TicketStatus.ASSIGNED) {
                        messages += "Bạn được giao việc mới: $title"
                    } else if (old != null && old != TicketStatus.CLOSED && now == TicketStatus.CLOSED) {
                        val stars = ticket.rating?.let { " ($it★)" } ?: ""
                        messages += "Phiếu $title đã được nghiệm thu$stars"
                    }
                }
                UserRole.ADMIN -> {
                    if (old == null) {
                        messages += "Có phiếu mới: $title"
                    } else if (reopened) {
                        messages += "Sinh viên đã mở lại phiếu $title"
                    } else if (old != now && (now == TicketStatus.IN_PROGRESS ||
                                now == TicketStatus.RESOLVED || now == TicketStatus.CLOSED)
                    ) {
                        messages += "Phiếu $title: ${now.label}"
                    }
                }
            }
        }
        return messages
    }
}
