import type { TicketStatus } from '../types/database'
import type { TicketComment } from '../types/comments'

export const COMMENT_MAX = 500

/** Phiếu đã đóng thì không bình luận thêm (khớp quy tắc phía server). */
export function canComment(status: TicketStatus): boolean {
  return status !== 'closed'
}

export function validateCommentBody(value: string): string | null {
  const v = value.trim()
  if (v.length === 0) return 'Hãy nhập nội dung bình luận.'
  if (v.length > COMMENT_MAX) return `Bình luận tối đa ${COMMENT_MAX} ký tự (hiện ${v.length}).`
  return null
}

/** Cũ lên trên, mới xuống dưới; cùng thời điểm thì giữ ổn định theo id. */
export function sortComments(list: readonly TicketComment[]): TicketComment[] {
  return [...list].sort((a, b) => {
    const diff = Date.parse(a.created_at) - Date.parse(b.created_at)
    return diff !== 0 && !Number.isNaN(diff) ? diff : a.id.localeCompare(b.id)
  })
}

export function authorLabel(c: Pick<TicketComment, 'author_name'>): string {
  return c.author_name?.trim() || '(không rõ tên)'
}
