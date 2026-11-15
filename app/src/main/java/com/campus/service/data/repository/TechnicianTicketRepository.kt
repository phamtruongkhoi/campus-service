package com.campus.service.data.repository

import com.campus.service.data.model.Ticket
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

@Serializable
private data class StatusUpdate(val status: String)

object TechnicianTicketRepository {

    private const val TICKET_COLUMNS =
        "*, areas(name,building), categories(name), ticket_images(path), " +
                "creator:profiles!created_by(full_name,student_id), " +
                "assignee:profiles!assigned_to(full_name)"

    /** Các phiếu được giao cho kỹ thuật viên này (mọi trạng thái), mới nhất trước. */
    suspend fun getAssignedTickets(technicianId: String): List<Ticket> =
        SupabaseProvider.client.from("tickets")
            .select(Columns.raw(TICKET_COLUMNS)) {
                filter { eq("assigned_to", technicianId) }
            }
            .decodeList<Ticket>()
            .sortedByDescending { it.createdAt }

    /** Đổi trạng thái; ném lỗi nếu không có dòng nào được cập nhật (bị RLS chặn). */
    suspend fun updateStatus(ticketId: String, newStatus: String) {
        val updated = SupabaseProvider.client.from("tickets")
            .update(StatusUpdate(newStatus)) {
                select()
                filter { eq("id", ticketId) }
            }
            .decodeList<Ticket>()
        if (updated.isEmpty()) {
            throw IllegalStateException("Không có quyền cập nhật phiếu này")
        }
    }
}