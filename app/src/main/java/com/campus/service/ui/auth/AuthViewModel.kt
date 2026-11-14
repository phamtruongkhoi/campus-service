package com.campus.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.campus.service.data.remote.SupabaseProvider
data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val signedInEmail: String? = null
)

class AuthViewModel : ViewModel() {
    private val auth = SupabaseProvider.client.auth
    private val _state = MutableStateFlow(
        AuthUiState(signedInEmail = auth.currentUserOrNull()?.email)
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearMessages() {
        _state.update { it.copy(error = null, info = null) }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                _state.update {
                    it.copy(loading = false, signedInEmail = auth.currentUserOrNull()?.email)
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e)) }
            }
        }
    }

    fun signUp(fullName: String, studentId: String, email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                    data = buildJsonObject {
                        put("full_name", fullName.trim())
                        put("student_id", studentId.trim())
                    }
                }
                val signedIn = auth.currentUserOrNull()?.email
                _state.update {
                    it.copy(
                        loading = false,
                        signedInEmail = signedIn,
                        info = if (signedIn == null)
                            "Đăng ký thành công. Vui lòng kiểm tra email để xác thực (bước OTP sẽ làm ở mục 1.4)."
                        else null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e)) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (_: Exception) {
            }
            _state.update { AuthUiState() }
        }
    }

    private fun translate(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            m.contains("Invalid login credentials", ignoreCase = true) -> "Sai email hoặc mật khẩu."
            m.contains("already registered", ignoreCase = true) -> "Email này đã được đăng ký."
            m.contains("rate limit", ignoreCase = true) -> "Gửi quá nhiều yêu cầu, vui lòng thử lại sau."
            m.contains("Email not confirmed", ignoreCase = true) -> "Email chưa được xác thực."
            m.contains("Unable to resolve host", ignoreCase = true) -> "Không có kết nối mạng."
            else -> "Đã có lỗi: $m"
        }
    }
}