import { useEffect, useRef, useState } from 'react'
import '../../styles/comments.css'
import { useAuth } from '../../auth/useAuth'
import { addComment } from '../../data/comments'
import { useTicketComments } from '../../hooks/useTicketComments'
import { COMMENT_MAX, authorLabel, canComment, validateCommentBody } from '../../lib/commentRules'
import { formatDateTime } from '../../lib/ticketFormat'
import { ROLE_LABELS } from '../../types/database'
import type { TicketStatus } from '../../types/database'
import { Button } from '../Button'
import { Notice } from '../Notice'

interface CommentsSectionProps {
  ticketId: string
  status: TicketStatus
}

/** Bình luận trong phiếu (5.5): danh sách tự cập nhật realtime + ô gửi. */
export function CommentsSection({ ticketId, status }: CommentsSectionProps) {
  const { user } = useAuth()
  const { comments, loading, error, live, reload } = useTicketComments(ticketId)
  const [body, setBody] = useState('')
  const [formError, setFormError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const endRef = useRef<HTMLLIElement | null>(null)
  const count = useRef(0)

  // Có bình luận mới (sau lần tải đầu) thì cuộn tới cuối danh sách.
  useEffect(() => {
    if (count.current > 0 && comments.length > count.current) {
      endRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
    count.current = comments.length
  }, [comments.length])

  async function send() {
    const invalid = validateCommentBody(body)
    if (invalid) {
      setFormError(invalid)
      return
    }
    setSending(true)
    setFormError(null)
    const failure = await addComment(ticketId, body)
    setSending(false)
    if (failure) {
      setFormError(failure)
      return
    }
    setBody('')
    await reload()
  }

  const writable = canComment(status)

  return (
    <section aria-labelledby={`cmt-${ticketId}`}>
      <div className="comments-head">
        <h3 id={`cmt-${ticketId}`}>Bình luận ({comments.length})</h3>
        <span className={live ? 'live live-on' : 'live'} role="status">
          <span className="live-dot" aria-hidden="true" />
          {live ? 'Trực tiếp' : 'Đang kết nối'}
        </span>
      </div>

      {error ? <Notice kind="error">{error}</Notice> : null}

      {loading ? (
        <div className="skeleton skeleton-panel" aria-busy="true" />
      ) : comments.length === 0 ? (
        <p className="chart-empty">Chưa có bình luận nào.</p>
      ) : (
        <ul className="comment-list">
          {comments.map((c) => (
            <li key={c.id} className={c.author_id === user?.id ? 'comment comment-mine' : 'comment'}>
              <div className="comment-meta">
                <span className="comment-author">{authorLabel(c)}</span>
                {c.author_role ? <span className="role-tag">{ROLE_LABELS[c.author_role]}</span> : null}
                <span className="comment-time">{formatDateTime(c.created_at)}</span>
              </div>
              <p className="comment-body">{c.body}</p>
            </li>
          ))}
          <li ref={endRef} aria-hidden="true" className="comment-end" />
        </ul>
      )}

      {writable ? (
        <form
          className="comment-form"
          onSubmit={(e) => {
            e.preventDefault()
            void send()
          }}
        >
          <div className="field">
            <label htmlFor={`cmt-body-${ticketId}`}>Viết bình luận</label>
            <div className="field-box">
              <textarea
                id={`cmt-body-${ticketId}`}
                rows={2}
                value={body}
                maxLength={COMMENT_MAX + 100}
                onChange={(e) => setBody(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey) && !sending) {
                    e.preventDefault()
                    void send()
                  }
                }}
                aria-invalid={formError ? true : undefined}
              />
            </div>
            {formError ? (
              <p className="field-error">{formError}</p>
            ) : (
              <p className="field-hint">
                {body.trim().length}/{COMMENT_MAX} · Ctrl+Enter để gửi
              </p>
            )}
          </div>
          <Button type="submit" loading={sending}>
            Gửi
          </Button>
        </form>
      ) : (
        <p className="chart-note">Phiếu đã đóng nên không thể bình luận thêm.</p>
      )}
    </section>
  )
}
