package com.campus.service.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Dữ liệu gửi lên khi sinh viên tạo phiếu. Trạng thái (open) và ưu tiên (medium) do DB đặt mặc định. */
@Serializable
data class NewTicket(
    @SerialName("created_by") val createdBy: String,
    @SerialName("area_id") val areaId: Int,
    @SerialName("category_id") val categoryId: Int,
    val title: String,
    val description: String? = null
)