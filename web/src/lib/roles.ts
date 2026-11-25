import type { Role } from '../types/database'

export interface NavItem {
  to: string
  label: string
  end?: boolean
}

/** Trang chủ của từng vai trò: /student, /admin, /technician. */
export function roleHomePath(role: Role): string {
  return `/${role}`
}

/** Menu theo vai trò. Các mục sau (5.4 trở đi) sẽ được thêm dần vào đây. */
export const NAV_BY_ROLE: Record<Role, NavItem[]> = {
  student: [
    { to: '/student', label: 'Trang chủ', end: true },
    { to: '/profile', label: 'Hồ sơ' },
  ],
  admin: [
    { to: '/admin', label: 'Tổng quan', end: true },
    { to: '/admin/dispatch', label: 'Điều phối' },
    { to: '/admin/history', label: 'Lịch sử' },
    { to: '/profile', label: 'Hồ sơ' },
  ],
  technician: [
    { to: '/technician', label: 'Công việc', end: true },
    { to: '/profile', label: 'Hồ sơ' },
  ],
}
