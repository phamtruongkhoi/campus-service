package com.campus.service.data.repository

import com.campus.service.data.model.NewComment
import com.campus.service.data.model.TicketComment
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object CommentRepository {

    const val MAX_LENGTH = 500

    /** Bình luận của một phiếu, cũ nhất trước. */
    suspend fun getComments(ticketId: String): List<TicketComment> =
        SupabaseProvider.client.from("ticket_comments")
            .select {
                filter { eq("ticket_id", ticketId) }
            }
            .decodeList<TicketComment>()
            .sortedBy { it.createdAt }

    /**
     * Bình luận mới nhất trong mọi phiếu người dùng được xem (dùng cho thông báo).
     * Lọc/sắp xếp ở phía app, đủ cho quy mô demo.
     */
    suspend fun getRecent(): List<TicketComment> =
        SupabaseProvider.client.from("ticket_comments")
            .select()
            .decodeList<TicketComment>()
            .sortedByDescending { it.createdAt }
            .take(100)

    suspend fun add(ticketId: String, body: String) {
        val text = body.trim()
        require(text.length in 1..MAX_LENGTH) { "Bình luận phải từ 1 đến $MAX_LENGTH ký tự" }
        SupabaseProvider.client.from("ticket_comments")
            .insert(NewComment(ticketId = ticketId, body = text))
    }

    /** Tín hiệu khi bảng ticket_comments thay đổi. */
    fun changes(): Flow<Unit> = TicketRealtime.commentChanges()
}