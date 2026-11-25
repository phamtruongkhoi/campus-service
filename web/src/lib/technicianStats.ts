// Thống kê chất lượng làm việc của kỹ thuật viên (5.6). Hàm thuần, dễ unit test ở mục 6.4.
// Đánh giá được gán cho kỹ thuật viên ĐANG được giao trên phiếu (tickets.assigned_to):
// bảng ticket_events không lưu người được giao nên không truy ngược được người cũ.

import type { Profile, Ticket } from '../types/database'
import { averageProcessing, isOverdue, parseTime } from './ticketLogic'
import type { CountItem } from './ticketLogic'

export interface TechnicianStats {
  technicianId: string
  /** Tổng số phiếu đang gắn tên kỹ thuật viên này. */
  assigned: number
  /** Đã phân công + đang xử lý. */
  active: number
  /** Đã khắc phục + đã đóng ("số phiếu đã xử lý"). */
  done: number
  overdue: number
  /** Số phiếu có đánh giá sao. */
  ratingCount: number
  /** Trung bình sao (chưa làm tròn); null nếu chưa có đánh giá. */
  ratingAverage: number | null
  avgProcessingMs: number | null
  processingSample: number
}

export interface TechnicianRow {
  profile: Profile
  stats: TechnicianStats
}

export type TechnicianSortKey = 'name' | 'rating' | 'done' | 'overdue'

export function displayName(profile: Pick<Profile, 'full_name' | 'email'>): string {
  return profile.full_name?.trim() || profile.email || '(không rõ tên)'
}

export function ticketsOf(technicianId: string, tickets: readonly Ticket[]): Ticket[] {
  return tickets.filter((t) => t.assigned_to === technicianId)
}

/** Thời điểm đóng phiếu (hoặc lần cập nhật cuối) để sắp xếp đánh giá. */
function reviewTime(ticket: Ticket): number {
  return parseTime(ticket.closed_at) ?? parseTime(ticket.updated_at) ?? parseTime(ticket.created_at) ?? 0
}

/** Các phiếu có đánh giá, mới nhất trước. */
export function ratedTickets(own: readonly Ticket[]): Ticket[] {
  return own.filter((t) => t.rating !== null).sort((a, b) => reviewTime(b) - reviewTime(a))
}

export function averageRating(rated: readonly Pick<Ticket, 'rating'>[]): {
  average: number | null
  count: number
} {
  let total = 0
  let count = 0
  for (const t of rated) {
    if (t.rating !== null) {
      total += t.rating
      count += 1
    }
  }
  return { average: count > 0 ? total / count : null, count }
}

/** Số đánh giá theo từng mức sao, từ 5 sao xuống 1 sao (luôn đủ 5 dòng). */
export function ratingDistribution(rated: readonly Pick<Ticket, 'rating'>[]): CountItem[] {
  const counts = [0, 0, 0, 0, 0, 0]
  for (const t of rated) {
    if (t.rating !== null && t.rating >= 1 && t.rating <= 5) counts[t.rating] += 1
  }
  return [5, 4, 3, 2, 1].map((stars) => ({ label: `${stars} sao`, count: counts[stars] }))
}

export function computeStats(
  technicianId: string,
  tickets: readonly Ticket[],
  now: number,
): TechnicianStats {
  const own = ticketsOf(technicianId, tickets)
  let active = 0
  let done = 0
  let overdue = 0
  for (const t of own) {
    if (t.status === 'assigned' || t.status === 'in_progress') active += 1
    if (t.status === 'resolved' || t.status === 'closed') done += 1
    if (isOverdue(t, now)) overdue += 1
  }
  const rating = averageRating(own)
  const processing = averageProcessing(own.filter((t) => t.status === 'resolved' || t.status === 'closed'))
  return {
    technicianId,
    assigned: own.length,
    active,
    done,
    overdue,
    ratingCount: rating.count,
    ratingAverage: rating.average,
    avgProcessingMs: processing.averageMs,
    processingSample: processing.sampleSize,
  }
}

export function buildRows(
  technicians: readonly Profile[],
  tickets: readonly Ticket[],
  now: number,
): TechnicianRow[] {
  return technicians.map((profile) => ({ profile, stats: computeStats(profile.id, tickets, now) }))
}

export function sortRows(rows: readonly TechnicianRow[], key: TechnicianSortKey): TechnicianRow[] {
  const byName = (a: TechnicianRow, b: TechnicianRow) =>
    displayName(a.profile).localeCompare(displayName(b.profile), 'vi')
  const sorted = [...rows]
  switch (key) {
    case 'name':
      return sorted.sort(byName)
    case 'done':
      return sorted.sort((a, b) => b.stats.done - a.stats.done || byName(a, b))
    case 'overdue':
      return sorted.sort((a, b) => b.stats.overdue - a.stats.overdue || byName(a, b))
    case 'rating':
      // Chưa có đánh giá xếp cuối; cùng điểm thì nhiều đánh giá hơn xếp trước.
      return sorted.sort((a, b) => {
        const ra = a.stats.ratingAverage
        const rb = b.stats.ratingAverage
        if (ra === null && rb === null) return byName(a, b)
        if (ra === null) return 1
        if (rb === null) return -1
        return rb - ra || b.stats.ratingCount - a.stats.ratingCount || byName(a, b)
      })
  }
}

/** Điểm trung bình dạng "4,5" (1 chữ số thập phân); "—" khi chưa có. */
export function formatRating(average: number | null): string {
  if (average === null) return '—'
  return average.toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}
