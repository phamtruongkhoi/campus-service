package com.campus.service.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Profile
import com.campus.service.data.remote.SupabaseProvider
import com.campus.service.data.repository.ProfileRepository
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class AuthStep {
    LOGIN, REGISTER, VERIFY_SIGNUP, FORGOT_EMAIL, FORGOT_VERIFY, FORGOT_NEW_PASSWORD
}

data class AuthUiState(
    val initializing: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val profile: Profile? = null,
    val step: AuthStep = AuthStep.LOGIN,
    val pendingEmail: String = ""
)

class AuthViewModel : ViewModel() {

    private val auth = SupabaseProvider.client.auth

    private val _state = MutableStateFlow(AuthUiState(initializing = true))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    // true trong lúc đang đặt lại mật khẩu bằng OTP: không tự coi là đã đăng nhập
    private var recovering = false

    init {
        viewModelScope.launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        if (!recovering) loadProfile()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _state.update { it.copy(initializing = false, profile = null) }
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun loadProfile() {
        try {
            val profile = ProfileRepository.getMyProfile()
            _state.update {
                it.copy(initializing = false, loading = false, profile = profile, error = null)
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    initializing = false,
                    loading = false,
                    profile = null,
                    error = "Không tải được hồ sơ người dùng: ${e.message}"
                )
            }
        }
    }

    fun goTo(step: AuthStep) {
        if (step == AuthStep.LOGIN || step == AuthStep.REGISTER) recovering = false
        _state.update { it.copy(step = step, error = null, info = null) }
    }

    fun showError(message: String) {
        _state.update { it.copy(error = message, info = null) }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, info = null) }
    }

    // ---------- Đăng nhập ----------
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                // Hồ sơ sẽ được tải khi sessionStatus báo Authenticated
                _state.update { it.copy(loading = false) }
            } catch (e: Exception) {
                val m = e.message.orEmpty()
                if (m.contains("Email not confirmed", ignoreCase = true)) {
                    _state.update {
                        it.copy(
                            loading = false,
                            step = AuthStep.VERIFY_SIGNUP,
                            pendingEmail = email.trim(),
                            info = "Tài khoản chưa xác thực. Đang gửi lại mã OTP..."
                        )
                    }
                    resendSignupOtp()
                } else {
                    _state.update { it.copy(loading = false, error = translate(e)) }
                }
            }
        }
    }

    // ---------- Đăng ký + OTP ----------
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
                if (auth.currentUserOrNull() != null) {
                    // Project không bật xác nhận email: đã có phiên, collector sẽ tải hồ sơ
                    _state.update { it.copy(loading = false) }
                } else {
                    _state.update {
                        it.copy(
                            loading = false,
                            step = AuthStep.VERIFY_SIGNUP,
                            pendingEmail = email.trim(),
                            info = "Đã gửi mã OTP tới ${email.trim()}."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e)) }
            }
        }
    }

    fun verifySignupOtp(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP,
                    email = _state.value.pendingEmail,
                    token = code.trim()
                )
                _state.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e)) }
            }
        }
    }

    fun resendSignupOtp() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                auth.resendEmail(
                    type = OtpType.Email.SIGNUP,
                    email = _state.value.pendingEmail
                )
                _state.update { it.copy(loading = false, info = "Đã gửi lại mã OTP.") }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e), info = null) }
            }
        }
    }

    // ---------- Quên mật khẩu ----------
    fun requestReset(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                auth.resetPasswordForEmail(email.trim())
                recovering = true
                _state.update {
                    it.copy(
                        loading = false,
                        step = AuthStep.FORGOT_VERIFY,
                        pendingEmail = email.trim(),
                        info = "Đã gửi mã OTP tới ${email.trim()}."
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e)) }
            }
        }
    }

    fun resendReset() {
        val email = _state.value.pendingEmail
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                auth.resetPasswordForEmail(email)
                _state.update { it.copy(loading = false, info = "Đã gửi lại mã OTP.") }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e), info = null) }
            }
        }
    }

    fun verifyResetOtp(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                auth.verifyEmailOtp(
                    type = OtpType.Email.RECOVERY,
                    email = _state.value.pendingEmail,
                    token = code.trim()
                )
                _state.update { it.copy(loading = false, step = AuthStep.FORGOT_NEW_PASSWORD) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e)) }
            }
        }
    }

    fun setNewPassword(newPassword: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                auth.updateUser { this.password = newPassword }
                try {
                    auth.signOut()
                } catch (_: Exception) {
                }
                recovering = false
                _state.update {
                    AuthUiState(
                        step = AuthStep.LOGIN,
                        info = "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = translate(e)) }
            }
        }
    }

    // ---------- Đăng xuất ----------
    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (_: Exception) {
            }
            recovering = false
            _state.update { AuthUiState() }
        }
    }

    private fun translate(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            m.contains("Invalid login credentials", ignoreCase = true) -> "Sai email hoặc mật khẩu."
            m.contains("already registered", ignoreCase = true) -> "Email này đã được đăng ký."
            m.contains("only request this after", ignoreCase = true) ||
                    m.contains("rate limit", ignoreCase = true) ->
                "Gửi quá nhanh hoặc quá nhiều lần, vui lòng đợi một lúc rồi thử lại."
            m.contains("expired", ignoreCase = true) ||
                    m.contains("invalid", ignoreCase = true) && m.contains("token", ignoreCase = true) ->
                "Mã OTP sai hoặc đã hết hạn."
            m.contains("same password", ignoreCase = true) ->
                "Mật khẩu mới phải khác mật khẩu cũ."
            m.contains("Unable to resolve host", ignoreCase = true) -> "Không có kết nối mạng."
            else -> "Đã có lỗi: $m"
        }
    }
}