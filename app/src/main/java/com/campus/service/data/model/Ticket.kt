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

/** Một dòng trong bảng ticket_images (khi ghi). */
@Serializable
data class NewTicketImage(
    @SerialName("ticket_id") val ticketId: String,
    val path: String
)

@Serializable
data class AreaRef(
    val name: String,
    val building: String? = null
) {
    val label: String
        get() = if (building.isNullOrBlank()) name else "$building - $name"
}

@Serializable
data class CategoryRef(val name: String)

@Serializable
data class ImageRef(val path: String)

/** Phiếu khi đọc, kèm khu vực, loại lỗi và ảnh (embed của PostgREST). */
@Serializable
data class Ticket(
    val id: String,
    @SerialName("created_by") val createdBy: String,
    val title: String,
    val description: String? = null,
    val status: String = "open",
    val priority: String = "medium",
    val eta: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    val rating: Int? = null,
    val feedback: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    val areas: AreaRef? = null,
    val categories: CategoryRef? = null,
    @SerialName("ticket_images") val images: List<ImageRef> = emptyList()
) {
    val statusEnum: TicketStatus get() = TicketStatus.from(status)
    val priorityEnum: TicketPriority get() = TicketPriority.from(priority)
    val areaLabel: String get() = areas?.label ?: "—"
    val categoryLabel: String get() = categories?.name ?: "—"
}