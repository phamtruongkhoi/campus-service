package com.campus.service.data.repository

import com.campus.service.data.model.Area
import com.campus.service.data.model.Category
import com.campus.service.data.model.NewTicket
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

object TicketRepository {

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

    suspend fun createTicket(
        areaId: Int,
        categoryId: Int,
        title: String,
        description: String
    ) {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Chưa đăng nhập")
        SupabaseProvider.client.from("tickets").insert(
            NewTicket(
                createdBy = userId,
                areaId = areaId,
                categoryId = categoryId,
                title = title,
                description = description
            )
        )
    }
}