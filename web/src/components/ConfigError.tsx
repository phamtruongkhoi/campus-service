/** Hiện khi thiếu web/.env.local, thay cho trang trắng khó hiểu. */
export function ConfigError() {
  return (
    <div className="fullpage">
      <div className="fullpage-box card">
        <h2>Thiếu cấu hình Supabase</h2>
        <p>
          Chưa có <code>VITE_SUPABASE_URL</code> hoặc <code>VITE_SUPABASE_ANON_KEY</code>.
        </p>
        <p>
          Sao chép <code>web/.env.example</code> thành <code>web/.env.local</code>, điền Project URL và
          khóa công khai (anon/publishable), rồi dừng và chạy lại <code>npm run dev</code>.
        </p>
      </div>
    </div>
  )
}
