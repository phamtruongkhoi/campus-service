import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import '../../styles/tickets.css'
import '../../styles/technicians.css'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { SelectField } from '../../components/SelectField'
import { useAllTickets } from '../../data/useAllTickets'
import { useTechnicians } from '../../data/useTechnicians'
import { useNow } from '../../hooks/useNow'
import { formatDuration } from '../../lib/ticketLogic'
import { buildRows, displayName, formatRating, sortRows } from '../../lib/technicianStats'
import type { TechnicianSortKey } from '../../lib/technicianStats'

const SORT_OPTIONS: { value: TechnicianSortKey; label: string }[] = [
  { value: 'name', label: 'Tên A → Z' },
  { value: 'rating', label: 'Điểm sao cao nhất' },
  { value: 'done', label: 'Đã xử lý nhiều nhất' },
  { value: 'overdue', label: 'Quá hạn nhiều nhất' },
]

/** Admin - Chất lượng kỹ thuật viên (5.6): bảng tóm tắt, bấm vào từng người để xem chi tiết. */
export function AdminTechnicians() {
  const { tickets, loading, error, live, reload } = useAllTickets()
  const { technicians, error: techError } = useTechnicians()
  const now = useNow()
  const [sort, setSort] = useState<TechnicianSortKey>('name')

  const rows = useMemo(
    () => sortRows(buildRows(technicians, tickets, now), sort),
    [technicians, tickets, now, sort],
  )

  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Kỹ thuật viên</h1>
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
      {techError ? <Notice kind="error">{techError}</Notice> : null}

      <div className="tech-toolbar">
        <SelectField
          label="Sắp xếp"
          value={sort}
          options={SORT_OPTIONS}
          onChange={(v) => setSort(v as TechnicianSortKey)}
        />
        <p className="chart-note">
          Đánh giá tính theo kỹ thuật viên đang được giao trên phiếu. Chỉ phiếu đã nghiệm thu mới có sao.
        </p>
      </div>

      {loading ? (
        <div className="skeleton skeleton-panel" aria-busy="true" />
      ) : rows.length === 0 ? (
        <p className="chart-empty">Chưa có kỹ thuật viên nào.</p>
      ) : (
        <ul className="tech-list">
          {rows.map(({ profile, stats }) => (
            <li key={profile.id}>
              <Link to={`/admin/technicians/${profile.id}`} className="tech-row card">
                <span className="tech-who">
                  <span className="tech-name">{displayName(profile)}</span>
                  <span className="tech-email">{profile.email}</span>
                </span>
                <span className="metrics">
                  <span className="metric">
                    <span className="metric-label">Được giao</span>
                    <span className="metric-value">{stats.assigned}</span>
                  </span>
                  <span className="metric">
                    <span className="metric-label">Đang xử lý</span>
                    <span className="metric-value">{stats.active}</span>
                  </span>
                  <span className="metric">
                    <span className="metric-label">Đã xử lý</span>
                    <span className="metric-value">{stats.done}</span>
                  </span>
                  <span className="metric">
                    <span className="metric-label">Quá hạn</span>
                    <span className={stats.overdue > 0 ? 'metric-value metric-danger' : 'metric-value'}>
                      {stats.overdue}
                    </span>
                  </span>
                  <span className="metric">
                    <span className="metric-label">Xử lý TB</span>
                    <span className="metric-value">
                      {stats.avgProcessingMs === null ? '—' : formatDuration(stats.avgProcessingMs)}
                    </span>
                  </span>
                  <span className="metric">
                    <span className="metric-label">Điểm sao TB</span>
                    <span className="metric-value">
                      {stats.ratingCount > 0
                        ? `★ ${formatRating(stats.ratingAverage)} (${stats.ratingCount})`
                        : 'Chưa có'}
                    </span>
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
