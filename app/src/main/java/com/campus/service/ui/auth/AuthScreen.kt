package com.campus.service.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.AppTextField
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText
import com.campus.service.util.isValidEmail
import com.campus.service.util.isValidOtp

@Composable
fun AuthScreen(state: AuthUiState, vm: AuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Banner gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.School, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Campus Service",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    "Báo cáo và theo dõi sự cố trong trường",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            AppCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    when (state.step) {
                        AuthStep.LOGIN -> LoginForm(state, vm)
                        AuthStep.REGISTER -> RegisterForm(state, vm)
                        AuthStep.VERIFY_SIGNUP -> OtpForm(
                            title = "Xác thực tài khoản",
                            description = "Nhập mã OTP đã gửi tới ${state.pendingEmail}",
                            state = state,
                            onSubmit = vm::verifySignupOtp,
                            onResend = vm::resendSignupOtp,
                            onBack = { vm.goTo(AuthStep.LOGIN) },
                            onInvalid = vm::showError
                        )
                        AuthStep.FORGOT_EMAIL -> ForgotEmailForm(state, vm)
                        AuthStep.FORGOT_VERIFY -> OtpForm(
                            title = "Nhập mã OTP",
                            description = "Mã đặt lại mật khẩu đã gửi tới ${state.pendingEmail}",
                            state = state,
                            onSubmit = vm::verifyResetOtp,
                            onResend = vm::resendReset,
                            onBack = { vm.goTo(AuthStep.LOGIN) },
                            onInvalid = vm::showError
                        )
                        AuthStep.FORGOT_NEW_PASSWORD -> NewPasswordForm(state, vm)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormTitle(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    if (subtitle != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun LoginForm(state: AuthUiState, vm: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    FormTitle("Đăng nhập", "Dùng email đã đăng ký để tiếp tục")
    AppTextField(email, { email = it }, "Email", leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email)
    Spacer(Modifier.height(12.dp))
    AppTextField(password, { password = it }, "Mật khẩu", leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password, password = true)
    Spacer(Modifier.height(16.dp))
    MessageText(error = localError ?: state.error, info = state.info)
    LoadingButton("Đăng nhập", state.loading) {
        if (!isValidEmail(email)) {
            localError = "Email không hợp lệ."
        } else if (password.length < 6) {
            localError = "Mật khẩu tối thiểu 6 ký tự."
        } else {
            localError = null
            vm.signIn(email, password)
        }
    }
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = { vm.goTo(AuthStep.FORGOT_EMAIL) }, modifier = Modifier.fillMaxWidth()) {
        Text("Quên mật khẩu?")
    }
    TextButton(onClick = { vm.goTo(AuthStep.REGISTER) }, modifier = Modifier.fillMaxWidth()) {
        Text("Chưa có tài khoản? Đăng ký")
    }
}

@Composable
private fun RegisterForm(state: AuthUiState, vm: AuthViewModel) {
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    FormTitle("Tạo tài khoản sinh viên", "Điền thông tin để bắt đầu báo cáo sự cố")
    AppTextField(fullName, { fullName = it }, "Họ và tên", leadingIcon = Icons.Outlined.Person)
    Spacer(Modifier.height(12.dp))
    AppTextField(studentId, { studentId = it }, "MSSV", leadingIcon = Icons.Outlined.Badge)
    Spacer(Modifier.height(12.dp))
    AppTextField(email, { email = it }, "Email", leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email)
    Spacer(Modifier.height(12.dp))
    AppTextField(password, { password = it }, "Mật khẩu", leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password, password = true)
    Spacer(Modifier.height(16.dp))
    MessageText(error = localError ?: state.error, info = state.info)
    LoadingButton("Đăng ký", state.loading) {
        when {
            fullName.isBlank() -> localError = "Vui lòng nhập họ tên."
            studentId.isBlank() -> localError = "Vui lòng nhập MSSV."
            !isValidEmail(email) -> localError = "Email không hợp lệ."
            password.length < 6 -> localError = "Mật khẩu tối thiểu 6 ký tự."
            else -> {
                localError = null
                vm.signUp(fullName, studentId, email, password)
            }
        }
    }
    TextButton(onClick = { vm.goTo(AuthStep.LOGIN) }, modifier = Modifier.fillMaxWidth()) {
        Text("Đã có tài khoản? Đăng nhập")
    }
}

@Composable
private fun OtpForm(
    title: String,
    description: String,
    state: AuthUiState,
    onSubmit: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onInvalid: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    FormTitle(title, description)
    AppTextField(
        code,
        { code = it.filter { c -> c.isDigit() }.take(10) },
        "Mã OTP",
        leadingIcon = Icons.Outlined.Pin,
        keyboardType = KeyboardType.Number
    )
    Spacer(Modifier.height(16.dp))
    MessageText(error = state.error, info = state.info)
    LoadingButton("Xác nhận", state.loading) {
        if (isValidOtp(code)) onSubmit(code) else onInvalid("Mã OTP gồm 6 đến 10 chữ số.")
    }
    TextButton(onClick = onResend, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
        Text("Gửi lại mã")
    }
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Quay lại đăng nhập") }
}

@Composable
private fun ForgotEmailForm(state: AuthUiState, vm: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    FormTitle("Quên mật khẩu", "Nhập email đã đăng ký, chúng tôi sẽ gửi mã OTP.")
    AppTextField(email, { email = it }, "Email", leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email)
    Spacer(Modifier.height(16.dp))
    MessageText(error = localError ?: state.error, info = state.info)
    LoadingButton("Gửi mã OTP", state.loading) {
        if (!isValidEmail(email)) {
            localError = "Email không hợp lệ."
        } else {
            localError = null
            vm.requestReset(email)
        }
    }
    TextButton(onClick = { vm.goTo(AuthStep.LOGIN) }, modifier = Modifier.fillMaxWidth()) {
        Text("Quay lại đăng nhập")
    }
}

@Composable
private fun NewPasswordForm(state: AuthUiState, vm: AuthViewModel) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    FormTitle("Đặt mật khẩu mới")
    AppTextField(password, { password = it }, "Mật khẩu mới", leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password, password = true)
    Spacer(Modifier.height(12.dp))
    AppTextField(confirm, { confirm = it }, "Nhập lại mật khẩu mới", leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password, password = true)
    Spacer(Modifier.height(16.dp))
    MessageText(error = localError ?: state.error, info = state.info)
    LoadingButton("Đổi mật khẩu", state.loading) {
        when {
            password.length < 6 -> localError = "Mật khẩu tối thiểu 6 ký tự."
            password != confirm -> localError = "Hai mật khẩu không khớp."
            else -> {
                localError = null
                vm.setNewPassword(password)
            }
        }
    }
}