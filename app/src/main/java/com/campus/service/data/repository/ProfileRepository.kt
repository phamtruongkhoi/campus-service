package com.campus.service.data.repository

import com.campus.service.data.model.Profile
import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ProfileUpdate(
    @SerialName("full_name") val fullName: String,
    @SerialName("student_id") val studentId: String? = null
)

object ProfileRepository {

    private fun currentUserId(): String =
        SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Chưa đăng nhập")

    suspend fun getMyProfile(): Profile {
        val userId = currentUserId()
        return SupabaseProvider.client.from("profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<Profile>()
    }

    /** studentId = null thì không cập nhật cột MSSV (dùng cho Admin). */
    suspend fun updateProfile(fullName: String, studentId: String?): Profile {
        val userId = currentUserId()
        return SupabaseProvider.client.from("profiles")
            .update(ProfileUpdate(fullName = fullName, studentId = studentId)) {
                select()
                filter { eq("id", userId) }
            }
            .decodeSingle<Profile>()
    }
}