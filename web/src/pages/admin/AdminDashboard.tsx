import { useMemo } from 'react'
import '../../styles/dashboard.css'
import { BarList } from '../../components/charts/BarList'
import { StatusBar } from '../../components/charts/StatusBar'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { StatTile } from '../../components/StatTile'
import { useAllTickets } from '../../data/useAllTickets'
import { useNow } from '../../hooks/useNow'
import {
  areaLabel,
  assigneeLabel,
  averageProcessing,
  countByCategory,
  countByStatus,
  formatDuration,
  isOverdue,
  parseTime,
} from '../../lib/ticketLogic'
import { STATUS_LABELS } from '../../types/database'

const OVERDUE_LIST_SIZE = 5

/** Admin - Tổng quan (5.2): số liệu, phân bố theo trạng thái và loại lỗi, phiếu quá hạn, thời gian xử lý TB. */
export function AdminDashboard() {
  const { tickets, loading, error, live, reload } = useAllTickets()
  const now = useNow()

  const stats = useMemo(() => {
    const byStatus = countByStatus(tickets)
    const unfinished = byStatus.open + byStatus.assigned + byStatus.in_progress
    const done = byStatus.resolved + byStatus.closed
    const overdue = tickets
      .filter((t) => isOverdue(t, now))
      // Quá hạn lâu nhất lên đầu (ETA cũ nhất).
      .sort((a, b) => (parseTime(a.eta) ?? 0) - (parseTime(b.eta) ?? 0))
    return {
      byStatus,
      total: tickets.length,
      unfinished,
      done,
      overdue,
      byCategory: countByCategory(tickets),
      processing: averageProcessing(tickets),
    }
  }, [tickets, now])

  return (
    <div className="page page-wide">
      <div className="page-head">
        <h1 className="page-title">Tổng quan</h1>
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

      {error ? (
        <Notice kind="error">
          {error}{' '}
          <button type="button" className="link-button" onClick={() => void reload()}>
            Thử lại
          </button>
        </Notice>
      ) : null}

      {loading ? (
        <DashboardSkeleton />
      ) : (
        <>
          <div className="tile-grid">
            <StatTile label="Tổng số phiếu" value={String(stats.total)} />
            <StatTile label="Chưa xong" value={String(stats.unfinished)} note="Chờ tiếp nhận, đã phân công, đang xử lý" />
            <StatTile
              label="Quá hạn"
              value={String(stats.overdue.length)}
              note="Chưa xong và đã qua ETA"
              tone={stats.overdue.length > 0 ? 'danger' : 'default'}
            />
            <StatTile label="Đã xong" value={String(stats.done)} note="Đã khắc phục hoặc đã đóng" />
            <StatTile
              label="Thời gian xử lý TB"
              value={
                stats.processing.averageMs === null ? '—' : formatDuration(stats.processing.averageMs)
              }
              note={
                stats.processing.sampleSize > 0
                  ? `Trên ${stats.processing.sampleSize} phiếu đã khắc phục`
                  : 'Chưa có phiếu nào được khắc phục'
              }
            />
          </div>

          <div className="chart-grid">
            <section className="panel card" aria-labelledby="h-status">
              <h2 id="h-status">Phân bố theo trạng thái</h2>
              <StatusBar counts={stats.byStatus} total={stats.total} />
            </section>

            <section className="panel card" aria-labelledby="h-category">
              <h2 id="h-category">Theo loại lỗi</h2>
              <BarList items={stats.byCategory} nameLabel="Loại lỗi" valueLabel="Số phiếu" />
            </section>
          </div>

          <section className="panel card" aria-labelledby="h-overdue">
            <h2 id="h-overdue">Phiếu quá hạn</h2>
            {stats.overdue.length === 0 ? (
              <p className="chart-empty">Không có phiếu nào quá hạn.</p>
            ) : (
              <>
                <ul className="overdue-list">
                  {stats.overdue.slice(0, OVERDUE_LIST_SIZE).map((t) => {
                    const dueMs = parseTime(t.eta) ?? now
                    return (
                      <li key={t.id} className="overdue-item">
                        <div className="overdue-main">
                          <span className="overdue-title">{t.title}</span>
                          <span className="overdue-meta">
                            {areaLabel(t)} · {assigneeLabel(t)} · {STATUS_LABELS[t.status]}
                          </span>
                        </div>
                        <span className="overdue-badge">Quá hạn {formatDuration(now - dueMs)}</span>
                      </li>
                    )
                  })}
                </ul>
                {stats.overdue.length > OVERDUE_LIST_SIZE ? (
                  <p className="chart-note">
                    và {stats.overdue.length - OVERDUE_LIST_SIZE} phiếu quá hạn khác.
                  </p>
                ) : null}
              </>
            )}
          </section>
        </>
      )}
    </div>
  )
}

function DashboardSkeleton() {
  return (
    <div aria-busy="true" aria-label="Đang tải số liệu">
      <div className="tile-grid">
        {[0, 1, 2, 3, 4].map((i) => (
          <div key={i} className="skeleton skeleton-tile" />
        ))}
      </div>
      <div className="chart-grid">
        <div className="skeleton skeleton-panel" />
        <div className="skeleton skeleton-panel" />
      </div>
    </div>
  )
}
