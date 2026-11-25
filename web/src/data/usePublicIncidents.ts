import { useCallback, useEffect, useRef, useState } from 'react'
import { supabase } from '../lib/supabase'
import type { PublicIncident } from '../types/student'
import { fetchPublicIncidents } from './student'

/** Bảng tin công khai, tự tải lại khi public_incidents đổi (mô hình "tín hiệu rồi tải lại", debounce 400 ms). */
export function usePublicIncidents() {
  const [incidents, setIncidents] = useState<PublicIncident[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [live, setLive] = useState(false)
  const seq = useRef(0)

  const reload = useCallback(async () => {
    const run = ++seq.current
    const result = await fetchPublicIncidents()
    if (run !== seq.current) return
    if (result.error) {
      setError(result.error)
    } else {
      setIncidents(result.data)
      setError(null)
    }
    setLoading(false)
  }, [])

  useEffect(() => {
    void reload()
    let timer: number | undefined
    const fire = () => {
      window.clearTimeout(timer)
      timer = window.setTimeout(() => void reload(), 400)
    }
    const channel = supabase
      .channel(`incidents-${Math.random().toString(36).slice(2)}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'public_incidents' }, fire)
      .subscribe((status) => setLive(status === 'SUBSCRIBED'))
    return () => {
      window.clearTimeout(timer)
      seq.current++
      void supabase.removeChannel(channel)
    }
  }, [reload])

  return { incidents, loading, error, live, reload }
}
