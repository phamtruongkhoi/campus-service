import { supabase } from '../lib/supabase'
import type { RoleChange } from '../types/catalog'
import type { Profile, Role } from '../types/database'

export async function fetchProfiles(): Promise<Profile[]> {
  const { data, error } = await supabase.from('profiles').select('*')
  if (error) throw new Error(error.message)
  return (data ?? []) as Profile[]
}

export async function fetchRoleChanges(limit = 20): Promise<RoleChange[]> {
  const { data, error } = await supabase
    .from('role_changes')
    .select('*')
    .order('created_at', { ascending: false })
    .limit(limit)
  if (error) throw new Error(error.message)
  return (data ?? []) as RoleChange[]
}

/** Trả null nếu thành công; lỗi từ RPC đã là tiếng Việt nên hiện nguyên văn. */
export async function setUserRole(userId: string, newRole: Exclude<Role, 'admin'>, note: string): Promise<string | null> {
  const { error } = await supabase.rpc('set_user_role', {
    p_user_id: userId,
    p_new_role: newRole,
    p_note: note.trim(),
  })
  return error ? error.message : null
}
