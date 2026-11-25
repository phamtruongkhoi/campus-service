import type { ReactNode } from 'react'
import { ticketImageUrl } from '../../data/images'
import { useTicketEvents } from '../../hooks/useTicketEvents'
import { canReviewResolved } from '../../lib/studentRules'
import { categoryLabel, formatDateTime, starsText } from '../../lib/ticketFormat'
import { areaLabel, formatDuration, isOverdue, overdueMillis } from '../../lib/ticketLogic'
import type { Ticket } from '../../types/database'
import { PriorityTag } from '../PriorityTag'
import { StatusBadge } from '../StatusBadge'
import { CommentsSection } from '../tickets/CommentsSection'
import { StatusTimeline } from '../tickets/StatusTimeline'
import { AcceptancePanel } from './AcceptancePanel'

interface Props {
  ticket: Ticket
  now: number
  onChanged: (message: string) => void
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{children}</dd>
    </>
  )
}

/** Chi tiết phiếu của sinh viên (6.1): thông tin, ảnh, lịch sử, nghiệm thu / mở lại, bình luận. */
export function StudentTicketDetail({ ticket, now, onChanged }: Props) {
  const { events, loading, error } = useTicketEvents(ticket.id, ticket.updated_at)
  const lateBy = overdueMillis(ticket, now)

  return (
    <article className="detail card">
      <header className="detail-head">
        <h2>{ticket.title}</h2>
        <div className="detail-badges">
          <StatusBadge status={ticket.status} />
          <PriorityTag priority={ticket.priority} />
          {isOverdue(ticket, now) ? (
            <span className="chip-overdue">Quá hạn{lateBy !== null ? ` ${formatDuration(lateBy)}` : ''}</span>
          ) : null}
        </div>
      </header>

      <dl className="detail-grid">
        <Row label="Khu vực">{areaLabel(ticket)}</Row>
        <Row label="Loại lỗi">{categoryLabel(ticket)}</Row>
        <Row label="Dự kiến xong">{formatDateTime(ticket.eta)}</Row>
        <Row label="Ngày tạo">{formatDateTime(ticket.created_at)}</Row>
        <Row label="Khắc phục lúc">{formatDateTime(ticket.resolved_at)}</Row>
      </dl>

      {ticket.description ? (
        <section>
          <h3>Mô tả</h3>
          <p className="detail-text">{ticket.description}</p>
        </section>
      ) : null}

      {ticket.ticket_images.length > 0 ? (
        <section>
          <h3>Ảnh đính kèm</h3>
          <ul className="image-grid">
            {ticket.ticket_images.map((image) => (
              <li key={image.path}>
                <a href={ticketImageUrl(image.path)} target="_blank" rel="noreferrer noopener">
                  <img src={ticketImageUrl(image.path)} alt={`Ảnh của phiếu ${ticket.title}`} loading="lazy" />
                </a>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {ticket.rating !== null ? (
        <section>
          <h3>Đánh giá của bạn</h3>
          <p className="rating-line">
            <span className="stars" role="img" aria-label={`${ticket.rating} trên 5 sao`}>
              {starsText(ticket.rating)}
            </span>
            <span>{ticket.rating}/5</span>
          </p>
          {ticket.feedback ? <p className="detail-text">{ticket.feedback}</p> : null}
        </section>
      ) : null}

      <section>
        <h3>Lịch sử trạng thái</h3>
        <StatusTimeline events={events} loading={loading} error={error} />
      </section>

      {canReviewResolved(ticket.status) ? <AcceptancePanel ticketId={ticket.id} onDone={onChanged} /> : null}

      <CommentsSection ticketId={ticket.id} status={ticket.status} />
    </article>
  )
}
