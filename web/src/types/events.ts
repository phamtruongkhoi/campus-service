import type { TicketStatus } from './database'

/** Một dòng lịch sử đổi trạng thái (bảng ticket_events). `note` có từ migration 4.4 (lý do mở lại). */
export interface TicketEvent {
  id: string
  ticket_id: string
  status: TicketStatus
  actor_id: string | null
  created_at: string
  note: string | null
}
