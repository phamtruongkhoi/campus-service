import { supabase } from '../lib/supabase'
import type { Ticket, TicketStatus } from '../types/database'
import { TICKET_SELECT } from './tickets'

/** Các phiếu được giao cho kỹ thuật viên này (mọi trạng thái), mới nhất trước. */
export async function fetchAssignedTickets(userId: string): Promise<{ data: Ticket[]; error: string | null }> {
  const { data, error } = await supabase
    .from('tickets')
    .select(TICKET_SELECT)
    .eq('assigned_to', userId)
    .order('created_at', { ascending: false })
  if (error) return { data: [], error: `Không tải được công việc: ${error.message}` }
  return { data: (data ?? []) as unknown as Ticket[], error: null }
}

/**
 * Đổi trạng thái từ `from` sang `next`. Điều kiện `status = from` chặn ghi đè khi phiếu vừa bị đổi ở nơi khác.
 * Trả null nếu thành công, ngược lại là thông báo lỗi.
 */
export async function advanceTicket(ticketId: string, from: TicketStatus, next: TicketStatus): Promise<string | null> {
  const { data, error } = await supabase
    .from('tickets')
    .update({ status: next })
    .eq('id', ticketId)
    .eq('status', from)
    .select('id')
  if (error) return error.message
  if (!data || data.length === 0) {
    return 'Phiếu đã thay đổi hoặc bạn không còn quyền cập nhật. Hãy làm mới danh sách rồi thử lại.'
  }
  return null
}
