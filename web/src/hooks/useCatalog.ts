import { useCallback, useEffect, useState } from 'react'
import { fetchCatalog } from '../data/catalog'
import type { Catalog } from '../data/catalog'

export function useCatalog() {
  const [catalog, setCatalog] = useState<Catalog | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const reload = useCallback(async () => {
    try {
      setCatalog(await fetchCatalog())
      setError(null)
    } catch (e) {
      setError(`Không tải được danh mục: ${e instanceof Error ? e.message : String(e)}`)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  return { catalog, error, loading, reload }
}
