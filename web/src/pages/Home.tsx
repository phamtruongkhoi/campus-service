import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { roleHomePath } from '../lib/roles'

/** Đường dẫn "/" chỉ làm một việc: chuyển người dùng về trang chủ đúng vai trò của họ. */
export function Home() {
  const { role } = useAuth()
  return <Navigate to={role ? roleHomePath(role) : '/login'} replace />
}
