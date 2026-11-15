package com.campus.service.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText
import com.campus.service.util.isValidEmail
import com.campus.service.util.isValidOtp

@Composable
fun AuthScreen(state: AuthUiState, vm: AuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Campus Service", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

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

@Composable
private fun LoginForm(state: AuthUiState, vm: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Text("Đăng nhập", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Mật khẩu") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
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
    TextButton(onClick = { vm.goTo(AuthStep.FORGOT_EMAIL) }) { Text("Quên mật khẩu?") }
    TextButton(onClick = { vm.goTo(AuthStep.REGISTER) }) { Text("Chưa có tài khoản? Đăng ký") }
}

@Composable
private fun RegisterForm(state: AuthUiState, vm: AuthViewModel) {
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Text("Tạo tài khoản sinh viên", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text("Họ và tên") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = studentId,
        onValueChange = { studentId = it },
        label = { Text("MSSV") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Mật khẩu") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
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
    TextButton(onClick = { vm.goTo(AuthStep.LOGIN) }) { Text("Đã có tài khoản? Đăng nhập") }
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

    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(description)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.filter { c -> c.isDigit() }.take(10) },
        label = { Text("Mã OTP") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    MessageText(error = state.error, info = state.info)
    LoadingButton("Xác nhận", state.loading) {
        if (isValidOtp(code)) onSubmit(code) else onInvalid("Mã OTP gồm 6 đến 10 chữ số.")
    }
    TextButton(onClick = onResend, enabled = !state.loading) { Text("Gửi lại mã") }
    TextButton(onClick = onBack) { Text("Quay lại đăng nhập") }
}

@Composable
private fun ForgotEmailForm(state: AuthUiState, vm: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Text("Quên mật khẩu", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text("Nhập email đã đăng ký, chúng tôi sẽ gửi mã OTP.")
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
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
    TextButton(onClick = { vm.goTo(AuthStep.LOGIN) }) { Text("Quay lại đăng nhập") }
}

@Composable
private fun NewPasswordForm(state: AuthUiState, vm: AuthViewModel) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Text("Đặt mật khẩu mới", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Mật khẩu mới") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = confirm,
        onValueChange = { confirm = it },
        label = { Text("Nhập lại mật khẩu mới") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
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