import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import '../../styles/tickets.css'
import '../../styles/technicians.css'
import { BarList } from '../../components/charts/BarList'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { StatTile } from '../../components/StatTile'
import { TicketDetail } from '../../components/tickets/TicketDetail'
import { useAllTickets } from '../../data/useAllTickets'
import { useTechnicians } from '../../data/useTechnicians'
import { useNow } from '../../hooks/useNow'
import { creatorLabel, formatDateTime, starsText } from '../../lib/ticketFormat'
import { areaLabel, formatDuration } from '../../lib/ticketLogic'
import {
  computeStats,
  displayName,
  formatRating,
  ratedTickets,
  ratingDistribution,
  ticketsOf,
} from '../../lib/technicianStats'

type StarFilter = 'all' | 1 | 2 | 3 | 4 | 5

const STAR_FILTERS: StarFilter[] = ['all', 5, 4, 3, 2, 1]

/** Admin - Chi tiết một kỹ thuật viên (5.6): thông tin, số liệu, phân bố sao, danh sách đánh giá. */
export function AdminTechnicianDetail() {
  const { id } = useParams<{ id: string }>()
  const { tickets, loading, error, live, reload } = useAllTickets()
  const { technicians, error: techError, loading: techLoading } = useTechnicians()
  const now = useNow()
  const [starFilter, setStarFilter] = useState<StarFilter>('all')
  const [openId, setOpenId] = useState<string | null>(null)

  const profile = technicians.find((t) => t.id === id) ?? null
  const own = useMemo(() => (id ? ticketsOf(id, tickets) : []), [id, tickets])
  const stats = useMemo(() => (id ? computeStats(id, tickets, now) : null), [id, tickets, now])
  const rated = useMemo(() => ratedTickets(own), [own])
  const distribution = useMemo(() => ratingDistribution(rated), [rated])
  const visibleReviews = useMemo(
    () => (starFilter === 'all' ? rated : rated.filter((t) => t.rating === starFilter)),
    [rated, starFilter],
  )

  const header = (
    <div className="page-head">
      <div>
        <Link to="/admin/technicians" className="back-link">
          ← Danh sách kỹ thuật viên
        </Link>
        <h1 className="page-title">{profile ? displayName(profile) : 'Kỹ thuật viên'}</h1>
      </div>
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
  )

  if (loading || techLoading) {
    return (
      <div className="page page-full">
        {header}
        <div className="skeleton skeleton-panel" aria-busy="true" />
      </div>
    )
  }

  if (!profile || !stats) {
    return (
      <div className="page page-full">
        {header}
        {error ? <Notice kind="error">{error}</Notice> : null}
        {techError ? <Notice kind="error">{techError}</Notice> : null}
        <Notice kind="info">Không tìm thấy kỹ thuật viên này.</Notice>
      </div>
    )
  }

  return (
    <div className="page page-full">
      {header}
      {error ? <Notice kind="error">{error}</Notice> : null}

      <section className="panel card" aria-labelledby="h-info">
        <h2 id="h-info">Thông tin</h2>
        <dl className="detail-grid">
          <dt>Họ và tên</dt>
          <dd>{profile.full_name?.trim() || '(chưa có tên)'}</dd>
          <dt>Email</dt>
          <dd>{profile.email ?? '—'}</dd>
          <dt>Vai trò</dt>
          <dd>Kỹ thuật viên</dd>
          <dt>Tạo tài khoản</dt>
          <dd>{formatDateTime(profile.created_at)}</dd>
        </dl>
      </section>

      <div className="tile-grid">
        <StatTile label="Được giao" value={String(stats.assigned)} />
        <StatTile label="Đang xử lý" value={String(stats.active)} note="Đã phân công và đang xử lý" />
        <StatTile label="Đã xử lý" value={String(stats.done)} note="Đã khắc phục hoặc đã đóng" />
        <StatTile
          label="Quá hạn"
          value={String(stats.overdue)}
          tone={stats.overdue > 0 ? 'danger' : 'default'}
        />
        <StatTile
          label="Xử lý TB"
          value={stats.avgProcessingMs === null ? '—' : formatDuration(stats.avgProcessingMs)}
          note={stats.processingSample > 0 ? `Trên ${stats.processingSample} phiếu` : 'Chưa có phiếu hoàn thành'}
        />
        <StatTile
          label="Điểm sao TB"
          value={stats.ratingCount > 0 ? `★ ${formatRating(stats.ratingAverage)}` : '—'}
          note={stats.ratingCount > 0 ? `Từ ${stats.ratingCount} đánh giá` : 'Chưa có đánh giá'}
        />
      </div>

      <div className="chart-grid">
        <section className="panel card" aria-labelledby="h-dist">
          <h2 id="h-dist">Phân bố số sao</h2>
          {stats.ratingCount === 0 ? (
            <p className="chart-empty">Chưa có đánh giá nào.</p>
          ) : (
            <BarList items={distribution} nameLabel="Mức đánh giá" valueLabel="Số đánh giá" />
          )}
        </section>
      </div>

      <section className="panel card" aria-labelledby="h-reviews">
        <h2 id="h-reviews">Chi tiết đánh giá</h2>

        <div className="chips" role="group" aria-label="Lọc theo số sao">
          {STAR_FILTERS.map((f) => (
            <button
              key={f}
              type="button"
              className={starFilter === f ? 'chip active' : 'chip'}
              aria-pressed={starFilter === f}
              onClick={() => setStarFilter(f)}
            >
              {f === 'all' ? `Tất cả (${rated.length})` : `${f} sao`}
            </button>
          ))}
        </div>

        {visibleReviews.length === 0 ? (
          <p className="chart-empty">
            {rated.length === 0 ? 'Chưa có đánh giá nào.' : 'Không có đánh giá nào ở mức sao này.'}
          </p>
        ) : (
          <ul className="review-list">
            {visibleReviews.map((t) => {
              const open = openId === t.id
              return (
                <li key={t.id} className="review">
                  <div className="review-head">
                    <span className="stars" role="img" aria-label={`${t.rating} trên 5 sao`}>
                      {starsText(t.rating)}
                    </span>
                    <span className="review-rating">{t.rating}/5</span>
                    <span className="review-date">{formatDateTime(t.closed_at ?? t.updated_at)}</span>
                  </div>
                  <p className="review-text">
                    {t.feedback?.trim() ? t.feedback : <span className="muted">(Không có nhận xét)</span>}
                  </p>
                  <p className="review-meta">
                    {t.title} · {areaLabel(t)} · {creatorLabel(t)}
                  </p>
                  <button
                    type="button"
                    className="link-button"
                    aria-expanded={open}
                    onClick={() => setOpenId(open ? null : t.id)}
                  >
                    {open ? 'Ẩn chi tiết phiếu' : 'Xem chi tiết phiếu'}
                  </button>
                  {open ? <TicketDetail ticket={t} now={now} /> : null}
                </li>
              )
            })}
          </ul>
        )}
      </section>
    </div>
  )
}
