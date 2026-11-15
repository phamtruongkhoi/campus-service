package com.campus.service.data.repository

import com.campus.service.data.model.TicketEvent
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from

object TicketEventRepository {

    /** Lịch sử đổi trạng thái của một phiếu, cũ nhất trước. */
    suspend fun getEvents(ticketId: String): List<TicketEvent> =
        SupabaseProvider.client.from("ticket_events")
            .select {
                filter { eq("ticket_id", ticketId) }
            }
            .decodeList<TicketEvent>()
            .sortedBy { it.createdAt }
}