package com.campus.service.data.model

import com.campus.service.util.parseIsoMillis
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

/** Thông tin người (embed từ profiles): người gửi phiếu hoặc kỹ thuật viên được giao. */
@Serializable
data class PersonRef(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("student_id") val studentId: String? = null
)

/** Phiếu khi đọc, kèm khu vực, loại lỗi và ảnh (embed của PostgREST). */
@Serializable
data class Ticket(
    val id: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("assigned_to") val assignedTo: String? = null,
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
    @SerialName("resolved_at") val resolvedAt: String? = null,
    val areas: AreaRef? = null,
    val categories: CategoryRef? = null,
    @SerialName("ticket_images") val images: List<ImageRef> = emptyList(),
    /** Chỉ có khi truy vấn của Admin/Kỹ thuật viên (embed profiles). */
    val creator: PersonRef? = null,
    val assignee: PersonRef? = null
) {
    val statusEnum: TicketStatus get() = TicketStatus.from(status)
    val priorityEnum: TicketPriority get() = TicketPriority.from(priority)
    val areaLabel: String get() = areas?.label ?: "—"
    val categoryLabel: String get() = categories?.name ?: "—"
    val creatorLabel: String
        get() {
            val name = creator?.fullName?.ifBlank { null } ?: "(chưa có tên)"
            val mssv = creator?.studentId?.ifBlank { null }
            return if (mssv != null) "$name - $mssv" else name
        }
    val assigneeLabel: String
        get() = assignee?.fullName?.ifBlank { null } ?: if (assignedTo == null) "Chưa phân công" else "(không rõ tên)"

    /** Quá hạn: chưa xong (Open/Assigned/In progress), có ETA và ETA đã qua. */
    val isOverdue: Boolean
        get() {
            val s = statusEnum
            if (s == TicketStatus.RESOLVED || s == TicketStatus.CLOSED) return false
            val due = parseIsoMillis(eta) ?: return false
            return due < System.currentTimeMillis()
        }

    /** Thời gian xử lý = resolved_at - created_at (null nếu chưa khắc phục hoặc thiếu dữ liệu). */
    val processingMillis: Long?
        get() {
            val end = parseIsoMillis(resolvedAt) ?: return null
            val start = parseIsoMillis(createdAt) ?: return null
            return (end - start).takeIf { it >= 0 }
        }
}