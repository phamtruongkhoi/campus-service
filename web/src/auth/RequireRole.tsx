import { Navigate, Outlet } from 'react-router-dom'
import { roleHomePath } from '../lib/roles'
import type { Role } from '../types/database'
import { useAuth } from './useAuth'

/**
 * Chặn route theo vai trò. Đặt BÊN TRONG RequireAuth (đã đảm bảo có phiên và hồ sơ).
 * Sai vai trò thì chuyển về trang chủ của chính vai trò đó.
 * Đây chỉ là chặn ở giao diện; quyền dữ liệu thật vẫn do RLS ở Supabase quyết định.
 */
export function RequireRole({ role }: { role: Role }) {
  const { role: current } = useAuth()

  if (current === null) return <Navigate to="/login" replace />
  if (current !== role) return <Navigate to={roleHomePath(current)} replace />
  return <Outlet />
}
