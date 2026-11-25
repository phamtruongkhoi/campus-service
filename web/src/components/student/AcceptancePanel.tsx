import { useState } from 'react'
import { Button } from '../Button'
import { Notice } from '../Notice'
import { acceptTicket, reopenTicket } from '../../data/student'
import {
  FEEDBACK_MAX,
  REOPEN_MAX,
  REOPEN_MIN,
  validateFeedback,
  validateRating,
  validateReopenReason,
} from '../../lib/studentRules'

type Mode = 'accept' | 'reopen'

interface AcceptancePanelProps {
  ticketId: string
  /** Gọi sau khi thao tác thành công, kèm lời nhắn hiển thị cho người dùng. */
  onDone: (message: string) => void
}

/** Phiếu "Đã khắc phục": sinh viên nghiệm thu (đánh giá sao + nhận xét) hoặc mở lại kèm lý do. */
export function AcceptancePanel({ ticketId, onDone }: AcceptancePanelProps) {
  const [mode, setMode] = useState<Mode>('accept')
  const [rating, setRating] = useState(0)
  const [feedback, setFeedback] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit() {
    const invalid =
      mode === 'accept'
        ? (validateRating(rating) ?? validateFeedback(feedback))
        : validateReopenReason(reason)
    if (invalid) {
      setError(invalid)
      return
    }
    setBusy(true)
    setError(null)
    const failure =
      mode === 'accept' ? await acceptTicket(ticketId, rating, feedback) : await reopenTicket(ticketId, reason)
    setBusy(false)
    if (failure) {
      setError(failure)
      return
    }
    onDone(
      mode === 'accept'
        ? 'Đã nghiệm thu và đóng phiếu. Cảm ơn bạn!'
        : 'Đã mở lại phiếu. Kỹ thuật viên sẽ xử lý lại.',
    )
  }

  return (
    <section className="acceptance" aria-label="Nghiệm thu phiếu">
      <h3>Phiếu đã được khắc phục. Bạn thấy thế nào?</h3>
      <div className="chips" role="group" aria-label="Chọn thao tác">
        <button
          type="button"
          className={mode === 'accept' ? 'chip active' : 'chip'}
          aria-pressed={mode === 'accept'}
          onClick={() => {
            setMode('accept')
            setError(null)
          }}
        >
          Đạt, đóng phiếu
        </button>
        <button
          type="button"
          className={mode === 'reopen' ? 'chip active' : 'chip'}
          aria-pressed={mode === 'reopen'}
          onClick={() => {
            setMode('reopen')
            setError(null)
          }}
        >
          Chưa đạt, mở lại
        </button>
      </div>

      <form
        className="acceptance-form"
        onSubmit={(e) => {
          e.preventDefault()
          void submit()
        }}
      >
        {mode === 'accept' ? (
          <>
            <div className="star-picker" role="radiogroup" aria-label="Số sao đánh giá">
              {[1, 2, 3, 4, 5].map((n) => (
                <button
                  key={n}
                  type="button"
                  role="radio"
                  aria-checked={rating === n}
                  aria-label={`${n} sao`}
                  className={n <= rating ? 'star on' : 'star'}
                  onClick={() => setRating(n)}
                >
                  {n <= rating ? '★' : '☆'}
                </button>
              ))}
              <span className="star-caption">{rating > 0 ? `${rating}/5` : 'Chưa chọn'}</span>
            </div>
            <div className="field">
              <label htmlFor={`fb-${ticketId}`}>Nhận xét (tùy chọn)</label>
              <div className="field-box">
                <textarea
                  id={`fb-${ticketId}`}
                  rows={3}
                  value={feedback}
                  maxLength={FEEDBACK_MAX + 50}
                  onChange={(e) => setFeedback(e.target.value)}
                />
              </div>
              <p className="field-hint">
                {feedback.trim().length}/{FEEDBACK_MAX}
              </p>
            </div>
          </>
        ) : (
          <div className="field">
            <label htmlFor={`rs-${ticketId}`}>Lý do chưa đạt (bắt buộc)</label>
            <div className="field-box">
              <textarea
                id={`rs-${ticketId}`}
                rows={3}
                value={reason}
                maxLength={REOPEN_MAX + 50}
                onChange={(e) => setReason(e.target.value)}
              />
            </div>
            <p className="field-hint">
              {reason.trim().length}/{REOPEN_MAX} (tối thiểu {REOPEN_MIN} ký tự)
            </p>
          </div>
        )}

        {error ? <Notice kind="error">{error}</Notice> : null}
        <Button type="submit" loading={busy}>
          {mode === 'accept' ? 'Nghiệm thu và đóng phiếu' : 'Mở lại phiếu'}
        </Button>
      </form>
    </section>
  )
}
