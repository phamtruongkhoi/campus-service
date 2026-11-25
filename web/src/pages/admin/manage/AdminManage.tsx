import { NavLink, Outlet } from 'react-router-dom'
import '../../../styles/manage.css'

const TABS = [
  { to: '/admin/manage/areas', label: 'Khu vực' },
  { to: '/admin/manage/categories', label: 'Loại lỗi' },
  { to: '/admin/manage/users', label: 'Người dùng' },
]

/** Admin - Quản trị (5.4): khung có ba tab. */
export function AdminManage() {
  return (
    <div className="page page-full">
      <div className="page-head">
        <h1 className="page-title">Quản trị</h1>
      </div>
      <nav className="tabs" aria-label="Mục quản trị">
        {TABS.map((t) => (
          <NavLink key={t.to} to={t.to} className={({ isActive }) => (isActive ? 'tab active' : 'tab')}>
            {t.label}
          </NavLink>
        ))}
      </nav>
      <Outlet />
    </div>
  )
}
