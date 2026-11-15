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

    /**
     * Phát một tín hiệu (Unit) khi đăng ký kênh thành công và mỗi khi bảng tickets
     * hoặc ticket_events thay đổi. Người dùng chỉ nhận sự kiện của dòng họ được phép
     * SELECT theo RLS. Hủy collect thì kênh được gỡ.
     */
    fun changes(): Flow<Unit> = callbackFlow {
        val client = SupabaseProvider.client
        val channel = client.channel("ticket-changes-${UUID.randomUUID()}")

        val ticketChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "tickets"
        }
        val eventChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "ticket_events"
        }

        val ticketJob = launch { ticketChanges.collect { trySend(Unit) } }
        val eventJob = launch { eventChanges.collect { trySend(Unit) } }

        channel.subscribe(blockUntilSubscribed = true)
        trySend(Unit)

        awaitClose {
            ticketJob.cancel()
            eventJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    client.realtime.removeChannel(channel)
                } catch (_: Exception) {
                }
            }
        }
    }
}