import { supabase } from '../lib/supabase'
import { translateAutoAssignError } from '../lib/autoAssignRules'
import { DEFAULT_AUTO_ASSIGN } from '../types/autoAssign'
import type { AutoAssignSettings, TechnicianCategory, TechnicianSetting } from '../types/autoAssign'

export interface AutoAssignData {
  settings: AutoAssignSettings
  availability: TechnicianSetting[]
  specialties: TechnicianCategory[]
}

/** Admin tải cài đặt, trạng thái sẵn sàng và chuyên môn của mọi kỹ thuật viên. */
export async function fetchAutoAssign(): Promise<AutoAssignData> {
  const [settings, availability, specialties] = await Promise.all([
    supabase.from('auto_assign_settings').select('enabled, eta_days, fallback_any').maybeSingle(),
    supabase.from('technician_settings').select('technician_id, is_available'),
    supabase.from('technician_categories').select('technician_id, category_id'),
  ])
  const failed = settings.error ?? availability.error ?? specialties.error
  if (failed) throw new Error(translateAutoAssignError(failed.message))
  return {
    settings: (settings.data as AutoAssignSettings | null) ?? DEFAULT_AUTO_ASSIGN,
    availability: (availability.data ?? []) as TechnicianSetting[],
    specialties: (specialties.data ?? []) as TechnicianCategory[],
  }
}

export interface RpcResult {
  error: string | null
  /** Số phiếu đang chờ vừa được giao (nếu hàm trả về). */
  assigned: number
}

function toResult(data: unknown, error: { message: string } | null): RpcResult {
  if (error) return { error: translateAutoAssignError(error.message), assigned: 0 }
  return { error: null, assigned: typeof data === 'number' ? data : 0 }
}

export async function saveAutoAssign(settings: AutoAssignSettings): Promise<RpcResult> {
  const { data, error } = await supabase.rpc('set_auto_assign', {
    p_enabled: settings.enabled,
    p_eta_days: settings.eta_days,
    p_fallback: settings.fallback_any,
  })
  return toResult(data, error)
}

export async function runAutoAssignNow(): Promise<RpcResult> {
  const { data, error } = await supabase.rpc('run_auto_assign_now')
  return toResult(data, error)
}

export async function setTechnicianAvailability(userId: string, available: boolean): Promise<RpcResult> {
  const { data, error } = await supabase.rpc('set_technician_availability', {
    p_user_id: userId,
    p_available: available,
  })
  return toResult(data, error)
}

export async function setSpecialty(technicianId: string, categoryId: number, on: boolean): Promise<string | null> {
  const query = on
    ? supabase.from('technician_categories').insert({ technician_id: technicianId, category_id: categoryId })
    : supabase
        .from('technician_categories')
        .delete()
        .eq('technician_id', technicianId)
        .eq('category_id', categoryId)
  const { error } = await query
  return error ? translateAutoAssignError(error.message) : null
}

/** Kỹ thuật viên đọc trạng thái của chính mình; chưa có dòng = chưa sẵn sàng. */
export async function fetchMyAvailability(userId: string): Promise<boolean> {
  const { data, error } = await supabase
    .from('technician_settings')
    .select('is_available')
    .eq('technician_id', userId)
    .maybeSingle()
  if (error) throw new Error(translateAutoAssignError(error.message))
  return Boolean(data?.is_available)
}

export async function setMyAvailability(available: boolean): Promise<RpcResult> {
  const { data, error } = await supabase.rpc('set_my_availability', { p_available: available })
  return toResult(data, error)
}
