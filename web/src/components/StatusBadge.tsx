import { STATUS_LABELS } from '../types/database'
import type { TicketStatus } from '../types/database'

export function StatusBadge({ status }: { status: TicketStatus }) {
  return (
    <span className={`badge badge-${status.replace('_', '-')}`}>{STATUS_LABELS[status]}</span>
  )
}
