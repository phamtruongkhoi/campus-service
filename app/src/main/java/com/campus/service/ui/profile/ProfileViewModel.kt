package com.campus.service.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.service.data.model.Profile
import com.campus.service.data.repository.AuthRepository
import com.campus.service.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: Profile? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val passwordChanges: Int = 0
)

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    /** Gọi khi vào màn hình; nếu đổi tài khoản thì nạp lại hồ sơ mới. */
    fun init(profile: Profile) {
        if (_state.value.profile?.id != profile.id) {
            _state.value = ProfileUiState(profile = profile)
        }
    }

    fun clearMessages() {
        _state.update { it.copy(message = null, error = null) }
    }

    fun showError(text: String) {
        _state.update { it.copy(error = text, message = null) }
    }

    fun save(fullName: String, studentId: String?) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            try {
                val updated = ProfileRepository.updateProfile(fullName.trim(), studentId?.trim())
                _state.update {
                    it.copy(busy = false, profile = updated, message = "Đã lưu thay đổi.")
                }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = "Không lưu được: ${e.message}") }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val email = _state.value.profile?.email ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            try {
                AuthRepository.changePassword(email, currentPassword, newPassword)
                _state.update {
                    it.copy(
                        busy = false,
                        message = "Đổi mật khẩu thành công.",
                        passwordChanges = it.passwordChanges + 1
                    )
                }
            } catch (e: Exception) {
                val m = e.message.orEmpty()
                val text = when {
                    m.contains("Invalid login credentials", ignoreCase = true) ->
                        "Mật khẩu hiện tại không đúng."
                    m.contains("same password", ignoreCase = true) ->
                        "Mật khẩu mới phải khác mật khẩu cũ."
                    else -> "Không đổi được mật khẩu: $m"
                }
                _state.update { it.copy(busy = false, error = text) }
            }
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            try {
                AuthRepository.deleteMyAccount()
                _state.update { ProfileUiState() }
                onDone()
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = "Không xóa được tài khoản: ${e.message}") }
            }
        }
    }
}