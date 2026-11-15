package com.campus.service.data.repository

import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

object AuthRepository {

    /** Xác minh mật khẩu hiện tại bằng cách đăng nhập lại, rồi mới đổi mật khẩu. */
    suspend fun changePassword(email: String, currentPassword: String, newPassword: String) {
        val auth = SupabaseProvider.client.auth
        auth.signInWith(Email) {
            this.email = email
            this.password = currentPassword
        }
        auth.updateUser { this.password = newPassword }
    }

    /** Gọi hàm SQL delete_my_account(): xóa user, hồ sơ và các phiếu của user đó. */
    suspend fun deleteMyAccount() {
        SupabaseProvider.client.postgrest.rpc("delete_my_account")
    }
}