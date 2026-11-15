package com.campus.service.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole {
    STUDENT, ADMIN, TECHNICIAN;

    val displayName: String
        get() = when (this) {
            STUDENT -> "Sinh viên"
            ADMIN -> "Quản trị viên (Phòng CSVC)"
            TECHNICIAN -> "Nhân viên sửa chữa"
        }

    companion object {
        fun from(value: String): UserRole = when (value.lowercase()) {
            "admin" -> ADMIN
            "technician" -> TECHNICIAN
            else -> STUDENT
        }
    }
}

@Serializable
data class Profile(
    val id: String,
    val email: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("student_id") val studentId: String? = null,
    val role: String = "student"
) {
    val userRole: UserRole get() = UserRole.from(role)
}