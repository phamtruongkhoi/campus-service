package com.campus.service.util

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Đổi chuỗi thời gian ISO của Supabase sang giờ máy, dạng dd/MM/yyyy HH:mm. */
fun formatDateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        OffsetDateTime.parse(iso)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(displayFormatter)
    } catch (e: Exception) {
        iso
    }
}