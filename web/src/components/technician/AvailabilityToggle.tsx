import { useEffect, useState } from 'react'
import '../../styles/autoassign.css'
import { useAuth } from '../../auth/useAuth'
import { fetchMyAvailability, setMyAvailability } from '../../data/autoAssign'
import { assignedMessage } from '../../lib/autoAssignRules'

/** Công tắc Sẵn sàng / Nghỉ của kỹ thuật viên. Khi bật, hệ thống có thể tự giao việc cho người này. */
export function AvailabilityToggle() {
  const { user } = useAuth()
  const [available, setAvailable] = useState<boolean | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<{ kind: 'error' | 'success'; text: string } | null>(null)

  useEffect(() => {
    if (!user) return
    let cancelled = false
    fetchMyAvailability(user.id)
      .then((value) => {
        if (!cancelled) setAvailable(value)
      })
      .catch((e: unknown) => {
        if (!cancelled) setMessage({ kind: 'error', text: e instanceof Error ? e.message : String(e) })
      })
    return () => {
      cancelled = true
    }
  }, [user])

  async function toggle() {
    if (available === null) return
    const next = !available
    setBusy(true)
    setMessage(null)
    const result = await setMyAvailability(next)
    setBusy(false)
    if (result.error) {
      setMessage({ kind: 'error', text: result.error })
      return
    }
    setAvailable(next)
    if (next && result.assigned > 0) setMessage({ kind: 'success', text: assignedMessage(result.assigned) })
  }

  if (available === null && !message) return null

  return (
    <div className="avail">
      {available !== null ? (
        <button
          type="button"
          className={available ? 'avail-btn avail-on' : 'avail-btn'}
          role="switch"
          aria-checked={available}
          onClick={() => void toggle()}
          disabled={busy}
        >
          <span className="avail-dot" aria-hidden="true" />
          {available ? 'Sẵn sàng nhận việc' : 'Đang nghỉ'}
        </button>
      ) : null}
      {message ? (
        <span className={message.kind === 'error' ? 'avail-msg avail-err' : 'avail-msg'} role="status">
          {message.text}
        </span>
      ) : null}
    </div>
  )
}
