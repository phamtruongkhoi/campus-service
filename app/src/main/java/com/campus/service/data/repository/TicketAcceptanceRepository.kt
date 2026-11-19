package com.campus.service.data.repository

import com.campus.service.data.model.Ticket
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class AcceptanceUpdate(
    val status: String,
    val rating: Int,
    val feedback: String?
)

@Serializable
private data class ReopenParams(
    @SerialName("p_ticket_id") val ticketId: String,
    @SerialName("p_reason") val reason: String
)

object TicketAcceptanceRepository {

    const val REOPEN_MIN = 10
    const val REOPEN_MAX = 300

    /** Sinh viên nghiệm thu: Resolved -> Closed kèm số sao (1-5) và nhận xét. */
    suspend fun acceptAndClose(ticketId: String, rating: Int, feedback: String?) {
        require(rating in 1..5) { "Số sao phải từ 1 đến 5" }
        val updated = SupabaseProvider.client.from("tickets")
            .update(AcceptanceUpdate(status = "closed", rating = rating, feedback = feedback)) {
                select()
                filter {
                    eq("id", ticketId)
                    eq("status", "resolved")
                }
            }
            .decodeList<Ticket>()
        if (updated.isEmpty()) {
            throw IllegalStateException("Phiếu không còn ở trạng thái chờ nghiệm thu")
        }
    }

    /**
     * Sinh viên mở lại phiếu chưa đạt: Resolved -> In progress kèm lý do bắt buộc.
     * Gọi hàm SQL reopen_ticket (kiểm tra chủ phiếu, trạng thái và độ dài lý do ở phía DB).
     */
    suspend fun reopen(ticketId: String, reason: String) {
        val text = reason.trim()
        require(text.length in REOPEN_MIN..REOPEN_MAX) {
            "Lý do phải từ $REOPEN_MIN đến $REOPEN_MAX ký tự"
        }
        SupabaseProvider.client.postgrest.rpc(
            "reopen_ticket",
            ReopenParams(ticketId = ticketId, reason = text)
        )
    }
}
