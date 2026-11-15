package com.campus.service.data.model

enum class TicketStatus(val dbValue: String, val label: String) {
    OPEN("open", "Chờ tiếp nhận"),
    ASSIGNED("assigned", "Đã phân công"),
    IN_PROGRESS("in_progress", "Đang xử lý"),
    RESOLVED("resolved", "Đã khắc phục"),
    CLOSED("closed", "Đã đóng");

    companion object {
        fun from(value: String): TicketStatus =
            entries.firstOrNull { it.dbValue == value } ?: OPEN
    }
}

enum class TicketPriority(val dbValue: String, val label: String) {
    LOW("low", "Thấp"),
    MEDIUM("medium", "Trung bình"),
    HIGH("high", "Cao");

    companion object {
        fun from(value: String): TicketPriority =
            entries.firstOrNull { it.dbValue == value } ?: MEDIUM
    }
}