import { supabase } from '../lib/supabase'
import type { TicketPriority, TicketStatus } from '../types/database'

/** Các cột Admin được ghi khi điều phối. Gửi đủ cả khi là null (xóa kỹ thuật viên/ETA). */
export interface DispatchUpdate {
  priority: TicketPriority
  assigned_to: string | null
  eta: string | null
  is_public: boolean
  status: TicketStatus
}

/** Trả về thông báo lỗi tiếng Việt, hoặc null nếu lưu thành công. */
export async function updateDispatch(ticketId: string, update: DispatchUpdate): Promise<string | null> {
  const { data, error } = await supabase
    .from('tickets')
    .update(update)
    .eq('id', ticketId)
    .select('id')

  if (error) return `Không lưu được: ${error.message}`
  // RLS chặn thì Supabase không báo lỗi mà trả về 0 dòng: phải kiểm tra riêng.
  if (!data || data.length === 0) return 'Không lưu được: không có quyền cập nhật phiếu này.'
  return null
}
