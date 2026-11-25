import { useEffect, useState } from 'react'
import { supabase } from '../lib/supabase'
import type { Profile } from '../types/database'

export function technicianName(profile: Pick<Profile, 'full_name' | 'email'>): string {
  return profile.full_name?.trim() || profile.email || '(không rõ tên)'
}

/** Danh sách kỹ thuật viên (Admin đọc được mọi hồ sơ theo policy profiles_select). */
export function useTechnicians() {
  const [technicians, setTechnicians] = useState<Profile[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    void supabase
      .from('profiles')
      .select('*')
      .eq('role', 'technician')
      .then(({ data, error: queryError }) => {
        if (cancelled) return
        if (queryError) {
          setError(`Không tải được danh sách kỹ thuật viên: ${queryError.message}`)
        } else {
          const list = (data ?? []) as Profile[]
          list.sort((a, b) => technicianName(a).localeCompare(technicianName(b), 'vi'))
          setTechnicians(list)
        }
        setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return { technicians, error, loading }
}
