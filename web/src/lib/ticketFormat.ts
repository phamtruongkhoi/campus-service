import type { Ticket } from '../types/database'

/** "Họ tên - MSSV" của người gửi phiếu. */
export function creatorLabel(ticket: Pick<Ticket, 'creator'>): string {
  const name = ticket.creator?.full_name?.trim() || '(chưa có tên)'
  const studentId = ticket.creator?.student_id?.trim()
  return studentId ? `${name} - ${studentId}` : name
}

export function categoryLabel(ticket: Pick<Ticket, 'categories'>): string {
  return ticket.categories?.name?.trim() || '—'
}

/** Ngày giờ theo kiểu Việt Nam: 25/11/2026 10:56. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

/** Số sao dạng chữ: 4 sao -> "★★★★☆". Trả về chuỗi rỗng khi chưa có đánh giá. */
export function starsText(rating: number | null | undefined): string {
  if (rating === null || rating === undefined) return ''
  const full = Math.max(0, Math.min(5, Math.round(rating)))
  return '★'.repeat(full) + '☆'.repeat(5 - full)
}
