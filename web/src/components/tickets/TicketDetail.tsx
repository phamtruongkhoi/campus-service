import type { ReactNode } from 'react'
import { ticketImageUrl } from '../../data/images'
import { useTicketEvents } from '../../hooks/useTicketEvents'
import { categoryLabel, creatorLabel, formatDateTime, starsText } from '../../lib/ticketFormat'
import { areaLabel, assigneeLabel, isOverdue } from '../../lib/ticketLogic'
import type { Ticket } from '../../types/database'
import { PriorityTag } from '../PriorityTag'
import { StatusBadge } from '../StatusBadge'
import { StatusTimeline } from './StatusTimeline'

interface TicketDetailProps {
  ticket: Ticket
  now: number
  /** Khu vực hiển thị thêm bên dưới thông tin (ví dụ form điều phối). */
  children?: ReactNode
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{children}</dd>
    </>
  )
}

/** Chi tiết phiếu (chỉ xem): thông tin, ảnh, đánh giá và lịch sử trạng thái. */
export function TicketDetail({ ticket, now, children }: TicketDetailProps) {
  const { events, loading, error } = useTicketEvents(ticket.id, ticket.updated_at)
  const overdue = isOverdue(ticket, now)

  return (
    <article className="detail card">
      <header className="detail-head">
        <h2>{ticket.title}</h2>
        <div className="detail-badges">
          <StatusBadge status={ticket.status} />
          <PriorityTag priority={ticket.priority} />
          {overdue ? <span className="chip-overdue">Quá hạn</span> : null}
        </div>
      </header>

      <dl className="detail-grid">
        <Row label="Khu vực">{areaLabel(ticket)}</Row>
        <Row label="Loại lỗi">{categoryLabel(ticket)}</Row>
        <Row label="Người gửi">{creatorLabel(ticket)}</Row>
        <Row label="Kỹ thuật viên">{assigneeLabel(ticket)}</Row>
        <Row label="ETA">{formatDateTime(ticket.eta)}</Row>
        <Row label="Công khai">{ticket.is_public ? 'Có' : 'Không'}</Row>
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
          <h3>Đánh giá của sinh viên</h3>
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

      {children}
    </article>
  )
}
