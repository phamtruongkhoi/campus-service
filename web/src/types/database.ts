// Kiểu dữ liệu khớp schema.sql và các migration (Ticket.kt / TicketStatus.kt phía Android).
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

// ---------- Phiếu sự cố ----------

export type TicketStatus = 'open' | 'assigned' | 'in_progress' | 'resolved' | 'closed'
export type TicketPriority = 'low' | 'medium' | 'high'

/** Thứ tự theo vòng đời phiếu. */
export const TICKET_STATUSES: readonly TicketStatus[] = [
  'open',
  'assigned',
  'in_progress',
  'resolved',
  'closed',
]

export const STATUS_LABELS: Record<TicketStatus, string> = {
  open: 'Chờ tiếp nhận',
  assigned: 'Đã phân công',
  in_progress: 'Đang xử lý',
  resolved: 'Đã khắc phục',
  closed: 'Đã đóng',
}

export const PRIORITY_LABELS: Record<TicketPriority, string> = {
  low: 'Thấp',
  medium: 'Trung bình',
  high: 'Cao',
}

export interface AreaRef {
  name: string
  building: string | null
}

export interface CategoryRef {
  name: string
}

export interface ImageRef {
  path: string
}

/** Thông tin người (embed từ profiles): người gửi phiếu hoặc kỹ thuật viên được giao. */
export interface PersonRef {
  full_name: string | null
  student_id: string | null
}

/** Phiếu khi đọc, kèm khu vực, loại lỗi, ảnh và người liên quan (embed của PostgREST). */
export interface Ticket {
  id: string
  created_by: string
  assigned_to: string | null
  title: string
  description: string | null
  status: TicketStatus
  priority: TicketPriority
  eta: string | null
  is_public: boolean
  rating: number | null
  feedback: string | null
  created_at: string
  updated_at: string | null
  resolved_at: string | null
  closed_at: string | null
  areas: AreaRef | null
  categories: CategoryRef | null
  ticket_images: ImageRef[]
  /** Admin và kỹ thuật viên đọc được hồ sơ người khác (policy profiles_select). */
  creator: PersonRef | null
  assignee: PersonRef | null
}
