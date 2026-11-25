import { useState } from 'react'
import { Button } from '../Button'
import { Notice } from '../Notice'
import { advanceTicket } from '../../data/technician'
import { nextStep } from '../../lib/jobs'
import { STATUS_LABELS } from '../../types/database'
import type { Ticket } from '../../types/database'

interface JobActionsProps {
  ticket: Ticket
  /** Gọi sau khi đổi trạng thái thành công. */
  onChanged: (message: string) => void
}

/** Nút chuyển bước cho kỹ thuật viên: Bắt đầu xử lý, Báo đã khắc phục (có xác nhận). */
export function JobActions({ ticket, onChanged }: JobActionsProps) {
  const step = nextStep(ticket.status)
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!step) {
    return (
      <p className="chart-note">
        {ticket.status === 'resolved'
          ? 'Đã báo khắc phục. Đang chờ sinh viên nghiệm thu (hoặc mở lại nếu chưa đạt).'
          : 'Phiếu không còn ở bước cần bạn xử lý.'}
      </p>
    )
  }

  async function run() {
    if (!step) return
    setBusy(true)
    setError(null)
    const failure = await advanceTicket(ticket.id, step.from, step.next)
    setBusy(false)
    setConfirming(false)
    if (failure) {
      setError(failure)
      return
    }
    onChanged(`Đã chuyển sang: ${STATUS_LABELS[step.next]}`)
  }

  return (
    <section className="job-actions" aria-label="Thao tác công việc">
      {error ? <Notice kind="error">{error}</Notice> : null}
      {confirming ? (
        <div className="job-confirm">
          <span>Báo đã khắc phục phiếu này? Sinh viên sẽ được mời nghiệm thu.</span>
          <Button onClick={() => void run()} loading={busy}>
            Xác nhận
          </Button>
          <Button variant="ghost" onClick={() => setConfirming(false)} disabled={busy}>
            Hủy
          </Button>
        </div>
      ) : (
        <Button
          loading={busy}
          onClick={() => {
            if (step.confirm) setConfirming(true)
            else void run()
          }}
        >
          {step.label}
        </Button>
      )}
    </section>
  )
}
