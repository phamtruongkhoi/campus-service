import { useCallback, useEffect, useState } from 'react'
import { fetchAutoAssign } from '../data/autoAssign'
import type { AutoAssignData } from '../data/autoAssign'

export function useAutoAssign() {
  const [data, setData] = useState<AutoAssignData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const reload = useCallback(async () => {
    try {
      setData(await fetchAutoAssign())
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  return { data, error, loading, reload }
}
