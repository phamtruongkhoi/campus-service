package com.campus.service.data.repository

import com.campus.service.data.model.PublicIncident
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from

object IncidentRepository {

    /** Các sự cố công khai, cập nhật gần nhất trước. */
    suspend fun getPublicIncidents(): List<PublicIncident> =
        SupabaseProvider.client.from("public_incidents")
            .select()
            .decodeList<PublicIncident>()
            .sortedByDescending { it.updatedAt }
}