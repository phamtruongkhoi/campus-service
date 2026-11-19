package com.campus.service.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Một bình luận trong phiếu (bảng ticket_comments). Tên và vai trò người gửi do trigger điền. */
@Serializable
data class TicketComment(
    val id: String,
    @SerialName("ticket_id") val ticketId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_role") val authorRole: String? = null,
    val body: String,
    @SerialName("created_at") val createdAt: String = ""
)

/** Dữ liệu gửi lên khi bình luận. Người gửi, tên, vai trò do DB đặt. */
@Serializable
data class NewComment(
    @SerialName("ticket_id") val ticketId: String,
    val body: String
)