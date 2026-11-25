import { PRIORITY_LABELS } from '../types/database'
import type { TicketPriority } from '../types/database'

/** Chấm màu + nhãn chữ (không chỉ dựa vào màu). */
export function PriorityTag({ priority }: { priority: TicketPriority }) {
  return (
    <span className="priority-tag">
      <span className={`priority-dot priority-${priority}`} aria-hidden="true" />
      {PRIORITY_LABELS[priority]}
    </span>
  )
}
