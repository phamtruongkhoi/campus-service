import type { UsageRow } from '../types/catalog'

export const NAME_MIN = 2
export const NAME_MAX = 60
export const BUILDING_MAX = 40
/** Loại lỗi dự phòng của hệ thống, không cho xóa / đổi tên. */
export const PROTECTED_CATEGORY = 'Khác'

export function normalizeName(value: string): string {
  return value.replace(/\s+/g, ' ').trim()
}

function key(value: string | null | undefined): string {
  return normalizeName(value ?? '').toLocaleLowerCase('vi')
}

export function validateCatalogName(value: string): string | null {
  const v = normalizeName(value)
  if (v.length < NAME_MIN) return `Tên cần ít nhất ${NAME_MIN} ký tự.`
  if (v.length > NAME_MAX) return `Tên tối đa ${NAME_MAX} ký tự.`
  return null
}

export function validateBuilding(value: string): string | null {
  return normalizeName(value).length > BUILDING_MAX ? `Tòa nhà tối đa ${BUILDING_MAX} ký tự.` : null
}

export function isProtectedCategory(name: string): boolean {
  return key(name) === key(PROTECTED_CATEGORY)
}

/** Tìm khu vực trùng (cùng tên + tòa nhà, không phân biệt hoa thường). excludeId để bỏ qua chính nó khi sửa. */
export function findDuplicateArea<T extends { id: number; name: string; building: string | null }>(
  list: readonly T[],
  name: string,
  building: string,
  excludeId?: number,
): T | undefined {
  return list.find(
    (a) => a.id !== excludeId && key(a.name) === key(name) && key(a.building) === key(building),
  )
}

export function findDuplicateCategory<T extends { id: number; name: string }>(
  list: readonly T[],
  name: string,
  excludeId?: number,
): T | undefined {
  return list.find((c) => c.id !== excludeId && key(c.name) === key(name))
}

export interface UsageCounts {
  byArea: Map<number, number>
  byCategory: Map<number, number>
}

export function usageCounts(rows: readonly UsageRow[]): UsageCounts {
  const byArea = new Map<number, number>()
  const byCategory = new Map<number, number>()
  for (const r of rows) {
    if (r.area_id !== null) byArea.set(r.area_id, (byArea.get(r.area_id) ?? 0) + 1)
    if (r.category_id !== null) byCategory.set(r.category_id, (byCategory.get(r.category_id) ?? 0) + 1)
  }
  return { byArea, byCategory }
}

/** Danh sách tòa nhà đã có (gợi ý khi nhập), bỏ trùng, sắp theo tiếng Việt. */
export function distinctBuildings(areas: readonly { building: string | null }[]): string[] {
  const seen = new Map<string, string>()
  for (const a of areas) {
    const b = normalizeName(a.building ?? '')
    if (b && !seen.has(key(b))) seen.set(key(b), b)
  }
  return [...seen.values()].sort((x, y) => x.localeCompare(y, 'vi'))
}
