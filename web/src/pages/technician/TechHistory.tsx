import { useMemo, useState } from 'react'
import '../../styles/dashboard.css'
import '../../styles/tickets.css'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { StatTile } from '../../components/StatTile'
import { TicketDetail } from '../../components/tickets/TicketDetail'
import { TicketList } from '../../components/tickets/TicketList'
import { useAssignedTickets } from '../../data/useAssignedTickets'
import { useNow } from '../../hooks/useNow'
import { doneJobs, historyStats } from '../../lib/jobs'

/** Kỹ thuật viên - Lịch sử: việc đã khắc phục / đã đóng, số liệu và điểm sao trung bình của chính mình. */
export function TechHistory() {
  const { user } = useAuth()
  const { tickets, loading, error, live, reload } = useAssignedTickets(user?.id)
  const now = useNow()
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const done = useMemo(() => doneJobs(tickets), [tickets])
  const stats = useMemo(() => historyStats(done), [done])
  const selected = done.find((t) => t.id === selectedId) ?? null

  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Lịch sử xử lý</h1>
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

      <div className="tile-grid">
        <StatTile label="Đã giải quyết" value={String(stats.done)} note="Đã khắc phục hoặc đã đóng" />
        <StatTile label="Đã đóng" value={String(stats.closed)} />
        <StatTile
          label="Điểm sao TB"
          value={stats.ratingText}
          note={stats.ratingCount > 0 ? `Từ ${stats.ratingCount} đánh giá` : 'Chưa có đánh giá'}
        />
      </div>

      <div className={selected ? 'split has-selection' : 'split'}>
        <section className="split-list" aria-label="Danh sách việc đã giải quyết">
          {loading ? (
            <div className="skeleton skeleton-panel" aria-busy="true" />
          ) : (
            <TicketList
              tickets={done}
              selectedId={selectedId}
              now={now}
              onSelect={(t) => setSelectedId(t.id)}
              emptyText="Chưa có việc nào được giải quyết."
            />
          )}
        </section>

        <section className="split-detail" aria-label="Chi tiết phiếu">
          {selected ? (
            <>
              <button type="button" className="back-link only-narrow" onClick={() => setSelectedId(null)}>
                ← Danh sách
              </button>
              <TicketDetail key={selected.id} ticket={selected} now={now} />
            </>
          ) : (
            <p className="detail-empty card">Chọn một phiếu ở bên trái để xem chi tiết.</p>
          )}
        </section>
      </div>
    </div>
  )
}
