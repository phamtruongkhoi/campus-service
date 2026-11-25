// Quy tắc điều phối của Admin (khớp AdminDispatchViewModel.save phía Android).

import type { TicketStatus } from '../types/database'

export type EtaChoiceId = 'keep' | '1' | '2' | '3' | '7' | 'clear'

export interface EtaChoice {
  id: EtaChoiceId
  label: string
}

/** ETA chọn theo mốc thay vì chọn ngày (quyết định từ Android, 2.6). */
export const ETA_CHOICES: readonly EtaChoice[] = [
  { id: 'keep', label: 'Giữ nguyên' },
  { id: '1', label: 'Trong 1 ngày' },
  { id: '2', label: 'Trong 2 ngày' },
  { id: '3', label: 'Trong 3 ngày' },
  { id: '7', label: 'Trong 7 ngày' },
  { id: 'clear', label: 'Xóa ETA' },
]

const DAY_MS = 86_400_000

/**
 * Tự đổi trạng thái theo việc phân công:
 * có kỹ thuật viên và đang Open -> Assigned; bỏ kỹ thuật viên và đang Assigned -> Open.
 * Các trạng thái khác giữ nguyên.
 */
export function nextStatusOnAssign(current: TicketStatus, technicianId: string | null): TicketStatus {
  if (technicianId !== null && current === 'open') return 'assigned'
  if (technicianId === null && current === 'assigned') return 'open'
  return current
}

/** ETA mới (chuỗi ISO) theo lựa chọn; "keep" giữ giá trị cũ, "clear" xóa. */
export function resolveEta(choice: EtaChoiceId, currentEta: string | null, now: number): string | null {
  if (choice === 'keep') return currentEta
  if (choice === 'clear') return null
  return new Date(now + Number(choice) * DAY_MS).toISOString()
}
