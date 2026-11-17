package com.campus.service.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Profile
import com.campus.service.data.model.UserRole
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.AppTextField
import com.campus.service.ui.common.GradientHeader
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

    val initial = (current.fullName?.trim()?.firstOrNull() ?: current.email?.firstOrNull() ?: 'U')
        .uppercaseChar().toString()

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(
                        current.fullName?.ifBlank { null } ?: "Cá nhân",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        role.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AppCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thông tin tài khoản", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = current.email.orEmpty(),
                        onValueChange = {},
                        label = "Email",
                        leadingIcon = Icons.Outlined.Email,
                        readOnly = true
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "Họ và tên",
                        leadingIcon = Icons.Outlined.Person,
                        readOnly = !canEditName
                    )
                    if (role == UserRole.STUDENT) {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = studentId,
                            onValueChange = { studentId = it },
                            label = "MSSV",
                            leadingIcon = Icons.Outlined.Badge,
                            readOnly = !canEditStudentId
                        )
                    }
                    Spacer(Modifier.height(16.dp))
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
                }
            }

            if (canChangePassword) {
                Spacer(Modifier.height(12.dp))
                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bảo mật", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = {
                                showPasswordForm = !showPasswordForm
                                vm.clearMessages()
                            }) {
                                Text(if (showPasswordForm) "Ẩn" else "Đổi mật khẩu")
                            }
                        }
                        if (showPasswordForm) {
                            AppTextField(oldPassword, { oldPassword = it }, "Mật khẩu hiện tại",
                                leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password, password = true)
                            Spacer(Modifier.height(8.dp))
                            AppTextField(newPassword, { newPassword = it }, "Mật khẩu mới",
                                leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password, password = true)
                            Spacer(Modifier.height(8.dp))
                            AppTextField(confirmPassword, { confirmPassword = it }, "Nhập lại mật khẩu mới",
                                leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password, password = true)
                            Spacer(Modifier.height(12.dp))
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
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.Logout, null)
                Spacer(Modifier.size(8.dp))
                Text("Đăng xuất")
            }

            if (canDelete) {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xóa tài khoản")
                }
            }
            Spacer(Modifier.height(24.dp))
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
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }
}