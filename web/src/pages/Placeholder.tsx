interface PlaceholderProps {
  title: string
  note: string
}

/** Trang tạm cho các màn sẽ làm ở các mục sau; xóa dần khi từng màn hoàn thiện. */
export function Placeholder({ title, note }: PlaceholderProps) {
  return (
    <div className="page">
      <h1 className="page-title">{title}</h1>
      <section className="panel card">
        <p>{note}</p>
      </section>
    </div>
  )
}
