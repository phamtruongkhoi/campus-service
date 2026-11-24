import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { ThemeToggle } from '../components/ThemeToggle'
import { NAV_BY_ROLE } from '../lib/roles'
import { ROLE_LABELS } from '../types/database'

/** Khung chung cho mọi trang sau đăng nhập: thanh bên (desktop) / ngăn kéo (màn hình hẹp). */
export function AppShell() {
  const { profile, signOut } = useAuth()
  const [open, setOpen] = useState(false)

  if (!profile) return null
  const items = NAV_BY_ROLE[profile.role]
  const displayName = profile.full_name || profile.email || 'Người dùng'

  return (
    <div className="shell">
      <header className="topbar">
        <button
          type="button"
          className="icon-btn"
          aria-label={open ? 'Đóng menu' : 'Mở menu'}
          aria-expanded={open}
          aria-controls="sidebar"
          onClick={() => setOpen((v) => !v)}
        >
          ☰
        </button>
        <span className="topbar-brand">Campus Service</span>
        <ThemeToggle />
      </header>

      {open ? <div className="backdrop" onClick={() => setOpen(false)} aria-hidden="true" /> : null}

      <aside id="sidebar" className={open ? 'sidebar sidebar-open' : 'sidebar'}>
        <div className="sidebar-brand">Campus Service</div>

        <nav className="sidebar-nav" aria-label="Menu chính">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              onClick={() => setOpen(false)}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-foot">
          <div className="user-chip">
            <span className="user-name">{displayName}</span>
            <span className="user-role">{ROLE_LABELS[profile.role]}</span>
          </div>
          <div className="sidebar-theme">
            <ThemeToggle />
          </div>
          <button type="button" className="btn btn-ghost btn-block" onClick={() => void signOut()}>
            Đăng xuất
          </button>
        </div>
      </aside>

      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  )
}
