import type { Profile, Role } from '../types/database'

export const NOTE_MIN = 5
export const NOTE_MAX = 300

export interface RoleAction {
  /** null = không có thao tác. */
  newRole: Exclude<Role, 'admin'> | null
  label: string
  /** Lý do không có thao tác (hiển thị thay nút). */
  reason?: string
}

export function roleActionFor(actorId: string | undefined, target: Pick<Profile, 'id' | 'role'>): RoleAction {
  if (actorId && target.id === actorId) {
    return { newRole: null, label: '', reason: 'Tài khoản của bạn' }
  }
  if (target.role === 'admin') {
    return { newRole: null, label: '', reason: 'Quản trị viên do hệ thống cấp' }
  }
  if (target.role === 'student') return { newRole: 'technician', label: 'Cấp Kỹ thuật viên' }
  return { newRole: 'student', label: 'Thu hồi về Sinh viên' }
}

export function validateRoleNote(value: string): string | null {
  const v = value.trim()
  if (v.length < NOTE_MIN) return `Căn cứ cần ít nhất ${NOTE_MIN} ký tự (số quyết định, người đề nghị...).`
  if (v.length > NOTE_MAX) return `Căn cứ tối đa ${NOTE_MAX} ký tự.`
  return null
}

export type RoleFilter = 'all' | Role

function label(p: Pick<Profile, 'full_name' | 'email'>): string {
  return p.full_name?.trim() || p.email || ''
}

export function filterProfiles(list: readonly Profile[], query: string, role: RoleFilter): Profile[] {
  const q = query.trim().toLocaleLowerCase('vi')
  return list.filter((p) => {
    if (role !== 'all' && p.role !== role) return false
    if (!q) return true
    return [p.full_name, p.email, p.student_id].some((v) => (v ?? '').toLocaleLowerCase('vi').includes(q))
  })
}

const ROLE_ORDER: Record<Role, number> = { admin: 0, technician: 1, student: 2 }

export function sortProfiles(list: readonly Profile[]): Profile[] {
  return [...list].sort(
    (a, b) => ROLE_ORDER[a.role] - ROLE_ORDER[b.role] || label(a).localeCompare(label(b), 'vi'),
  )
}
