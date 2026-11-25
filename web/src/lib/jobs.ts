// Logic thuần cho Công việc / Lịch sử của kỹ thuật viên. Khớp TechJobsViewModel.kt và TechHistoryViewModel.kt.

import type { Ticket, TicketPriority, TicketStatus } from '../types/database'
import { parseTime } from './ticketLogic'
import { averageRating, formatRating } from './technicianStats'

const PRIORITY_RANK: Record<TicketPriority, number> = { low: 0, medium: 1, high: 2 }

export interface NextStep {
  from: TicketStatus
  next: TicketStatus
  label: string
  /** Bước khó hoàn tác (sinh viên phải mở lại mới sửa được) nên hỏi xác nhận. */
  confirm: boolean
}

/** Đã phân công -> Đang xử lý; Đang xử lý -> Đã khắc phục. Trạng thái khác: không có bước tiếp. */
export function nextStep(status: TicketStatus): NextStep | null {
  if (status === 'assigned') return { from: 'assigned', next: 'in_progress', label: 'Bắt đầu xử lý', confirm: false }
  if (status === 'in_progress') return { from: 'in_progress', next: 'resolved', label: 'Báo đã khắc phục', confirm: true }
  return null
}

/** Việc chưa hoàn thành: ưu tiên cao trước, cùng mức thì phiếu cũ trước. */
export function activeJobs(tickets: readonly Ticket[]): Ticket[] {
  return tickets
    .filter((t) => t.status === 'assigned' || t.status === 'in_progress')
    .sort(
      (a, b) =>
        PRIORITY_RANK[b.priority] - PRIORITY_RANK[a.priority] ||
        (parseTime(a.created_at) ?? 0) - (parseTime(b.created_at) ?? 0),
    )
}

/** Việc đã giải quyết (Đã khắc phục hoặc Đã đóng), cập nhật gần nhất trước. */
export function doneJobs(tickets: readonly Ticket[]): Ticket[] {
  return tickets
    .filter((t) => t.status === 'resolved' || t.status === 'closed')
    .sort(
      (a, b) =>
        (parseTime(b.updated_at ?? b.created_at) ?? 0) - (parseTime(a.updated_at ?? a.created_at) ?? 0),
    )
}

export interface HistoryStats {
  done: number
  closed: number
  ratingCount: number
  /** Ví dụ "4,5/5" hoặc "—". */
  ratingText: string
}

export function historyStats(done: readonly Ticket[]): HistoryStats {
  const { average, count } = averageRating(done)
  return {
    done: done.length,
    closed: done.filter((t) => t.status === 'closed').length,
    ratingCount: count,
    ratingText: average === null ? '—' : `${formatRating(average)}/5`,
  }
}
