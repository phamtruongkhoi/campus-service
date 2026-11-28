import { useMemo } from 'react'
import '../../styles/dashboard.css'
import '../../styles/tickets.css'
import '../../styles/radar.css'
import { useAuth } from '../../auth/useAuth'
import { BarList } from '../../components/charts/BarList'
import { Button } from '../../components/Button'
import { AvailabilityToggle } from '../../components/technician/AvailabilityToggle'
import { Notice } from '../../components/Notice'
import { PriorityTag } from '../../components/PriorityTag'
import { StatTile } from '../../components/StatTile'
import { StatusBadge } from '../../components/StatusBadge'
import { useAllTickets } from '../../data/useAllTickets'
import { useNow } from '../../hooks/useNow'
import { groupByBuilding, radarCounts, workload } from '../../lib/radar'
import { categoryLabel } from '../../lib/ticketFormat'
import { areaLabel, assigneeLabel, isOverdue } from '../../lib/ticketLogic'

/** Kỹ thuật viên - Radar: toàn cảnh sự cố đang mở, khối lượng việc từng người, nhóm theo tòa nhà (chỉ xem). */
export function TechRadar() {
  const { user } = useAuth()
  const { tickets, loading, error, live, reload } = useAllTickets()
  const now = useNow()
  const userId = user?.id ?? ''

  const counts = useMemo(() => radarCounts(tickets), [tickets])
  const load = useMemo(() => workload(tickets, userId), [tickets, userId])
  const groups = useMemo(() => groupByBuilding(tickets, userId), [tickets, userId])

  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Radar sự cố</h1>
        <div className="page-head-actions">
          <AvailabilityToggle />
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

      {loading ? (
        <div className="skeleton skeleton-panel" aria-busy="true" />
      ) : (
        <>
          <div className="tile-grid">
            <StatTile label="Đang mở" value={String(counts.active)} note="Chưa khắc phục" />
            <StatTile
              label="Chưa phân công"
              value={String(counts.unassigned)}
              tone={counts.unassigned > 0 ? 'danger' : 'default'}
            />
            <StatTile label="Đang xử lý" value={String(counts.inProgress)} />
          </div>

          <section className="panel card" aria-labelledby="h-load">
            <h2 id="h-load">Khối lượng việc</h2>
            <BarList items={load} nameLabel="Kỹ thuật viên" valueLabel="Số phiếu đang mở" />
          </section>

          <section aria-labelledby="h-groups">
            <h2 id="h-groups" className="section-title">
              Theo tòa nhà
            </h2>
            {groups.length === 0 ? (
              <p className="chart-empty card">Hiện không có sự cố nào đang mở.</p>
            ) : (
              <ul className="radar-groups">
                {groups.map((g) => (
                  <li key={g.building} className={g.mine ? 'radar-group card radar-mine' : 'radar-group card'}>
                    <header className="radar-head">
                      <h3>{g.building}</h3>
                      <span className="muted">{g.tickets.length} sự cố</span>
                      {g.mine ? <span className="role-tag">Có việc của bạn</span> : null}
                    </header>
                    <p className="muted">
                      {g.colleagues.length > 0 ? `Đang xử lý: ${g.colleagues.join(', ')}` : 'Chưa có ai được phân công'}
                    </p>
                    <ul className="radar-tickets">
                      {g.tickets.map((t) => (
                        <li key={t.id} className="radar-ticket">
                          <div className="radar-ticket-top">
                            <span className="ticket-title">{t.title}</span>
                            {isOverdue(t, now) ? <span className="chip-overdue">Quá hạn</span> : null}
                          </div>
                          <span className="ticket-row-meta">
                            {areaLabel(t)} · {categoryLabel(t)} · {assigneeLabel(t)}
                          </span>
                          <span className="ticket-row-bottom">
                            <StatusBadge status={t.status} />
                            <PriorityTag priority={t.priority} />
                          </span>
                        </li>
                      ))}
                    </ul>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  )
}
