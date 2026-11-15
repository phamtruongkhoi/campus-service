package com.campus.service.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Area(
    val id: Int,
    val name: String,
    val building: String? = null
) {
    val label: String
        get() = if (building.isNullOrBlank()) name else "$building - $name"
}

@Serializable
data class Category(
    val id: Int,
    val name: String
)