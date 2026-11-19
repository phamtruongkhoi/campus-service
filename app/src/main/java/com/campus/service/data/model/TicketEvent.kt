package com.campus.service.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Một lần đổi trạng thái của phiếu (bảng ticket_events, do trigger ghi).
 * note: lý do (hiện chỉ dùng khi sinh viên mở lại phiếu), null nếu không có.
 */
@Serializable
data class TicketEvent(
    val id: String,
    @SerialName("ticket_id") val ticketId: String,
    val status: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    val note: String? = null
)
