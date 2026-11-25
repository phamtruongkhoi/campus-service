// Quy tắc phía Sinh viên (logic thuần, có unit test ở 6.4).
// Giới hạn độ dài tiêu đề/mô tả/nhận xét là mức web tự đặt; DB là nơi quyết định cuối cùng.

import type { Ticket, TicketStatus } from '../types/database'
import type { PublicIncident } from '../types/student'
import { parseTime } from './ticketLogic'

export const MAX_IMAGES = 3
export const MAX_IMAGE_BYTES = 20 * 1024 * 1024 // ảnh gốc tối đa 20 MB, trước khi nén
export const TITLE_MIN = 3
export const TITLE_MAX = 100
export const DESCRIPTION_MAX = 1000
export const FEEDBACK_MAX = 500
/** Khớp TicketAcceptanceRepository.kt và hàm SQL reopen_ticket. */
export const REOPEN_MIN = 10
export const REOPEN_MAX = 300

export function validateTitle(value: string): string | null {
  const v = value.trim()
  if (v.length < TITLE_MIN) return `Tiêu đề cần ít nhất ${TITLE_MIN} ký tự.`
  if (v.length > TITLE_MAX) return `Tiêu đề tối đa ${TITLE_MAX} ký tự.`
  return null
}

export function validateDescription(value: string): string | null {
  return value.trim().length > DESCRIPTION_MAX ? `Mô tả tối đa ${DESCRIPTION_MAX} ký tự.` : null
}

export function validateRating(rating: number): string | null {
  return Number.isInteger(rating) && rating >= 1 && rating <= 5 ? null : 'Vui lòng chọn số sao từ 1 đến 5.'
}

export function validateFeedback(value: string): string | null {
  return value.trim().length > FEEDBACK_MAX ? `Nhận xét tối đa ${FEEDBACK_MAX} ký tự.` : null
}

export function validateReopenReason(value: string): string | null {
  const n = value.trim().length
  if (n < REOPEN_MIN) return `Lý do cần ít nhất ${REOPEN_MIN} ký tự.`
  if (n > REOPEN_MAX) return `Lý do tối đa ${REOPEN_MAX} ký tự.`
  return null
}

/** Chỉ phiếu "Đã khắc phục" mới nghiệm thu hoặc mở lại được. */
export function canReviewResolved(status: TicketStatus): boolean {
  return status === 'resolved'
}

export function validateImageFile(file: { type: string; size: number }): string | null {
  if (!file.type.startsWith('image/')) return 'Tệp không phải ảnh.'
  if (file.size > MAX_IMAGE_BYTES) return 'Ảnh quá lớn (tối đa 20 MB).'
  return null
}

/** Thu nhỏ để cạnh dài nhất không vượt max, giữ tỉ lệ; không phóng to. */
export function fitWithin(width: number, height: number, max: number): { width: number; height: number } {
  const longest = Math.max(width, height)
  if (longest <= max || longest <= 0) return { width, height }
  const scale = max / longest
  return { width: Math.max(1, Math.round(width * scale)), height: Math.max(1, Math.round(height * scale)) }
}

/** Đường dẫn trong bucket, khớp Android: userId/ticketId/<số thứ tự từ 1>.jpg */
export function imagePath(userId: string, ticketId: string, index: number): string {
  return `${userId}/${ticketId}/${index + 1}.jpg`
}

export function incidentAreaLabel(i: Pick<PublicIncident, 'area_name' | 'building'>): string {
  const name = i.area_name?.trim() || '—'
  return i.building && i.building.trim() ? `${i.building} - ${name}` : name
}

/** Bảng tin: cập nhật gần nhất trước. */
export function sortIncidents(list: readonly PublicIncident[]): PublicIncident[] {
  return [...list].sort((a, b) => (parseTime(b.updated_at) ?? 0) - (parseTime(a.updated_at) ?? 0))
}

export function filterIncidents(
  list: readonly PublicIncident[],
  status: TicketStatus | null,
  query: string,
): PublicIncident[] {
  const q = query.trim().toLocaleLowerCase('vi')
  return list.filter((i) => {
    if (status !== null && i.status !== status) return false
    if (!q) return true
    return `${incidentAreaLabel(i)} ${i.category_name ?? ''}`.toLocaleLowerCase('vi').includes(q)
  })
}

export function filterByStatus(list: readonly Ticket[], status: TicketStatus | null): Ticket[] {
  return status === null ? [...list] : list.filter((t) => t.status === status)
}
