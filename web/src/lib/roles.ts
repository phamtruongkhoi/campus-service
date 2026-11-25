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

/** Menu theo vai trò. */
export const NAV_BY_ROLE: Record<Role, NavItem[]> = {
  student: [
    { to: '/student', label: 'Bảng tin', end: true },
    { to: '/student/create', label: 'Tạo phiếu' },
    { to: '/student/tickets', label: 'Phiếu của tôi' },
    { to: '/profile', label: 'Hồ sơ' },
  ],
  admin: [
    { to: '/admin', label: 'Tổng quan', end: true },
    { to: '/admin/dispatch', label: 'Điều phối' },
    { to: '/admin/history', label: 'Lịch sử' },
    { to: '/admin/technicians', label: 'Kỹ thuật viên' },
    { to: '/admin/manage', label: 'Quản trị' },
    { to: '/profile', label: 'Hồ sơ' },
  ],
  technician: [
    { to: '/technician', label: 'Radar', end: true },
    { to: '/technician/jobs', label: 'Công việc' },
    { to: '/technician/history', label: 'Lịch sử' },
    { to: '/profile', label: 'Hồ sơ' },
  ],
}
