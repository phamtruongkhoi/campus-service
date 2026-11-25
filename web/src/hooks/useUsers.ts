import { useCallback, useEffect, useState } from 'react'
import { fetchProfiles, fetchRoleChanges } from '../data/users'
import type { RoleChange } from '../types/catalog'
import type { Profile } from '../types/database'

export function useUsers() {
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [changes, setChanges] = useState<RoleChange[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const reload = useCallback(async () => {
    try {
      const [p, c] = await Promise.all([fetchProfiles(), fetchRoleChanges()])
      setProfiles(p)
      setChanges(c)
      setError(null)
    } catch (e) {
      setError(`Không tải được người dùng: ${e instanceof Error ? e.message : String(e)}`)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  return { profiles, changes, error, loading, reload }
}
