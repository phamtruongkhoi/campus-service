import { supabase } from '../lib/supabase'
import type { Ticket } from '../types/database'

/**
 * Cột đọc về cho phiếu, kèm khu vực, loại lỗi, ảnh và hồ sơ người gửi/kỹ thuật viên.
 * Bảng tickets có HAI khóa ngoại tới profiles (created_by, assigned_to) nên phải chỉ rõ cột
 * bằng cú pháp `bí_danh:profiles!tên_cột(...)`.
 */
export const TICKET_SELECT =
  '*, areas(name, building), categories(name), ticket_images(path), ' +
  'creator:profiles!created_by(full_name, student_id), ' +
  'assignee:profiles!assigned_to(full_name, student_id)'

export interface TicketsResult {
  data: Ticket[]
  error: string | null
}

/** Admin đọc được toàn bộ phiếu (policy tickets_select). PostgREST mặc định trả tối đa 1000 dòng. */
export async function fetchAllTickets(): Promise<TicketsResult> {
  const { data, error } = await supabase
    .from('tickets')
    .select(TICKET_SELECT)
    .order('created_at', { ascending: false })

  if (error) return { data: [], error: `Không tải được danh sách phiếu: ${error.message}` }
  return { data: (data ?? []) as unknown as Ticket[], error: null }
}
