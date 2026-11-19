package com.campus.service.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.TicketComment
import com.campus.service.data.remote.SupabaseProvider
import com.campus.service.data.repository.CommentRepository
import com.campus.service.util.formatDateTime
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Khối bình luận của một phiếu, dùng chung cho Sinh viên, Kỹ thuật viên và Admin.
 * Tự tải, tự cập nhật realtime (rời màn hình thì dừng). canPost = false: chỉ xem (phiếu đã đóng).
 * Quyền ghi cuối cùng do RLS ở DB quyết định.
 */
@OptIn(FlowPreview::class)
@Composable
fun CommentSection(
    ticketId: String,
    canPost: Boolean,
    modifier: Modifier = Modifier
) {
    val myId = remember { SupabaseProvider.client.auth.currentUserOrNull()?.id }
    var comments by remember(ticketId) { mutableStateOf<List<TicketComment>>(emptyList()) }
    var loading by remember(ticketId) { mutableStateOf(true) }
    var live by remember(ticketId) { mutableStateOf(false) }
    var sending by remember(ticketId) { mutableStateOf(false) }
    var error by remember(ticketId) { mutableStateOf<String?>(null) }
    var draft by rememberSaveable(ticketId) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try {
            comments = CommentRepository.getComments(ticketId)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Không tải được bình luận: ${e.message}"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(ticketId) {
        load()
        try {
            CommentRepository.changes()
                .onEach { live = true }
                .debounce(300)
                .collect { load() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            live = false
        } finally {
            live = false
        }
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) {
            error = "Nhập nội dung bình luận."
            return
        }
        scope.launch {
            sending = true
            error = null
            try {
                CommentRepository.add(ticketId, text)
                draft = ""
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                error = if (msg.contains("row-level security", ignoreCase = true)) {
                    "Không gửi được: phiếu đã đóng hoặc bạn không có quyền bình luận."
                } else {
                    "Không gửi được bình luận: $msg"
                }
            } finally {
                sending = false
            }
        }
    }

    AppCard(modifier = modifier.fillMaxWidth()) {
        SectionTitle(
            text = "Bình luận (${comments.size})",
            trailing = { LiveIndicator(live) }
        )
        Spacer(Modifier.height(8.dp))

        when {
            loading && comments.isEmpty() -> Text(
                "Đang tải bình luận...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            comments.isEmpty() -> Text(
                "Chưa có bình luận nào.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                comments.forEach { c ->
                    CommentBubble(comment = c, mine = c.authorId == myId)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        if (canPost) {
            AppTextField(
                value = draft,
                onValueChange = { if (it.length <= CommentRepository.MAX_LENGTH) draft = it },
                label = "Viết bình luận",
                singleLine = false,
                minLines = 2,
                readOnly = sending,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${draft.trim().length}/${CommentRepository.MAX_LENGTH} ký tự",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            MessageText(error = error, info = null)
            LoadingButton(text = "Gửi bình luận", loading = sending, onClick = { send() })
        } else {
            MessageText(error = error, info = null)
            Text(
                "Phiếu đã đóng, bình luận chỉ để xem.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun roleLabel(role: String?): String? = when (role) {
    "student" -> "Sinh viên"
    "technician" -> "Kỹ thuật viên"
    "admin" -> "Admin"
    else -> null
}

@Composable
private fun CommentBubble(comment: TicketComment, mine: Boolean) {
    val colors = MaterialTheme.colorScheme
    val background = if (mine) colors.primaryContainer else colors.surfaceVariant
    val content = if (mine) colors.onPrimaryContainer else colors.onSurfaceVariant
    val name = if (mine) "Bạn" else comment.authorName?.ifBlank { null } ?: "Người dùng"
    val role = roleLabel(comment.authorRole)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.labelLarge, color = content)
                if (role != null) {
                    Spacer(Modifier.width(6.dp))
                    Text("• $role", style = MaterialTheme.typography.labelSmall, color = content)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(comment.body, style = MaterialTheme.typography.bodyMedium, color = content)
            Spacer(Modifier.height(2.dp))
            Text(
                formatDateTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = content
            )
        }
    }
}