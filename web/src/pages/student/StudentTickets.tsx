import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import '../../styles/tickets.css'
import '../../styles/student.css'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { StudentTicketDetail } from '../../components/student/StudentTicketDetail'
import { TicketList } from '../../components/tickets/TicketList'
import { useMyTickets } from '../../data/useMyTickets'
import { useNow } from '../../hooks/useNow'
import { filterByStatus } from '../../lib/studentRules'
import { STATUS_LABELS, TICKET_STATUSES } from '../../types/database'
import type { TicketStatus } from '../../types/database'

/** Sinh viên - Phiếu của tôi: danh sách, lọc theo trạng thái, chi tiết, nghiệm thu / mở lại, bình luận. */
export function StudentTickets() {
  const { user } = useAuth()
  const { tickets, loading, error, live, reload } = useMyTickets(user?.id)
  const now = useNow()
  const [status, setStatus] = useState<TicketStatus | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  const visible = useMemo(() => filterByStatus(tickets, status), [tickets, status])
  const selected = tickets.find((t) => t.id === selectedId) ?? null

  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Phiếu của tôi</h1>
        <div className="page-head-actions">
          <span className={live ? 'live live-on' : 'live'} role="status">
            <span className="live-dot" aria-hidden="true" />
            {live ? 'Trực tiếp' : 'Đang kết nối'}
          </span>
          <Button variant="ghost" onClick={() => void reload()}>
            Làm mới
          </Button>
          <Link to="/student/create" className="btn btn-primary">
            Tạo phiếu mới
          </Link>
        </div>
      </div>

      {error ? <Notice kind="error">{error}</Notice> : null}
      {info ? <Notice kind="success">{info}</Notice> : null}

      <div className="chips" role="group" aria-label="Lọc theo trạng thái">
        <button type="button" className={status === null ? 'chip active' : 'chip'} aria-pressed={status === null} onClick={() => setStatus(null)}>
          Tất cả ({tickets.length})
        </button>
        {TICKET_STATUSES.map((s) => (
          <button key={s} type="button" className={status === s ? 'chip active' : 'chip'} aria-pressed={status === s} onClick={() => setStatus(s)}>
            {STATUS_LABELS[s]}
          </button>
        ))}
      </div>

      <div className={selected ? 'split has-selection' : 'split'}>
        <section className="split-list" aria-label="Danh sách phiếu">
          {loading ? (
            <div className="skeleton skeleton-panel" aria-busy="true" />
          ) : (
            <TicketList
              tickets={visible}
              selectedId={selectedId}
              now={now}
              onSelect={(t) => {
                setSelectedId(t.id)
                setInfo(null)
              }}
              emptyText={tickets.length === 0 ? 'Bạn chưa gửi phiếu nào.' : 'Không có phiếu nào ở trạng thái này.'}
            />
          )}
        </section>

        <section className="split-detail" aria-label="Chi tiết phiếu">
          {selected ? (
            <>
              <button type="button" className="back-link only-narrow" onClick={() => setSelectedId(null)}>
                ← Danh sách phiếu
              </button>
              <StudentTicketDetail
                key={selected.id}
                ticket={selected}
                now={now}
                onChanged={(message) => {
                  setInfo(message)
                  void reload()
                }}
              />
            </>
          ) : (
            <p className="detail-empty card">Chọn một phiếu ở bên trái để xem chi tiết.</p>
          )}
        </section>
      </div>
    </div>
  )
}
