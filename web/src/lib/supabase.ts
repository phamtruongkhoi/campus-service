import { createClient } from '@supabase/supabase-js'

const url = import.meta.env.VITE_SUPABASE_URL
const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

/** false khi thiếu web/.env.local: main.tsx sẽ hiện màn hình hướng dẫn thay vì để trang trắng. */
export const isSupabaseConfigured = Boolean(url && anonKey)

// Chỉ dùng khóa công khai (anon/publishable). Quyền truy cập do RLS phía Supabase quyết định.
export const supabase = createClient(
  url || 'http://localhost:54321',
  anonKey || 'missing-anon-key',
)
