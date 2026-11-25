import { useEffect, useRef, useState } from 'react'
import { supabase } from '../lib/supabase'

/**
 * Realtime theo mô hình "tín hiệu rồi tải lại" (ADR-04): lắng nghe postgres_changes của
 * tickets và ticket_events, chỉ phát tín hiệu (không giải mã từng bản ghi), debounce rồi gọi
 * onSignal để tải lại dữ liệu. Quyền xem vẫn do RLS quyết định.
 * Trả về true khi kênh đã kết nối.
 */
export function useTicketSignal(onSignal: () => void, debounceMs = 400): boolean {
  const [live, setLive] = useState(false)
  const handlerRef = useRef(onSignal)

  useEffect(() => {
    handlerRef.current = onSignal
  })

  useEffect(() => {
    let timer: number | undefined
    const fire = () => {
      window.clearTimeout(timer)
      timer = window.setTimeout(() => handlerRef.current(), debounceMs)
    }

    // Tên kênh duy nhất cho mỗi lần gắn (StrictMode gắn rồi gỡ rồi gắn lại ở chế độ dev).
    const channelName = `tickets-signal-${Math.random().toString(36).slice(2)}`
    const channel = supabase
      .channel(channelName)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'tickets' }, fire)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'ticket_events' }, fire)
      .subscribe((status) => setLive(status === 'SUBSCRIBED'))

    return () => {
      window.clearTimeout(timer)
      void supabase.removeChannel(channel)
    }
  }, [debounceMs])

  return live
}
