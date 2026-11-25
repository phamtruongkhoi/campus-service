import type { CountItem } from '../../lib/ticketLogic'

interface BarListProps {
  items: CountItem[]
  /** Tên cột đếm trong bảng, ví dụ "Số phiếu". */
  valueLabel: string
  /** Tên cột nhãn trong bảng, ví dụ "Loại lỗi". */
  nameLabel: string
}

/** Cột ngang xếp hạng, một màu (so sánh độ lớn), mỗi hàng: tên, thanh, số lượng. */
export function BarList({ items, valueLabel, nameLabel }: BarListProps) {
  if (items.length === 0) {
    return <p className="chart-empty">Chưa có dữ liệu.</p>
  }
  const max = Math.max(...items.map((item) => item.count), 1)

  return (
    <div className="chart">
      <ul className="barlist">
        {items.map((item) => (
          <li key={item.label} className="barlist-row" title={`${item.label}: ${item.count} phiếu`}>
            <span className="barlist-name">{item.label}</span>
            <span className="barlist-track" aria-hidden="true">
              <span className="barlist-fill" style={{ width: `${(item.count / max) * 100}%` }} />
            </span>
            <span className="barlist-count">{item.count}</span>
          </li>
        ))}
      </ul>

      <details className="chart-table">
        <summary>Xem dạng bảng</summary>
        <table>
          <thead>
            <tr>
              <th scope="col">{nameLabel}</th>
              <th scope="col">{valueLabel}</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.label}>
                <th scope="row">{item.label}</th>
                <td>{item.count}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  )
}
