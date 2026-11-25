// Logic thuần về phiếu (không phụ thuộc React/Supabase) để dễ unit test ở mục 6.4.
// Quy tắc khớp Ticket.kt phía Android: isOverdue, processingMillis.

import type { Ticket, TicketStatus } from '../types/database'

const MINUTE = 60_000
const HOUR = 3_600_000
const DAY = 86_400_000

/** Đọc chuỗi thời gian ISO, trả về mili giây hoặc null nếu trống/không hợp lệ. */
export function parseTime(iso: string | null | undefined): number | null {
  if (!iso) return null
  const value = Date.parse(iso)
  return Number.isNaN(value) ? null : value
}

/** Chưa xong = Open, Assigned hoặc In progress. */
export function isUnfinished(status: TicketStatus): boolean {
  return status === 'open' || status === 'assigned' || status === 'in_progress'
}

/** Quá hạn: chưa xong, có ETA và ETA đã qua. */
export function isOverdue(ticket: Pick<Ticket, 'status' | 'eta'>, now: number): boolean {
  if (!isUnfinished(ticket.status)) return false
  const due = parseTime(ticket.eta)
  return due !== null && due < now
}

/** Số mili giây đã quá hạn so với ETA; null nếu không quá hạn. */
export function overdueMillis(ticket: Pick<Ticket, 'status' | 'eta'>, now: number): number | null {
  if (!isOverdue(ticket, now)) return null
  const due = parseTime(ticket.eta)
  return due === null ? null : now - due
}

/** Thời gian xử lý = resolved_at - created_at; null nếu chưa khắc phục hoặc dữ liệu lệch. */
export function processingMillis(ticket: Pick<Ticket, 'resolved_at' | 'created_at'>): number | null {
  const end = parseTime(ticket.resolved_at)
  const start = parseTime(ticket.created_at)
  if (end === null || start === null) return null
  const diff = end - start
  return diff >= 0 ? diff : null
}

export interface AverageProcessing {
  /** Trung bình (ms), null nếu chưa có phiếu nào đủ dữ liệu. */
  averageMs: number | null
  /** Số phiếu được dùng để tính. */
  sampleSize: number
}

export function averageProcessing(
  tickets: ReadonlyArray<Pick<Ticket, 'resolved_at' | 'created_at'>>,
): AverageProcessing {
  let total = 0
  let count = 0
  for (const ticket of tickets) {
    const ms = processingMillis(ticket)
    if (ms !== null) {
      total += ms
      count += 1
    }
  }
  return { averageMs: count > 0 ? total / count : null, sampleSize: count }
}

export function countByStatus(
  tickets: ReadonlyArray<Pick<Ticket, 'status'>>,
): Record<TicketStatus, number> {
  const counts = { open: 0, assigned: 0, in_progress: 0, resolved: 0, closed: 0 }
  for (const ticket of tickets) {
    if (ticket.status in counts) counts[ticket.status] += 1
  }
  return counts
}

export interface CountItem {
  label: string
  count: number
}

export const UNCATEGORIZED_LABEL = 'Chưa phân loại'
export const OTHER_LABEL = 'Khác'

/** Đếm theo loại lỗi, giảm dần; phần đuôi gộp vào "Khác" khi vượt quá topN. */
export function countByCategory(
  tickets: ReadonlyArray<Pick<Ticket, 'categories'>>,
  topN = 7,
): CountItem[] {
  const map = new Map<string, number>()
  for (const ticket of tickets) {
    const name = ticket.categories?.name?.trim() || UNCATEGORIZED_LABEL
    map.set(name, (map.get(name) ?? 0) + 1)
  }
  const sorted = [...map.entries()]
    .map(([label, count]) => ({ label, count }))
    .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label, 'vi'))

  if (sorted.length <= topN) return sorted
  const head = sorted.slice(0, topN)
  const tail = sorted.slice(topN).reduce((sum, item) => sum + item.count, 0)
  return [...head, { label: OTHER_LABEL, count: tail }]
}

/** Tỷ lệ phần trăm làm tròn; 0 khi tổng bằng 0. */
export function percent(part: number, total: number): number {
  return total > 0 ? Math.round((part / total) * 100) : 0
}

/** Định dạng khoảng thời gian bằng tiếng Việt: "3 ngày 4 giờ", "2 giờ 15 phút", "45 phút". */
export function formatDuration(ms: number): string {
  if (!Number.isFinite(ms) || ms < 0) return '—'
  if (ms < MINUTE) return 'dưới 1 phút'
  const days = Math.floor(ms / DAY)
  const hours = Math.floor((ms % DAY) / HOUR)
  const minutes = Math.floor((ms % HOUR) / MINUTE)
  if (days > 0) return hours > 0 ? `${days} ngày ${hours} giờ` : `${days} ngày`
  if (hours > 0) return minutes > 0 ? `${hours} giờ ${minutes} phút` : `${hours} giờ`
  return `${minutes} phút`
}

/** "Tòa - Khu vực" nếu có tòa nhà, ngược lại chỉ tên khu vực. */
export function areaLabel(ticket: Pick<Ticket, 'areas'>): string {
  const area = ticket.areas
  if (!area) return '—'
  return area.building && area.building.trim() ? `${area.building} - ${area.name}` : area.name
}

export function assigneeLabel(ticket: Pick<Ticket, 'assigned_to' | 'assignee'>): string {
  const name = ticket.assignee?.full_name?.trim()
  if (name) return name
  return ticket.assigned_to === null ? 'Chưa phân công' : '(không rõ tên)'
}
