package com.campus.service

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun AuthScreen(
    state: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (fullName: String, studentId: String, email: String, password: String) -> Unit,
    onClearMessages: () -> Unit
) {
    var isRegister by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        if (isRegister && fullName.isBlank()) {
            localError = "Vui lòng nhập họ tên."; return false
        }
        if (isRegister && studentId.isBlank()) {
            localError = "Vui lòng nhập MSSV."; return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            localError = "Email không hợp lệ."; return false
        }
        if (password.length < 6) {
            localError = "Mật khẩu tối thiểu 6 ký tự."; return false
        }
        localError = null
        return true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Campus Service",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = if (isRegister) "Tạo tài khoản sinh viên" else "Đăng nhập",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(24.dp))
        if (isRegister) {
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
        }
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
        val errorText = localError ?: state.error
        if (errorText != null) {
            Text(errorText, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        if (state.info != null) {
            Text(state.info, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (validate()) {
                    if (isRegister) onSignUp(fullName, studentId, email, password)
                    else onSignIn(email, password)
                }
            },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(if (isRegister) "Đăng ký" else "Đăng nhập")
            }
        }
        TextButton(onClick = {
            isRegister = !isRegister
            localError = null
            onClearMessages()
        }) {
            Text(if (isRegister) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký")
        }
    }
}