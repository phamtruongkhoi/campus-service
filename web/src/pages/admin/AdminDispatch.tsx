import { useMemo, useState } from 'react'
import '../../styles/tickets.css'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { DispatchForm } from '../../components/tickets/DispatchForm'
import { TicketDetail } from '../../components/tickets/TicketDetail'
import { TicketList } from '../../components/tickets/TicketList'
import { useAllTickets } from '../../data/useAllTickets'
import { useTechnicians } from '../../data/useTechnicians'
import { useNow } from '../../hooks/useNow'
import { sortForDispatch } from '../../lib/ticketFilters'
import { countByStatus } from '../../lib/ticketLogic'
import { STATUS_LABELS, TICKET_STATUSES } from '../../types/database'
import type { TicketStatus } from '../../types/database'

type StatusFilter = TicketStatus | 'all'

/** Admin - Điều phối (5.3): đặt ưu tiên, phân công kỹ thuật viên, ETA, công khai. */
export function AdminDispatch() {
  const { tickets, loading, error, live, reload } = useAllTickets()
  const { technicians, error: techError } = useTechnicians()
  const now = useNow()
  const [filter, setFilter] = useState<StatusFilter>('open')
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const counts = useMemo(() => countByStatus(tickets), [tickets])
  const visible = useMemo(
    () => sortForDispatch(filter === 'all' ? tickets : tickets.filter((t) => t.status === filter)),
    [tickets, filter],
  )
  const selected = tickets.find((t) => t.id === selectedId) ?? null

  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Điều phối</h1>
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

      <div className="chips" role="group" aria-label="Lọc theo trạng thái">
        <button
          type="button"
          className={filter === 'all' ? 'chip active' : 'chip'}
          aria-pressed={filter === 'all'}
          onClick={() => setFilter('all')}
        >
          Tất cả ({tickets.length})
        </button>
        {TICKET_STATUSES.map((s) => (
          <button
            key={s}
            type="button"
            className={filter === s ? 'chip active' : 'chip'}
            aria-pressed={filter === s}
            onClick={() => setFilter(s)}
          >
            {STATUS_LABELS[s]} ({counts[s]})
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
              onSelect={(t) => setSelectedId(t.id)}
              emptyText="Không có phiếu nào ở trạng thái này."
            />
          )}
        </section>

        <section className="split-detail" aria-label="Chi tiết và điều phối">
          {selected ? (
            <>
              <button type="button" className="back-link only-narrow" onClick={() => setSelectedId(null)}>
                ← Danh sách phiếu
              </button>
              <TicketDetail ticket={selected} now={now}>
                <DispatchForm
                  key={selected.id}
                  ticket={selected}
                  technicians={technicians}
                  onSaved={reload}
                />
              </TicketDetail>
            </>
          ) : (
            <p className="detail-empty card">Chọn một phiếu ở bên trái để xem chi tiết và điều phối.</p>
          )}
        </section>
      </div>
    </div>
  )
}
