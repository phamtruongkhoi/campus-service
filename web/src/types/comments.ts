/** Một bình luận trong phiếu (bảng ticket_comments). Tên và vai trò người gửi do trigger trong DB điền. */
export interface TicketComment {
  id: string
  ticket_id: string
  author_id: string
  author_name: string | null
  author_role: 'student' | 'admin' | 'technician' | null
  body: string
  created_at: string
}
