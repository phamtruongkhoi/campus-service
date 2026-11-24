import { createContext } from 'react'
import type { Session, User } from '@supabase/supabase-js'
import type { Profile, Role } from '../types/database'

export interface AuthContextValue {
  session: Session | null
  user: User | null
  profile: Profile | null
  role: Role | null
  /** true khi đang khôi phục phiên hoặc đang tải hồ sơ sau khi có phiên. */
  loading: boolean
  /** Lỗi khi tải hồ sơ (đã có phiên nhưng không đọc được profiles). */
  profileError: string | null
  signOut: () => Promise<void>
  refreshProfile: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
