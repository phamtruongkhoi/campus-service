package com.campus.service.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Dữ liệu gửi lên khi sinh viên tạo phiếu. Trạng thái (open) và ưu tiên (medium) do DB đặt mặc định. */
@Serializable
data class NewTicket(
    val id: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("area_id") val areaId: Int,
    @SerialName("category_id") val categoryId: Int,
    val title: String,
    val description: String? = null
)

/** Một dòng trong bảng ticket_images. */
@Serializable
data class NewTicketImage(
    @SerialName("ticket_id") val ticketId: String,
    val path: String
)