// Lọc và sắp xếp phiếu phía trình duyệt (đủ cho quy mô demo; dữ liệu lớn thì chuyển sang lọc phía máy chủ).
// Quy tắc khớp AdminHistoryViewModel.kt và AdminDispatchViewModel.kt phía Android.

import type { Ticket, TicketPriority, TicketStatus } from '../types/database'
import { areaLabel, parseTime } from './ticketLogic'
import { categoryLabel, creatorLabel } from './ticketFormat'

/** null nghĩa là "Tất cả". Loại lỗi và khu vực so khớp theo nhãn hiển thị. */
export interface HistoryFilters {
  query: string
  status: TicketStatus | null
  priority: TicketPriority | null
  category: string | null
  area: string | null
}

export const EMPTY_FILTERS: HistoryFilters = {
  query: '',
  status: null,
  priority: null,
  category: null,
  area: null,
}

export function hasActiveFilters(filters: HistoryFilters): boolean {
  return (
    filters.query.trim() !== '' ||
    filters.status !== null ||
    filters.priority !== null ||
    filters.category !== null ||
    filters.area !== null
  )
}

export function filterTickets(tickets: readonly Ticket[], filters: HistoryFilters): Ticket[] {
  const q = filters.query.trim().toLowerCase()
  return tickets.filter((t) => {
    if (filters.status !== null && t.status !== filters.status) return false
    if (filters.priority !== null && t.priority !== filters.priority) return false
    if (filters.category !== null && categoryLabel(t) !== filters.category) return false
    if (filters.area !== null && areaLabel(t) !== filters.area) return false
    if (q === '') return true
    return (
      t.title.toLowerCase().includes(q) ||
      (t.description ?? '').toLowerCase().includes(q) ||
      creatorLabel(t).toLowerCase().includes(q) ||
      areaLabel(t).toLowerCase().includes(q)
    )
  })
}

function distinctSorted(values: string[]): string[] {
  return [...new Set(values)].sort((a, b) => a.localeCompare(b, 'vi'))
}

export function distinctCategories(tickets: readonly Ticket[]): string[] {
  return distinctSorted(tickets.map(categoryLabel))
}

export function distinctAreas(tickets: readonly Ticket[]): string[] {
  return distinctSorted(tickets.map(areaLabel))
}

const PRIORITY_RANK: Record<TicketPriority, number> = { low: 0, medium: 1, high: 2 }

/** Mới nhất trước. */
export function sortNewestFirst(tickets: readonly Ticket[]): Ticket[] {
  return [...tickets].sort((a, b) => (parseTime(b.created_at) ?? 0) - (parseTime(a.created_at) ?? 0))
}

/** Điều phối: ưu tiên cao xếp trước, cùng mức thì mới nhất trước. */
export function sortForDispatch(tickets: readonly Ticket[]): Ticket[] {
  return [...tickets].sort(
    (a, b) =>
      PRIORITY_RANK[b.priority] - PRIORITY_RANK[a.priority] ||
      (parseTime(b.created_at) ?? 0) - (parseTime(a.created_at) ?? 0),
  )
}
