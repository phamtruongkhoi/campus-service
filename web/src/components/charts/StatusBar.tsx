import { percent } from '../../lib/ticketLogic'
import { STATUS_LABELS, TICKET_STATUSES } from '../../types/database'
import type { TicketStatus } from '../../types/database'

interface StatusBarProps {
  counts: Record<TicketStatus, number>
  total: number
}

function statusClass(status: TicketStatus): string {
  return `seg-${status.replace('_', '-')}`
}

/**
 * Thanh phân đoạn: mỗi trạng thái một khối theo tỷ lệ, khoảng hở 2px giữa các khối.
 * Tên, số lượng và phần trăm nằm ở chú giải bên dưới nên không chỉ dựa vào màu.
 */
export function StatusBar({ counts, total }: StatusBarProps) {
  if (total === 0) {
    return <p className="chart-empty">Chưa có phiếu nào.</p>
  }

  const summary = TICKET_STATUSES.map((s) => `${STATUS_LABELS[s]}: ${counts[s]}`).join('; ')

  return (
    <div className="chart">
      <div className="statusbar" role="img" aria-label={`Phân bố theo trạng thái. ${summary}`}>
        {TICKET_STATUSES.filter((s) => counts[s] > 0).map((s) => (
          <div
            key={s}
            className={`statusbar-seg ${statusClass(s)}`}
            style={{ flexGrow: counts[s] }}
            title={`${STATUS_LABELS[s]}: ${counts[s]} phiếu (${percent(counts[s], total)}%)`}
          />
        ))}
      </div>

      <ul className="legend">
        {TICKET_STATUSES.map((s) => (
          <li key={s} className="legend-row">
            <span className={`legend-swatch ${statusClass(s)}`} aria-hidden="true" />
            <span className="legend-name">{STATUS_LABELS[s]}</span>
            <span className="legend-count">{counts[s]}</span>
            <span className="legend-pct">{percent(counts[s], total)}%</span>
          </li>
        ))}
      </ul>

      <details className="chart-table">
        <summary>Xem dạng bảng</summary>
        <table>
          <thead>
            <tr>
              <th scope="col">Trạng thái</th>
              <th scope="col">Số phiếu</th>
              <th scope="col">Tỷ lệ</th>
            </tr>
          </thead>
          <tbody>
            {TICKET_STATUSES.map((s) => (
              <tr key={s}>
                <th scope="row">{STATUS_LABELS[s]}</th>
                <td>{counts[s]}</td>
                <td>{percent(counts[s], total)}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  )
}
