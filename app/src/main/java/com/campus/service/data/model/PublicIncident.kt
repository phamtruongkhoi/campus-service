package com.campus.service.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Một sự cố công khai trên bảng tin (bảng public_incidents). */
@Serializable
data class PublicIncident(
    @SerialName("ticket_id") val ticketId: String,
    @SerialName("area_name") val areaName: String,
    val building: String? = null,
    @SerialName("category_name") val categoryName: String,
    val status: String = "open",
    val eta: String? = null,
    @SerialName("updated_at") val updatedAt: String = ""
) {
    val statusEnum: TicketStatus get() = TicketStatus.from(status)
    val areaLabel: String
        get() = if (building.isNullOrBlank()) areaName else "$building - $areaName"
}