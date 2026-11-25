import { useCallback, useEffect, useRef, useState } from 'react'
import type { Ticket } from '../types/database'
import { fetchAllTickets } from './tickets'
import { useTicketSignal } from './useTicketSignal'

export interface UseAllTickets {
  tickets: Ticket[]
  loading: boolean
  error: string | null
  /** true khi kênh realtime đã kết nối. */
  live: boolean
  reload: () => Promise<void>
}

/** Toàn bộ phiếu (dành cho Admin), tự tải lại khi có thay đổi realtime. */
export function useAllTickets(): UseAllTickets {
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // Chỉ nhận kết quả của lần tải mới nhất (tránh kết quả cũ đến muộn ghi đè kết quả mới).
  const requestSeq = useRef(0)

  const reload = useCallback(async () => {
    const seq = ++requestSeq.current
    const result = await fetchAllTickets()
    if (seq !== requestSeq.current) return
    if (result.error) {
      setError(result.error)
    } else {
      setTickets(result.data)
      setError(null)
    }
    setLoading(false)
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const live = useTicketSignal(() => {
    void reload()
  })

  return { tickets, loading, error, live, reload }
}
