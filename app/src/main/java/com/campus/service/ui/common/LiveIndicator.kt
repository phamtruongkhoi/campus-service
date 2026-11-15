package com.campus.service.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun LiveIndicator(live: Boolean) {
    Text(
        text = if (live) "● Đang cập nhật trực tiếp" else "○ Chưa kết nối cập nhật trực tiếp",
        style = MaterialTheme.typography.labelMedium,
        color = if (live) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    )
}