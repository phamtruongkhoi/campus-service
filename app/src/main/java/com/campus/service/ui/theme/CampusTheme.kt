package com.campus.service.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.service.data.model.TicketPriority
import com.campus.service.data.model.TicketStatus

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE7FF),
    onPrimaryContainer = Color(0xFF0B2A6B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCF3EE),
    onSecondaryContainer = Color(0xFF042F2A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9C2),
    onTertiaryContainer = Color(0xFF4A2A00),
    background = Color(0xFFF4F7FC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF4F7FC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE6ECF6),
    onSurfaceVariant = Color(0xFF55627A),
    outline = Color(0xFF8A96AB),
    outlineVariant = Color(0xFFD5DDEB),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E0),
    onErrorContainer = Color(0xFF5C0A0A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFE),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEFF3FA),
    surfaceContainerHighest = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EA0FF),
    onPrimary = Color(0xFF06214F),
    primaryContainer = Color(0xFF1B3A7A),
    onPrimaryContainer = Color(0xFFDBE7FF),
    secondary = Color(0xFF4FD1C5),
    onSecondary = Color(0xFF03332E),
    secondaryContainer = Color(0xFF0F4F49),
    onSecondaryContainer = Color(0xFFCCF3EE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3A2400),
    tertiaryContainer = Color(0xFF5A3A00),
    onTertiaryContainer = Color(0xFFFFE9C2),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6EBF5),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFE6EBF5),
    surfaceVariant = Color(0xFF1B2538),
    onSurfaceVariant = Color(0xFFA3AFC5),
    outline = Color(0xFF6B7890),
    outlineVariant = Color(0xFF2A364D),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF4A0000),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF080D18),
    surfaceContainerLow = Color(0xFF101828),
    surfaceContainer = Color(0xFF141D2E),
    surfaceContainerHigh = Color(0xFF1B2538),
    surfaceContainerHighest = Color(0xFF18233A)
)

/** Bộ màu cho một trạng thái phiếu: màu nhấn (chấm), nền chip, màu chữ trên chip. */
@Immutable
data class StatusStyle(val accent: Color, val container: Color, val onContainer: Color)

/** Màu ngoài bộ màu chuẩn của Material: trạng thái, ưu tiên, dải màu tiêu đề. */
@Immutable
class ExtraColors(
    val open: StatusStyle,
    val assigned: StatusStyle,
    val inProgress: StatusStyle,
    val resolved: StatusStyle,
    val closed: StatusStyle,
    val priorityLow: Color,
    val priorityMedium: Color,
    val priorityHigh: Color,
    val success: Color,
    val headerStart: Color,
    val headerEnd: Color
) {
    fun status(status: TicketStatus): StatusStyle = when (status) {
        TicketStatus.OPEN -> open
        TicketStatus.ASSIGNED -> assigned
        TicketStatus.IN_PROGRESS -> inProgress
        TicketStatus.RESOLVED -> resolved
        TicketStatus.CLOSED -> closed
    }

    fun priority(priority: TicketPriority): Color = when (priority) {
        TicketPriority.LOW -> priorityLow
        TicketPriority.MEDIUM -> priorityMedium
        TicketPriority.HIGH -> priorityHigh
    }
}

private val LightExtra = ExtraColors(
    open = StatusStyle(Color(0xFF64748B), Color(0xFFE8ECF3), Color(0xFF3B4658)),
    assigned = StatusStyle(Color(0xFF2563EB), Color(0xFFDBE7FF), Color(0xFF0B2A6B)),
    inProgress = StatusStyle(Color(0xFFD97706), Color(0xFFFFEBC7), Color(0xFF5A3300)),
    resolved = StatusStyle(Color(0xFF16A34A), Color(0xFFD9F5E3), Color(0xFF0B4A22)),
    closed = StatusStyle(Color(0xFF475569), Color(0xFFDDE3EC), Color(0xFF1F2937)),
    priorityLow = Color(0xFF64748B),
    priorityMedium = Color(0xFFD97706),
    priorityHigh = Color(0xFFDC2626),
    success = Color(0xFF16A34A),
    headerStart = Color(0xFF2563EB),
    headerEnd = Color(0xFF0D9488)
)

private val DarkExtra = ExtraColors(
    open = StatusStyle(Color(0xFF94A3B8), Color(0xFF263146), Color(0xFFD3DBE8)),
    assigned = StatusStyle(Color(0xFF6EA0FF), Color(0xFF1B3A7A), Color(0xFFDBE7FF)),
    inProgress = StatusStyle(Color(0xFFFBBF24), Color(0xFF4D3300), Color(0xFFFFE9C2)),
    resolved = StatusStyle(Color(0xFF4ADE80), Color(0xFF0F4228), Color(0xFFD1F7DF)),
    closed = StatusStyle(Color(0xFFA8B4C7), Color(0xFF2A364D), Color(0xFFE2E8F2)),
    priorityLow = Color(0xFF94A3B8),
    priorityMedium = Color(0xFFFBBF24),
    priorityHigh = Color(0xFFFF8A8A),
    success = Color(0xFF4ADE80),
    headerStart = Color(0xFF1D4ED8),
    headerEnd = Color(0xFF0F766E)
)

private val LocalExtraColors = staticCompositionLocalOf { LightExtra }

/** Truy cập màu mở rộng: Campus.extra.status(TicketStatus.OPEN)... */
object Campus {
    val extra: ExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current
}

private val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val BaseTypography = Typography()

private val CampusTypography = BaseTypography.copy(
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(
        fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp
    ),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun CampusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalExtraColors provides (if (darkTheme) DarkExtra else LightExtra)
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = CampusTypography,
            shapes = CampusShapes,
            content = content
        )
    }
}