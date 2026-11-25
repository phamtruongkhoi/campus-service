import { useMemo, useState } from 'react'
import '../../styles/tickets.css'
import '../../styles/student.css'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { StatusBadge } from '../../components/StatusBadge'
import { TextField } from '../../components/TextField'
import { usePublicIncidents } from '../../data/usePublicIncidents'
import { useNow } from '../../hooks/useNow'
import { filterIncidents, incidentAreaLabel, sortIncidents } from '../../lib/studentRules'
import { formatDateTime } from '../../lib/ticketFormat'
import { isOverdue } from '../../lib/ticketLogic'
import { STATUS_LABELS, TICKET_STATUSES } from '../../types/database'
import type { TicketStatus } from '../../types/database'

/** Sinh viên - Bảng tin: các sự cố công khai toàn trường, cập nhật trực tiếp. */
export function StudentFeed() {
  const { incidents, loading, error, live, reload } = usePublicIncidents()
  const now = useNow()
  const [status, setStatus] = useState<TicketStatus | null>(null)
  const [query, setQuery] = useState('')

  const visible = useMemo(() => sortIncidents(filterIncidents(incidents, status, query)), [incidents, status, query])

  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Bảng tin sự cố</h1>
        <div className="page-head-actions">
          <span className={live ? 'live live-on' : 'live'} role="status">
            <span className="live-dot" aria-hidden="true" />
            {live ? 'Trực tiếp' : 'Đang kết nối'}
          </span>
          <Button variant="ghost" onClick={() => void reload()}>
            Làm mới
          </Button>
        </div>
      </div>

      {error ? <Notice kind="error">{error}</Notice> : null}

      <section className="filters card" aria-label="Bộ lọc">
        <TextField
          label="Tìm kiếm"
          type="search"
          placeholder="Khu vực hoặc loại lỗi"
          value={query}
          onChange={setQuery}
        />
        <div className="chips" role="group" aria-label="Lọc theo trạng thái">
          <button type="button" className={status === null ? 'chip active' : 'chip'} aria-pressed={status === null} onClick={() => setStatus(null)}>
            Tất cả ({incidents.length})
          </button>
          {TICKET_STATUSES.map((s) => (
            <button key={s} type="button" className={status === s ? 'chip active' : 'chip'} aria-pressed={status === s} onClick={() => setStatus(s)}>
              {STATUS_LABELS[s]}
            </button>
          ))}
        </div>
      </section>

      {loading ? (
        <div className="skeleton skeleton-panel" aria-busy="true" />
      ) : visible.length === 0 ? (
        <p className="chart-empty">{incidents.length === 0 ? 'Chưa có sự cố công khai nào.' : 'Không có sự cố nào khớp bộ lọc.'}</p>
      ) : (
        <ul className="feed-grid">
          {visible.map((i) => (
            <li key={i.ticket_id} className="feed-card card">
              <div className="feed-top">
                <span className="feed-title">{i.category_name ?? 'Chưa phân loại'}</span>
                {isOverdue(i, now) ? <span className="chip-overdue">Quá hạn</span> : null}
              </div>
              <span className="feed-area">{incidentAreaLabel(i)}</span>
              <StatusBadge status={i.status} />
              <dl className="feed-meta">
                <dt>Dự kiến xong</dt>
                <dd>{formatDateTime(i.eta)}</dd>
                <dt>Cập nhật</dt>
                <dd>{formatDateTime(i.updated_at)}</dd>
              </dl>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
