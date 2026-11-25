import { supabase } from '../lib/supabase'
import type { Area, Category, UsageRow } from '../types/catalog'

export interface Catalog {
  areas: Area[]
  categories: Category[]
  usage: UsageRow[]
}

export async function fetchCatalog(): Promise<Catalog> {
  const [areas, categories, usage] = await Promise.all([
    supabase.from('areas').select('id, name, building').order('name'),
    supabase.from('categories').select('id, name').order('name'),
    supabase.from('tickets').select('area_id, category_id'),
  ])
  const failed = areas.error ?? categories.error ?? usage.error
  if (failed) throw new Error(failed.message)
  return {
    areas: (areas.data ?? []) as Area[],
    categories: (categories.data ?? []) as Category[],
    usage: (usage.data ?? []) as UsageRow[],
  }
}

export function translateCatalogError(error: { code?: string; message: string }): string {
  switch (error.code) {
    case '23505':
      return 'Tên này đã tồn tại.'
    case '23503':
      return 'Mục này đang được phiếu sử dụng nên không thể xóa.'
    case '42501':
      return 'Bạn không có quyền thực hiện thao tác này.'
    default:
      return error.message
  }
}

type Table = 'areas' | 'categories'

/** Trả null nếu thành công, ngược lại là thông báo lỗi tiếng Việt. */
async function run(table: Table, op: PromiseLike<{ data: { id: number }[] | null; error: { code?: string; message: string } | null }>): Promise<string | null> {
  const { data, error } = await op
  if (error) return translateCatalogError(error)
  // RLS chặn thì không báo lỗi mà trả 0 dòng.
  if (!data || data.length === 0) return `Không thực hiện được trên bảng ${table} (bị từ chối hoặc không còn tồn tại).`
  return null
}

export function createArea(name: string, building: string) {
  return run('areas', supabase.from('areas').insert({ name, building: building || null }).select('id'))
}

export function updateArea(id: number, name: string, building: string) {
  return run('areas', supabase.from('areas').update({ name, building: building || null }).eq('id', id).select('id'))
}

export function deleteArea(id: number) {
  return run('areas', supabase.from('areas').delete().eq('id', id).select('id'))
}

export function createCategory(name: string) {
  return run('categories', supabase.from('categories').insert({ name }).select('id'))
}

export function updateCategory(id: number, name: string) {
  return run('categories', supabase.from('categories').update({ name }).eq('id', id).select('id'))
}

export function deleteCategory(id: number) {
  return run('categories', supabase.from('categories').delete().eq('id', id).select('id'))
}
