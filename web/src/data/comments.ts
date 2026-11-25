import { supabase } from '../lib/supabase'
import type { TicketComment } from '../types/comments'

export async function fetchComments(ticketId: string): Promise<{ data: TicketComment[]; error: string | null }> {
  const { data, error } = await supabase
    .from('ticket_comments')
    .select('id, ticket_id, author_id, author_name, author_role, body, created_at')
    .eq('ticket_id', ticketId)
    .order('created_at', { ascending: true })
  if (error) return { data: [], error: `Không tải được bình luận: ${error.message}` }
  return { data: (data ?? []) as TicketComment[], error: null }
}

/** Trả null nếu thành công, ngược lại là thông báo lỗi. Người gửi do DB điền, client chỉ gửi ticket_id và body. */
export async function addComment(ticketId: string, body: string): Promise<string | null> {
  const { data, error } = await supabase
    .from('ticket_comments')
    .insert({ ticket_id: ticketId, body: body.trim() })
    .select('id')
  if (error) {
    if (error.code === '42501') return 'Bạn không có quyền bình luận trong phiếu này (hoặc phiếu đã đóng).'
    if (error.code === '23514') return 'Nội dung bình luận không hợp lệ (1–500 ký tự).'
    return error.message
  }
  if (!data || data.length === 0) return 'Không gửi được bình luận (bị từ chối).'
  return null
}
