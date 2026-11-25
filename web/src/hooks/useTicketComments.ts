import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchComments } from '../data/comments'
import { supabase } from '../lib/supabase'
import { sortComments } from '../lib/commentRules'
import type { TicketComment } from '../types/comments'

/**
 * Bình luận của một phiếu, realtime theo mô hình "tín hiệu rồi tải lại" (ADR-04):
 * nghe postgres_changes của ticket_comments theo ticket_id, debounce rồi tải lại.
 * Quyền xem vẫn do RLS quyết định. `live` = kênh đã kết nối.
 */
export function useTicketComments(ticketId: string, debounceMs = 300) {
  const [comments, setComments] = useState<TicketComment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [live, setLive] = useState(false)
  const latest = useRef(0)

  const reload = useCallback(async () => {
    const run = ++latest.current
    const result = await fetchComments(ticketId)
    if (run !== latest.current) return // đã có lần tải mới hơn
    setComments(sortComments(result.data))
    setError(result.error)
    setLoading(false)
  }, [ticketId])

  useEffect(() => {
    setLoading(true)
    setComments([])
    void reload()
    let timer: number | undefined
    const fire = () => {
      window.clearTimeout(timer)
      timer = window.setTimeout(() => void reload(), debounceMs)
    }
    // Tên kênh duy nhất mỗi lần gắn (StrictMode gắn-gỡ-gắn ở chế độ dev).
    const channel = supabase
      .channel(`comments-${ticketId}-${Math.random().toString(36).slice(2)}`)
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'ticket_comments', filter: `ticket_id=eq.${ticketId}` },
        fire,
      )
      .subscribe((status) => setLive(status === 'SUBSCRIBED'))
    return () => {
      window.clearTimeout(timer)
      latest.current++
      void supabase.removeChannel(channel)
    }
  }, [ticketId, reload, debounceMs])

  return { comments, loading, error, live, reload }
}
