package com.campus.service.data.repository

import com.campus.service.data.model.Profile
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

object ProfileRepository {

    suspend fun getMyProfile(): Profile {
        val client = SupabaseProvider.client
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Chưa đăng nhập")
        return client.from("profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<Profile>()
    }
}