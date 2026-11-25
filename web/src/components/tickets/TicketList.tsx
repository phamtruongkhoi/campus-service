import { PriorityTag } from '../PriorityTag'
import { StatusBadge } from '../StatusBadge'
import { categoryLabel, formatDateTime } from '../../lib/ticketFormat'
import { areaLabel, isOverdue } from '../../lib/ticketLogic'
import type { Ticket } from '../../types/database'

interface TicketListProps {
  tickets: readonly Ticket[]
  selectedId: string | null
  now: number
  onSelect: (ticket: Ticket) => void
  emptyText: string
}

export function TicketList({ tickets, selectedId, now, onSelect, emptyText }: TicketListProps) {
  if (tickets.length === 0) {
    return <p className="chart-empty">{emptyText}</p>
  }

  return (
    <ul className="ticket-list">
      {tickets.map((t) => (
        <li key={t.id}>
          <button
            type="button"
            className={t.id === selectedId ? 'ticket-row selected' : 'ticket-row'}
            aria-pressed={t.id === selectedId}
            onClick={() => onSelect(t)}
          >
            <span className="ticket-row-top">
              <span className="ticket-title">{t.title}</span>
              {isOverdue(t, now) ? <span className="chip-overdue">Quá hạn</span> : null}
            </span>
            <span className="ticket-row-meta">
              {areaLabel(t)} · {categoryLabel(t)}
            </span>
            <span className="ticket-row-bottom">
              <StatusBadge status={t.status} />
              <PriorityTag priority={t.priority} />
              <span className="ticket-date">{formatDateTime(t.created_at)}</span>
            </span>
          </button>
        </li>
      ))}
    </ul>
  )
}
