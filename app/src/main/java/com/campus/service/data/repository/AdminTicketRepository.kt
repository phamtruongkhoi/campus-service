package com.campus.service.data.repository

import com.campus.service.data.model.Profile
import com.campus.service.data.model.Ticket
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Dữ liệu Admin ghi khi điều phối. Không đặt giá trị mặc định để mọi trường đều được gửi (kể cả null). */
@Serializable
private data class DispatchUpdate(
    val priority: String,
    @SerialName("assigned_to") val assignedTo: String?,
    val eta: String?,
    @SerialName("is_public") val isPublic: Boolean,
    val status: String
)

object AdminTicketRepository {

    private const val TICKET_COLUMNS =
        "*, areas(name,building), categories(name), ticket_images(path), " +
                "creator:profiles!created_by(full_name,student_id), " +
                "assignee:profiles!assigned_to(full_name)"

    /** Toàn bộ phiếu trong hệ thống (RLS chỉ cho Admin và kỹ thuật viên), mới nhất trước. */
    suspend fun getAllTickets(): List<Ticket> =
        SupabaseProvider.client.from("tickets")
            .select(Columns.raw(TICKET_COLUMNS))
            .decodeList<Ticket>()
            .sortedByDescending { it.createdAt }

    /** Danh sách kỹ thuật viên để phân công (Admin được đọc mọi profile theo RLS). */
    suspend fun getTechnicians(): List<Profile> =
        SupabaseProvider.client.from("profiles")
            .select {
                filter { eq("role", "technician") }
            }
            .decodeList<Profile>()
            .sortedBy { it.fullName ?: it.email ?: "" }

    suspend fun updateDispatch(
        ticketId: String,
        priority: String,
        assignedTo: String?,
        eta: String?,
        isPublic: Boolean,
        status: String
    ) {
        SupabaseProvider.client.from("tickets")
            .update(
                DispatchUpdate(
                    priority = priority,
                    assignedTo = assignedTo,
                    eta = eta,
                    isPublic = isPublic,
                    status = status
                )
            ) {
                filter { eq("id", ticketId) }
            }
    }
}