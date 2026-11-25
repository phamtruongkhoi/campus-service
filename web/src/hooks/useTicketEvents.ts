import { useEffect, useState } from 'react'
import { fetchTicketEvents } from '../data/events'
import type { TicketEvent } from '../types/events'

/**
 * Lịch sử trạng thái của phiếu đang chọn. `version` đổi (ví dụ updated_at của phiếu) thì tải lại,
 * nhờ vậy timeline tự cập nhật khi có thay đổi realtime.
 */
export function useTicketEvents(ticketId: string | null, version: string | null) {
  const [events, setEvents] = useState<TicketEvent[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!ticketId) {
      setEvents([])
      setError(null)
      return
    }
    let cancelled = false
    setLoading(true)
    void fetchTicketEvents(ticketId).then((result) => {
      if (cancelled) return
      setEvents(result.data)
      setError(result.error)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [ticketId, version])

  return { events, loading, error }
}
