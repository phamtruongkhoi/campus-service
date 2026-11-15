package com.campus.service.data.repository

import com.campus.service.data.model.Area
import com.campus.service.data.model.Category
import com.campus.service.data.model.NewTicket
import com.campus.service.data.model.NewTicketImage
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.util.UUID

object TicketRepository {

    private const val BUCKET = "ticket-images"

    suspend fun getAreas(): List<Area> =
        SupabaseProvider.client.from("areas")
            .select()
            .decodeList<Area>()
            .sortedBy { it.id }

    suspend fun getCategories(): List<Category> =
        SupabaseProvider.client.from("categories")
            .select()
            .decodeList<Category>()
            .sortedBy { it.id }

    /**
     * Thứ tự: upload ảnh -> tạo phiếu -> ghi ticket_images.
     * Nếu upload lỗi thì chưa có phiếu nào được tạo.
     */
    suspend fun createTicket(
        areaId: Int,
        categoryId: Int,
        title: String,
        description: String,
        images: List<ByteArray>
    ) {
        val client = SupabaseProvider.client
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Chưa đăng nhập")
        val ticketId = UUID.randomUUID().toString()

        val bucket = client.storage.from(BUCKET)
        val paths = images.mapIndexed { index, bytes ->
            val path = "$userId/$ticketId/${index + 1}.jpg"
            bucket.upload(path, bytes)
            path
        }

        client.from("tickets").insert(
            NewTicket(
                id = ticketId,
                createdBy = userId,
                areaId = areaId,
                categoryId = categoryId,
                title = title,
                description = description
            )
        )

        if (paths.isNotEmpty()) {
            client.from("ticket_images").insert(
                paths.map { NewTicketImage(ticketId = ticketId, path = it) }
            )
        }
    }
}