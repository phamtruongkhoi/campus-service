package com.campus.service.data.repository

import com.campus.service.data.remote.SupabaseProvider
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

object TicketRealtime {

    /** Tín hiệu khi bảng tickets hoặc ticket_events thay đổi (phiếu của người dùng). */
    fun changes(): Flow<Unit> = tableChanges("tickets", "ticket_events")

    /** Tín hiệu khi bảng tin công khai (public_incidents) thay đổi. */
    fun incidentChanges(): Flow<Unit> = tableChanges("public_incidents")

    /** Tín hiệu khi bảng bình luận (ticket_comments) thay đổi. */
    fun commentChanges(): Flow<Unit> = tableChanges("ticket_comments")

    /**
     * Phát một tín hiệu (Unit) khi đăng ký kênh thành công và mỗi khi một trong các bảng
     * thay đổi. Người dùng chỉ nhận sự kiện của dòng họ được phép SELECT theo RLS.
     * Hủy collect thì kênh được gỡ.
     */
    private fun tableChanges(vararg tableNames: String): Flow<Unit> = callbackFlow {
        val client = SupabaseProvider.client
        val channel = client.channel("changes-${UUID.randomUUID()}")

        val jobs = tableNames.map { tableName ->
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = tableName
            }
            launch { changeFlow.collect { trySend(Unit) } }
        }

        channel.subscribe(blockUntilSubscribed = true)
        trySend(Unit)

        awaitClose {
            jobs.forEach { it.cancel() }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    client.realtime.removeChannel(channel)
                } catch (_: Exception) {
                }
            }
        }
    }
}