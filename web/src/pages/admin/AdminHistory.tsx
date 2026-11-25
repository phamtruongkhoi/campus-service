import { useMemo, useState } from 'react'
import '../../styles/tickets.css'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { SelectField } from '../../components/SelectField'
import { TextField } from '../../components/TextField'
import { TicketDetail } from '../../components/tickets/TicketDetail'
import { TicketList } from '../../components/tickets/TicketList'
import { useAllTickets } from '../../data/useAllTickets'
import { useNow } from '../../hooks/useNow'
import { csvFilename, downloadCsv, toCsv } from '../../lib/csv'
import {
  EMPTY_FILTERS,
  distinctAreas,
  distinctCategories,
  filterTickets,
  hasActiveFilters,
  sortNewestFirst,
} from '../../lib/ticketFilters'
import type { HistoryFilters } from '../../lib/ticketFilters'
import { TICKET_CSV_COLUMNS } from '../../lib/ticketCsv'
import { PRIORITY_LABELS, STATUS_LABELS, TICKET_STATUSES } from '../../types/database'
import type { TicketPriority, TicketStatus } from '../../types/database'

const ALL = ''

const STATUS_OPTIONS = [
  { value: ALL, label: 'Tất cả trạng thái' },
  ...TICKET_STATUSES.map((s) => ({ value: s, label: STATUS_LABELS[s] })),
]

const PRIORITY_OPTIONS = [
  { value: ALL, label: 'Tất cả mức ưu tiên' },
  ...(['high', 'medium', 'low'] as const).map((p) => ({ value: p, label: PRIORITY_LABELS[p] })),
]

/** Admin - Lịch sử toàn trường (5.3): tìm kiếm, lọc, chi tiết chỉ xem, xuất CSV. */
export function AdminHistory() {
  const { tickets, loading, error, live, reload } = useAllTickets()
  const now = useNow()
  const [filters, setFilters] = useState<HistoryFilters>(EMPTY_FILTERS)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const visible = useMemo(() => sortNewestFirst(filterTickets(tickets, filters)), [tickets, filters])
  const categoryOptions = useMemo(
    () => [
      { value: ALL, label: 'Tất cả loại lỗi' },
      ...distinctCategories(tickets).map((c) => ({ value: c, label: c })),
    ],
    [tickets],
  )
  const areaOptions = useMemo(
    () => [
      { value: ALL, label: 'Tất cả khu vực' },
      ...distinctAreas(tickets).map((a) => ({ value: a, label: a })),
    ],
    [tickets],
  )
  const selected = tickets.find((t) => t.id === selectedId) ?? null

  function update(change: Partial<HistoryFilters>) {
    setFilters((current) => ({ ...current, ...change }))
  }

  function handleExport() {
    downloadCsv(csvFilename('lich-su-phieu', new Date()), toCsv(visible, TICKET_CSV_COLUMNS))
  }

  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Lịch sử toàn trường</h1>
        <div className="page-head-actions">
          <span className={live ? 'live live-on' : 'live'} role="status">
            <span className="live-dot" aria-hidden="true" />
            {live ? 'Trực tiếp' : 'Đang kết nối'}
          </span>
          <Button variant="ghost" onClick={() => void reload()}>
            Làm mới
          </Button>
          <Button onClick={handleExport} disabled={visible.length === 0}>
            Xuất CSV ({visible.length})
          </Button>
        </div>
      </div>

      {error ? <Notice kind="error">{error}</Notice> : null}

      <section className="filters card" aria-label="Bộ lọc">
        <TextField
          label="Tìm kiếm"
          type="search"
          placeholder="Tiêu đề, mô tả, người gửi, khu vực"
          value={filters.query}
          onChange={(query) => update({ query })}
        />
        <SelectField
          label="Trạng thái"
          value={filters.status ?? ALL}
          options={STATUS_OPTIONS}
          onChange={(v) => update({ status: v === ALL ? null : (v as TicketStatus) })}
        />
        <SelectField
          label="Ưu tiên"
          value={filters.priority ?? ALL}
          options={PRIORITY_OPTIONS}
          onChange={(v) => update({ priority: v === ALL ? null : (v as TicketPriority) })}
        />
        <SelectField
          label="Loại lỗi"
          value={filters.category ?? ALL}
          options={categoryOptions}
          onChange={(v) => update({ category: v === ALL ? null : v })}
        />
        <SelectField
          label="Khu vực"
          value={filters.area ?? ALL}
          options={areaOptions}
          onChange={(v) => update({ area: v === ALL ? null : v })}
        />
        <div className="filters-foot">
          <span className="chart-note">
            Hiển thị {visible.length} / {tickets.length} phiếu
          </span>
          {hasActiveFilters(filters) ? (
            <Button variant="ghost" onClick={() => setFilters(EMPTY_FILTERS)}>
              Xóa bộ lọc
            </Button>
          ) : null}
        </div>
      </section>

      <div className={selected ? 'split has-selection' : 'split'}>
        <section className="split-list" aria-label="Danh sách phiếu">
          {loading ? (
            <div className="skeleton skeleton-panel" aria-busy="true" />
          ) : (
            <TicketList
              tickets={visible}
              selectedId={selectedId}
              now={now}
              onSelect={(t) => setSelectedId(t.id)}
              emptyText={
                hasActiveFilters(filters) ? 'Không có phiếu nào khớp bộ lọc.' : 'Chưa có phiếu nào.'
              }
            />
          )}
        </section>

        <section className="split-detail" aria-label="Chi tiết phiếu">
          {selected ? (
            <>
              <button type="button" className="back-link only-narrow" onClick={() => setSelectedId(null)}>
                ← Danh sách phiếu
              </button>
              <TicketDetail ticket={selected} now={now} />
            </>
          ) : (
            <p className="detail-empty card">Chọn một phiếu ở bên trái để xem chi tiết.</p>
          )}
        </section>
      </div>
    </div>
  )
}
