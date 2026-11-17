package com.campus.service.util

import java.util.Locale

/** Chuyển chuỗi thời gian ISO-8601 của Supabase (timestamptz) sang mili-giây; null nếu rỗng/sai định dạng. */
fun parseIsoMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

/** Rút gọn khoảng thời gian: "45 phút", "3,5 giờ", "2,3 ngày". */
fun formatDuration(millis: Long): String {
    val minutes = millis / 60_000
    val vi = Locale("vi", "VN")
    return when {
        minutes < 60 -> "$minutes phút"
        minutes < 1440 ->
            String.format(vi, "%.1f", minutes / 60.0).removeSuffix(",0") + " giờ"
        else ->
            String.format(vi, "%.1f", minutes / 1440.0).removeSuffix(",0") + " ngày"
    }
}