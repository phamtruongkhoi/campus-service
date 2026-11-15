package com.campus.service.data.repository

import com.campus.service.data.model.Ticket
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
private data class AcceptanceUpdate(
    val status: String,
    val rating: Int,
    val feedback: String?
)

object TicketAcceptanceRepository {

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
}