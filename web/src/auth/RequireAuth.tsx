import { Navigate, Outlet } from 'react-router-dom'
import { Button } from '../components/Button'
import { FullPageLoader } from '../components/FullPageLoader'
import { Notice } from '../components/Notice'
import { useAuth } from './useAuth'

/** Chặn các route cần đăng nhập. Phân quyền theo vai trò (RequireRole) thêm ở 5.1b. */
export function RequireAuth() {
  const { session, profile, loading, profileError, signOut, refreshProfile } = useAuth()

  if (loading) return <FullPageLoader />
  if (!session) return <Navigate to="/login" replace />

  if (!profile) {
    return (
      <div className="fullpage">
        <div className="fullpage-box card">
          <h2>Không vào được hệ thống</h2>
          <Notice kind="error">{profileError ?? 'Không đọc được hồ sơ người dùng.'}</Notice>
          <div className="fullpage-actions">
            <Button onClick={refreshProfile}>Thử lại</Button>
            <Button variant="ghost" onClick={() => void signOut()}>
              Đăng xuất
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return <Outlet />
}
