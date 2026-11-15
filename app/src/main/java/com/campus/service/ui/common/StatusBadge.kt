package com.campus.service.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.TicketStatus

@Composable
fun StatusBadge(status: TicketStatus) {
    val colors = MaterialTheme.colorScheme
    val (background, content) = when (status) {
        TicketStatus.OPEN -> colors.surfaceVariant to colors.onSurfaceVariant
        TicketStatus.ASSIGNED -> colors.secondaryContainer to colors.onSecondaryContainer
        TicketStatus.IN_PROGRESS -> colors.tertiaryContainer to colors.onTertiaryContainer
        TicketStatus.RESOLVED -> colors.primaryContainer to colors.onPrimaryContainer
        TicketStatus.CLOSED -> colors.outlineVariant to colors.onSurface
    }
    Surface(
        color = background,
        contentColor = content,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}