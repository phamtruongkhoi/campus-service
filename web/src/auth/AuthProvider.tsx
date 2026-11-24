import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import type { Session } from '@supabase/supabase-js'
import { supabase } from '../lib/supabase'
import type { Profile } from '../types/database'
import { AuthContext } from './AuthContext'
import type { AuthContextValue } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const [initializing, setInitializing] = useState(true)
  const [profile, setProfile] = useState<Profile | null>(null)
  const [profileError, setProfileError] = useState<string | null>(null)
  // Id người dùng mà hồ sơ đã tải xong: dùng để biết "có phiên nhưng chưa tải hồ sơ" là loading.
  const [loadedFor, setLoadedFor] = useState<string | null>(null)
  const [reloadTick, setReloadTick] = useState(0)

  const userId = session?.user.id ?? null

  // 1) Khôi phục và theo dõi phiên đăng nhập (supabase-js lưu phiên trong localStorage, tự làm mới token).
  useEffect(() => {
    void supabase.auth.getSession().then(({ data }) => {
      setSession(data.session)
      setInitializing(false)
    })
    const { data } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession)
      setInitializing(false)
    })
    return () => data.subscription.unsubscribe()
  }, [])

  // 2) Tải hồ sơ (vai trò) khi đổi người dùng. Tách khỏi callback ở trên để không gọi Supabase bên trong
  //    onAuthStateChange (có thể gây treo theo khuyến cáo của supabase-js).
  useEffect(() => {
    if (!userId) {
      setProfile(null)
      setProfileError(null)
      setLoadedFor(null)
      return
    }
    let cancelled = false
    void supabase
      .from('profiles')
      .select('*')
      .eq('id', userId)
      .maybeSingle()
      .then(({ data, error }) => {
        if (cancelled) return
        if (error) {
          setProfile(null)
          setProfileError(`Không tải được hồ sơ: ${error.message}`)
        } else if (!data) {
          setProfile(null)
          setProfileError('Không tìm thấy hồ sơ của tài khoản này.')
        } else {
          setProfile(data as Profile)
          setProfileError(null)
        }
        setLoadedFor(userId)
      })
    return () => {
      cancelled = true
    }
  }, [userId, reloadTick])

  const signOut = useCallback(async () => {
    await supabase.auth.signOut()
  }, [])

  // Tải lại hồ sơ âm thầm (không bật loading) để trang đang mở không bị thay bằng vòng xoay.
  const refreshProfile = useCallback(() => {
    setReloadTick((tick) => tick + 1)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      user: session?.user ?? null,
      profile,
      role: profile?.role ?? null,
      loading: initializing || (userId !== null && loadedFor !== userId),
      profileError,
      signOut,
      refreshProfile,
    }),
    [session, profile, initializing, userId, loadedFor, profileError, signOut, refreshProfile],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
