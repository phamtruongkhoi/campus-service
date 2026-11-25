import { supabase } from '../lib/supabase'
import type { TicketEvent } from '../types/events'

export interface EventsResult {
  data: TicketEvent[]
  error: string | null
}

/** Lịch sử đổi trạng thái của một phiếu, cũ nhất trước. Quyền xem do RLS (events_select) quyết định. */
export async function fetchTicketEvents(ticketId: string): Promise<EventsResult> {
  const { data, error } = await supabase
    .from('ticket_events')
    .select('*')
    .eq('ticket_id', ticketId)
    .order('created_at', { ascending: true })

  if (error) return { data: [], error: `Không tải được lịch sử trạng thái: ${error.message}` }
  return { data: (data ?? []) as TicketEvent[], error: null }
}
