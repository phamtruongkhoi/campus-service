import { formatDateTime } from '../../lib/ticketFormat'
import { STATUS_LABELS } from '../../types/database'
import type { TicketEvent } from '../../types/events'

interface StatusTimelineProps {
  events: readonly TicketEvent[]
  loading: boolean
  error: string | null
}

export function StatusTimeline({ events, loading, error }: StatusTimelineProps) {
  if (error) return <p className="field-error">{error}</p>
  if (loading && events.length === 0) return <p className="chart-empty">Đang tải lịch sử...</p>
  if (events.length === 0) return <p className="chart-empty">Chưa có lịch sử trạng thái.</p>

  return (
    <ol className="timeline">
      {events.map((event) => (
        <li key={event.id} className="timeline-item">
          <span className="timeline-dot" aria-hidden="true" />
          <div>
            <div className="timeline-status">{STATUS_LABELS[event.status]}</div>
            <div className="timeline-time">{formatDateTime(event.created_at)}</div>
            {event.note ? <div className="timeline-note">Lý do: {event.note}</div> : null}
          </div>
        </li>
      ))}
    </ol>
  )
}
