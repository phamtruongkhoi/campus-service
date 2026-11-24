import { Navigate, Outlet } from 'react-router-dom'
import { FullPageLoader } from '../components/FullPageLoader'
import { useAuth } from './useAuth'

/** Các trang đăng nhập/đăng ký/quên mật khẩu: người đã đăng nhập thì chuyển về trang chủ. */
export function GuestOnly() {
  const { session, loading } = useAuth()

  if (loading) return <FullPageLoader />
  if (session) return <Navigate to="/" replace />
  return <Outlet />
}
