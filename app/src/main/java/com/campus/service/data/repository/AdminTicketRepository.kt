package com.campus.service.data.repository

import com.campus.service.data.model.Ticket
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

object AdminTicketRepository {

    private const val TICKET_COLUMNS =
        "*, areas(name,building), categories(name), ticket_images(path)"

    /** Toàn bộ phiếu trong hệ thống (RLS chỉ cho Admin và kỹ thuật viên), mới nhất trước. */
    suspend fun getAllTickets(): List<Ticket> =
        SupabaseProvider.client.from("tickets")
            .select(Columns.raw(TICKET_COLUMNS))
            .decodeList<Ticket>()
            .sortedByDescending { it.createdAt }
}