import { Button } from '../components/Button'
import { useAuth } from '../auth/useAuth'
import { ROLE_LABELS } from '../types/database'

/** Trang tạm của 5.1a: chứng minh đăng nhập và đọc vai trò đúng. Layout và menu theo vai trò làm ở 5.1b. */
export function Home() {
  const { profile, signOut } = useAuth()
  if (!profile) return null

  return (
    <main className="home-page">
      <section className="home-card card">
        <h1>Xin chào, {profile.full_name ?? profile.email}</h1>
        <dl>
          <dt>Vai trò</dt>
          <dd>{ROLE_LABELS[profile.role]}</dd>
          <dt>Email</dt>
          <dd>{profile.email}</dd>
          {profile.role === 'student' ? (
            <>
              <dt>MSSV</dt>
              <dd>{profile.student_id ?? '(chưa có)'}</dd>
            </>
          ) : null}
        </dl>
        <div>
          <Button variant="ghost" onClick={() => void signOut()}>
            Đăng xuất
          </Button>
        </div>
      </section>
    </main>
  )
}
