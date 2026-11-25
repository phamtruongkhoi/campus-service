import { useCallback, useEffect, useRef, useState } from 'react'
import type { Ticket } from '../types/database'
import { fetchMyTickets } from './student'
import { useTicketSignal } from './useTicketSignal'

/** Phiếu của chính sinh viên, tự tải lại khi tickets/ticket_events đổi (RLS chỉ gửi tín hiệu về phiếu xem được). */
export function useMyTickets(userId: string | undefined) {
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const seq = useRef(0)

  const reload = useCallback(async () => {
    if (!userId) return
    const run = ++seq.current
    const result = await fetchMyTickets(userId)
    if (run !== seq.current) return
    if (result.error) {
      setError(result.error)
    } else {
      setTickets(result.data)
      setError(null)
    }
    setLoading(false)
  }, [userId])

  useEffect(() => {
    void reload()
  }, [reload])

  const live = useTicketSignal(() => {
    void reload()
  })

  return { tickets, loading, error, live, reload }
}
