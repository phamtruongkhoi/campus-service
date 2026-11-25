interface StatTileProps {
  label: string
  value: string
  note?: string
  /** "danger" thêm vạch nhấn đỏ bên trái; chữ vẫn dùng màu chữ chuẩn. */
  tone?: 'default' | 'danger'
}

export function StatTile({ label, value, note, tone = 'default' }: StatTileProps) {
  return (
    <div className={tone === 'danger' ? 'stat-tile card stat-tile-danger' : 'stat-tile card'}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {note ? <div className="stat-note">{note}</div> : null}
    </div>
  )
}
