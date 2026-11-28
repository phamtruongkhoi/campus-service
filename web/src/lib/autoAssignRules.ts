// Quy tắc thuần cho màn Tự động phân công (không gọi mạng, dễ viết unit test ở 6.4).

import type { TechnicianCategory, TechnicianSetting } from '../types/autoAssign'

export const ETA_DAY_CHOICES = [
  { value: '', label: 'Không đặt ETA' },
  { value: '1', label: 'Trong 1 ngày' },
  { value: '2', label: 'Trong 2 ngày' },
  { value: '3', label: 'Trong 3 ngày' },
  { value: '7', label: 'Trong 7 ngày' },
] as const

export function etaChoiceToDays(choice: string): number | null {
  return choice === '' ? null : Number(choice)
}

export function etaDaysToChoice(days: number | null): string {
  return days === null ? '' : String(days)
}

/** Chưa có dòng trong technician_settings nghĩa là chưa sẵn sàng (khớp phía SQL). */
export function availabilityMap(rows: readonly TechnicianSetting[]): Map<string, boolean> {
  return new Map(rows.map((r) => [r.technician_id, r.is_available]))
}

export function specialtyMap(rows: readonly TechnicianCategory[]): Map<string, Set<number>> {
  const map = new Map<string, Set<number>>()
  for (const r of rows) {
    const set = map.get(r.technician_id) ?? new Set<number>()
    set.add(r.category_id)
    map.set(r.technician_id, set)
  }
  return map
}

/** Câu tóm tắt cho Admin biết hệ thống sẽ làm gì với cấu hình hiện tại. */
export function summarize(enabled: boolean, availableCount: number, fallbackAny: boolean): string {
  if (!enabled) return 'Đang TẮT: phiếu mới chờ Admin phân công thủ công.'
  if (availableCount === 0) {
    return 'Đang BẬT nhưng chưa có kỹ thuật viên nào sẵn sàng: phiếu mới giữ trạng thái Chờ tiếp nhận.'
  }
  return fallbackAny
    ? `Đang BẬT: ${availableCount} kỹ thuật viên sẵn sàng. Ưu tiên người đúng chuyên môn, không có thì giao người ít việc nhất.`
    : `Đang BẬT: ${availableCount} kỹ thuật viên sẵn sàng. Chỉ giao cho người đúng chuyên môn, không có thì phiếu giữ Chờ tiếp nhận.`
}

/** Thông báo khi bấm lưu / chạy: số phiếu vừa được giao. */
export function assignedMessage(count: number): string {
  return count > 0 ? `Đã giao ${count} phiếu đang chờ.` : 'Không có phiếu chờ nào được giao.'
}

/** Lỗi thiếu hàm/bảng nghĩa là chưa chạy migration 6.A. */
export function translateAutoAssignError(message: string): string {
  if (/schema cache|could not find|does not exist/i.test(message)) {
    return 'Chưa có cấu trúc tự động phân công trong Supabase. Hãy chạy migration_6a_auto_assign.sql.'
  }
  return message
}
