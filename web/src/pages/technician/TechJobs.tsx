import { useMemo, useState } from 'react'
import '../../styles/tickets.css'
import '../../styles/radar.css'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { JobActions } from '../../components/technician/JobActions'
import { TicketDetail } from '../../components/tickets/TicketDetail'
import { TicketList } from '../../components/tickets/TicketList'
import { useAssignedTickets } from '../../data/useAssignedTickets'
import { useNow } from '../../hooks/useNow'
import { activeJobs } from '../../lib/jobs'

/** Kỹ thuật viên - Công việc: phiếu được giao (chưa xong), bắt đầu xử lý, báo đã khắc phục. */
export function TechJobs() {
  const { user } = useAuth()
  const { tickets, loading, error, live, reload } = useAssignedTickets(user?.id)
  const now = useNow()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  const jobs = useMemo(() => activeJobs(tickets), [tickets])
  // Tìm trong toàn bộ phiếu để vẫn xem được phiếu vừa báo khắc phục.
  const selected = tickets.find((t) => t.id === selectedId) ?? null

  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Công việc của tôi</h1>
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
      {info ? <Notice kind="success">{info}</Notice> : null}

      <div className={selected ? 'split has-selection' : 'split'}>
        <section className="split-list" aria-label="Danh sách công việc">
          {loading ? (
            <div className="skeleton skeleton-panel" aria-busy="true" />
          ) : (
            <TicketList
              tickets={jobs}
              selectedId={selectedId}
              now={now}
              onSelect={(t) => {
                setSelectedId(t.id)
                setInfo(null)
              }}
              emptyText="Bạn chưa có công việc nào đang chờ xử lý."
            />
          )}
        </section>

        <section className="split-detail" aria-label="Chi tiết công việc">
          {selected ? (
            <>
              <button type="button" className="back-link only-narrow" onClick={() => setSelectedId(null)}>
                ← Danh sách công việc
              </button>
              <TicketDetail key={selected.id} ticket={selected} now={now}>
                <JobActions
                  ticket={selected}
                  onChanged={(message) => {
                    setInfo(message)
                    void reload()
                  }}
                />
              </TicketDetail>
            </>
          ) : (
            <p className="detail-empty card">Chọn một công việc ở bên trái để xem chi tiết.</p>
          )}
        </section>
      </div>
    </div>
  )
}
