package com.campus.service.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Profile
import com.campus.service.data.model.UserRole
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText

@Composable
fun ProfileScreen(
    profile: Profile,
    vm: ProfileViewModel,
    onSignOut: () -> Unit
) {
    LaunchedEffect(profile.id) { vm.init(profile) }

    val state by vm.state.collectAsState()
    val current = state.profile ?: profile
    val role = current.userRole

    val canEditName = role == UserRole.STUDENT || role == UserRole.ADMIN
    val canEditStudentId = role == UserRole.STUDENT
    val canChangePassword = role == UserRole.STUDENT
    val canDelete = role == UserRole.STUDENT

    var fullName by remember(current.id, current.fullName) {
        mutableStateOf(current.fullName.orEmpty())
    }
    var studentId by remember(current.id, current.studentId) {
        mutableStateOf(current.studentId.orEmpty())
    }
    var showPasswordForm by remember { mutableStateOf(false) }
    var oldPassword by remember(state.passwordChanges) { mutableStateOf("") }
    var newPassword by remember(state.passwordChanges) { mutableStateOf("") }
    var confirmPassword by remember(state.passwordChanges) { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Cá nhân", style = MaterialTheme.typography.headlineMedium)
        Text(role.displayName, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = current.email.orEmpty(),
            onValueChange = {},
            label = { Text("Email") },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Họ và tên") },
            readOnly = !canEditName,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (role == UserRole.STUDENT) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("MSSV") },
                readOnly = !canEditStudentId,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))

        MessageText(error = state.error, info = state.message)

        if (canEditName) {
            LoadingButton("Lưu thay đổi", state.busy) {
                when {
                    fullName.isBlank() -> vm.showError("Họ tên không được để trống.")
                    canEditStudentId && studentId.isBlank() -> vm.showError("MSSV không được để trống.")
                    else -> vm.save(fullName, if (canEditStudentId) studentId else null)
                }
            }
        }

        if (canChangePassword) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            TextButton(onClick = {
                showPasswordForm = !showPasswordForm
                vm.clearMessages()
            }) {
                Text(if (showPasswordForm) "Ẩn đổi mật khẩu" else "Đổi mật khẩu")
            }
            if (showPasswordForm) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Mật khẩu hiện tại") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Nhập lại mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LoadingButton("Xác nhận đổi mật khẩu", state.busy) {
                    when {
                        oldPassword.isBlank() -> vm.showError("Vui lòng nhập mật khẩu hiện tại.")
                        newPassword.length < 6 -> vm.showError("Mật khẩu mới tối thiểu 6 ký tự.")
                        newPassword != confirmPassword -> vm.showError("Hai mật khẩu mới không khớp.")
                        else -> vm.changePassword(oldPassword, newPassword)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Đăng xuất")
        }

        if (canDelete) {
            TextButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Xóa tài khoản")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa tài khoản?") },
            text = {
                Text("Tài khoản và toàn bộ phiếu yêu cầu của bạn sẽ bị xóa vĩnh viễn, không thể hoàn tác.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        vm.deleteAccount(onDone = onSignOut)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }
}