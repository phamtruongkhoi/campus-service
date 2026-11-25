import { supabase } from '../lib/supabase'
import { imagePath } from '../lib/studentRules'
import type { Ticket } from '../types/database'
import type { AreaOption, CategoryOption, PublicIncident } from '../types/student'
import { TICKET_SELECT } from './tickets'

const BUCKET = 'ticket-images'

export async function fetchPublicIncidents(): Promise<{ data: PublicIncident[]; error: string | null }> {
  const { data, error } = await supabase.from('public_incidents').select('*')
  if (error) return { data: [], error: `Không tải được bảng tin: ${error.message}` }
  return { data: (data ?? []) as PublicIncident[], error: null }
}

/** Các phiếu do chính sinh viên tạo, mới nhất trước. */
export async function fetchMyTickets(userId: string): Promise<{ data: Ticket[]; error: string | null }> {
  const { data, error } = await supabase
    .from('tickets')
    .select(TICKET_SELECT)
    .eq('created_by', userId)
    .order('created_at', { ascending: false })
  if (error) return { data: [], error: `Không tải được danh sách phiếu: ${error.message}` }
  return { data: (data ?? []) as unknown as Ticket[], error: null }
}

export async function fetchOptions(): Promise<{ areas: AreaOption[]; categories: CategoryOption[]; error: string | null }> {
  const [areas, categories] = await Promise.all([
    supabase.from('areas').select('id, name, building').order('id'),
    supabase.from('categories').select('id, name').order('id'),
  ])
  const failed = areas.error ?? categories.error
  if (failed) return { areas: [], categories: [], error: `Không tải được danh sách khu vực/loại lỗi: ${failed.message}` }
  return {
    areas: (areas.data ?? []) as AreaOption[],
    categories: (categories.data ?? []) as CategoryOption[],
    error: null,
  }
}

export interface NewTicketInput {
  userId: string
  areaId: number
  categoryId: number
  title: string
  description: string
  images: Blob[]
}

/**
 * Thứ tự giống Android: tải ảnh lên -> tạo phiếu -> ghi ticket_images.
 * Trả null nếu thành công, ngược lại là thông báo lỗi. Ảnh đã tải mà bước sau lỗi thì cố gắng xóa lại (không bắt buộc thành công).
 */
export async function createTicket(input: NewTicketInput): Promise<string | null> {
  const ticketId = crypto.randomUUID()
  const bucket = supabase.storage.from(BUCKET)
  const uploaded: string[] = []
  try {
    for (const [index, blob] of input.images.entries()) {
      const path = imagePath(input.userId, ticketId, index)
      const { error } = await bucket.upload(path, blob, { contentType: 'image/jpeg' })
      if (error) throw new Error(`Không tải được ảnh ${index + 1}: ${error.message}`)
      uploaded.push(path)
    }

    const created = await supabase.from('tickets').insert({
      id: ticketId,
      created_by: input.userId,
      area_id: input.areaId,
      category_id: input.categoryId,
      title: input.title.trim(),
      description: input.description.trim(),
    })
    if (created.error) throw new Error(created.error.message)

    if (uploaded.length > 0) {
      const linked = await supabase
        .from('ticket_images')
        .insert(uploaded.map((path) => ({ ticket_id: ticketId, path })))
      if (linked.error) throw new Error(`Phiếu đã tạo nhưng không ghi được ảnh: ${linked.error.message}`)
    }
    return null
  } catch (e) {
    if (uploaded.length > 0) void bucket.remove(uploaded)
    return e instanceof Error ? e.message : String(e)
  }
}

/** Nghiệm thu: Resolved -> Closed kèm số sao và nhận xét. Trả null nếu thành công. */
export async function acceptTicket(ticketId: string, rating: number, feedback: string): Promise<string | null> {
  const { data, error } = await supabase
    .from('tickets')
    .update({ status: 'closed', rating, feedback: feedback.trim() || null })
    .eq('id', ticketId)
    .eq('status', 'resolved')
    .select('id')
  if (error) return error.message
  if (!data || data.length === 0) return 'Phiếu không còn ở trạng thái chờ nghiệm thu.'
  return null
}

/** Mở lại phiếu chưa đạt (Resolved -> In progress) qua hàm SQL reopen_ticket. Trả null nếu thành công. */
export async function reopenTicket(ticketId: string, reason: string): Promise<string | null> {
  const { error } = await supabase.rpc('reopen_ticket', { p_ticket_id: ticketId, p_reason: reason.trim() })
  return error ? error.message : null
}
