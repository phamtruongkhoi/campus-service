import { useCallback, useEffect, useRef, useState } from 'react'
import type { Ticket } from '../types/database'
import { fetchAssignedTickets } from './technician'
import { useTicketSignal } from './useTicketSignal'

/** Phiếu được giao cho kỹ thuật viên đang đăng nhập, tự tải lại khi có thay đổi realtime. */
export function useAssignedTickets(userId: string | undefined) {
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const seq = useRef(0)

  const reload = useCallback(async () => {
    if (!userId) return
    const run = ++seq.current
    const result = await fetchAssignedTickets(userId)
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
