// Kiểu dữ liệu khớp schema.sql (bảng profiles). Các bảng khác thêm dần ở các mục sau.
// Dùng union thay cho enum vì tsconfig bật erasableSyntaxOnly.

export type Role = 'student' | 'admin' | 'technician'

export interface Profile {
  id: string
  email: string | null
  full_name: string | null
  student_id: string | null
  role: Role
  created_at: string
}

export const ROLE_LABELS: Record<Role, string> = {
  student: 'Sinh viên',
  admin: 'Quản trị viên',
  technician: 'Kỹ thuật viên',
}
